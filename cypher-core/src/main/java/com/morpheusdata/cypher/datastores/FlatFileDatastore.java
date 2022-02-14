package com.morpheusdata.cypher.datastores;

import com.morpheusdata.cypher.CypherValue;
import com.morpheusdata.cypher.Datastore;
import com.morpheusdata.cypher.ValueEncoder;
import com.morpheusdata.cypher.exception.DatastoreException;
import com.morpheusdata.cypher.util.DatatypeConverterUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a standard flat file datastore implementation. MD5 hashing is used to optimize key file matching.
 * NOTE: Not Recommended for HA Deployments or Production.
 *
 * @author David Estes
 */
public class FlatFileDatastore implements Datastore {
	private Log log = LogFactory.getLog(FlatFileDatastore.class);
	private String basePath;
	Map<String, Object> fileLocks = new HashMap<String, Object>();

	/**
	 * The flat-file {@link Datastore} must be implemented targeting a specific base path
	 * @param basePath
	 */
	public FlatFileDatastore(String basePath) {
		this.basePath = basePath;

	}

	@Override
	public CypherValue read(String cypherId, String key) throws DatastoreException{
		try {
			String realKey = cypherId + "/" + key;
			String hash = keyHash(realKey);
			Map<String,Map<String,String>> dataSet = readFile(hash);
			Map<String,String> result = dataSet.get(realKey);
			if(result != null) {
				return CypherValue.fromMap(result);
			}
		} catch(Exception ex) {
			throw new DatastoreException("An Error Occurred while trying to read a key value", ex);
		}
		return null;
	}

	@Override
	public synchronized void write(String cypherId, CypherValue value) throws DatastoreException {
		try {
			String realKey = cypherId + "/" + value.key;
			String hash = keyHash(realKey);
			Map<String,Map<String,String>> dataSet = readFile(hash);
			dataSet.put(realKey,value.toMap());
			writeFile(hash,dataSet);
		} catch(Exception ex) {
			throw new DatastoreException("An Error Occurred while trying to persist a key value", ex);
		}
	}

	@Override
	public synchronized void delete(String cypherId, String key) throws DatastoreException {
		try {
			String realKey = cypherId + "/" + key;
			String hash = keyHash(realKey);
			Map<String,Map<String,String>> dataSet = readFile(hash);
			if(dataSet.get(realKey) != null) {
				dataSet.remove(realKey);
				writeFile(hash,dataSet);
			}
		} catch(Exception ex) {
			throw new DatastoreException("An Error Occurred while trying to persist a key value", ex);
		}
	}

	@Override
	public List<String> listExpiredKeys(String cypherId) {

		Date now = new Date();
		try {
			ArrayList<String> keys = new ArrayList<String>();
			List<String> hashNames = getHashFileNames();
			for(int counter=0;counter < hashNames.size();counter++) {
				String hash = hashNames.get(counter);
				Map<String,Map<String,String>> dataSet = readFile(hash);
				boolean changes = false;
				for (String key : dataSet.keySet()) {
					if(key.startsWith(cypherId + "/")) {
						String leaseTimeoutString = dataSet.get(key).get("leaseExpireTime");
						if(leaseTimeoutString != null) {
							Long leaseTimeout = new Long(leaseTimeoutString);
							if(leaseTimeout != 0 && leaseTimeout < now.getTime()) {
								String realKey = key.substring(cypherId.length() + 1);
								keys.add(realKey);
							}
						}
					}
				}
			}

			return keys;
		} catch(Exception ex) {
			throw new DatastoreException("An Error Occurred while trying to list known keys in a Datastore.",ex);
		}
	}

	@Override
	public List<String> listKeysForLeaseRef(String cypherId, String leaseObjectRef) throws DatastoreException {
		try {
			List<String> hashNames = getHashFileNames();
			ArrayList<String> keys = new ArrayList<String>();
			for(int counter=0;counter < hashNames.size();counter++) {
				String hash = hashNames.get(counter);
				Map<String,Map<String,String>> dataSet = readFile(hash);
				boolean changes = false;
				for (String key : dataSet.keySet()) {
					if(key.startsWith(cypherId + "/")) {
						String tmpLeaseObjectRef = dataSet.get(key).get("leaseObjectRef");
						if(tmpLeaseObjectRef == leaseObjectRef) {
							String realKey = key.substring(cypherId.length() + 1);
							keys.add(realKey);
						}
					}
				}
			}
			return keys;
		} catch(Exception ex) {
			throw new DatastoreException("An Error Occurred while trying to list known keys in a Datastore.",ex);
		}
	}

	@Override
	public List<String> listKeys(String cypherId) {
		try {
			List<String> hashNames = getHashFileNames();
			ArrayList<String> keys = new ArrayList<String>();
			for(int counter=0;counter < hashNames.size();counter++) {
				String hash = hashNames.get(counter);
				Map<String,Map<String,String>> dataSet = readFile(hash);

				for (String key : dataSet.keySet()) {
					if(key.startsWith(cypherId + "/")) {
						String realKey = key.substring(cypherId.length() + 1);
						keys.add(realKey);
					}
				}
			}
			return keys;
		} catch(Exception ex) {
			throw new DatastoreException("An Error Occurred while trying to list known keys in a Datastore.",ex);
		}
	}

	@Override
	public List<String> listKeys(String cypherId, String regex) {
		try {
			Pattern pattern = Pattern.compile(regex);
			List<String> hashNames = getHashFileNames();
			ArrayList<String> keys = new ArrayList<String>();
			for(int counter=0;counter < hashNames.size();counter++) {
				String hash = hashNames.get(counter);
				Map<String,Map<String,String>> dataSet = readFile(hash);
				for (String key : dataSet.keySet()) {
					if(key.startsWith(cypherId + "/")) {
						String realKey = key.substring(cypherId.length()+1);
						Matcher matcher = pattern.matcher(realKey);
						if(matcher.find()) {
							keys.add(realKey);
						}
					}

				}
			}
			return keys;
		} catch(Exception ex) {
			throw new DatastoreException("An Error Occurred while trying to list known keys in a Datastore.",ex);
		}
	}

	protected Map<String,Map<String,String>> readFile(String hash) throws IOException, ClassNotFoundException {
		Object lockObj = getLockObject(hash);

		File file = new File(this.basePath,hash);
		String fileLocation = file.getCanonicalPath();
		if(!file.exists()) {
			return new HashMap<String,Map<String,String>>();
		}
		synchronized(lockObj) {
			FileInputStream fis = null;
			ObjectInputStream ois = null;
			try {
				fis = new FileInputStream(fileLocation);
				ois = new ObjectInputStream(fis);
				Map<String,Map<String,String>> result = (Map<String,Map<String,String>>)ois.readObject();
				return result;
			} finally {
				if(ois != null) {
					ois.close();
				}
				if(fis != null) {
					fis.close();
				}

			}

		}
	}

	protected void writeFile(String hash, Map<String,Map<String,String>> data) throws IOException, ClassNotFoundException  {
		Object lockObj = getLockObject(hash);
		File file = new File(this.basePath,hash);
		String fileLocation = file.getCanonicalPath();
		if(!file.exists()) {
			file.getParentFile().mkdirs();
//			file.createNewFile();
		}
		synchronized(lockObj) {
			FileOutputStream fos = new FileOutputStream(fileLocation);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(data);
			oos.flush();
			fos.flush();
			oos.close();
			fos.close();
		}
	}

	protected List<String> getHashFileNames() {
		File file = new File(this.basePath);
		File[] files = file.listFiles();
		ArrayList<String> hashNames = new ArrayList<String>();

		for(int counter=0;counter < files.length;counter++) {
			if(files[counter].isFile()) {
				hashNames.add(files[counter].getName());
			}
		}
		return hashNames;
	}


	protected Object getLockObject(String hash) {
		synchronized(fileLocks) {
			Object obj = fileLocks.get(hash);
			if(obj != null) {
				return obj;
			} else {
				obj = new Object();
				fileLocks.put(hash, obj);
				return obj;
			}
		}
	}


	protected String keyHash(String key) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("MD5");
		digest.update(key.getBytes("UTF-8"));
		String fullHash = DatatypeConverterUtil.printBase64Binary(digest.digest());
		return fullHash.substring(0,3);
	}
}
