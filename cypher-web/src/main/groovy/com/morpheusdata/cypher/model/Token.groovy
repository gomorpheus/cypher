package com.morpheusdata.cypher.model

class Token {
    String id
    String token
    Long ttl
    Date expires
    Collection<String> policies
}
