package com.morpheusdata.cypher.modules;

import com.morpheusdata.cypher.*;
import com.morpheusdata.cypher.util.SecurityUtils;

/**
 * Standard Cypher module for simply storing and retrieving passed in keys and values. No manipulation is done to the
 * data passed through this module and is by default mounted to '/secret'
 *
 * @author David Estes
 */
public class SecretModule extends AbstractCypherModule {

	/**
	 * Some Modules need the cypher object to query config data. This allows it to be assigned on the constructor
	 *
	 * @param cypher
	 */
	@Override
	public void setCypher(Cypher cypher) {

	}

	/**
	 * The endpoint hit during write of a key that simply passes on the user passed value for encryption and storage.
	 * @param relativeKey the key passed excluding the mount point i.e. if mounted at '/secret' writing to '/secret/hello'
	 *                    would simply return 'hello'
	 * @param path the mount path used when creating the key. the original key can often be rebuilt by joining the path
	 *             and relativeKey value with a '/'
	 * @param value the unencrypted value we wish to write to the cypher {@link Datastore} and/or capture for configuration
	 * @param leaseTimeout the user specified leaseTimeout in milliseconds for when this key will expire.
	 * @return
	 */
	public CypherObject write(String relativeKey, String path, String value, Long leaseTimeout, String leaseObjectRef, String createdBy) {
		String key = relativeKey;
		if(path != null) {
			key = path + "/" + key;
		}
		return new CypherObject(key,value,leaseTimeout, leaseObjectRef, createdBy);
	}

	/**
	 * Unimplemented read method. This method is only reached if a key is requested that does not exist. Since this
	 * {@link CypherModule} does not do any data manipulation this method always returns null.
	 *
	 * @param relativeKey the key passed excluding the mount point i.e. if mounted at '/secret' writing to '/secret/hello'
	 *                    would simply return 'hello'
	 * @param path the mount path used when creating the key. the original key can often be rebuilt by joining the path
	 *             and relativeKey value with a '/'
	 * @param leaseTimeout the user specified leaseTimeout in milliseconds for when this key will expire.
	 * @return
	 */
	public CypherObject read(String relativeKey, String path, Long leaseTimeout, String leaseObjectRef, String createdBy) {

		//no dynamic generation available for this pattern... read from store
		return null;
	}

	/**
	 * Unimplemented delete method. No special functions are necessary and therefore this method always returns true.
	 * @param relativeKey the key passed excluding the mount point i.e. if mounted at '/secret' writing to '/secret/hello'
	 *                    would simply return 'hello'
	 * @param path the mount path used when creating the key. the original key can often be rebuilt by joining the path
	 *             and relativeKey value with a '/'
	 * @param object the decryped {@link CypherObject} read from the {@link Datastore} in case any information encoded
	 *               in the value is needed to perform proper removal operations.
	 * @return
	 */
	public boolean delete(String relativeKey, String path, CypherObject object) {
		return true;
	}

	public String getUsage() {
		StringBuilder usage = new StringBuilder();

		usage.append("This is the standard secret module that stores a key/value in encrypted form.");

		return usage.toString();
	}

	public String getHTMLUsage() {
		StringBuilder usage = new StringBuilder();

		usage.append("<p>This is the standard secret module that stores a key/value in encrypted form.</p>");

		return usage.toString();
	}
}
