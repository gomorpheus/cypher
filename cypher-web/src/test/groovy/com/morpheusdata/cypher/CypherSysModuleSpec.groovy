package com.morpheusdata.cypher

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.morpheusdata.cypher.datastores.FlatFileDatastore
import com.morpheusdata.cypher.model.Passwd
import com.morpheusdata.cypher.model.Token
import com.morpheusdata.cypher.modules.SysModule
import spock.lang.Shared
import spock.lang.Specification

import javax.crypto.KeyGenerator
import java.security.SecureRandom

/**
 * Test Specification for the SysModule and the token management behavior when storing in the /sys/passwd file
 */
class CypherSysModuleSpec extends Specification{
    @Shared
    Cypher cypher

    def setup() {
        SecureRandom rand = new SecureRandom();
        def keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256,rand);
        byte[] key = keyGenerator.generateKey().getEncoded()
        cypher = new Cypher("test",new FlatFileDatastore("./test-data"))
        cypher.registerModule("sys", new SysModule())
        cypher.setMasterKey(key);
    }

    void "it should be able to store token information in the passwd file"() {
        given:
        Token token = new Token(id: UUID.randomUUID().toString(), token: UUID.randomUUID().toString(), ttl: 36000, policies: ["default"])
        token.expires = new Date(new Date().time + 36000)
        cypher.write("sys/tokens/${token.id}",new ObjectMapper().writeValueAsString(token));
        when:
        def result = cypher.read("sys/passwd")
        List<Passwd> passwdData = new ObjectMapper().readValue(result.value, new TypeReference<List<Passwd>>(){})
        then:
        passwdData.any{it.id == token.id}
    }

    void "it should be able to store multiple token information records in the passwd file"() {
        given:
        Token token = new Token(id: UUID.randomUUID().toString(), token: UUID.randomUUID().toString(), ttl: 36000, policies: ["default"])
        token.expires = new Date(new Date().time + 36000)
        cypher.write("sys/tokens/${token.id}",new ObjectMapper().writeValueAsString(token));
        Token token2 = new Token(id: UUID.randomUUID().toString(), token: UUID.randomUUID().toString(), ttl: 36000, policies: ["default"])
        token2.expires = new Date(new Date().time + 36000)
        cypher.write("sys/tokens/${token2.id}",new ObjectMapper().writeValueAsString(token2));
        when:
        def result = cypher.read("sys/passwd")
        println("CypherObject: ${result.value}")
        List<Passwd> passwdData = new ObjectMapper().readValue(result.value, new TypeReference<List<Passwd>>(){})
        then:
        passwdData.any{it.id == token.id}
        passwdData.any{it.id == token2.id}
    }

    void "it should cleanup the passwd file on token removal"() {
        given:
        Token token = new Token(id: UUID.randomUUID().toString(), token: UUID.randomUUID().toString(), ttl: 36000, policies: ["default"])
        token.expires = new Date(new Date().time + 36000)
        cypher.write("sys/tokens/${token.id}",new ObjectMapper().writeValueAsString(token));
        Token token2 = new Token(id: UUID.randomUUID().toString(), token: UUID.randomUUID().toString(), ttl: 36000, policies: ["default"])
        token2.expires = new Date(new Date().time + 36000)
        cypher.write("sys/tokens/${token2.id}",new ObjectMapper().writeValueAsString(token2));
        cypher.delete("sys/tokens/${token.id}")
        when:
        def result = cypher.read("sys/passwd")
        println("CypherObject: ${result.value}")
        List<Passwd> passwdData = new ObjectMapper().readValue(result.value, new TypeReference<List<Passwd>>(){})
        then:
        !passwdData.any{it.id == token.id}
        passwdData.any{it.id == token2.id}
    }


    def cleanup() {
        new File("./test-data").listFiles().each { file ->
            file.delete()
        }
    }
}

