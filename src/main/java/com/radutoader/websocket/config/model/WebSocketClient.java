package com.radutoader.websocket.config.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.socket.WebSocketSession;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.concurrent.*;

public class WebSocketClient {
	
	private static final Logger logger = LogManager.getLogger( WebSocketClient.class );
	
	private HttpSession httpSession;
	private Set<String> subscriptions = new ConcurrentSkipListSet<String>( );
	private WebSocketSession webSocketSession;
	private volatile long lastHeartbeatTime = System.currentTimeMillis( );
	
	public WebSocketClient( WebSocketSession session, HttpSession httpSession ) {
		
		webSocketSession = session;
		this.httpSession = httpSession;
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
	
	public void touchHeartBeatTime() {
		
		lastHeartbeatTime = System.currentTimeMillis( );
	}
	
	public long getLastHeartbeatTime() {
		
		return lastHeartbeatTime;
	}
}
