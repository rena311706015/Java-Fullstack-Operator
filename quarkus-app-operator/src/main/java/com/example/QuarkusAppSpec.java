package com.example;

import com.example.mysql.InnoDBClusterSpec;

public class QuarkusAppSpec {
    private FrontendSpec frontend;
    private BackendSpec backend;
    private InnoDBClusterSpec database;
    private IngressSpec ingress;

    public FrontendSpec getFrontend() { return frontend; }
    public void setFrontend(FrontendSpec frontend) { this.frontend = frontend; }
    public BackendSpec getBackend() { return backend; }
    public void setBackend(BackendSpec backend) { this.backend = backend; }
    public InnoDBClusterSpec getDatabase() { return database; }
    public void setDatabase(InnoDBClusterSpec database) { this.database = database; }
    public IngressSpec getIngress() { return ingress; }
    public void setIngress(IngressSpec ingress) { this.ingress = ingress; }
}