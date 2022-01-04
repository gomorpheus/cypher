package com.morpheusdata.cypher;

import com.morpheusdata.cypher.exception.EncoderException;
import com.morpheusdata.cypher.modules.PasswordModule;
import com.morpheusdata.cypher.modules.RandomKeyModule;
import com.morpheusdata.cypher.modules.SecretModule;
import com.morpheusdata.cypher.modules.UUIDModule;
import com.morpheusdata.cypher.util.SecurityUtils;

import javax.crypto.KeyGenerator;
import jakarta.xml.bind.DatatypeConverter;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Primary Entrypoint Class for accessing keys and configuring a Cypher vault.
 *
 * @author David Estes
 */
public class Cypher {
	static long DEFAULT_LEASE_TIME = 32l*24l*60l*60l*1000l; //32 days
	private String id;
	private CypherMeta cypherMeta;

	private Status status;
	private ValueEncoder valueEncoder;
	private Datastore datastore;
	private Long leaseTimeout;
	private Map<String,CypherModule> mountedModules;
	public static List<Cypher> cypherClasses = new ArrayList<>();
	public static CypherCleanupThread cleanupThread;



	public Cypher(String id, Datastore datastore) {
		this.status = Status.LOCKED;
		this.id = id;
		this.valueEncoder = new AESValueEncoder();
		this.cypherMeta = new CypherMeta();
		this.datastore = datastore;
		this.leaseTimeout = new Long(DEFAULT_LEASE_TIME);
		registerDefaultModules();
		cypherClasses.add(this);
		initializeCleanupThread();

	}

	public Cypher(String id, Datastore datastore, ValueEncoder valueEncoder) {
		this.status = Status.LOCKED;
		this.id = id;
		this.valueEncoder = valueEncoder;
		this.datastore = datastore;
		this.leaseTimeout = new Long(DEFAULT_LEASE_TIME);
		registerDefaultModules();
		cypherClasses.add(this);
		initializeCleanupThread();
	}

	static private synchronized void initializeCleanupThread() {
		if(cleanupThread == null) {
			cleanupThread = new CypherCleanupThread("cypher-cleanup");
			cleanupThread.start();
		}
	}

	public void registerDefaultModules() {
		this.mountedModules = new HashMap<>();
		mountedModules.put("secret",new SecretModule());
		mountedModules.put("uuid",new UUIDModule());
		mountedModules.put("key",new RandomKeyModule());
		mountedModules.put("password",new PasswordModule());
	}

	public void registerModule(String mountPoint, CypherModule module) {
		module.setCypher(this);
		mountedModules.put("mountPoint",module);
	}

	public void deregisterModule(String mountPoint) {
		mountedModules.remove(mountPoint);
	}

	public Map<String, CypherModule> getMountedModules() {
		return this.mountedModules;
	}

	public void setMasterKey(String masterKey) {
		this.cypherMeta.masterKey = DatatypeConverter.parseBase64Binary(masterKey);
		initializeInternal();
	}

	public void setMasterKey(byte[] masterKey) {
		this.cypherMeta.masterKey = masterKey;
		initializeInternal();
	}

	public Long getLeaseTimeout() {
		return this.leaseTimeout;
	}

	public void setLeaseTimeout(Long leaseTimeout) {
		this.leaseTimeout = leaseTimeout;
	}


	public String getAt(String key) throws IllegalStateException {
		return read(key,null).value;
	}

	public void putAt(String key, String value) throws IllegalStateException {
		write(key,value,null);
	}

	public CypherObject write(String key, String value) throws IllegalStateException {
		return write(key,value,null,null,null);
	}

	public CypherObject write(String key, String value, String createdBy) throws IllegalStateException {
		return write(key,value,null,null,createdBy);
	}

	public CypherObject write(String key, String value, Long leaseTimeout, String leaseObjectRef, String createdBy) throws IllegalStateException {
		if(this.status == Status.LOCKED) {
			throw new IllegalStateException("Cannot read from nor write to a locked Cypher");
		}
		String path = getMountPathForKey(key);
		if(path == null) {
			//TODO: Throw an exception
			return null;
		}

		CypherModule module = mountedModules.get(path);
		String relativeKey = key.substring(path.length()+1);
		CypherObject obj = module.write(relativeKey,path, value, leaseTimeout != null ? leaseTimeout : this.leaseTimeout, leaseObjectRef, createdBy);
		if(obj != null) {
			writeCypherObject(obj);
			return obj;
		}
		return null;
	}

	public CypherObject read(String key) throws IllegalStateException {
		return read(key,null,null,null);
	}

	public CypherObject read(String key, String createdBy) throws IllegalStateException {
		return read(key,null,null,createdBy);
	}

	public CypherObject read(String key, Long leaseTimeout, String createdBy) throws IllegalStateException {
		return read(key,leaseTimeout,null,createdBy);
	}

	public CypherObject read(String key, String leaseObjectRef, String createdBy) throws IllegalStateException {
		return read(key,null,leaseObjectRef,createdBy);
	}

	public CypherObject read(String key, Long leaseTimeout, String leaseObjectRef, String createdBy) throws IllegalStateException {
		if(this.status == Status.LOCKED) {
			throw new IllegalStateException("Cannot read from nor write to a locked Cypher");
		}
		String path = getMountPathForKey(key);
		if(path == null) {
			return null;
		}

		CypherValue value = datastore.read(id,key);
		if(value != null) {
			byte[] encryptedEncryptionKey = DatatypeConverter.parseBase64Binary(value.encryptedEncryptionKey);
			byte[] decryptedEncryptionKey = valueEncoder.decode(cypherMeta.masterKey,encryptedEncryptionKey);
			String decryptedValue = valueEncoder.decode(decryptedEncryptionKey, value.value);
			SecurityUtils.secureErase(decryptedEncryptionKey);
			return new CypherObject(key,decryptedValue,value.leaseTimeout, value.leaseObjectRef, value.createdBy);
		} else {
			CypherModule module = mountedModules.get(path);
			String relativeKey = key.substring(path.length()+1);
			CypherObject obj = module.read(relativeKey,path, leaseTimeout != null ? leaseTimeout : this.leaseTimeout, leaseObjectRef, createdBy);
			if(obj != null) {
				if(obj.shouldPersist != false) {
					writeCypherObject(obj);
				}
				return obj;
			}
			return null;
		}

	}

	/**
	 * Main endpoint for deleting keys and their values. This method also ties into any mounted modules and passes on
	 * any delete requests as well. This is useful if the module needs to clear out any saved credentials.
	 * @param key
	 *
	 * @return
	 */
	public boolean delete(String key) {
		String path = getMountPathForKey(key);
		if(path == null) {
			return false;
		}
		CypherObject obj = read(key);
		CypherModule module = mountedModules.get(path);
		String relativeKey = key.substring(path.length()+1);
		if(module.delete(relativeKey,path, obj)) {
			datastore.delete(id,key);
			return true;
		} else {
			return false;
		}
	}

	public void purgeKeysForLeaseRef(String leaseObjectRef) {
		List<String> keys = datastore.listKeysForLeaseRef(id,leaseObjectRef);
		if(keys.size() > 0) {
			for(String key : keys) {
				delete(key);
			}
		}
	}

	public void purgeExpiredKeys() {
		List<String> keys = datastore.listExpiredKeys(id);
		if(keys.size() > 0) {
			for(String key : keys) {
				delete(key);
			}
		}
	}

	public List<String> listKeys() {
		return datastore.listKeys(this.id);
	}

	public List<String> listKeys(String regex) {
		return datastore.listKeys(this.id,regex);
	}

	protected void writeCypherObject(CypherObject obj) {
		byte[] decryptedEncryptionKey = valueEncoder.decode(cypherMeta.masterKey,cypherMeta.encryptedEncryptionKey);
		String encryptedValue = valueEncoder.encode(decryptedEncryptionKey, obj.value);
		SecurityUtils.secureErase(decryptedEncryptionKey);
		CypherValue value = new CypherValue(obj.key,encryptedValue,DatatypeConverter.printBase64Binary(cypherMeta.encryptedEncryptionKey),obj.leaseTimeout, obj.leaseObjectRef, obj.createdBy);
		datastore.write(id,value);
	}

	/**
	 * Cypher can be configured to use a specific encryption key after the master key has been setup
	 * WARNING: This is destructive on the passed byte array of encryption key to minimize key footprint in memory.
	 * @param encryptionKey
	 */
	public void setEncryptionKey(byte[] encryptionKey) throws IllegalStateException, EncoderException {
		if(this.status == Status.LOCKED) {
			throw new IllegalStateException("Cannot set a new encryption key without an unlocked Cypher");
		}
		this.cypherMeta.encryptedEncryptionKey = valueEncoder.encode(cypherMeta.masterKey, encryptionKey);
		SecurityUtils.secureErase(encryptionKey);
	}

	/**
	 * Cypher can be configured to use a specific encryption key that is base64 encoded after the master key has been setup
	 * @param encryptionKey
	 */
	public void setEncryptionKey(String encryptionKey) throws IllegalStateException, EncoderException {
		if(this.status == Status.LOCKED) {
			throw new IllegalStateException("Cannot set a new encryption key without an unlocked Cypher");
		}
		byte[] encryptionKeyBytes = DatatypeConverter.parseBase64Binary(encryptionKey);
		SecurityUtils.secureErase(encryptionKeyBytes);
		this.cypherMeta.encryptedEncryptionKey = valueEncoder.encode(cypherMeta.masterKey, encryptionKeyBytes );
	}

	/**
	 * Rotates the Encryption key used for storing values
	 */
	public void rotate() throws IllegalStateException, EncoderException {
		if(this.status == Status.LOCKED) {
			throw new IllegalStateException("Cannot set a new encryption key without an unlocked Cypher");
		}
		try {
			SecureRandom rand = new SecureRandom();
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(256,rand);
			byte[] encryptionKey = keyGenerator.generateKey().getEncoded();
			this.cypherMeta.encryptedEncryptionKey = valueEncoder.encode(cypherMeta.masterKey,encryptionKey);
			SecurityUtils.secureErase(encryptionKey);
		} catch(NoSuchAlgorithmException ex) {
			throw new EncoderException("Error Generating new Encryption Key", ex);
		}
	}


	protected String getMountPathForKey(String key) {
		for(String mount : mountedModules.keySet()) {
			if(key.toLowerCase().startsWith(mount.toLowerCase() + "/")) {
				return mount;
			}
		}
		return null;
	}


	/**
	 * Internally initializes the Cypher keyset and verifies everything is functioning as expected
	 */
	protected void initializeInternal() {
		if(cypherMeta.masterKey == null || cypherMeta.masterKey.length == 0) {
			this.status = Status.LOCKED;
			return;
		}
		this.status = Status.UNLOCKED;
		if(cypherMeta.encryptedEncryptionKey == null) {
			rotate();
		}

	}



	/**
	 * Returns the ID of the cypher instance. This is user generated and can be useful for cypher object referencing
	 * @return
	 */
	public String getId() {
		return this.id;
	}

	public Status getStatus() {
		return this.status;
	}

	public enum Status {
		LOCKED("locked"),
		UNLOCKED("unlocked");

		/** The header value representing the canned acl */
		private final String status;

		Status(String status) {
			this.status = status;
		}

		/**
		 * Returns the header value for this canned acl.
		 */
		public String toString() {
			return status;
		}
	}
}



