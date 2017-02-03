package com.morpheusdata.cypher;

/**
 * Created by davydotcom on 1/31/17.
 */
public interface CypherModule {
	public CypherObject write(String relativeKey, String path, String value, Long leaseTimeout);
	public CypherObject read(String relativeKey, String path, Long leaseTimeout);
}
