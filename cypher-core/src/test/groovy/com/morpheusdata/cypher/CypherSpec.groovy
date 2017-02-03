package com.morpheusdata.cypher

import com.morpheusdata.cypher.datastores.FlatFileDatastore
import spock.lang.Shared
import spock.lang.Specification

import javax.crypto.KeyGenerator
import javax.xml.bind.DatatypeConverter
import java.security.SecureRandom

/**
 * Created by davydotcom on 1/31/17.
 */
class CypherSpec extends Specification{
	@Shared
	Cypher cypher

	def setup() {
		SecureRandom rand = new SecureRandom();
		def keyGenerator = KeyGenerator.getInstance("AES")
		keyGenerator.init(256,rand);
		byte[] key = keyGenerator.generateKey().getEncoded()
		cypher = new Cypher("test",new FlatFileDatastore("./test-data"))
		cypher.setMasterKey(key);
	}

	void "it should be able to store and retrieve secrets"() {
		given:
			String secret = "Hello David Estes"
			cypher.write("secret/hello",secret);
		when:
			def result = cypher.read("secret/hello")
		then:
			result?.value == secret
	}


	void "it should be able to store and retrieve a UUID"() {
		given:
		String secret = cypher.read("uuid/hello").value;

		when:
		def result = cypher.read("uuid/hello")
		then:
		result?.value == secret
	}



	void "it should be able to store and retrieve a key"() {
		given:
		String secret = cypher.read("key/hello/128").value;

		when:
		def result = cypher.read("key/hello/128")
		then:
		DatatypeConverter.parseBase64Binary(result?.value)?.size() == 16
		result?.value == secret
	}


	def cleanup() {
		new File("./test-data").listFiles().each { file ->
			file.delete()
		}
	}
}
