package com.tanadelgigante.civilservant;

public class ServiceConfigException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ServiceConfigException(String message) {
		super(message);
	}

	public ServiceConfigException(String message, Throwable cause) {
		super(message, cause);
	}
}
