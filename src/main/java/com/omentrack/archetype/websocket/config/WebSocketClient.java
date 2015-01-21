package com.omentrack.archetype.websocket.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.springframework.web.socket.WebSocketSession;

public class WebSocketClient {

	private HttpSession httpSession;
	private Set<String> subscriptions = Collections.synchronizedSet( new HashSet<String>( ) );
	private WebSocketSession webSocketSession;
	
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
	
}
