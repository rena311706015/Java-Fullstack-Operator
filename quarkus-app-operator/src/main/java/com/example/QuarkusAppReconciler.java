package com.example;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.example.mysql.InnoDBCluster;
import com.example.mysql.InnoDBClusterSpec;

import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscalerBuilder;
import io.fabric8.kubernetes.api.model.autoscaling.v2.MetricSpecBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPathBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackendBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRuleBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.ServiceBackendPortBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.config.informer.InformerEventSourceConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import jakarta.inject.Inject;

@ControllerConfiguration
public class QuarkusAppReconciler implements Reconciler<QuarkusApp>{

    @Inject
    KubernetesClient client;

    @Override
    public UpdateControl<QuarkusApp> reconcile(QuarkusApp resource, Context<QuarkusApp> context) {
        final String namespace = resource.getMetadata().getNamespace();
        final String appName = resource.getMetadata().getName();
        final QuarkusAppSpec spec = resource.getSpec();
        QuarkusAppStatus status = getOrInitializeStatus(resource);

        reconcileInnoDBCluster(appName, namespace, spec.getDatabase(), resource);
        Optional<InnoDBCluster> dbClusterOpt = context.getSecondaryResource(InnoDBCluster.class);
        if (dbClusterOpt.isPresent() &&
            dbClusterOpt.get().getStatus() != null &&
            dbClusterOpt.get().getStatus().getCluster() != null &&
            "ONLINE".equalsIgnoreCase(dbClusterOpt.get().getStatus().getCluster().getStatus())) {
            
            status.setDbReady(true);

        } else {            
            System.out.println("DB is not ready, reconciling later...");
            status.setDbReady(false);
            return UpdateControl.patchStatus(resource).rescheduleAfter(15, TimeUnit.SECONDS);
        }

        if (status.isDbReady()) {
            reconcileBackendService(appName, namespace, spec.getBackend(), resource);
            reconcileBackendDeployment(appName, namespace, spec.getBackend(), spec.getDatabase(), resource);

            reconcileFrontendService(appName, namespace, spec.getFrontend(), resource);
            reconcileFrontendDeployment(appName, namespace, spec.getFrontend(), resource);
            reconcileFrontendHpa(appName, namespace, spec.getFrontend(), resource);
            
            reconcileIngress(appName, namespace, spec, resource);
            status.setAreResourcesReady(true);
        }
        reconcileIngress(appName, namespace, spec, resource);

        return UpdateControl.patchStatus(resource);
    }

    private QuarkusAppStatus getOrInitializeStatus(QuarkusApp resource) {
        if (resource.getStatus() == null) {
            resource.setStatus(new QuarkusAppStatus());
        }
        return resource.getStatus();
    }

    private void reconcileInnoDBCluster(String appName, String namespace, InnoDBClusterSpec dbSpec, QuarkusApp owner) {
        String clusterName = appName + "-db-cluster";

        InnoDBClusterSpec icSpec = new InnoDBClusterSpec();
        icSpec.setInstances(dbSpec.getInstances());
        icSpec.setVersion(dbSpec.getVersion());
        icSpec.setSecretName(dbSpec.getSecretName()); 
        icSpec.setRouter(dbSpec.getRouter());
        icSpec.setTlsUseSelfSigned(true);

        InnoDBCluster ic = new InnoDBCluster();
        ic.setMetadata(new io.fabric8.kubernetes.api.model.ObjectMetaBuilder()
            .withName(clusterName)
            .withNamespace(namespace)
            .withOwnerReferences(new OwnerReferenceBuilder()
                .withApiVersion(owner.getApiVersion())
                .withKind(owner.getKind())
                .withName(owner.getMetadata().getName())
                .withUid(owner.getMetadata().getUid())
                .build())
            .build());
        ic.setSpec(icSpec);

        client.resources(InnoDBCluster.class).inNamespace(namespace).resource(ic).serverSideApply();
    }

    private void reconcileBackendDeployment(String appName, String namespace, BackendSpec backendSpec, InnoDBClusterSpec dbSpec, QuarkusApp owner) {
        String deploymentName = appName + "-backend";
        String dbSecretName = dbSpec.getSecretName();

        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                    .withName(deploymentName)
                    .withNamespace(namespace)
                    .withOwnerReferences(new OwnerReferenceBuilder()
                        .withApiVersion(owner.getApiVersion())
                        .withKind(owner.getKind())
                        .withName(owner.getMetadata().getName())
                        .withUid(owner.getMetadata().getUid())
                        .build())
                .endMetadata()
                .withNewSpec()
                    .withReplicas(backendSpec.getReplicas())
                    .withNewSelector().withMatchLabels(Map.of("app", deploymentName)).endSelector()
                    .withNewTemplate()
                        .withNewMetadata().withLabels(Map.of("app", deploymentName)).endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName("backend")
                                .withImage(backendSpec.getImage())
                                .withImagePullPolicy("IfNotPresent")
                                .withPorts(new io.fabric8.kubernetes.api.model.ContainerPortBuilder().withContainerPort(backendSpec.getPort()).build())
                                .withEnv(
                                    new EnvVarBuilder().withName("DB_HOST").withValue(appName + "-db-cluster").build(),
                                    new EnvVarBuilder().withName("DB_PORT").withValue("6446").build(),
                                    new EnvVarBuilder().withName("DB_USER").withNewValueFrom().withNewSecretKeyRef("user", dbSecretName, false).endValueFrom().build(),
                                    new EnvVarBuilder().withName("DB_PASSWORD").withNewValueFrom().withNewSecretKeyRef("password", dbSecretName, false).endValueFrom().build(),
                                    new EnvVarBuilder().withName("DB_NAME").withNewValueFrom().withNewSecretKeyRef("database", dbSecretName, false).endValueFrom().build()
                                )
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();
        
        client.apps().deployments().inNamespace(namespace).resource(deployment).serverSideApply();
    }

    private void reconcileBackendService(String appName, String namespace, BackendSpec backendSpec, QuarkusApp owner) {
        String serviceName = appName + "-backend";
        Service service = new ServiceBuilder()
                .withNewMetadata()
                    .withName(serviceName)
                    .withNamespace(namespace)
                    .withOwnerReferences(new OwnerReferenceBuilder()
                        .withApiVersion(owner.getApiVersion())
                        .withKind(owner.getKind())
                        .withName(owner.getMetadata().getName())
                        .withUid(owner.getMetadata().getUid())
                        .build())
                .endMetadata()
                .withNewSpec()
                    .withType(backendSpec.getServiceType())
                    .withSelector(Map.of("app", serviceName))
                    .withPorts(new ServicePortBuilder()
                        .withName(String.valueOf(backendSpec.getPort()))
                        .withPort(backendSpec.getPort())
                        .withTargetPort(new IntOrString(backendSpec.getPort()))
                        .build())
                .endSpec()
                .build();

        client.services().inNamespace(namespace).resource(service).serverSideApply();
    }

    private void reconcileFrontendService(String appName, String namespace, FrontendSpec frontendSpec, QuarkusApp owner) {
        String serviceName = appName + "-frontend";
        Service service = new ServiceBuilder()
                .withNewMetadata()
                    .withName(serviceName)
                    .withNamespace(namespace)
                    .withOwnerReferences(new OwnerReferenceBuilder()
                        .withApiVersion(owner.getApiVersion())
                        .withKind(owner.getKind())
                        .withName(owner.getMetadata().getName())
                        .withUid(owner.getMetadata().getUid())
                        .build())
                .endMetadata()
                .withNewSpec()
                    .withType(frontendSpec.getServiceType())
                    .withSelector(Map.of("app", serviceName))
                    .withPorts(new ServicePortBuilder()
                        .withName("http")
                        .withPort(8080) 
                        .withTargetPort(new IntOrString(frontendSpec.getPort()))
                        .build())
                .endSpec()
                .build();

        client.services().inNamespace(namespace).resource(service).serverSideApply();
    }

    private void reconcileFrontendDeployment(String appName, String namespace, FrontendSpec frontendSpec, QuarkusApp owner) {
        String deploymentName = appName + "-frontend";
        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                    .withName(deploymentName)
                    .withNamespace(namespace)
                    .withOwnerReferences(new OwnerReferenceBuilder()
                        .withApiVersion(owner.getApiVersion())
                        .withKind(owner.getKind())
                        .withName(owner.getMetadata().getName())
                        .withUid(owner.getMetadata().getUid())
                        .build())
                .endMetadata()
                .withNewSpec()
                    .withReplicas(frontendSpec.getReplicas().getMin())
                    .withNewSelector().withMatchLabels(Map.of("app", deploymentName)).endSelector()
                    .withNewTemplate()
                        .withNewMetadata().withLabels(Map.of("app", deploymentName)).endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName("frontend")
                                .withImage(frontendSpec.getImage())
                                .withImagePullPolicy("Never")
                                .withPorts(new ContainerPortBuilder().withContainerPort(frontendSpec.getPort()).build())
                                .withNewResources()
                                    .withRequests(Map.of("cpu", new Quantity("100m")))
                                    .withLimits(Map.of("cpu", new Quantity("500m")))
                                .endResources()
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();
        
        client.apps().deployments().inNamespace(namespace).resource(deployment).serverSideApply();
    }
    
    private void reconcileFrontendHpa(String appName, String namespace, FrontendSpec frontendSpec, QuarkusApp owner) {
        String hpaName = appName + "-frontend-hpa";
        String deploymentName = appName + "-frontend";
        
        HorizontalPodAutoscaler hpa = new HorizontalPodAutoscalerBuilder()
                .withNewMetadata()
                    .withName(hpaName)
                    .withNamespace(namespace)
                    .withOwnerReferences(new OwnerReferenceBuilder()
                        .withApiVersion(owner.getApiVersion())
                        .withKind(owner.getKind())
                        .withName(owner.getMetadata().getName())
                        .withUid(owner.getMetadata().getUid())
                        .build())
                .endMetadata()
                .withNewSpec()
                    .withNewScaleTargetRef()
                        .withApiVersion("apps/v1")
                        .withKind("Deployment")
                        .withName(deploymentName)
                    .endScaleTargetRef()
                    .withMinReplicas(frontendSpec.getReplicas().getMin())
                    .withMaxReplicas(frontendSpec.getReplicas().getMax())
                    .withMetrics(new MetricSpecBuilder()
                        .withType("Resource")
                        .withNewResource()
                            .withName("cpu")
                            .withNewTarget()
                                .withType("Utilization")
                                .withAverageUtilization(frontendSpec.getCpuThreshold())
                            .endTarget()
                        .endResource()
                        .build())
                .endSpec()
                .build();

        client.autoscaling().v2().horizontalPodAutoscalers().inNamespace(namespace).resource(hpa).serverSideApply();
    }

    private void reconcileIngress(String appName, String namespace, QuarkusAppSpec spec, QuarkusApp owner) {
        String ingressName = appName + "-ingress";
        
        if (spec.getIngress() == null || !spec.getIngress().isEnabled()) {
            client.network().v1().ingresses().inNamespace(namespace).withName(ingressName).delete();
            System.out.println("Ingress " + ingressName + " is disabled, ensuring it is deleted.");
            return;
        }

        IngressSpec ingressSpec = spec.getIngress();
        
        
        var backendPath = new HTTPIngressPathBuilder()
                .withPath("/submit")
                .withPathType("Prefix")
                .withBackend(new IngressBackendBuilder()
                        .withService(new io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackendBuilder()
                                .withName(appName + "-backend")
                                .withPort(new ServiceBackendPortBuilder().withNumber(spec.getBackend().getPort()).build())
                                .build())
                        .build())
                .build();
        
        
        var frontendPath = new HTTPIngressPathBuilder()
                .withPath("/")
                .withPathType("Prefix")
                .withBackend(new IngressBackendBuilder()
                        .withService(new io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackendBuilder()
                                .withName(appName + "-frontend")
                                .withPort(new ServiceBackendPortBuilder().withNumber(8080).build())
                                .build())
                        .build())
                .build();
        
        Ingress ingress = new IngressBuilder()
                .withNewMetadata()
                    .withName(ingressName)
                    .withNamespace(namespace)
                    .withOwnerReferences(new OwnerReferenceBuilder()
                        .withApiVersion(owner.getApiVersion())
                        .withKind(owner.getKind())
                        .withName(owner.getMetadata().getName())
                        .withUid(owner.getMetadata().getUid())
                        .build())
                .endMetadata()
                .withNewSpec()
                    .withRules(new IngressRuleBuilder()
                            .withHost(ingressSpec.getHost())
                            .withNewHttp()
                                .withPaths(backendPath, frontendPath) 
                            .endHttp()
                            .build())
                    .endSpec()
                .build();

        client.network().v1().ingresses().inNamespace(namespace).resource(ingress).serverSideApply();
        System.out.println("Ingress " + ingressName + " reconciled.");
    }

    @Override
    public List<EventSource<?, QuarkusApp>> prepareEventSources(EventSourceContext<QuarkusApp> context) {
        InformerEventSourceConfiguration<InnoDBCluster> dbConfig =
            InformerEventSourceConfiguration
                .from(InnoDBCluster.class, QuarkusApp.class)
                .build();

        InformerEventSource<InnoDBCluster, QuarkusApp> dbInformer =
            new InformerEventSource<>(dbConfig, context);

        return List.of(dbInformer);
    }


    @Override
    public ErrorStatusUpdateControl<QuarkusApp> updateErrorStatus(QuarkusApp resource, Context<QuarkusApp> context, Exception e) {
        return ErrorStatusUpdateControl.noStatusUpdate();
    }
}
