package com.morpheusdata.cypher;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by davydotcom on 1/31/17.
 */
public class CypherObject {
	public String key;
	public String value;
	public Long leaseTimeout;
	public String leaseObjectRef;
	public String createdBy;
	public Boolean shouldPersist = true;

	public CypherObject(String key, String value, Long leaseTimeout, String leaseObjectRef, String createdBy) {
		this.value = value;
		this.key = key;
		this.leaseTimeout = leaseTimeout;
		this.createdBy = createdBy;
		this.leaseObjectRef = leaseObjectRef;
	}

	public Map<String,String> toMap() {
		Map<String,String> map = new HashMap<String,String>();
		map.put("value",value);
		map.put("leaseTimeout",leaseTimeout.toString());
		map.put("leaseObjectRef",leaseObjectRef);
		return map;
	}

	public static CypherObject fromMap(Map<String,String> map) {
		return new CypherObject(map.get("key"),map.get("value"),new Long((map.get("leaseTimeout"))),map.get("leaseObjectRef"),map.get("createdBy"));
	}
}
