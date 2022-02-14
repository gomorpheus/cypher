package com.morpheusdata.cypher.util;

import java.util.Base64;

/**
 * Created by Dustin DeYoung on 2/14/2022.
 */
public class DatatypeConverterUtil {

	public static byte[] parseBase64Binary(String value) {
		return Base64.getDecoder().decode(value);
	}
	
	public static String printBase64Binary(byte[] val) {
		return Base64.getEncoder().encodeToString(val);
	}
	
}
