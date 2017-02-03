package com.morpheusdata.cypher.exception;

/**
 * Created by davydotcom on 1/31/17.
 */
public class DatastoreException extends RuntimeException {
	public DatastoreException(Exception ex) {
		super(ex);

	}

	public DatastoreException(String message, Exception ex) {
		super(message, ex);
	}
}
