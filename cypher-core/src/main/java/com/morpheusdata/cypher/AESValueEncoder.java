package com.morpheusdata.cypher;

import com.morpheusdata.cypher.exception.EncoderException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import jakarta.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * This is the default {@link ValueEncoder} for Cypher. It presents an AES encryption back end to be used for
 * encrypting values.
 *
 * @author David Estes
 */

public class AESValueEncoder implements ValueEncoder {

	private Log log = LogFactory.getLog(AESValueEncoder.class);

	/**
	 * Encodes the given input value using AES encryption to a base64 encoded string. It is recommended
	 * that a 256-bit AES Encryption key be used for added security.
	 * @param encryptionKey the encryption key to be used when encoding the value
	 * @param value the given value that is wished for encryption
	 * @return String base64 encoded of the encrypted value
	 * @throws EncoderException
	 */
	@Override
	public String encode(byte[] encryptionKey, String value) throws EncoderException {
		try {
			return DatatypeConverter.printBase64Binary(encode(encryptionKey, value.getBytes("UTF-8")));
		} catch(UnsupportedEncodingException ex) {
			throw new EncoderException("Unsupported Encoding of String value. UTF-8 is required",ex);
		}
	}

	@Override
	public byte[] encode(byte[] encryptionKey, byte[] value) throws EncoderException {
		byte[] output;
		try {
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"));
			output = c.doFinal(value);
		} catch(NoSuchAlgorithmException algorithmException) {
			throw(new EncoderException("The AES Algorithm cannot be loaded.",algorithmException));
		} catch(InvalidKeyException invalidKeyException) {
			if(encryptionKey.length > 16) {
				log.warn("Current Security Policies for this JRE Do not support " + (encryptionKey.length*8) + "-bit encryption keys... truncating to 128.");
				return encode(truncateBytes(encryptionKey,16), value);
			}
			throw(new EncoderException("The passed in encryption key appears to be invalid.",invalidKeyException));
		} catch(NoSuchPaddingException paddingException) {
			throw(new EncoderException("Error in Cipher padding", paddingException));
		} catch(Exception ex) {
			throw(new EncoderException("Generic Exception occurred while attempting to encode value", ex));
		}
		return output;
	}

	/**
	 * Decodes an AES base64 encrypted string value using the given encryption key
	 *
	 * @param encryptionKey the encryption key to be used when decoding the value
	 * @param encryptedValue the given base64 encrypted value that one wishes to decrypt into its original value
	 * @return The decoded String output of the value
	 * @throws EncoderException
	 */
	@Override
	public String decode(byte[] encryptionKey, String encryptedValue) throws EncoderException {
		return new String(decode(encryptionKey,DatatypeConverter.parseBase64Binary(encryptedValue)), StandardCharsets.UTF_8);
	}

	@Override
	public byte[] decode(byte[] encryptionKey, byte[] encryptedValue) throws EncoderException {
		byte[] output;
		try {
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"));
			output = c.doFinal(encryptedValue);
		} catch(NoSuchAlgorithmException algorithmException) {
			throw(new EncoderException("The AES Algorithm cannot be loaded.",algorithmException));
		} catch(InvalidKeyException invalidKeyException) {
			if(encryptionKey.length > 16) {
				log.warn("Current Security Policies for this JRE Do not support " + (encryptionKey.length*8) + "-bit encryption keys... truncating to 128.");
				return decode(truncateBytes(encryptionKey,16), encryptedValue);
			}
			throw(new EncoderException("The passed in encryption key appears to be invalid.",invalidKeyException));
		} catch(BadPaddingException paddingException) {
			throw(new EncoderException("Error in Cipher padding", paddingException));
		} catch(Exception ex) {
			throw(new EncoderException("Generic Exception occurred while attempting to encode value", ex));
		}
		return output;
	}

	private byte[] truncateBytes(byte[] byteArray, int length) {
		byte[] output = new byte[length];

		for(int x=0;x<length;x++) {
			output[x] = byteArray[x];
		}
		return output;
	}
}
