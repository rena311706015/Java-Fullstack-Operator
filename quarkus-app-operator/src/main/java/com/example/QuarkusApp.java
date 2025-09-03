package com.example;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("com.example")
@Version("v1alpha1")
public class QuarkusApp extends CustomResource<QuarkusAppSpec, QuarkusAppStatus> implements Namespaced {}