package com.morpheusdata.cypher.modules;

import com.morpheusdata.cypher.Cypher;
import com.morpheusdata.cypher.CypherMeta;
import com.morpheusdata.cypher.CypherModule;
import com.morpheusdata.cypher.CypherObject;
import java.security.SecureRandom;

/**
 *
 * @author David Estes
 */
public class PasswordModule extends AbstractCypherModule {
	private CypherMeta cypherMeta;

	private String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~`!@#%^&*()-_=+[{]}\\|;:,<.>/";
	private String symbols = "~`!@#%^&*()-_=+[{]}\\|;:,<.>/";
	private String upperCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private String numbers = "0123456789";

	/**
	 * Some Modules need the cypher object to query config data. This allows it to be assigned on the constructor
	 *
	 * @param cypher
	 */
	@Override
	public void setCypher(Cypher cypher) {

	}

	public CypherObject write(String relativeKey, String path, String value, Long leaseTimeout, String leaseObjectRef, String createdBy) {
		if(value != null && value.length() > 0) {
			String key = relativeKey;
			if(path != null) {
				key = path + "/" + key;
			}
			return new CypherObject(key,value,leaseTimeout, leaseObjectRef, createdBy);
		} else {
			return null; //we dont write no value to a key
		}
	}

	public CypherObject read(String relativeKey, String path, Long leaseTimeout, String leaseObjectRef, String createdBy) {
		String key = relativeKey;
		if(path != null) {
			key = path + "/" + key;
		}
		String[] keyArgs = relativeKey.split("/");
		Integer length = 15;

		if(keyArgs.length > 1) {
			try {
				length = Integer.parseInt(keyArgs[0]);
			} catch(NumberFormatException ex) {
				//its ok we default to 16 anyway
			}
		}
		return new CypherObject(key,generateRandomPassword(length),leaseTimeout,leaseObjectRef, createdBy);

	}

	public boolean delete(String relativeKey, String path, CypherObject object) {
		return true;
	}

	public String getUsage() {
		StringBuilder usage = new StringBuilder();

		usage.append("Generates a secure password of specified character length in the key pattern (or 15) with symbols, numbers, upper case, and lower case letters (i.e. {mountPoint}/15/mypass generates a 15 character password).");

		return usage.toString();
	}

	public String getHTMLUsage() {
		StringBuilder usage = new StringBuilder();

		usage.append("<p>Generates a secure password of specified character length in the key pattern (or 15) with symbols, numbers, upper case, and lower case letters (i.e. {mountPoint}/15/mypass generates a 15 character password).</p>");

		return usage.toString();
	}

	protected String generateRandomPassword(Integer length) {
		if(length == null) {
			length = 15;
		}
		SecureRandom rand = new SecureRandom();
		StringBuilder password = new StringBuilder(length);
		boolean numberFound = false;
		boolean upperFound = false;
		boolean symbolFound = false;
		int suffixPad = 3;
		for(int x=0;x<length ; x++) {
			if(length-1-x < suffixPad) {
				if(!upperFound ) {
					upperFound = true;
					suffixPad--;
					char c = upperCharacters.charAt(rand.nextInt(upperCharacters.length()));
					password.append(c);
					continue;
				}
				if(!symbolFound) {
					symbolFound = true;
					suffixPad--;
					char c = symbols.charAt(rand.nextInt(symbols.length()));
					password.append(c);
					continue;
				}
				if(!numberFound) {
					numberFound = true;
					suffixPad--;
					char c = numbers.charAt(rand.nextInt(numbers.length()));
					password.append(c);
					continue;
				}
			}
			char c = characters.charAt(rand.nextInt(characters.length()));
			password.append(c);


			if(!upperFound && upperCharacters.indexOf(c) > -1) {
				upperFound = true;
				suffixPad--;
			}
			if(!symbolFound && symbols.indexOf(c) > -1) {
				symbolFound = true;
				suffixPad--;
			}
			if(!numberFound && numbers.indexOf(c) > -1) {
				numberFound = true;
				suffixPad--;
			}

		}

		return password.toString();
	}

}
