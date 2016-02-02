package com.omentrack.websocket.config;

public class MessageStatus {
	private Exception exception;
	private boolean sent;
	private boolean notSubscribed;
	
	public MessageStatus( Exception exception ) {
		this.exception = exception;
		sent = false;
	}
	
	public MessageStatus() {
		sent = true;
	}
	
	public MessageStatus( boolean sent, boolean notSubscribed ) {
		this.sent = sent;
		this.notSubscribed = notSubscribed;
	}
	
	public boolean isSent() {
		
		return sent;
	}
	
	public void setSent( boolean sent ) {
		
		this.sent = sent;
	}
	
	public Exception getException() {
		
		return exception;
	}
	
	public void setException( Exception exception ) {
		
		this.exception = exception;
	}
	
	public boolean isNotSubscribed() {
		
		return notSubscribed;
	}
	
	public void setNotSubscribed( boolean subscriptionNotPresent ) {
		
		notSubscribed = subscriptionNotPresent;
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
	
}
