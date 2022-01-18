package com.morpheusdata.cypher.controller

import com.bertramlabs.plugins.hcl4j.HCLParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.morpheusdata.cypher.CypherObject
import com.morpheusdata.cypher.model.CypherItem
import com.morpheusdata.cypher.model.CypherItemObject
import com.morpheusdata.cypher.model.CypherItemString
import com.morpheusdata.cypher.model.CypherList
import com.morpheusdata.cypher.model.UnauthorizedException
import com.morpheusdata.cypher.service.CypherService
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import jakarta.inject.Inject

@Slf4j
@Controller("/v1/")
class CypherController {
    @Inject CypherService cypherService

    @Get("/{?list}")
    HttpResponse<CypherList> index(HttpRequest request, @QueryValue @Nullable Boolean list) {

        String token = getToken(request)
        if(list) {
            CypherList cypherList = new CypherList()
            cypherList.keys = cypherService.listKeys(token,"/")
            return HttpResponse.ok(cypherList)
        } else {
            show(request,"")
        }
    }

    @Get("{/path:.*}")
    HttpResponse<CypherItem> show(HttpRequest request, @Nullable @PathVariable String path, @Nullable @QueryValue Long leaseTimeout, @Nullable @QueryValue String ttl) {
        String token = getToken(request)
        Long parsedLeaseTimeout = parseLeaseTimeout(leaseTimeout,ttl)
        try {
            CypherObject cypherObject = cypherService.read(token,path,parsedLeaseTimeout)
            Long calculatedLeaseTimeout = cypherObject?.leaseTimeout
            if(cypherObject) {
                def cypherData = parseCypherData(cypherObject)
                if(cypherData.dataType == 'string') {
                    return HttpResponse.ok(new CypherItemString(dataType: cypherData.dataType, data: cypherData.data as String, leaseTimeout: calculatedLeaseTimeout))

                } else {
                    return HttpResponse.ok(new CypherItemObject(dataType: cypherData.dataType, data: cypherData.data, leaseTimeout: calculatedLeaseTimeout))
                }
            } else {
                return HttpResponse.notFound()
            }
        } catch(UnauthorizedException unauthorizedE) {
            return HttpResponse.unauthorized()
        }



    }

    @Post("{/path:.+}{?leaseTimeout,ttl,type}")
    HttpResponse<CypherItem> save(HttpRequest request, @Nullable @PathVariable String path, @Nullable @QueryValue Long leaseTimeout, @Nullable @QueryValue String ttl, @Nullable @QueryValue String type, @Body @Nullable String bodyText) {
        String token = getToken(request)
        try {
            if(request.getContentType().get().toString() == 'application/hcl') {

                Map hclParsedRequest = new HCLParser().parse(bodyText)
                ObjectMapper mapper = new ObjectMapper()
                String jsonBody = mapper.writeValueAsString(hclParsedRequest)
                Long parsedLeaseTimeout = parseLeaseTimeout(leaseTimeout,ttl ?: (hclParsedRequest?.ttl as String))

            } else if(request.getContentType().get().toString() == 'application/json') {
                def jsonBody = new JsonSlurper().parseText(bodyText)
                Long parsedLeaseTimeout = parseLeaseTimeout(leaseTimeout,ttl ?: (jsonBody?.ttl as String))
                String dataType
                String keyValue
                if (type == 'string') {
                    dataType = 'string'
                    if (jsonBody?.value) {
                        keyValue = jsonBody?.value?.toString()
                    } else if (jsonBody?.data) {
                        keyValue = jsonBody?.data?.toString()
                    }
                } else if (jsonBody != null) {
                    dataType = 'object'
                    keyValue = new JsonOutput().toJson(jsonBody)
                }
                CypherObject cypherObject = cypherService.write(token,path,keyValue,parsedLeaseTimeout)
                Long calculatedLeaseTimeout = cypherObject.leaseTimeout
                def cypherData = parseCypherData(cypherObject)
                if(cypherData.dataType == 'string') {
                    return HttpResponse.ok(new CypherItemString(dataType: cypherData.dataType, data: cypherData.data as String, leaseTimeout: calculatedLeaseTimeout))

                } else {
                    return HttpResponse.ok(new CypherItemObject(dataType: cypherData.dataType, data: cypherData.data, leaseTimeout: calculatedLeaseTimeout))
                }
            }
        } catch(UnauthorizedException unauthorizedE) {
         return HttpResponse.unauthorized()
        }
    }

    @Delete("{/path:.+}")
    HttpResponse delete(HttpRequest request, @Nullable @PathVariable String path) {
        String token = getToken(request)
        cypherService.delete(token,path)
        return HttpResponse.ok()
    }

    protected String getToken(HttpRequest request) {
        String token = request.getHeaders().get("X-Cypher-Token")
        if(!token) {
            token = request.getHeaders().get("X-Vault-Token")
        }
        return token
    }

    /**
     * Serialize the cypher object value which depends on type.
     * this tries to convert strings that look like JSON to a JSONObject
     * so it can be rendered as such.
     * @return Map like [dataType: "string": data: "a secret"]
     *               or [dataType: "object": data: {"value":"hello world"}]
     */
    protected parseCypherData(cypherObject) {
        def rtn = [dataType: "string", data: null]
        def data = cypherObject?.value
        if (data instanceof String) {
            if (data.startsWith('{') && data.endsWith('}')) {
                try {

                    rtn.data = new JsonSlurper().parseText(data)
                    rtn.dataType = "object"
                } catch (ex) {
                    log.warn("Cypher value could not be decrypted as JSON. Error: ${ex.message}")
                    //data = decryptedValue
                }
            } else {
                rtn.data = data
            }
        }
        return rtn
    }

    private Long parseLeaseTimeout(Long leaseTimeout, String ttl) {
        if (leaseTimeout) {
           return leaseTimeout
        } else if (ttl) {
            def ttlValue = ttl
            try {
                def leaseSeconds = cypherService.parseLeaseSeconds(ttlValue)
                leaseTimeout = leaseSeconds ? (leaseSeconds * 1000).toLong() : 0
            } catch(e) {
                log.error("TTL parse error: ${e}", e)
                log.info("Failed to parse ttl '${ttlValue}'. Using the default: 0 (never expires).")
                leaseTimeout = 0
            }
        }
        return leaseTimeout
    }
}
