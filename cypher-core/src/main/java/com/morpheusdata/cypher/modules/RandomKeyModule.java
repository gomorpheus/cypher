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

	public CypherObject write(String relativeKey, String path, String value, Long leaseTimeout) {
		return null;
	}

	public CypherObject read(String relativeKey, String path, Long leaseTimeout) {
		String key = relativeKey;
		if(path != null) {
			key = path + "/" + key;
		}
		String[] keyArgs = relativeKey.split("/");
		Integer length = 128;

		if(keyArgs.length > 1) {
			try {
				length = Integer.parseInt(keyArgs[keyArgs.length - 1]);
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
			return new CypherObject(key,value,leaseTimeout);
		} catch(Exception ex2) {
			throw new EncoderException("Error generating Encryption Key",ex2);
		}
	}

	public boolean delete(String relativeKey, String path, CypherObject object) {
		return true;
	}

}
