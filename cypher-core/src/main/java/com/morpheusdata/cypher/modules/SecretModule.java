package com.morpheusdata.cypher.modules;

import com.morpheusdata.cypher.*;
import com.morpheusdata.cypher.util.SecurityUtils;

/**
 * Created by davydotcom on 1/31/17.
 */
public class SecretModule implements CypherModule {

	public CypherObject write(String relativeKey, String path, String value, Long leaseTimeout) {
		String key = relativeKey;
		if(path != null) {
			key = path + "/" + key;
		}
		return new CypherObject(key,value,leaseTimeout);
	}

	public CypherObject read(String relativeKey, String path, Long leaseTimeout) {

		//no dynamic generation available for this pattern... read from store
		return null;
	}


}
