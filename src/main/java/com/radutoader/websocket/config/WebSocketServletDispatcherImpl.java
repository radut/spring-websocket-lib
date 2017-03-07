package com.radutoader.websocket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.radutoader.websocket.config.annotation.WebSocketController;
import com.radutoader.websocket.config.annotation.WebSocketGet;
import com.radutoader.websocket.config.annotation.WebSocketSubscribe;
import com.radutoader.websocket.config.annotation.WebSocketUnSubscribe;
import com.radutoader.websocket.config.model.*;
import org.apache.catalina.session.StandardSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;

/**
 * @author Radu Toader
 */
public class WebSocketServletDispatcherImpl extends TextWebSocketHandler implements WebSocketServletDispatcher {

    private static final Logger logger = LogManager.getLogger(WebSocketServletDispatcherImpl.class);

    @Autowired
    private ApplicationContext ctx;

    private final Map<String, WebSocketClient> sessions = new ConcurrentHashMap<String, WebSocketClient>();

    private ObjectMapper objectMapper;

    @Autowired
    public void setJacksonConverter(MappingJackson2HttpMessageConverter jacksonConverter) {

        objectMapper = jacksonConverter.getObjectMapper();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

        removeClient(session);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        HttpSession httpSession = (HttpSession) session.getAttributes().get("httpSession");
        WebSocketClient webSocketClient = new WebSocketClient(session, httpSession);
        sessions.put(session.getId(), webSocketClient);
    }

    private MessageStatus getMessageStatus(Future<MessageStatus> future) {

        try {
            return future.get();
        } catch (Exception e) {
            return MessageStatus.buildErrorResult(e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

        removeClient(session);
    }

    @Override
    @Async
    public Future<Map<WebSocketClient, MessageStatus>> sendMessageToAllHttpSession(String jSessionId, String subscription, Object message) {

        List<WebSocketClient> webSocketClientsWithSubscription = new ArrayList<WebSocketClient>();
        for (WebSocketClient webSocketClient : sessions.values()) {
            if (webSocketClient.getHttpSession().getId().equals(jSessionId)) {
                if (webSocketClient.getSubscriptions().contains(subscription)) {
                    webSocketClientsWithSubscription.add(webSocketClient);
                }
            }
        }
        return sendMessageToClients(webSocketClientsWithSubscription, subscription, message);
    }

    @Override
    @Async
    public Future<Map<WebSocketClient, MessageStatus>> sendMessageToAllSubscribers(String subscription, Object message) {

        List<WebSocketClient> webSocketClientsWithSubscription = new ArrayList<WebSocketClient>();
        for (WebSocketClient webSocketClient : sessions.values()) {
            if (webSocketClient.getSubscriptions().contains(subscription)) {
                webSocketClientsWithSubscription.add(webSocketClient);
            }
        }

        return sendMessageToClients(webSocketClientsWithSubscription, subscription, message);

    }

    @Override
    @Async
    public Future<Map<WebSocketClient, MessageStatus>> sendMessageToClients(List<WebSocketClient> clients, String subscription, Object message) {

        Map<WebSocketClient, Future<MessageStatus>> tempFutureMap = new HashMap<>();
        for (WebSocketClient wsClient : clients) {
            WebSocketResponse webSocketResponse = buildWebSocketResponseMessage(subscription, message);
            tempFutureMap.put(wsClient, sendMessageInternal(wsClient, webSocketResponse));
        }
        Map<WebSocketClient, MessageStatus> resultMap = new HashMap<>();
        for (Entry<WebSocketClient, Future<MessageStatus>> entry : tempFutureMap.entrySet()) {
            WebSocketClient key = entry.getKey();
            Future<MessageStatus> value = entry.getValue();
            resultMap.put(key, getMessageStatus(value));
        }
        return new AsyncResult<Map<WebSocketClient, MessageStatus>>(resultMap);
    }

    @Override
    @Async
    public Future<MessageStatus> sendMessageToWebSocketClient(String webSocketSessionId, String subscription, Object message) {

        WebSocketClient webSocketClient = sessions.get(webSocketSessionId);
        MessageStatus result = null;
        if (webSocketClient != null && webSocketClient.getSubscriptions().contains(subscription)) {
            WebSocketResponse webSocketResponse = buildWebSocketResponseMessage(subscription, message);
            result = getMessageStatus(sendMessageInternal(webSocketClient, webSocketResponse));
        } else {
            result = MessageStatus.buildNotSubscribed();
        }

        return new AsyncResult<MessageStatus>(result);

    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {

        super.handlePongMessage(session, message);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        WebSocketClient webSocketClient = getWebSocketClient(session);

        if (message.getPayloadLength() == 1) {
            if (handleClientHeartBeat(webSocketClient, message)) {
                return;
            }
        }

        WebSocketResponse response = new WebSocketResponse();
        response.setUrl("error");
        response.setResultOk(false);
        if (webSocketClient == null) {
            response.setErrorMessage("client session not found");
        } else {
            try {
                WebSocketMessage webSocketMessage = objectMapper.readValue(message.getPayload(), WebSocketMessage.class);
                response.setType(webSocketMessage.getType());
                response.setUrl(webSocketMessage.getUrl());
                // synchronized ( session ) { // we must sync on session, because a thread could send a message to one of the user subscription, while this user sends, unsubscribe to alter that subscriptions list;
                Object methodResultValue = handleWebSocketMessage(webSocketClient, objectMapper, webSocketMessage);
                response.setResultOk(true);
                response.setResult(methodResultValue);
                // }
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
                response.setErrorMessage(e.getMessage());
            }
        }

        sendMessageInternal(webSocketClient, response);
    }

    private boolean handleClientHeartBeat(WebSocketClient webSocketClient, TextMessage message) {

        if ("p".equals(message.getPayload())) {
            touchHttpSession(webSocketClient);
            webSocketClient.touchHeartBeatTime();
            return true;
        }
        return false;
    }

    private WebSocketMessage buildUnSubscribeMessageFor(String subscription, WebSocketClient webSocketClient) {

        WebSocketMessage message = new WebSocketMessage();
        message.setType(WebSocketMessageType.UNSUBSCRIBE);
        message.setUrl(subscription);
        return message;
    }

    private WebSocketResponse buildWebSocketResponseMessage(String subscription, Object message) {

        WebSocketResponse wsResponse = new WebSocketResponse();
        wsResponse.setResultOk(true);
        wsResponse.setType(WebSocketMessageType.UPDATE);
        wsResponse.setUrl(subscription);
        wsResponse.setResult(message);
        return wsResponse;
    }

    private String combineAndCleanUrl(String wsControllerUrl, String wsMethodUrl) {

        return (wsControllerUrl + "/" + wsMethodUrl).replaceAll("//", "/");
    }

    @Override
    public WebSocketClient getWebSocketClient(WebSocketSession session) {

        return sessions.get(session.getId());
    }

    private WebSocketInvocableHandlerMethod getWebSocketInvocableHandlerMethod(WebSocketMessage webSocketMessage) {

        Map<String, Object> wsControllers = ctx.getBeansWithAnnotation(WebSocketController.class);

        WebSocketMessageType type = webSocketMessage.getType();

        for (Entry<String, Object> entry : wsControllers.entrySet()) {
            Object controller = entry.getValue();
            Class<?> realController = AopUtils.getTargetClass(controller);
            RequestMapping controllerMapping = AnnotationUtils.findAnnotation(controller.getClass(), RequestMapping.class);
            if (controllerMapping == null)
                continue;
            String[] controllerMappings = controllerMapping.value();
            for (String controllerUrl : controllerMappings) {
                if (webSocketMessage.getUrl().startsWith(controllerUrl)) {
                    Method[] methods = realController.getMethods();
                    for (Method method : methods) {
                        method = AopUtils.getMostSpecificMethod(method, realController);
                        switch (type) {
                            case GET:
                                WebSocketGet get = AnnotationUtils.findAnnotation(method, WebSocketGet.class);
                                if (get != null) {
                                    if (webSocketMessage.getUrl().equals(combineAndCleanUrl(controllerUrl, get.value()))) {
                                        return new WebSocketInvocableHandlerMethod(controller, method);
                                    }
                                }
                                break;
                            case SUBSCRIBE:
                                WebSocketSubscribe subscribe = AnnotationUtils.findAnnotation(method, WebSocketSubscribe.class);
                                if (subscribe != null) {
                                    if (webSocketMessage.getUrl().equals(combineAndCleanUrl(controllerUrl, subscribe.value()))) {
                                        return new WebSocketInvocableHandlerMethod(controller, method);
                                    }
                                }
                                break;
                            case UNSUBSCRIBE:
                                WebSocketUnSubscribe unSubscribe = AnnotationUtils.findAnnotation(method, WebSocketUnSubscribe.class);
                                if (unSubscribe != null) {
                                    if (webSocketMessage.getUrl().equals(combineAndCleanUrl(controllerUrl, unSubscribe.value()))) {
                                        return new WebSocketInvocableHandlerMethod(controller, method);
                                    }
                                }
                                break;
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("No Mapping Found for '" + webSocketMessage.getUrl() + "'");
    }

    private Object handleWebSocketMessage(WebSocketClient webSocketClient, ObjectMapper objectMapper, WebSocketMessage webSocketMessage) throws Exception {

        touchHttpSession(webSocketClient);

        // synchronized ( webSocketClient.getWebSocketSession( ) ) {
        WebSocketInvocableHandlerMethod wsihm = getWebSocketInvocableHandlerMethod(webSocketMessage);
        Object returnValue = wsihm.invokeWithArguments(objectMapper, webSocketMessage.getData(),//
                new WebSocketSessionWrapper(webSocketClient.getWebSocketSession(), this), webSocketClient.getHttpSession());

        switch (webSocketMessage.getType()) {
            case SUBSCRIBE:
                webSocketClient.getSubscriptions().add(webSocketMessage.getUrl());
                break;
            case UNSUBSCRIBE:
                webSocketClient.getSubscriptions().remove(webSocketMessage.getUrl());
                break;
            case GET: // do nothing
            default: // do nothing
                break;
        }
        return returnValue;
        // }

    }

    private void removeClient(WebSocketSession session) {

        WebSocketClient webSocketClient = sessions.remove(session.getId());
        if (webSocketClient != null) {
            Set<String> subscriptions = webSocketClient.getSubscriptions();
            for (String subscription : subscriptions) {
                try {
                    WebSocketMessage webSocketMessage = buildUnSubscribeMessageFor(subscription, webSocketClient);
                    handleWebSocketMessage(webSocketClient, objectMapper, webSocketMessage);
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
    }

    @Async
    private Future<MessageStatus> sendMessageInternal(WebSocketClient webSocketClient, WebSocketResponse message) {

        MessageStatus result = null;
        WebSocketSession webSocketSession = webSocketClient.getWebSocketSession();
        touchHttpSession(webSocketClient);
        String messageAsString;
        try {
            messageAsString = objectMapper.writeValueAsString(message);
            int retry = 4;
            Exception exp = null;
            boolean sent = false;
            while (retry-- > 0) {
                try {
                    // synchronized ( webSocketSession ) {
                    if (webSocketSession.isOpen()) {
                        webSocketSession.sendMessage(new TextMessage(messageAsString));
                        sent = true;
                    }
                    // }
                } catch (Exception e) {
                    exp = e;
                }
                if (sent) {
                    break;
                }
            }
            if (sent) {
                if (logger.isDebugEnabled())
                    logger.debug("sent msg : " + webSocketSession.getId() + " retriesLeft:" + retry + " msgLength = " + messageAsString.length() + " =  " + messageAsString);
                result = MessageStatus.buildOkResult();
            } else {
                if (retry > 0) {
                    logError(webSocketClient, webSocketSession, exp);
                    result = MessageStatus.buildErrorResult(exp);
                } else {
                    result = MessageStatus.buildMaxRetryReached();
                }
            }
        } catch (Exception e) {
            logError(webSocketClient, webSocketSession, e);
            result = MessageStatus.buildErrorResult(e);
        }
        return new AsyncResult<MessageStatus>(result);
    }

    public void logError(WebSocketClient webSocketClient, WebSocketSession webSocketSession, Exception exp) {

        if (logger.isDebugEnabled())
            logger.warn("could not send message to client : jSessionId= " + webSocketClient.getHttpSession().getId() + " webSocketSessionId=" + webSocketSession.getId(), exp);
        logger.warn("could not send message to client : jSessionId= " + webSocketClient.getHttpSession().getId() + " webSocketSessionId=" + webSocketSession.getId() + " : " + exp.getMessage());
    }

    public void touchHttpSession(WebSocketClient webSocketClient) {

        StandardSession session = webSocketClient.getRealSession();
        if (session != null) {
            session.access();
            session.endAccess();
        }

    }

    @Override
    public void close(WebSocketSession session) {

        WebSocketClient webSocketClient = sessions.get(session.getId());
        try {
            removeClient(session);
            if (webSocketClient != null)
                webSocketClient.getWebSocketSession().close();
        } catch (IOException e) {
            logger.info(e.getMessage(), e);
        }
    }

    @Override
    public void close(WebSocketSession session, CloseStatus status) {

        WebSocketClient webSocketClient = sessions.get(session.getId());
        try {
            removeClient(session);
            if (webSocketClient != null)
                webSocketClient.getWebSocketSession().close(status);
        } catch (IOException e) {
            logger.info(e.getMessage(), e);
        }

    }

}
