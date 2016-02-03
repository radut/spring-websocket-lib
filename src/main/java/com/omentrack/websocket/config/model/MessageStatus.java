package com.omentrack.websocket.config.model;

public class MessageStatus {
	private Exception exception;
	private boolean sent;
	private boolean notSubscribed;
	private String reason;
	
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
	
	public MessageStatus( boolean sent, String reason ) {
		this.sent = sent;
		this.reason = reason;
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
	
	public String getReason() {
		
		return reason;
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
		builder.append( ", " );
		if ( reason != null ) {
			builder.append( "reason=" );
			builder.append( reason );
		}
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
	
	public static MessageStatus buildMaxRetryReached() {
		
		return new MessageStatus( false, "maxRetryReached" );
	}
	
}
