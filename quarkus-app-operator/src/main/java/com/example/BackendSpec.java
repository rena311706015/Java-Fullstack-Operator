package com.example;

public class BackendSpec {
    private String image;
    private int replicas;
    private int port;
    private String serviceType;

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public int getReplicas() { return replicas; }
    public void setReplicas(int replicas) { this.replicas = replicas; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
}
