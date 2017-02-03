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

	public CypherObject(String key, String value, Long leaseTimeout) {
		this.value = value;
		this.key = key;
		this.leaseTimeout = leaseTimeout;
	}

	public Map<String,String> toMap() {
		Map<String,String> map = new HashMap<String,String>();
		map.put("value",value);
		map.put("leaseTimeout",leaseTimeout.toString());
		return map;
	}

	public static CypherObject fromMap(Map<String,String> map) {
		return new CypherObject(map.get("key"),map.get("value"),new Long((map.get("leaseTimeout"))));
	}
}
