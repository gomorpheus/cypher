package com.morpheus.cypher.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.*;
import com.bertramlabs.plugins.hcl4j.HCLParser;
import com.bertramlabs.plugins.hcl4j.HCLParserException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.morpheusdata.cypher.Cypher;
import com.morpheusdata.cypher.CypherMeta;
import com.morpheusdata.cypher.CypherModule;
import com.morpheusdata.cypher.CypherObject;
import com.morpheusdata.cypher.Datastore;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author David Estes
 */
public class AwsModule implements CypherModule {
	private CypherMeta cypherMeta;
	private Cypher cypher;

	/**
	 * Some Modules need the cypher object to query config data. This allows it to be assigned on the constructor
	 *
	 * @param cypher
	 */
	@Override
	public void setCypher(Cypher cypher) {
		this.cypher = cypher;
	}

	/**
	 * The write endpoint is called when any write requests are made toward a modules mount point. Most custom modules
	 * do not do anything special with write actions and simply pass on a generated CypherObject to be sent to a {@link Datastore}.
	 * Some exceptions to this may be the use of configuration. It could be used to capture configuration/settings for
	 * the specified module to be used.
	 *
	 * @param relativeKey    the key passed excluding the mount point i.e. if mounted at '/secret' writing to '/secret/hello'
	 *                       would simply return 'hello'
	 * @param path           the mount path used when creating the key. the original key can often be rebuilt by joining the path
	 *                       and relativeKey value with a '/'
	 * @param value          the unencrypted value we wish to write to the cypher {@link Datastore} and/or capture for configuration
	 * @param leaseTimeout   the user specified leaseTimeout in milliseconds for when this key will expire.
	 * @param leaseObjectRef an object reference that can be checked against instead of a temporal value for key invalidation
	 * @param createdBy      an object reference for a user that may have been used to create the key. Useful for permissions tracking
	 * @return a {@link CypherObject} that is then passed on to a {@link Datastore} encrypted for safe keeping.
	 */
	@Override
	public CypherObject write(String relativeKey, String path, String value, Long leaseTimeout, String leaseObjectRef, String createdBy) {
		String key = relativeKey;
		if(path != null) {
			key = path + "/" + key;
		}

		String[] pathArgs = relativeKey.split("/");
		if(pathArgs[0].equals("creds") && pathArgs.length == 3) {
			return new CypherObject(key,value,leaseTimeout,leaseObjectRef,createdBy);
		} else if(pathArgs[0].equals("roles") && pathArgs.length == 2) {
			 //oh we have some policy config likely in hcl
			return new CypherObject(key,value,leaseTimeout,leaseObjectRef,createdBy);
		}
		return null;
	}

	/**
	 * The read endpoint is called when a value for a key is requested by the user. Typically if a value is already saved
	 * and not expired in a {@link Datastore} then this endpoint is never reached. However if no value is stored for the
	 * requested key then it is possible to generate a new value here and optionally return a {@link CypherObject} that
	 * can be saved for future retrieval.
	 *
	 * @param relativeKey    the key passed excluding the mount point i.e. if mounted at '/secret' writing to '/secret/hello'
	 *                       would simply return 'hello'
	 * @param path           the mount path used when creating the key. the original key can often be rebuilt by joining the path
	 *                       and relativeKey value with a '/'
	 * @param leaseTimeout   the user specified leaseTimeout in milliseconds for when this key will expire.
	 * @param leaseObjectRef an object reference that can be checked against instead of a temporal value for key invalidation
	 * @param createdBy      an object reference for a user that may have been used to create the key. Useful for permissions tracking
	 * @return a {@link CypherObject} that is then passed on to a {@link Datastore} encrypted for safe keeping.
	 */
	@Override
	public CypherObject read(String relativeKey, String path, Long leaseTimeout, String leaseObjectRef, String createdBy) {
		// String key = relativeKey;
		// if(path != null) {
		// 	key = path + "/" + key;
		// }
		// if(relativeKey.startsWith("config")) {
		// 	return null;
		// }

		// String[] pathArgs = relativeKey.split("/");
		// if(pathArgs[0].equals("creds")) {
		// 	String userName = pathArgs[1];
		// 	String roleConfigPath = path + "/roles" + pathArgs[1];
		// 	CypherObject roleConfigResults = cypher.read(roleConfigPath);
		// 	if(roleConfigResults != null) {
		// 		Map<String,Object> baseConfig = loadConfig(null);

		// 		AmazonIdentityManagement idManagement = getAmazonIdentityManagementClient(baseConfig);

		// 		GetUserResult userResult = idManagement.getUser(new GetUserRequest().withUserName(userName));
		// 		if(userResult.getUser() != null) {
		// 			//user exists, just do some updates to arn config
		// 		} else {
		// 			// we need to create this user
		// 			CreateUserRequest createUserRequest = new CreateUserRequest().withUserName(userName);
		// 			CreatePolicyRequest createPolicyRequest = new CreatePolicyRequest().withPolicyName(userName).withPolicyDocument()
		// 		}
		// 		CreateAccessKeyRequest createRequest = new CreateAccessKeyRequest(userName);
		// 		CreateAccessKeyResult createResult = idManagement.createAccessKey(createRequest);
		// 		Map<String,String> resultSet = new HashMap<>();
		// 		resultSet.put("access_key",createResult.getAccessKey().getAccessKeyId());
		// 		resultSet.put("secret_key",createResult.getAccessKey().getSecretAccessKey());
		// 		AwsAccessKeyMeta persistedData = new AwsAccessKeyMeta();

		// 		String persistanceKey = path + "/" + relativeKey + "/" + UUID.randomUUID().toString();
		// 		persistedData.accessKey = createResult.getAccessKey().getAccessKeyId();
		// 		Gson gson = new GsonBuilder().create();


		// 		cypher.write(persistanceKey,gson.toJson(persistedData),leaseTimeout,leaseObjectRef,createdBy);

		// 		CypherObject resultObject =  new CypherObject(key,gson.toJson(resultSet),leaseTimeout,leaseObjectRef, createdBy);
		// 		resultObject.shouldPersist = false;
		// 		return resultObject;
		// 	} else {
		// 		//would be nice to throw like an invalid response back
		// 		return null;
		// 	}


//		}




		return null;
	}

	/**
	 * The delete endpoint is called when a user requests that a key be deleted or potentially called when a key has expired.
	 * This is useful to capture if one must make a remote API call to potentially delete any related data pertaining to the key.
	 *
	 * @param relativeKey the key passed excluding the mount point i.e. if mounted at '/secret' writing to '/secret/hello'
	 *                    would simply return 'hello'
	 * @param path        the mount path used when creating the key. the original key can often be rebuilt by joining the path
	 *                    and relativeKey value with a '/'
	 * @param object      the decryped {@link CypherObject} read from the {@link Datastore} in case any information encoded
	 *                    in the value is needed to perform proper removal operations.
	 * @return success or failure of delete request. If this returns false, then the key is not removed from the {@link Datastore}.
	 */
	@Override
	public boolean delete(String relativeKey, String path, CypherObject object) {
		if(relativeKey.startsWith("config")) {
			return true;
		}
		Map<String,Object> baseConfig = loadConfig(null);

		AmazonIdentityManagement idManagement = getAmazonIdentityManagementClient(baseConfig);

		if(object.value != null) {
			//TODO: Parse Value JSON and removeKey
			Gson gson = new GsonBuilder().create();

			AwsAccessKeyMeta accessKeyMeta = gson.fromJson(object.value,AwsAccessKeyMeta.class);
			String accessKey = accessKeyMeta.accessKey;
			String[] keyArgs = relativeKey.split("/");
			String userName = keyArgs[1];

			DeleteAccessKeyRequest accessKeyRequest = new DeleteAccessKeyRequest(userName,accessKey);

			idManagement.deleteAccessKey(accessKeyRequest);
			return true;
		}
		return true;
	}

	/**
	 * Returns a usage string useful as help when trying to figure out possible options or configuration that can be passed
	 * to a registered module in the {@link Cypher} instance.
	 *
	 * @return description of how this module can be used. It is possible to string replace the {mountPoint} pattern for usage examples.
	 */
	@Override
	public String getUsage() {
		return null;
	}

	/**
	 * Returns an HTML usage string useful as help when trying to figure out possible options or configuration that can be passed
	 * to a registered module in the {@link Cypher} instance.
	 *
	 * @return description of how this module can be used. It is possible to string replace the {mountPoint} pattern for usage examples.
	 */
	@Override
	public String getHTMLUsage() {
		return null;
	}


	private Map<String,Object> loadConfig(String role) {
		HCLParser parser = new HCLParser();
		CypherObject result;
		if(role != null) {
			result = cypher.read("amazon/config" + role);
		} else {
			result = cypher.read("amazon/config" + role);
		}
		try {
			if(result != null && result.value != null) {
				return parser.parse(result.value);
			} else {
				return null;
			}
		} catch(IOException ioe) {
			return null;
		} catch(HCLParserException hcle) {
			return null;
			//TODO: MAYBE LOG THIS
		}
	}

	private AmazonIdentityManagement getAmazonIdentityManagementClient(Map<String,Object> baseConfig) {
		String accessKey =(String)baseConfig.get("access_key");
		String secretKey =(String)baseConfig.get("secret_key");
		String region =(String)baseConfig.get("region");
		if(accessKey != null && secretKey != null && region != null) {
			AWSStaticCredentialsProvider creds = new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));

			return AmazonIdentityManagementClient.builder().withCredentials(creds).withRegion(region).build();
		} else {
			return null;
		}


	}
}