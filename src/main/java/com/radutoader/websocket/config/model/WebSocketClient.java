package com.radutoader.websocket.config.model;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.servlet.http.HttpSession;

import org.apache.catalina.session.StandardSession;
import org.apache.catalina.session.StandardSessionFacade;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.web.socket.WebSocketSession;

public class WebSocketClient {
	
	private static final Logger logger = LogManager.getLogger( WebSocketClient.class );
	
	private HttpSession httpSession;
	private Set<String> subscriptions = new ConcurrentSkipListSet<String>( );
	private WebSocketSession webSocketSession;
	private StandardSession realSession;
	private volatile long lastHeartbeatTime = System.currentTimeMillis( );
	
	public WebSocketClient( WebSocketSession session, HttpSession httpSession ) {
		
		webSocketSession = session;
		this.httpSession = httpSession;
		StandardSessionFacade facadeSession = (StandardSessionFacade) httpSession;
		try {
			realSession = (StandardSession) FieldUtils.readField( facadeSession, "session", true );
		} catch ( IllegalAccessException e ) {
			logger.warn( e.getMessage( ), e );
		}
	}
	
	public HttpSession getHttpSession() {
		
		return httpSession;
	}
	
	public Set<String> getSubscriptions() {
		
		return subscriptions;
	}
	
	public WebSocketSession getWebSocketSession() {
		
		return webSocketSession;
	}
	
	public StandardSession getRealSession() {
		
		return realSession;
	}
	
	public void touchHeartBeatTime() {
		
		lastHeartbeatTime = System.currentTimeMillis( );
	}
	
	public long getLastHeartbeatTime() {
		
		return lastHeartbeatTime;
	}
}
