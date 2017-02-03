package com.morpheusdata.cypher.modules;

import com.morpheusdata.cypher.CypherMeta;
import com.morpheusdata.cypher.CypherModule;
import com.morpheusdata.cypher.CypherObject;

/**
 * Created by davydotcom on 2/3/17.
 */
public class UUIDModule implements CypherModule {
	private CypherMeta cypherMeta;

	public CypherObject write(String relativeKey, String path, String value, Long leaseTimeout) {
		return null;
	}

	public CypherObject read(String relativeKey, String path, Long leaseTimeout) {
		String key = relativeKey;
		if(path != null) {
			key = path + "/" + key;
		}
		String value = java.util.UUID.randomUUID().toString();
		return new CypherObject(key,value,leaseTimeout);
	}

}
