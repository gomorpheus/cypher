package com.morpheusdata.cypher.modules;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.morpheusdata.cypher.Cypher;
import com.morpheusdata.cypher.CypherModule;
import com.morpheusdata.cypher.CypherObject;
import com.morpheusdata.cypher.util.RestApiUtil;
import com.morpheusdata.cypher.util.ServiceResponse;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;

public class VaultModule extends AbstractCypherModule {

    Cypher cypher;
    @Override
    public void setCypher(Cypher cypher) {
        this.cypher = cypher;
    }

    @Override
    public CypherObject write(String relativeKey, String path, String value, Long leaseTimeout, String leaseObjectRef, String createdBy) {
        if(value != null && value.length() > 0) {
            String key = relativeKey;
            if(path != null) {
                key = path + "/" + key;
            }
            if(relativeKey.startsWith("config/")) {
                System.out.println("Writing to : " + key);
                return new CypherObject(key,value,0l, leaseObjectRef, createdBy);
            } else {
                String vaultUrl = cypher.read("vault/config/url").value;
                String vaultToken = cypher.read("vault/config/token").value;

                //we gotta fetch from Vault
                String vaultPath="v1/" + relativeKey;
                //TODO: HTTP Client time
                //TODO: Send to Vault
                RestApiUtil.RestOptions restOptions = new RestApiUtil.RestOptions();
                restOptions.headers = new LinkedHashMap<>();
                restOptions.headers.put("X-VAULT-TOKEN",vaultToken);
                restOptions.contentType = "application/json";

                restOptions.body = "{\"data\": " + value + "}";
//                restOptions.body =
                try {
                    ServiceResponse resp = RestApiUtil.callApi(vaultUrl,vaultPath,null,null,restOptions,"POST");
                    if(resp.getSuccess()) {
                        return new CypherObject(key,value,leaseTimeout, leaseObjectRef, createdBy);
                    } else {
                        return null;
                    }
                } catch(Exception ex) {
                    return null;
                }
            }

        } else {
            return null; //we dont write no value to a key
        }
    }

    @Override
    public CypherObject read(String relativeKey, String path, Long leaseTimeout, String leaseObjectRef, String createdBy) {
        String key = relativeKey;
        if(path != null) {
            key = path + "/" + key;
        }
        if(relativeKey.startsWith("config/")) {
            return null;
        } else {
            String vaultUrl = cypher.read("vault/config/url").value;
            String vaultToken = cypher.read("vault/config/token").value;

            //we gotta fetch from Vault
            String vaultPath="/v1/" + relativeKey;
            RestApiUtil.RestOptions restOptions = new RestApiUtil.RestOptions();
            restOptions.headers = new LinkedHashMap<>();
            restOptions.headers.put("X-VAULT-TOKEN",vaultToken);
            restOptions.contentType = "application/json";
            try {
                ServiceResponse resp = RestApiUtil.callApi(vaultUrl,vaultPath,null,null,restOptions,"GET");
                if(resp.getSuccess()) {
                    ObjectMapper mapper = new ObjectMapper();
                    TypeReference<HashMap<String,Object>> typeRef
                            = new TypeReference<HashMap<String,Object>>() {};
                    HashMap<String,Object> responseJson = mapper.readValue(resp.getContent(), typeRef);
                    Map<String,Object> dataMap = (java.util.Map<String,Object>)(((java.util.Map<String,Object>)(responseJson.get("data"))).get("data"));
                    CypherObject vaultResult = new CypherObject(key,mapper.writeValueAsString(dataMap),leaseTimeout,leaseObjectRef, createdBy);
                    vaultResult.shouldPersist = false;
                    return vaultResult;
                } else {
                    return null;//throw exception?
                }
            } catch(Exception ex) {
                ex.printStackTrace();
                return null;
            }

            //TODO: HTTP Client time
        }
    }

    @Override
    public boolean delete(String relativeKey, String path, CypherObject object) {
        if(relativeKey.startsWith("config/")) {
            return true;
        } else {
            String vaultUrl = cypher.read("vault/config/url").value;
            String vaultToken = cypher.read("vault/config/token").value;

            //we gotta fetch from Vault
            String vaultPath="v1/" + relativeKey;
            //TODO: HTTP Client time
            RestApiUtil.RestOptions restOptions = new RestApiUtil.RestOptions();
            restOptions.headers = new LinkedHashMap<>();
            restOptions.headers.put("X-VAULT-TOKEN",vaultToken);
            restOptions.contentType = "application/json";
            try {
                RestApiUtil.callApi(vaultUrl,vaultPath,null,null,restOptions,"DELETE");
            } catch(Exception ex) {

            }
            return true;
        }
    }



    @Override
    public String getUsage() {
        StringBuilder usage = new StringBuilder();

        usage.append("This allows secret data to be fetched from a HashiCorp Vault integration. This can be configured in the vault/config key setup");

        return usage.toString();
    }

    @Override
    public String getHTMLUsage() {
        return null;
    }
}
