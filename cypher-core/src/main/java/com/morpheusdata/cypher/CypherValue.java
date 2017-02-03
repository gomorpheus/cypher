package com.morpheusdata.cypher;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by davydotcom on 1/31/17.
 */
public class CypherValue {
	public String encryptedEncryptionKey;
	public String value;
	public String key;
	public Long leaseTimeout;

	CypherValue(String key, String value, String encryptedEncryptionKey, Long leaseTimeout) {
		this.value = value;
		this.encryptedEncryptionKey = encryptedEncryptionKey;
		this.leaseTimeout = leaseTimeout;
	}

	public Map<String,String> toMap() {
		Map<String,String> map = new HashMap<String,String>();
		map.put("encryptionKey", encryptedEncryptionKey);
		map.put("value",value);
		map.put("key",key);
		map.put("leaseTimeout",leaseTimeout.toString());
		map.put("leaseExpireTime",new Long((new Date().getTime() + leaseTimeout)).toString());
		return map;
	}


	public static CypherValue fromMap(Map<String,String> map) {
		return new CypherValue(map.get("key"),map.get("value"), map.get("encryptionKey"),new Long(map.get("leaseTimeout")));
	}
}
