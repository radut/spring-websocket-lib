package com.omentrack.websocket.config.model;

public class WebSocketResponse {
	
	private boolean isResultOk;
	private WebSocketMessageType type;
	private String url;
	private Object result;
	private String errorMessage;
	
	public boolean isResultOk() {
	
		return isResultOk;
	}
	
	public void setResultOk( boolean isResultOk ) {
	
		this.isResultOk = isResultOk;
	}
	
	public WebSocketMessageType getType() {
	
		return type;
	}
	
	public void setType( WebSocketMessageType type ) {
	
		this.type = type;
	}
	
	public Object getResult() {
	
		return result;
	}
	
	public void setResult( Object result ) {
	
		this.result = result;
	}

	public String getUrl() {
	
		return url;
	}
	
	public void setUrl( String url ) {
	
		this.url = url;
	}

	public String getErrorMessage() {
	
		return errorMessage;
	}
	
	public void setErrorMessage( String errorMessage ) {
	
		this.errorMessage = errorMessage;
	}

}
