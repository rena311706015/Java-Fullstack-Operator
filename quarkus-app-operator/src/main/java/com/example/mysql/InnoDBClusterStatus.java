package com.example.mysql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) 
public class InnoDBClusterStatus {

    private String phase;
    private String message;
    private ClusterStatus cluster;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClusterStatus {
        private String status;
        private int primaryInstance;
        private int instances;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getPrimaryInstance() { return primaryInstance; }
        public void setPrimaryInstance(int primaryInstance) { this.primaryInstance = primaryInstance; }
        public int getInstances() { return instances; }
        public void setInstances(int instances) { this.instances = instances; }
    }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public ClusterStatus getCluster() { return cluster; }
    public void setCluster(ClusterStatus cluster) { this.cluster = cluster; }
}