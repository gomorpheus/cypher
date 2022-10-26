package com.morpheusdata.cypher.modules

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.morpheusdata.cypher.Cypher
import com.morpheusdata.cypher.CypherModule
import com.morpheusdata.cypher.CypherObject
import com.morpheusdata.cypher.model.Passwd
import com.morpheusdata.cypher.model.Token
import groovy.util.logging.Slf4j

@Slf4j
class SysModule implements CypherModule {
    private Cypher cypher;

    @Override
    void setCypher(Cypher cypher) {
        this.cypher = cypher;
    }

    @Override
    CypherObject write(String relativeKey, String path, String value, Long leaseTimeout, String leaseObjectRef, String createdBy) {
        if(relativeKey.startsWith('tokens/')) {
            writeTokenToPasswd(value)
        }
        String key = relativeKey;
        if(path != null) {
            key = path + "/" + key;
        }
        return new CypherObject(key,value,leaseTimeout, leaseObjectRef, createdBy);
    }

    @Override
    CypherObject read(String relativeKey, String path, Long leaseTimeout, String leaseObjectRef, String createdBy) {
        return null
    }

    @Override
    boolean delete(String relativeKey, String path, CypherObject object) {
        if(relativeKey.startsWith('tokens/')) {
            //we need to also cleanup the passwd file
            String[] keyArgs = relativeKey.tokenize('/')
            String tokenId = keyArgs[-1]
            removeTokenFromPasswd(tokenId)
        }
        return true
    }

    @Override
    String getUsage() {
        return null
    }

    @Override
    String getHTMLUsage() {
        return null
    }
    
    @Override 
    Boolean alwaysRead() {
      return false 
    }

    protected writeTokenToPasswd(String tokenValue) {
        CypherObject passwdData = cypher.read("sys/passwd")
        Token token = new ObjectMapper().readValue(tokenValue,Token)
        List<Passwd> passwdInfo = []
        if(passwdData && passwdData.value) {
            passwdInfo = new ObjectMapper().readValue(passwdData.value, new TypeReference<List<Passwd>>(){})
        }
        Passwd existingEntry = passwdInfo.find{it.id == token.id}
        if(existingEntry) {
            existingEntry.token = token.token
        } else {
            passwdInfo.add(new Passwd(id: token.id, token:token.token))
        }
        String passwdValue = new ObjectMapper().writeValueAsString(passwdInfo)
        cypher.write("sys/passwd",passwdValue,0,null,null)
    }

    protected removeTokenFromPasswd(String tokenId) {
        CypherObject passwdData = cypher.read("sys/passwd")
        List<Passwd> passwdInfo = []
        if(passwdData && passwdData.value) {
            passwdInfo = new ObjectMapper().readValue(passwdData.value, new TypeReference<List<Passwd>>(){})
        }
        Passwd existingEntry = passwdInfo.find{it.id == tokenId}
        if(existingEntry) {
            passwdInfo.remove(existingEntry)
            String passwdValue = new ObjectMapper().writeValueAsString(passwdInfo)
            cypher.write("sys/passwd",passwdValue,0,null,null)
        }
    }
}
