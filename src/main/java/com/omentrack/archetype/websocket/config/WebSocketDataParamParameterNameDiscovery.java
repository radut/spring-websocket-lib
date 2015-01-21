package com.omentrack.archetype.websocket.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.ParameterNameDiscoverer;

import com.omentrack.archetype.websocket.config.annotation.WebSocketParam;

public class WebSocketDataParamParameterNameDiscovery implements ParameterNameDiscoverer {
	
	@Override
	public String[] getParameterNames( Method method ) {
	
		List<String> parametersList = new ArrayList<String>( );
		Annotation[][] parameterAnnotations = method.getParameterAnnotations( );
		int i = 0;
		outer: for ( i = 0; i < parameterAnnotations.length; i++ ) {
			Annotation[] annotations = parameterAnnotations[i];
			for ( Annotation annotation : annotations ) {
				if ( annotation instanceof WebSocketParam ) {
					parametersList.add( ( (WebSocketParam) annotation ).value( ) );
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
