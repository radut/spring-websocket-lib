package com.radutoader.websocket.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.Conventions;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.StringUtils;

import com.radutoader.websocket.config.annotation.WebSocketParam;

public class WebSocketDataParamParameterNameDiscovery implements ParameterNameDiscoverer {
	
	private ParameterNameDiscoverer localParametersNameDiscovery = new LocalVariableTableParameterNameDiscoverer( );
	
	@Override
	public String[] getParameterNames( Method method ) {
	
		List<String> parametersList = new ArrayList<String>( );
		Annotation[][] parameterAnnotations = method.getParameterAnnotations( );
		String[] parameterNames = localParametersNameDiscovery.getParameterNames( method );
		int i = 0;
		outer: for ( i = 0; i < parameterAnnotations.length; i++ ) {
			Annotation[] annotations = parameterAnnotations[i];
			String parameterName = parameterNames[i];
			for ( Annotation annotation : annotations ) {
				if ( annotation instanceof WebSocketParam ) {
					WebSocketParam webSocketParam = (WebSocketParam) annotation;
					if ( !StringUtils.isEmpty( webSocketParam.value( ) ) ) {
						parameterName = webSocketParam.value( );
					}
					parametersList.add( parameterName );
					continue outer;
				}
			}
			parametersList.add( null );
		}
		
		return parametersList.toArray( new String[parametersList.size( )] );
	}
	
	@Override
	public String[] getParameterNames( Constructor<?> ctor ) {
	
		return null;
	}
	
}
