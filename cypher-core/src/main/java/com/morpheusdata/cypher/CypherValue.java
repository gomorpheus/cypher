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
	public String createdBy;
	public String key;
	public Long leaseTimeout;
	public String leaseObjectRef;

	CypherValue(String key, String value, String encryptedEncryptionKey, Long leaseTimeout, String leaseObjectRef, String createdBy) {
		this.value = value;
		this.key = key;
		this.createdBy = createdBy;
		this.encryptedEncryptionKey = encryptedEncryptionKey;
		this.leaseTimeout = leaseTimeout;
		this.leaseObjectRef = leaseObjectRef;
	}

	public Map<String,String> toMap() {
		Map<String,String> map = new HashMap<String,String>();
		map.put("encryptionKey", encryptedEncryptionKey);
		map.put("value",value);
		map.put("key",key);
		map.put("createdBy",createdBy);
		map.put("leaseTimeout",leaseTimeout.toString());
		if(leaseTimeout != null && leaseTimeout > 0) {
			map.put("leaseExpireTime",new Long((new Date().getTime() + leaseTimeout)).toString());
		} else {
			map.put("leaseExpireTime","0");
		}
		map.put("leaseObjectRef",leaseObjectRef);
		return map;
	}


	public static CypherValue fromMap(Map<String,String> map) {
		return new CypherValue(map.get("key"),map.get("value"), map.get("encryptionKey"),new Long(map.get("leaseTimeout")), map.get("leaseObjectRef"), map.get("createdBy"));
	}
}
