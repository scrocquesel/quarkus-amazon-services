package io.quarkus.amazon.common.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.jboss.jandex.DotName;

import io.quarkus.amazon.common.runtime.AsyncHttpClientBuildTimeConfig.AsyncClientType;
import io.quarkus.amazon.common.runtime.SdkBuildTimeConfig;
import io.quarkus.amazon.common.runtime.SyncHttpClientBuildTimeConfig.SyncClientType;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.runtime.configuration.ConfigurationException;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.http.async.SdkAsyncHttpService;

public class AmazonServicesClientsProcessor {
    public static final String AWS_SDK_APPLICATION_ARCHIVE_MARKERS = "software/amazon/awssdk";
    public static final String AWS_SDK_XRAY_ARCHIVE_MARKER = "com/amazonaws/xray";

    private static final DotName EXECUTION_INTERCEPTOR_NAME = DotName
            .createSimple(ExecutionInterceptor.class.getName());

    @BuildStep
    void globalInterceptors(BuildProducer<AmazonClientInterceptorsPathBuildItem> producer) {
        producer.produce(
                new AmazonClientInterceptorsPathBuildItem(
                        "software/amazon/awssdk/global/handlers/execution.interceptors"));
    }

    @BuildStep
    void awsAppArchiveMarkers(BuildProducer<AdditionalApplicationArchiveMarkerBuildItem> archiveMarker) {
        archiveMarker.produce(new AdditionalApplicationArchiveMarkerBuildItem(AWS_SDK_APPLICATION_ARCHIVE_MARKERS));
        archiveMarker.produce(new AdditionalApplicationArchiveMarkerBuildItem(AWS_SDK_XRAY_ARCHIVE_MARKER));
    }

    @BuildStep
    void runtimeInitialize(BuildProducer<RuntimeInitializedClassBuildItem> producer) {
        // FullJitterBackoffStrategy uses j.u.Ramdom, so needs to be runtime-initialized
        producer.produce(
                new RuntimeInitializedClassBuildItem(
                        "software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy"));
        // CachedSupplier uses j.u.Ramdom, so needs to be runtime-initialized
        producer.produce(
                new RuntimeInitializedClassBuildItem("software.amazon.awssdk.utils.cache.CachedSupplier"));
    }

    @BuildStep
    void setup(CombinedIndexBuildItem combinedIndexBuildItem,
            List<AmazonClientBuildItem> amazonClients,
            List<AmazonClientInterceptorsPathBuildItem> interceptors,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<GeneratedResourceBuildItem> generatedResource,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinition,
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {

        interceptors.stream().map(AmazonClientInterceptorsPathBuildItem::getInterceptorsPath)
                .forEach(path -> resource.produce(new NativeImageResourceBuildItem(path)));

        // Discover all interceptor implementations
        List<String> knownInterceptorImpls = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementors(EXECUTION_INTERCEPTOR_NAME)
                .stream()
                .map(c -> c.name().toString()).collect(Collectors.toList());

        // Validate configurations
        for (AmazonClientBuildItem client : amazonClients) {
            SdkBuildTimeConfig clientSdkConfig = client.getBuildTimeSdkConfig();
            if (clientSdkConfig != null) {
                clientSdkConfig.interceptors.orElse(Collections.emptyList()).forEach(interceptorClassName -> {
                    interceptorClassName = interceptorClassName.trim();
                    if (!knownInterceptorImpls.contains(interceptorClassName)) {
                        throw new ConfigurationException(
                                String.format(
                                        "quarkus.%s.interceptors (%s) - must list only existing implementations of software.amazon.awssdk.core.interceptor.ExecutionInterceptor",
                                        client.getAwsClientName(),
                                        clientSdkConfig.interceptors.toString()));
                    }
                });
            }
        }

        reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false,
                knownInterceptorImpls.toArray(new String[knownInterceptorImpls.size()])));

        reflectiveClasses
                .produce(new ReflectiveClassBuildItem(true, false, "com.sun.xml.internal.stream.XMLInputFactoryImpl"));
        reflectiveClasses
                .produce(new ReflectiveClassBuildItem(true, false, "com.sun.xml.internal.stream.XMLOutputFactoryImpl"));

        boolean syncTransportNeeded = amazonClients.stream().anyMatch(item -> item.getSyncClassName().isPresent());
        boolean asyncTransportNeeded = amazonClients.stream().anyMatch(item -> item.getAsyncClassName().isPresent());
        final Predicate<AmazonClientBuildItem> isSyncApache = client -> client
                .getBuildTimeSyncConfig().type == SyncClientType.APACHE;
        final Predicate<AmazonClientBuildItem> isSyncUrlConnection = client -> client
                .getBuildTimeSyncConfig().type == SyncClientType.URL;
        final Predicate<AmazonClientBuildItem> isAsyncNetty = client -> client
                .getBuildTimeAsyncConfig().type == AsyncClientType.NETTY;
        final Predicate<AmazonClientBuildItem> isAsyncAwsCrt = client -> client
                .getBuildTimeAsyncConfig().type == AsyncClientType.AWS_CRT;

        // Register what's needed depending on the clients in the classpath and the
        // configuration.
        // We use the configuration to guide us but if we don't have any clients
        // configured,
        // we still register what's needed depending on what is in the classpath.
        boolean isSyncApacheInClasspath = isInClasspath(AmazonHttpClients.APACHE_HTTP_SERVICE);
        boolean isSyncUrlConnectionInClasspath = isInClasspath(AmazonHttpClients.URL_CONNECTION_HTTP_SERVICE);
        boolean isAsyncNettyInClasspath = isInClasspath(AmazonHttpClients.NETTY_HTTP_SERVICE);
        boolean isAsyncAwsCrtInClasspath = isInClasspath(AmazonHttpClients.AWS_CRT_HTTP_SERVICE);

        // Check that the clients required by the configuration are available
        boolean defaultClasspathSdkSyncHttpServiceRegistered = false;
        if (syncTransportNeeded) {
            if (amazonClients.stream().filter(isSyncApache).findAny().isPresent()) {
                if (isSyncApacheInClasspath) {
                    registerSyncApacheClient(proxyDefinition, serviceProvider);
                    defaultClasspathSdkSyncHttpServiceRegistered = true;
                } else {
                    throw missingDependencyException("apache-client");
                }
            }
            if (amazonClients.stream().filter(isSyncUrlConnection).findAny().isPresent()) {
                if (isSyncUrlConnectionInClasspath) {
                    // prefer apache
                    if (!defaultClasspathSdkSyncHttpServiceRegistered) {
                        registerSyncUrlConnectionClient(serviceProvider);
                    }
                } else {
                    throw missingDependencyException("url-connection-client");
                }
            }
        } else {
            // even if we don't register any clients via configuration, we still register
            // the clients
            // but this time only based on the classpath.
            if (isSyncApacheInClasspath) {
                registerSyncApacheClient(proxyDefinition, serviceProvider);
            } else if (isSyncUrlConnectionInClasspath) {
                registerSyncUrlConnectionClient(serviceProvider);
            }
        }

        boolean defaultClasspathSdkAsyncHttpServiceRegistered = false;
        if (asyncTransportNeeded) {
            if (amazonClients.stream().filter(isAsyncNetty).findAny().isPresent()) {
                if (isAsyncNettyInClasspath) {
                    registerAsyncNettyClient(serviceProvider);
                    defaultClasspathSdkAsyncHttpServiceRegistered = true;
                } else {
                    throw missingDependencyException("netty-nio-client");
                }
            }
            if (amazonClients.stream().filter(isAsyncAwsCrt).findAny().isPresent()) {
                if (isAsyncAwsCrtInClasspath) {
                    registerAwsCrtJni(runtimeInitializedClass, resource, generatedResource);
                    if (!defaultClasspathSdkAsyncHttpServiceRegistered) {
                        registerAsyncAwsCrtClient(serviceProvider);
                    }
                } else {
                    throw missingDependencyException("aws-crt-client");
                }
            }
        } else {
            // even if we don't register any clients via configuration, we still register
            // the clients
            // but this time only based on the classpath.
            if (isAsyncNettyInClasspath) {
                registerAsyncNettyClient(serviceProvider);
            } else if (isAsyncAwsCrtInClasspath) {
                registerAwsCrtJni(runtimeInitializedClass, resource, generatedResource);
                registerAsyncAwsCrtClient(serviceProvider);

            }
        }
    }

    private static void registerSyncApacheClient(BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinition,
            BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        proxyDefinition
                .produce(new NativeImageProxyDefinitionBuildItem("org.apache.http.conn.HttpClientConnectionManager",
                        "org.apache.http.pool.ConnPoolControl",
                        "software.amazon.awssdk.http.apache.internal.conn.Wrapped"));

        serviceProvider.produce(
                new ServiceProviderBuildItem(SdkHttpService.class.getName(), AmazonHttpClients.APACHE_HTTP_SERVICE));
    }

    private static void registerSyncUrlConnectionClient(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(
                new ServiceProviderBuildItem(SdkHttpService.class.getName(),
                        AmazonHttpClients.URL_CONNECTION_HTTP_SERVICE));
    }

    private static void registerAsyncNettyClient(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        serviceProvider.produce(
                new ServiceProviderBuildItem(SdkAsyncHttpService.class.getName(),
                        AmazonHttpClients.NETTY_HTTP_SERVICE));
    }

    private static void registerAsyncAwsCrtClient(BuildProducer<ServiceProviderBuildItem> serviceProvider) {

        serviceProvider.produce(
                new ServiceProviderBuildItem(SdkAsyncHttpService.class.getName(),
                        AmazonHttpClients.AWS_CRT_HTTP_SERVICE));
    }

    private static void registerAwsCrtJni(
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<GeneratedResourceBuildItem> generatedResource) {

        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem("software.amazon.awssdk.crt.CRT"));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem("software.amazon.awssdk.crt.CrtRuntimeException"));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem("software.amazon.awssdk.crt.CrtResource"));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem("software.amazon.awssdk.crt.Log"));

        // provided by aws-crt
        resource.produce(new NativeImageResourceBuildItem("linux/x86_64/libaws-crt-jni.so"));
        resource.produce(new NativeImageResourceBuildItem("osx/x86_64/libaws-crt-jni.dylib"));

        // todo replace with JniRuntimeAccessBuildItem/JniRuntimeAccessFieldBuildItem/JniRuntimeAccessMethodBuildItem with Quarkus 3.1
        // copy jni-config.json to produced jar
        try (InputStream jniConfig = AmazonServicesClientsProcessor.class.getClassLoader()
                .getResourceAsStream("native-image/software.amazon.awssdk/aws-crt-client/jni-config.json")) {
            generatedResource.produce(new GeneratedResourceBuildItem(
                    "META-INF/native-image/software.amazon.awssdk/aws-crt-client/jni-config.json",
                    IoUtil.readBytes(jniConfig)));
        } catch (IOException e) {
            throw new DeploymentException("Unable to read jni-config.json", e);
        }
    }

    private static boolean isInClasspath(String className) {
        try {
            Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private DeploymentException missingDependencyException(String dependencyName) {
        return new DeploymentException(
                "Missing 'software.amazon.awssdk:" + dependencyName + "' dependency on the classpath");
    }
}
