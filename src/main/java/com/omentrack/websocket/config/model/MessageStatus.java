package com.omentrack.websocket.config.model;

public class MessageStatus {
	private Exception exception;
	private boolean sent;
	private boolean notSubscribed;
	
	private MessageStatus( Exception exception ) {
		this.exception = exception;
		sent = false;
	}
	
	private MessageStatus() {
		sent = true;
	}
	
	private MessageStatus( boolean notSubscribed ) {
		this.notSubscribed = notSubscribed;
	}
	
	public boolean isSent() {
		
		return sent;
	}
	
	public Exception getException() {
		
		return exception;
	}
	
	public boolean isNotSubscribed() {
		
		return notSubscribed;
	}
	
	@Override
	public String toString() {
		
		StringBuilder builder = new StringBuilder( );
		builder.append( "MessageStatus [" );
		if ( exception != null ) {
			builder.append( "exception=" );
			builder.append( exception.getMessage( ) );
			builder.append( ", " );
		}
		builder.append( "sent=" );
		builder.append( sent );
		builder.append( ", notSubscribed=" );
		builder.append( notSubscribed );
		builder.append( "]" );
		return builder.toString( );
	}
	
	public static MessageStatus buildOkResult() {
		
		return new MessageStatus( );
	}
	
	public static MessageStatus buildErrorResult( Exception e ) {
		
		return new MessageStatus( e );
	}
	
	public static MessageStatus buildNotSubscribed() {
		
		return new MessageStatus( true );
	}
	
}
