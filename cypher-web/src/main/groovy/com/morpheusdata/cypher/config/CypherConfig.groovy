package com.morpheusdata.cypher.config

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Context

@Context
@ConfigurationProperties('cypher')
@CompileStatic
class CypherConfig {
    String storageLocation
    String masterKey
}
