package com.morpheusdata.cypher

import com.fasterxml.jackson.databind.ObjectMapper
import com.morpheusdata.cypher.datastores.FlatFileDatastore
import groovy.json.JsonSlurper
import jakarta.xml.bind.DatatypeConverter
import spock.lang.Shared
import spock.lang.Specification

import javax.crypto.KeyGenerator
import java.security.SecureRandom

class VaultModuleSpec extends Specification {
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

    void "it should be able to store and retrieve vault config"() {
        given:
        cypher.write("vault/config/token","s.vCIMpxGVFfOuIZwhPqGSTLat")
        cypher.write("vault/config/url","http://127.0.0.1:8200")

        when:
        def result = cypher.read("vault/config/token")
        println("Result: ${result}")
        then:
        result?.value == "s.vCIMpxGVFfOuIZwhPqGSTLat"
    }

    void "it should be able to store and retrieve secrets from vault"() {
        given:
        cypher.write("vault/config/token","s.vCIMpxGVFfOuIZwhPqGSTLat")
        cypher.write("vault/config/url","http://127.0.0.1:8200")
        when:
        cypher.write("vault/secret/data/cypher-secret","{\"value\":\"test\"}")
        def result = cypher.read("vault/secret/data/cypher-secret")
        def data = new ObjectMapper().readValue(result.value,Map)
        then:
        data.value == "test"
    }


    def cleanup() {
        new File("./test-data").listFiles().each { file ->
            file.delete()
        }
    }
}
