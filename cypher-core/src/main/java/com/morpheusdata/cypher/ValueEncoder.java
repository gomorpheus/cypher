package com.morpheusdata.cypher;

import com.morpheusdata.cypher.exception.EncoderException;

/**
 * Base interface for implementing a custom value encoder.
 * This is useful in the event the method of encryption needs to be swapped out. The default is {@link AESValueEncoder}.
 *
 * @author David Estes
 */
public interface ValueEncoder {
	/**
	 * Method provided to encrypt a given string into its encoded format based on a passed in Encryption Key
	 * @param encryptionKey the encryption key to be used when encoding the value
	 * @param value the given value that is wished for encryption
	 * @return an encrypted version of the passed in value that can now be used for persistence
	 */
	String encode(byte[] encryptionKey , String value) throws EncoderException;

	/**
	 * Method provided to encrypt a given string into its encoded format based on a passed in Encryption Key
	 * @param encryptionKey
	 * @param value
	 * @return
	 * @throws EncoderException
	 */
	byte[] encode(byte[] encryptionKey , byte[] value) throws EncoderException;

	/**
	 * Method provided for decrypting a previously encrypted value with the given encryption key
	 * @param encryptionKey the encryption key to be used when decoding the value
	 * @param encryptedValue the given encrypted value that one wishes to decrypt into its original value
	 * @return a decrypted version of the value passed in
	 */
	String decode(byte[] encryptionKey , String encryptedValue) throws EncoderException;

	/**
	 * Method provided for decrypting a previously encrypted value with the given encryption key
	 * @param encryptionKey
	 * @param encryptedValue
	 * @return
	 * @throws EncoderException
	 */
	byte[] decode(byte[] encryptionKey , byte[] encryptedValue) throws EncoderException;
}
