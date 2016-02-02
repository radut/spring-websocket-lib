package com.omentrack.websocket.config.model;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import com.omentrack.websocket.config.WebSocketServletDispatcher;

public class WebSocketSessionWrapper implements WebSocketSession {
	
	private WebSocketSession session;
	private WebSocketServletDispatcher webSocketServletDispatcher;
	
	public WebSocketSessionWrapper( WebSocketSession session, WebSocketServletDispatcher webSocketServletDispatcher ) {
		
		this.session = session;
		this.webSocketServletDispatcher = webSocketServletDispatcher;
		
	}
	
	@Override
	public void close() throws IOException {
		
		webSocketServletDispatcher.close( this );
		
	}
	
	@Override
	public void close( CloseStatus status ) throws IOException {
		
		webSocketServletDispatcher.close( this, status );
		
	}
	
	@Override
	public String getAcceptedProtocol() {
		
		return session.getAcceptedProtocol( );
	}
	
	@Override
	public Map<String, Object> getAttributes() {
		
		return session.getAttributes( );
	}
	
	@Override
	public int getBinaryMessageSizeLimit() {
		
		return session.getBinaryMessageSizeLimit( );
	}
	
	@Override
	public List<WebSocketExtension> getExtensions() {
		
		return session.getExtensions( );
	}
	
	@Override
	public HttpHeaders getHandshakeHeaders() {
		
		return session.getHandshakeHeaders( );
	}
	
	@Override
	public String getId() {
		
		return session.getId( );
	}
	
	@Override
	public InetSocketAddress getLocalAddress() {
		
		return session.getLocalAddress( );
	}
	
	@Override
	public Principal getPrincipal() {
		
		return session.getPrincipal( );
	}
	
	@Override
	public InetSocketAddress getRemoteAddress() {
		
		return session.getRemoteAddress( );
	}
	
	@Override
	public int getTextMessageSizeLimit() {
		
		return session.getTextMessageSizeLimit( );
	}
	
	@Override
	public URI getUri() {
		
		return session.getUri( );
	}
	
	@Override
	public boolean isOpen() {
		
		return session.isOpen( );
	}
	
	@Override
	public void sendMessage( WebSocketMessage<?> message ) throws IOException {
		
		throw new IllegalArgumentException( "Cannot modify read only session" );
		
	}
	
	@Override
	public void setBinaryMessageSizeLimit( int messageSizeLimit ) {
		
		throw new IllegalArgumentException( "Cannot modify read only session" );
		
	}
	
	@Override
	public void setTextMessageSizeLimit( int messageSizeLimit ) {
		
		throw new IllegalArgumentException( "Cannot modify read only session" );
		
	}
	
}
