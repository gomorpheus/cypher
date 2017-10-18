package com.morpheusdata.cypher.modules;


import com.morpheusdata.cypher.CypherMeta;
import com.morpheusdata.cypher.CypherModule;
import com.morpheusdata.cypher.CypherObject;
import com.morpheusdata.cypher.exception.EncoderException;

import javax.crypto.KeyGenerator;
import javax.xml.bind.DatatypeConverter;
import java.security.SecureRandom;
import java.text.NumberFormat;

/**
 * Created by davydotcom on 2/3/17.
 */
public class RandomKeyModule implements CypherModule {

	public CypherObject write(String relativeKey, String path, String value, Long leaseTimeout, String leaseObjectRef, String createdBy) {
		return null;
	}

	public CypherObject read(String relativeKey, String path, Long leaseTimeout, String leaseObjectRef, String createdBy) {
		String key = relativeKey;
		if(path != null) {
			key = path + "/" + key;
		}
		String[] keyArgs = relativeKey.split("/");
		Integer length = 128;

		if(keyArgs.length > 1) {
			try {
				length = Integer.parseInt(keyArgs[0]);
			} catch(NumberFormatException ex) {
				//its ok we default to 16 anyway
			}
		}


		SecureRandom rand = new SecureRandom();
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(length,rand);
			byte[] generatedKey = keyGenerator.generateKey().getEncoded();
			System.out.println("Generating Key in Module of length: " + length + " with final size of " + generatedKey.length);
			String value = DatatypeConverter.printBase64Binary(generatedKey);
			return new CypherObject(key,value,leaseTimeout,leaseObjectRef, createdBy);
		} catch(Exception ex2) {
			throw new EncoderException("Error generating Encryption Key",ex2);
		}
	}

	public boolean delete(String relativeKey, String path, CypherObject object) {
		return true;
	}

	public String getUsage() {
		StringBuilder usage = new StringBuilder();

		usage.append("Generates a Base 64 encoded AES Key of specified bit length in the key pattern (i.e. {mountPoint}/128/mykey generates a 128-bit key)");

		return usage.toString();
	}

	public String getHTMLUsage() {
		StringBuilder usage = new StringBuilder();

		usage.append("<p>Generates a Base 64 encoded AES Key of specified bit length in the key pattern (i.e. {mountPoint}/128/mykey generates a 128-bit key)</p>");

		return usage.toString();
	}
}
