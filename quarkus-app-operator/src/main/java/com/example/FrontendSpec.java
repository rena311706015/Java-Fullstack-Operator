package com.example;

public class FrontendSpec {
    private String image;
    private ReplicaSpec replicas;
    private int cpuThreshold;
    private int port;
    private String serviceType;

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public ReplicaSpec getReplicas() { return replicas; }
    public void setReplicas(ReplicaSpec replicas) { this.replicas = replicas; }
    public int getCpuThreshold() { return cpuThreshold; }
    public void setCpuThreshold(int cpuThreshold) { this.cpuThreshold = cpuThreshold; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
}