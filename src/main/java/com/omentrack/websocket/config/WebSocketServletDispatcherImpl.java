package com.omentrack.websocket.config;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import annotation.WebSocketController;
import annotation.WebSocketGet;
import annotation.WebSocketSubscribe;
import annotation.WebSocketUnSubscribe;

/**
 *
 * @author Radu Toader
 */
@Component( value = "webSocketServletDispatcher" )
public class WebSocketServletDispatcherImpl extends TextWebSocketHandler implements WebSocketServletDispatcher {
	
	private static final Logger logger = Logger.getLogger( WebSocketServletDispatcherImpl.class );
	
	@Autowired
	private ApplicationContext ctx;
	
	@Autowired
	private MappingJackson2HttpMessageConverter jacksonConverter;
	private final Map<String, WebSocketClient> sessions = new ConcurrentHashMap<String, WebSocketClient>( );
	
	@Override
	public void afterConnectionClosed( WebSocketSession session, CloseStatus status ) throws Exception {
		
		removeClient( session );
	}
	
	@Override
	public void afterConnectionEstablished( WebSocketSession session ) throws Exception {
		
		HttpSession httpSession = (HttpSession) session.getAttributes( ).get( "httpSession" );
		WebSocketClient webSocketClient = new WebSocketClient( session, httpSession );
		sessions.put( session.getId( ), webSocketClient );
	}
	
	public MessageStatus getMessageStatus( Future<MessageStatus> future ) {
		
		try {
			return future.get( );
		} catch ( Exception e ) {
			return new MessageStatus( e );
		}
	}
	
	@Override
	public void handleTransportError( WebSocketSession session, Throwable exception ) throws Exception {
		
		removeClient( session );
	}
	
	@Override
	@Async
	public Future<Map<WebSocketClient, MessageStatus>> sendMessageToAllHttpSession( String jSessionId, String subscription, Object message ) {
		
		Logger.getLogger( WebSocketServletDispatcherImpl.class ).error( " (clients, subscription, message) " );
		List<WebSocketClient> webSocketClientsWithSubscription = new ArrayList<WebSocketClient>( );
		for ( WebSocketClient webSocketClient : sessions.values( ) ) {
			if ( webSocketClient.getHttpSession( ).getId( ).equals( jSessionId ) ) {
				if ( webSocketClient.getSubscriptions( ).contains( subscription ) ) {
					webSocketClientsWithSubscription.add( webSocketClient );
				}
			}
		}
		return sendMessageToClients( webSocketClientsWithSubscription, subscription, message );
	}
	
	@Override
	@Async
	public Future<Map<WebSocketClient, MessageStatus>> sendMessageToAllSubscribers( String subscription, Object message ) {
		
		Logger.getLogger( WebSocketServletDispatcherImpl.class ).error( " (clients, subscription, message) " );
		List<WebSocketClient> webSocketClientsWithSubscription = new ArrayList<WebSocketClient>( );
		for ( WebSocketClient webSocketClient : sessions.values( ) ) {
			if ( webSocketClient.getSubscriptions( ).contains( subscription ) ) {
				webSocketClientsWithSubscription.add( webSocketClient );
			}
		}
		
		return sendMessageToClients( webSocketClientsWithSubscription, subscription, message );
		
	}
	
	@Override
	@Async
	public Future<Map<WebSocketClient, MessageStatus>> sendMessageToClients( List<WebSocketClient> clients, String subscription, Object message ) {
		
		Logger.getLogger( WebSocketServletDispatcherImpl.class ).error( " (clients, subscription, message) " );
		Map<WebSocketClient, Future<MessageStatus>> tempFutureMap = new HashMap<>( );
		for ( WebSocketClient wsClient : clients ) {
			WebSocketResponse webSocketResponse = buildWebSocketResponseMessage( subscription, message );
			tempFutureMap.put( wsClient, sendMessageInternal( wsClient, webSocketResponse ) );
		}
		Map<WebSocketClient, MessageStatus> resultMap = new HashMap<>( );
		for ( Entry<WebSocketClient, Future<MessageStatus>> entry : tempFutureMap.entrySet( ) ) {
			WebSocketClient key = entry.getKey( );
			Future<MessageStatus> value = entry.getValue( );
			resultMap.put( key, getMessageStatus( value ) );
		}
		return new AsyncResult<Map<WebSocketClient, MessageStatus>>( resultMap );
	}
	
	@Override
	@Async
	public Future<MessageStatus> sendMessageToWebSocketClient( String webSocketSessionId, String subscription, Object message ) {
		
		Logger.getLogger( WebSocketServletDispatcherImpl.class ).error( " (clients, subscription, message) " );
		WebSocketClient webSocketClient = sessions.get( webSocketSessionId );
		MessageStatus result = null;
		if ( webSocketClient != null && webSocketClient.getSubscriptions( ).contains( subscription ) ) {
			WebSocketResponse webSocketResponse = buildWebSocketResponseMessage( subscription, message );
			result = getMessageStatus( sendMessageInternal( webSocketClient, webSocketResponse ) );
		} else {
			result = new MessageStatus( false, true );
		}
		
		return new AsyncResult<MessageStatus>( result );
		
	}
	
	@Override
	protected void handlePongMessage( WebSocketSession session, PongMessage message ) throws Exception {
		
		super.handlePongMessage( session, message );
	}
	
	@Override
	protected void handleTextMessage( WebSocketSession session, TextMessage message ) throws Exception {
		
		ObjectMapper objectMapper = jacksonConverter.getObjectMapper( );
		WebSocketClient webSocketClient = getWebSocketClient( session );
		
		WebSocketResponse response = new WebSocketResponse( );
		response.setUrl( "error" );
		response.setResultOk( false );
		if ( webSocketClient == null ) {
			response.setErrorMessage( "client session not found" );
		} else {
			try {
				WebSocketMessage webSocketMessage = objectMapper.readValue( message.getPayload( ), WebSocketMessage.class );
				response.setType( webSocketMessage.getType( ) );
				response.setUrl( webSocketMessage.getUrl( ) );
				synchronized ( session ) { // we must sync on session, because a thread could send a message to one of the user subscription, while this user sends, unsubscribe to alter that subscriptions list;
					Object methodResultValue = handleWebSocketMessage( webSocketClient, objectMapper, webSocketMessage );
					response.setResultOk( true );
					response.setResult( methodResultValue );
				}
			} catch ( Exception e ) {
				logger.warn( e.getMessage( ), e );
				response.setErrorMessage( e.getMessage( ) );
			}
		}
		
		sendMessageInternal( webSocketClient, response );
	}
	
	private WebSocketMessage buildUnSubscribeMessageFor( String subscription, WebSocketClient webSocketClient ) {
		
		WebSocketMessage message = new WebSocketMessage( );
		message.setType( WebSocketMessageType.UNSUBSCRIBE );
		message.setUrl( subscription );
		return message;
	}
	
	private WebSocketResponse buildWebSocketResponseMessage( String subscription, Object message ) {
		
		WebSocketResponse wsResponse = new WebSocketResponse( );
		wsResponse.setResultOk( true );
		wsResponse.setType( WebSocketMessageType.UPDATE );
		wsResponse.setUrl( subscription );
		wsResponse.setResult( message );
		return wsResponse;
	}
	
	private String combineAndCleanUrl( String wsControllerUrl, String wsMethodUrl ) {
		
		return ( wsControllerUrl + "/"
						 + wsMethodUrl ).replaceAll( "//", "/" );
	}
	
	private WebSocketClient getWebSocketClient( WebSocketSession session ) {
		
		return sessions.get( session.getId( ) );
	}
	
	private WebSocketInvocableHandlerMethod getWebSocketInvocableHandlerMethod( WebSocketMessage webSocketMessage ) {
		
		Map<String, Object> wsControllers = ctx.getBeansWithAnnotation( WebSocketController.class );
		
		WebSocketMessageType type = webSocketMessage.getType( );
		
		for ( Entry<String, Object> entry : wsControllers.entrySet( ) ) {
			Object controller = entry.getValue( );
			Class<?> realController = AopUtils.getTargetClass( controller );
			RequestMapping controllerMapping = AnnotationUtils.findAnnotation( controller.getClass( ), RequestMapping.class );
			if ( controllerMapping == null )
				continue;
			String[] controllerMappings = controllerMapping.value( );
			for ( String controllerUrl : controllerMappings ) {
				if ( webSocketMessage.getUrl( ).startsWith( controllerUrl ) ) {
					Method[] methods = realController.getMethods( );
					for ( Method method : methods ) {
						method = AopUtils.getMostSpecificMethod( method, realController );
						switch ( type ) {
							case GET:
								WebSocketGet get = AnnotationUtils.findAnnotation( method, WebSocketGet.class );
								if ( get != null ) {
									if ( webSocketMessage.getUrl( ).equals( combineAndCleanUrl( controllerUrl, get.value( ) ) ) ) {
										return new WebSocketInvocableHandlerMethod( controller, method );
									}
								}
								break;
							case SUBSCRIBE:
								WebSocketSubscribe subscribe = AnnotationUtils.findAnnotation( method, WebSocketSubscribe.class );
								if ( subscribe != null ) {
									if ( webSocketMessage.getUrl( ).equals( combineAndCleanUrl( controllerUrl, subscribe.value( ) ) ) ) {
										return new WebSocketInvocableHandlerMethod( controller, method );
									}
								}
								break;
							case UNSUBSCRIBE:
								WebSocketUnSubscribe unSubscribe = AnnotationUtils.findAnnotation( method, WebSocketUnSubscribe.class );
								if ( unSubscribe != null ) {
									if ( webSocketMessage.getUrl( ).equals( combineAndCleanUrl( controllerUrl, unSubscribe.value( ) ) ) ) {
										return new WebSocketInvocableHandlerMethod( controller, method );
									}
								}
								break;
						}
					}
				}
			}
		}
		throw new IllegalStateException( "No Mapping Found for '" + webSocketMessage.getUrl( )
																		 + "'" );
	}
	
	private Object handleWebSocketMessage( WebSocketClient webSocketClient, ObjectMapper objectMapper, WebSocketMessage webSocketMessage ) throws Exception {
		
		synchronized ( webSocketClient.getWebSocketSession( ) ) {
			WebSocketInvocableHandlerMethod wsihm = getWebSocketInvocableHandlerMethod( webSocketMessage );
			Object returnValue = wsihm.invokeWithArguments( objectMapper, webSocketMessage.getData( ),//
					new ReadOnlyWebSocketSession( webSocketClient.getWebSocketSession( ) ), webSocketClient.getHttpSession( ) );
					
			switch ( webSocketMessage.getType( ) ) {
				case SUBSCRIBE:
					webSocketClient.getSubscriptions( ).add( webSocketMessage.getUrl( ) );
					break;
				case UNSUBSCRIBE:
					webSocketClient.getSubscriptions( ).remove( webSocketMessage.getUrl( ) );
					break;
				case GET: // do nothing
				default: // do nothing
					break;
			}
			return returnValue;
		}
		
	}
	
	private void removeClient( WebSocketSession session ) {
		
		WebSocketClient webSocketClient = sessions.remove( session.getId( ) );
		if ( webSocketClient != null ) {
			Set<String> subscriptions = webSocketClient.getSubscriptions( );
			for ( String subscription : subscriptions ) {
				try {
					WebSocketMessage webSocketMessage = buildUnSubscribeMessageFor( subscription, webSocketClient );
					handleWebSocketMessage( webSocketClient, jacksonConverter.getObjectMapper( ), webSocketMessage );
				} catch ( Exception e ) {
					logger.warn( e.getMessage( ), e );
				}
			}
		}
	}
	
	@Async
	private Future<MessageStatus> sendMessageInternal( WebSocketClient client, WebSocketResponse message ) {
		
		Logger.getLogger( WebSocketServletDispatcherImpl.class ).error( " (client, message) " );
		MessageStatus result = null;
		WebSocketSession webSocketSession = client.getWebSocketSession( );
		try {
			String messageAsString = jacksonConverter.getObjectMapper( ).writeValueAsString( message );
			synchronized ( webSocketSession ) {
				webSocketSession.sendMessage( new TextMessage( messageAsString ) );
			}
			logger.debug( "send msg : " + webSocketSession.getId( )
										+ "   =  "
										+ messageAsString );
			result = new MessageStatus( );
		} catch ( Exception e ) {
			logger.warn( "could not send message to client : jSessionId= " + client.getHttpSession( ).getId( )
									 + " webSocketSessionId="
									 + webSocketSession.getId( ),
					e );
			result = new MessageStatus( e );
		}
		return new AsyncResult<MessageStatus>( result );
	}
	
}
