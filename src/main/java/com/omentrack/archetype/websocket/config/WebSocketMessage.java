package com.omentrack.archetype.websocket.config;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties( ignoreUnknown = true )
public class WebSocketMessage {
	
	private WebSocketMessageType type;
	private String url;
	private Map<String, Object> data = new HashMap<String, Object>( );
	
	public WebSocketMessage() {
	
	}

	public WebSocketMessageType getType() {
	
		return type;
	}
	
	public void setType( WebSocketMessageType type ) {
	
		this.type = type;
	}
	
	public String getUrl() {
	
		return url;
	}
	
	public void setUrl( String url ) {
	
		this.url = url;
	}
	
	public Map<String, Object> getData() {
	
		return data;
	}
	
	public void setData( Map<String, Object> data ) {
	
		this.data = data;
	}
	
	@Override
	public String toString() {
	
		StringBuilder builder = new StringBuilder( );
		builder.append( "WebSocketMessage [" );
		if ( type != null )
			builder.append( "type=" ).append( type ).append( ", " );
		if ( url != null )
			builder.append( "url=" ).append( url ).append( ", " );
		if ( data != null )
			builder.append( "data=" ).append( data );
		builder.append( "]" );
		return builder.toString( );
	}
	
}

