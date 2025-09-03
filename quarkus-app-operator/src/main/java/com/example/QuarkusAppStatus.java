package com.example;

public class QuarkusAppStatus {
    private boolean areResourcesReady;
    private boolean dbReady;
    private boolean backendReady;
    private boolean frontendReady;

    public boolean areResourcesReady() { return areResourcesReady; }
    public void setAreResourcesReady(boolean areResourcesReady) { this.areResourcesReady = areResourcesReady; }
    public boolean isDbReady() { return dbReady; }
    public void setDbReady(boolean dbReady) { this.dbReady = dbReady; }
    public boolean isBackendReady() { return backendReady; }
    public void setBackendReady(boolean backendReady) { this.backendReady = backendReady; }
    public boolean isFrontendReady() { return frontendReady; }
    public void setFrontendReady(boolean frontendReady) { this.frontendReady = frontendReady; }
}