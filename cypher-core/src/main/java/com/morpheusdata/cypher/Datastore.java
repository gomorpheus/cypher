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
	List<String> listExpiredKeys(String cypherId) throws DatastoreException;
	List<String> listKeysForLeaseRef(String cypherId, String leaseObjectRef) throws DatastoreException;

	List<String> listKeys(String cypherId) throws DatastoreException;
	List<String> listKeys(String cypherId, String regex) throws DatastoreException;

}
