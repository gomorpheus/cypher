package com.morpheusdata.cypher.modules;

import com.morpheusdata.cypher.Cypher;
import com.morpheusdata.cypher.CypherMeta;
import com.morpheusdata.cypher.CypherModule;
import com.morpheusdata.cypher.CypherObject;

/**
 * Created by davydotcom on 2/3/17.
 */
public class UUIDModule extends AbstractCypherModule {
	private CypherMeta cypherMeta;

	/**
	 * Some Modules need the cypher object to query config data. This allows it to be assigned on the constructor
	 *
	 * @param cypher
	 */
	@Override
	public void setCypher(Cypher cypher) {

	}

	public CypherObject write(String relativeKey, String path, String value, Long leaseTimeout, String leaseObjectRef, String createdBy) {
		return null;
	}

	public CypherObject read(String relativeKey, String path, Long leaseTimeout, String leaseObjectRef, String createdBy) {
		String key = relativeKey;
		if(path != null) {
			key = path + "/" + key;
		}
		String value = java.util.UUID.randomUUID().toString();
		return new CypherObject(key,value,leaseTimeout, leaseObjectRef, createdBy);
	}

	public boolean delete(String relativeKey, String path, CypherObject object) {
		return true;
	}

	public String getUsage() {
		StringBuilder usage = new StringBuilder();

		usage.append("Returns a new UUID by key name when requested and stores the generated UUID by key name for a given lease timeout period.");

		return usage.toString();
	}

	public String getHTMLUsage() {
		StringBuilder usage = new StringBuilder();

		usage.append("<p>Returns a new UUID by key name when requested and stores the generated UUID by key name for a given lease timeout period.</p>");

		return usage.toString();
	}
}
