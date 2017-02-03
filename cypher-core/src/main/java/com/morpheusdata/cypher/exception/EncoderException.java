package com.morpheusdata.cypher.exception;

/**
 * Created by davydotcom on 1/31/17.
 */
public class EncoderException extends RuntimeException {
	public EncoderException(Exception ex) {
		super(ex);

	}

	public EncoderException(String message, Exception ex) {
		super(message, ex);
	}
}
