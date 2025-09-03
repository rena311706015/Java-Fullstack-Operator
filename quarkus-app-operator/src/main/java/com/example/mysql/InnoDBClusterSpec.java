package com.example.mysql;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InnoDBClusterSpec {
    private Integer instances;
    private String version;
    private String secretName;
    private Boolean tlsUseSelfSigned; 
    private Integer baseServerId;
    private RouterSpec router;

    public Integer getInstances() { return instances; }
    public void setInstances(Integer instances) { this.instances = instances; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; } 
    public String getSecretName() { return secretName; }
    public void setSecretName(String secretName) { this.secretName = secretName; }
    public Boolean getTlsUseSelfSigned() { return tlsUseSelfSigned; }
    public void setTlsUseSelfSigned(Boolean tlsUseSelfSigned) { this.tlsUseSelfSigned = tlsUseSelfSigned; }
    public Integer getBaseServerId() { return baseServerId; }
    public void setBaseServerId(Integer baseServerId) { this.baseServerId = baseServerId; }
    public RouterSpec getRouter() { return router;}
    public void setRouter(RouterSpec router) { this.router = router;}
}