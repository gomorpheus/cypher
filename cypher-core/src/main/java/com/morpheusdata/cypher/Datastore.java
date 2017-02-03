package com.morpheusdata.cypher;

import com.morpheusdata.cypher.exception.DatastoreException;

import java.util.List;

/**
 * Represents a Persistent Storage Backend
 */
public interface Datastore {

	CypherValue read(String key) throws DatastoreException;
	void write(String key, CypherValue value) throws DatastoreException;
	void delete(String key) throws DatastoreException;
	void purgeExpiredKeys() throws DatastoreException;

	List<String> listKeys() throws DatastoreException;
	List<String> listKeys(String regex) throws DatastoreException;

}
