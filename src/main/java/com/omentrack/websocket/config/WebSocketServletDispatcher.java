package com.omentrack.websocket.config;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omentrack.websocket.config.annotation.WebSocketController;
import com.omentrack.websocket.config.annotation.WebSocketGet;
import com.omentrack.websocket.config.annotation.WebSocketSubscribe;
import com.omentrack.websocket.config.annotation.WebSocketUnSubscribe;

/**
 * 
 * @author Radu Toader
 *         do not add public method with ASYNC here
 */
public class WebSocketServletDispatcher extends TextWebSocketHandler {
	
	@Autowired
	private ApplicationContext ctx;
	
	@Autowired
	private MappingJackson2HttpMessageConverter jacksonConverter;
	
	private final Map<String, WebSocketClient> sessions = new ConcurrentHashMap<String, WebSocketClient>( );
	private static final Logger logger = Logger.getLogger( WebSocketServletDispatcher.class );
	
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
	
	@Override
	public void handleTransportError( WebSocketSession session, Throwable exception ) throws Exception {
		
		removeClient( session );
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
	
	@Async
	private void sendMessageInternal( WebSocketClient client, WebSocketResponse message ) {
		
		WebSocketSession webSocketSession = client.getWebSocketSession( );
		try {
			String messageAsString = jacksonConverter.getObjectMapper( ).writeValueAsString( message );
			synchronized ( webSocketSession ) {
				webSocketSession.sendMessage( new TextMessage( messageAsString ) );
			}
			logger.debug( "send msg : " + webSocketSession.getId( )
										+ "   =  "
										+ messageAsString );
		} catch ( IOException e ) {
			logger.warn( "could not send message to client : jSessionId= " + client.getHttpSession( ).getId( )
									 + " webSocketSessionId="
									 + webSocketSession.getId( ),
					e );
		}
	}
	
	private WebSocketMessage buildUnSubscribeMessageFor( String subscription, WebSocketClient webSocketClient ) {
		
		WebSocketMessage message = new WebSocketMessage( );
		message.setType( WebSocketMessageType.UNSUBSCRIBE );
		message.setUrl( subscription );
		return message;
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
			RequestMapping controllerMapping = AnnotationUtils.findAnnotation( controller.getClass( ), RequestMapping.class );
			if ( controllerMapping == null )
				continue;
			String[] controllerMappings = controllerMapping.value( );
			for ( String controllerUrl : controllerMappings ) {
				if ( webSocketMessage.getUrl( ).startsWith( controllerUrl ) ) {
					Method[] methods = controller.getClass( ).getMethods( );
					for ( Method method : methods ) {
						switch ( type ) {
							case GET:
								if ( method.isAnnotationPresent( WebSocketGet.class ) ) {
									WebSocketGet get = AnnotationUtils.findAnnotation( method, WebSocketGet.class );
									if ( webSocketMessage.getUrl( ).equals( ( combineAndCleanUrl( controllerUrl, get.value( ) ) ) ) ) {
										return new WebSocketInvocableHandlerMethod( controller, method );
									}
								}
								break;
							case SUBSCRIBE:
								if ( method.isAnnotationPresent( WebSocketSubscribe.class ) ) {
									WebSocketSubscribe subscribe = AnnotationUtils.findAnnotation( method, WebSocketSubscribe.class );
									if ( webSocketMessage.getUrl( ).equals( ( combineAndCleanUrl( controllerUrl, subscribe.value( ) ) ) ) ) {
										return new WebSocketInvocableHandlerMethod( controller, method );
									}
								}
								break;
							case UNSUBSCRIBE:
								if ( method.isAnnotationPresent( WebSocketUnSubscribe.class ) ) {
									WebSocketUnSubscribe unSubscribe = AnnotationUtils.findAnnotation( method, WebSocketUnSubscribe.class );
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
	
	public boolean sendMessageToAllSubscribers( String subscription, Object message ) {
		
		List<WebSocketClient> webSocketClientsWithSubscription = new ArrayList<WebSocketClient>( );
		for ( WebSocketClient webSocketClient : sessions.values( ) ) {
			if ( webSocketClient.getSubscriptions( ).contains( subscription ) ) {
				webSocketClientsWithSubscription.add( webSocketClient );
			}
		}
		
		sendMessageToClients( webSocketClientsWithSubscription, subscription, message );
		return !webSocketClientsWithSubscription.isEmpty( );
	}
	
	public boolean sendMessageToWebSocketClient( String webSocketSessionId, String subscription, Object message ) {
		
		WebSocketClient webSocketClient = sessions.get( webSocketSessionId );
		if ( webSocketClient != null && webSocketClient.getSubscriptions( ).contains( subscription ) ) {
			sendMessageToClients( Collections.singletonList( webSocketClient ), subscription, message );
			return true;
		}
		return false;
		
	}
	
	public boolean sendMessageToAllHttpSession( String jSessionId, String subscription, Object message ) {
		
		List<WebSocketClient> webSocketClientsWithSubscription = new ArrayList<WebSocketClient>( );
		for ( WebSocketClient webSocketClient : sessions.values( ) ) {
			if ( webSocketClient.getHttpSession( ).getId( ).equals( jSessionId ) ) {
				if ( webSocketClient.getSubscriptions( ).contains( subscription ) ) {
					webSocketClientsWithSubscription.add( webSocketClient );
				}
			}
		}
		sendMessageToClients( webSocketClientsWithSubscription, subscription, message );
		return !webSocketClientsWithSubscription.isEmpty( );
	}
	
	@Async
	private void sendMessageToClients( List<WebSocketClient> clients, String subscription, Object message ) {
		
		for ( WebSocketClient wsClient : clients ) {
			WebSocketResponse webSocketResponse = buildWebSocketResponseMessage( subscription, message );
			sendMessageInternal( wsClient, webSocketResponse );
		}
	}
	
	private WebSocketResponse buildWebSocketResponseMessage( String subscription, Object message ) {
		
		WebSocketResponse wsResponse = new WebSocketResponse( );
		wsResponse.setResultOk( true );
		wsResponse.setType( WebSocketMessageType.UPDATE );
		wsResponse.setUrl( subscription );
		wsResponse.setResult( message );
		return wsResponse;
	}
}
