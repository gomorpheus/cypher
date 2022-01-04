package com.morpheusdata.cypher.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.morpheusdata.cypher.Cypher
import com.morpheusdata.cypher.CypherObject
import com.morpheusdata.cypher.config.CypherConfig
import com.morpheusdata.cypher.datastores.FlatFileDatastore
import com.morpheusdata.cypher.model.Policy
import com.morpheusdata.cypher.model.PolicyInfo
import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Slf4j
@Singleton
class CypherService {
    @Inject CypherConfig cypherConfig
    Cypher cypher

    List<String> listKeys(String vaultToken, String pattern) {
        String policyName = getPolicyForToken(vaultToken)
        if(isAccessAllowed(pattern,policyName,'list')) {
            return cypher.listKeys(pattern)
        } else {
            //TODO: Throw Exception for Access Denied?
            return null
        }
    }

    CypherObject read(String vaultToken, String key, Long leaseTimeout = null, String createdBy = null) {
        String policyName = getPolicyForToken(vaultToken)
        if(isAccessAllowed(key,policyName,'read')) {
            CypherObject cypherObject = cypher.read(key,leaseTimeout, createdBy)
            if(createdBy && cypherObject.createdBy && cypherObject.createdBy != createdBy) {
                return null
            }
            return cypherObject
        } else {
            log.info(":Access Denied)")
            //TODO: Throw Exception for Access Denied?
            return null
        }
    }

    CypherObject write(String vaultToken, String key, String value, Long leaseTimeout = null, String createdBy = null) {
        String policyName = getPolicyForToken(vaultToken)
        if(isAccessAllowed(key,policyName,'write')) {
            return cypher.write(key, value, leaseTimeout, null, createdBy)
        } else {
            log.info("Access Denied")
            //TODO: Throw Exception for Access Denied?
            return null
        }
    }

    String readValue(String key, Long leaseTimeout = null, String createdBy = null) {
        read(key, leaseTimeout, createdBy)?.value
    }

    Boolean delete(String vaultToken, String key) {
        String policyName = getPolicyForToken(vaultToken)
        if(isAccessAllowed(key,policyName,'delete')) {
            return cypher.delete(key)
        } else {
            return false
        }
    }

    String getPolicyForToken(String vaultToken) {
        String tokenInfo = cypher.read("sys/tokens/${vaultToken}")?.value
        //TODO: Token info model
        return 'default'
    }

    Boolean isAccessAllowed(String path, String policyName, String accessType) {
        ObjectMapper objectMapper = new ObjectMapper();
        if(path.startsWith('sys/')) {
            accessType = 'sudo'
        }
        String policyString = cypher.read("sys/policies/${policyName}")?.value
        Policy policy = null
        if(!policyString && policyName == 'default') {
            //Default isnt set so assume full access for default policy
            policy = new Policy()
            PolicyInfo policyInfo = new PolicyInfo()
            policyInfo.capabilities = ["read","write","list","delete","update"]
            policy.path = ['/.*': policyInfo]
        } else if (policyString) {
            policy = objectMapper.readValue(policyString, Policy)
        } else {
            return false
        }
        if(policy) {
            log.info("Policy Keyset: ${policy.path.keySet()}")
            String bestMatch = matchPolicyPath(path,policy.path.keySet())
            log.info("Best Match: ${bestMatch}")
            if(bestMatch) {
                PolicyInfo info = policy.path[bestMatch]
                if(info.capabilities.contains(accessType)) {
                    return true
                } else {
                    return false
                }
            } else {
                return false
            }
        } else {
            return false
        }

    }

    protected String matchPolicyPath(String path, Collection<String> pathPatterns) {
        String matchedPattern
        pathPatterns.each { pattern ->
            String testPattern = pattern
            if(pattern.startsWith("/")) {
                testPattern = pattern.substring(1)
            }
            if(path.matches(testPattern)) {
                if(matchedPattern) {
                    if(pattern.size() > matchedPattern) {
                        matchedPattern = pattern
                    }
                } else {
                    matchedPattern = pattern
                }

            }
        }
        return matchedPattern
    }


    /**
     * Parse ttl seconds with support for human readable values eg. 24h, 60m, 30d, 3M, 5y
     * The default unit is seconds. 0 is allowed.
     * @param ttl the time to live
     * @return Long seconds or null
     * todo: make smarter and parse multiple units too eg. 1h30m
     */
    Long parseLeaseSeconds(val) {
        Long seconds = 0L
        def ttl = val?.toString()?.trim() ?: ''
        // try {
        if (ttl == '' || ttl == '0' || ttl == '-1') {
            // seconds = 0
            return 0
        } else if (ttl.contains('millisecond')) {
            ttl = ttl.replace('millisecond', '').trim()
            seconds = ttl.toFloat() / 1000
        } else if (ttl.contains('second')) {
            ttl = ttl.replace('second', '').replace('s','').trim()
            seconds = ttl.toFloat() * 1
        } else if (ttl.contains('minute')) {
            ttl = ttl.replace('minute', '').replace('s','').trim()
            seconds = ttl.toFloat() * (60)
        } else if (ttl.contains('hour')) {
            ttl = ttl.replace('hour', '').replace('s','').trim()
            seconds = ttl.toFloat() * (60*60)
        } else if (ttl.contains('day')) {
            ttl = ttl.replace('day', '').replace('s','').trim()
            seconds = ttl.toFloat() * (60*60*24)
        } else if (ttl.contains('month')) {
            ttl = ttl.replace('month', '').replace('s','').trim()
            //  why is month 32 days? how about actually using a dang calendar
            seconds = ttl.toFloat() * (60*60*24*32)
        } else if (ttl.contains('year')) {
            ttl = ttl.replace('year', '').replace('s','').trim()
            seconds = ttl.toFloat() * (60*60*24*365)
        } else if (ttl.endsWith('ms')) {
            ttl = ttl.substring(0, ttl.size()-2)
            seconds = ttl.toFloat() / 1000
        } else if (ttl.endsWith('s')) {
            ttl = ttl.substring(0, ttl.size()-1)
            seconds = ttl.toFloat() * 1
        } else if (ttl.endsWith('m')) {
            ttl = ttl.substring(0, ttl.size()-1)
            seconds = ttl.toFloat() * (60)
        } else if (ttl.endsWith('h')) {
            ttl = ttl.substring(0, ttl.size()-1)
            seconds = ttl.toFloat() * (60*60)
        } else if (ttl.endsWith('d')) {
            ttl = ttl.substring(0, ttl.size()-1)
            seconds = ttl.toFloat() * (60*60*24)
        } else if (ttl.endsWith('M')) {
            ttl = ttl.substring(0, ttl.size()-1)
            //  why is month 32 days? how about actually using a dang calendar
            seconds = ttl.toFloat() * (60*60*24*32)
        } else if (ttl.endsWith('y')) {
            ttl = ttl.substring(0, ttl.size()-1)
            seconds = ttl.toFloat() * (60*60*24*365)
        } else {
            seconds = ttl.toFloat()
        }
        // } catch(ex) {
        // 	// failed to parse ttl as a number of seconds
        // }

        return seconds?.toLong()
    }

    @PostConstruct
    void initializeCypherEngine() {
        cypher = new Cypher("cypher", new FlatFileDatastore(cypherConfig.storageLocation))
        cypher.setMasterKey(cypherConfig.masterKey)
    }
}
