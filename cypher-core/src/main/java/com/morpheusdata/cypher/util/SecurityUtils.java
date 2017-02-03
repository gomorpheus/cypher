package com.morpheusdata.cypher.util;

import java.security.SecureRandom;

/**
 * Created by davydotcom on 1/31/17.
 */
public class SecurityUtils {

	public static void secureErase(byte[] byteArray) {
		SecureRandom rand = new SecureRandom();
		rand.nextBytes(byteArray);
	}
}
