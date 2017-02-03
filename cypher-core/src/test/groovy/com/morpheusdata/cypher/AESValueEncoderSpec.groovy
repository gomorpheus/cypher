/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.morpheus.sdk

import com.morpheusdata.cypher.AESValueEncoder
import spock.lang.Specification

import javax.crypto.KeyGenerator
import java.security.SecureRandom

/**
 * @author David Estes
 */
class AESValueEncoderSpec extends Specification {
	

	def setup() {

	}

	def cleanup() {

	}

	void "it should successfully encrypt using 256-bit key and decrypt to the original value"() {
		given:
			def encoder = new AESValueEncoder()
			SecureRandom rand = new SecureRandom();
			def keyGenerator = KeyGenerator.getInstance("AES")
			keyGenerator.init(256,rand);
			//keyGenerator.init(128);
			byte[] key = keyGenerator.generateKey().getEncoded()
			println("Key: ${key.encodeBase64()}")
			String value = "Secret, Secret. I've got a secret!"

		when:
			String encryptedValue = encoder.encode(key,value)
			println "Encrypted Value: ${encryptedValue}"
			String decodedValue = encoder.decode(key,encryptedValue)
		then:
			decodedValue == value
	}
}
