package com.morpheusdata.cypher;

/**
 * Creates a common interface for registering Cypher Modules. Modules are automatically processed when registered via the
 * registerModule() method in {@link Cypher}. These modules are bound to key paths which should be seperated using (/) symbols
 *
 * @see com.morpheusdata.cypher.modules.SecretModule
 * @see com.morpheusdata.cypher.modules.UUIDModule
 * @see com.morpheusdata.cypher.modules.RandomKeyModule
 *
 * @author David Estes
 */
public interface CypherModule {

	/**
	 * Some Modules need the cypher object to query config data. This allows it to be assigned on the constructor
	 * @param cypher
	 */
	public void setCypher(Cypher cypher);

	/**
	 * The write endpoint is called when any write requests are made toward a modules mount point. Most custom modules
	 * do not do anything special with write actions and simply pass on a generated CypherObject to be sent to a {@link Datastore}.
	 * Some exceptions to this may be the use of configuration. It could be used to capture configuration/settings for
	 * the specified module to be used.
	 * @param relativeKey the key passed excluding the mount point i.e. if mounted at '/secret' writing to '/secret/hello'
	 *                    would simply return 'hello'
	 * @param path the mount path used when creating the key. the original key can often be rebuilt by joining the path
	 *             and relativeKey value with a '/'
	 * @param value the unencrypted value we wish to write to the cypher {@link Datastore} and/or capture for configuration
	 * @param leaseTimeout the user specified leaseTimeout in milliseconds for when this key will expire.
	 * @param leaseObjectRef an object reference that can be checked against instead of a temporal value for key invalidation
	 * @param createdBy an object reference for a user that may have been used to create the key. Useful for permissions tracking
	 * @return a {@link CypherObject} that is then passed on to a {@link Datastore} encrypted for safe keeping.
	 */
	public CypherObject write(String relativeKey, String path, String value, Long leaseTimeout, String leaseObjectRef, String createdBy);

	/**
	 * The read endpoint is called when a value for a key is requested by the user. Typically if a value is already saved
	 * and not expired in a {@link Datastore} then this endpoint is never reached. However if no value is stored for the
	 * requested key then it is possible to generate a new value here and optionally return a {@link CypherObject} that
	 * can be saved for future retrieval.
	 * @param relativeKey the key passed excluding the mount point i.e. if mounted at '/secret' writing to '/secret/hello'
	 *                    would simply return 'hello'
	 * @param path the mount path used when creating the key. the original key can often be rebuilt by joining the path
	 *             and relativeKey value with a '/'
	 * @param leaseTimeout the user specified leaseTimeout in milliseconds for when this key will expire.
	 * @param leaseObjectRef an object reference that can be checked against instead of a temporal value for key invalidation
	 * @param createdBy an object reference for a user that may have been used to create the key. Useful for permissions tracking
	 *
	 * @return a {@link CypherObject} that is then passed on to a {@link Datastore} encrypted for safe keeping.
	 */
	public CypherObject read(String relativeKey, String path, Long leaseTimeout, String leaseObjectRef, String createdBy);

	/**
	 * The delete endpoint is called when a user requests that a key be deleted or potentially called when a key has expired.
	 * This is useful to capture if one must make a remote API call to potentially delete any related data pertaining to the key.
	 * @param relativeKey the key passed excluding the mount point i.e. if mounted at '/secret' writing to '/secret/hello'
	 *                    would simply return 'hello'
	 * @param path the mount path used when creating the key. the original key can often be rebuilt by joining the path
	 *             and relativeKey value with a '/'
	 * @param object the decryped {@link CypherObject} read from the {@link Datastore} in case any information encoded
	 *               in the value is needed to perform proper removal operations.
	 * @return success or failure of delete request. If this returns false, then the key is not removed from the {@link Datastore}.
	 */
	public boolean delete(String relativeKey, String path, CypherObject object);

	/**
	 * Returns a usage string useful as help when trying to figure out possible options or configuration that can be passed
	 * to a registered module in the {@link Cypher} instance.
	 * @return description of how this module can be used. It is possible to string replace the {mountPoint} pattern for usage examples.
	 */
	public String getUsage();

	/**
	 * Returns an HTML usage string useful as help when trying to figure out possible options or configuration that can be passed
	 * to a registered module in the {@link Cypher} instance.
	 * @return description of how this module can be used. It is possible to string replace the {mountPoint} pattern for usage examples.
	 */
	public String getHTMLUsage();
	
	/**
	 * The readFromDatastore method is used to determine if Cypher should read from the value stored within the {@link Datastore} on read requests 
	 * @return if this returns false then Cypher read requests are always executed through the module and do not read from a value that exists within the {@link Datastore}.
	 */
	public Boolean readFromDatastore();
}
