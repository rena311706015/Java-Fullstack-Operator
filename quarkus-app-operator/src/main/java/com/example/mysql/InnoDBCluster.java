package com.example.mysql;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("mysql.oracle.com")
@Version("v2")
public class InnoDBCluster extends CustomResource<InnoDBClusterSpec, InnoDBClusterStatus> implements Namespaced {}