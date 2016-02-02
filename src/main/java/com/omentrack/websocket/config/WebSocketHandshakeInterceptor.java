package com.omentrack.websocket.config;

import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

public class WebSocketHandshakeInterceptor extends HttpSessionHandshakeInterceptor {
	
	private static final Logger logger = Logger.getLogger( WebSocketHandshakeInterceptor.class );
	
	@Override
	public boolean beforeHandshake( ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes ) throws Exception {
		
		HttpSession session = ( (ServletServerHttpRequest) request ).getServletRequest( ).getSession( );
		attributes.put( "httpSession", session );
		if ( logger.isDebugEnabled( ) )
			logger.debug( "Before Handshake : jSessionId=" + session.getId( ) );
		return super.beforeHandshake( request, response, wsHandler, attributes );
	}
	
	@Override
	public void afterHandshake( ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception ex ) {
		
		HttpSession session = ( (ServletServerHttpRequest) request ).getServletRequest( ).getSession( );
		if ( logger.isDebugEnabled( ) )
			logger.debug( "After Handshake : jSessionId=" + session.getId( ) );
		super.afterHandshake( request, response, wsHandler, ex );
	}
	
}