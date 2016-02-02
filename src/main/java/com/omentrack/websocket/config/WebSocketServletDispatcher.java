package com.omentrack.websocket.config;


import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public interface WebSocketServletDispatcher {
	
	Future<MessageStatus> sendMessageToWebSocketClient( String webSocketSessionId, String subscription, Object message );
	
	Future<Map<WebSocketClient, MessageStatus>> sendMessageToAllHttpSession( String jSessionId, String subscription, Object message );
	
	Future<Map<WebSocketClient, MessageStatus>> sendMessageToClients( List<WebSocketClient> clients, String subscription, Object message );
	
	Future<Map<WebSocketClient, MessageStatus>> sendMessageToAllSubscribers( String subscription, Object message );

	void refreshConnectedSessions();
	
}
