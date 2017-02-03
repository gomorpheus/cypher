package com.morpheusdata.cypher;

import com.morpheusdata.cypher.exception.DatastoreException;

import java.util.List;

/**
 * Represents a Persistent Storage Backend
 */
public interface Datastore {

	CypherValue read(String cypherId,String key) throws DatastoreException;
	void write(String cypherId, CypherValue value) throws DatastoreException;
	void delete(String cypherId, String key) throws DatastoreException;
	void purgeExpiredKeys(String cypherId) throws DatastoreException;

	List<String> listKeys(String cypherId) throws DatastoreException;
	List<String> listKeys(String cypherId, String regex) throws DatastoreException;

}
