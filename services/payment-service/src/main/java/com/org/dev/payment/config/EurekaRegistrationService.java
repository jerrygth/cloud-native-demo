package com.org.dev.payment.config;

import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;

import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.Service;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class EurekaRegistrationService {

    private static final Logger LOG = Logger.getLogger(EurekaRegistrationService.class);

    @ConfigProperty(name = "quarkus.application.name")
    String serviceName;

    @ConfigProperty(name = "quarkus.http.port")
    int port;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("StartupEvent detected. Triggering Eureka registration...");

        // We call the register method and subscribe because it's a Mutiny Uni (reactive)
/*        this.register().subscribe().with(
                success -> LOG.info("Successfully registered with Eureka"),
                failure -> LOG.error("Failed to register with Eureka", failure)
        );*/
    }
    /*public Uni<Void> register() {
        Stork stork = Stork.getInstance();

        // Retrieve the service definition by name
        Optional<Service> service = stork.getServiceOptional(serviceName);
        if (service.isPresent() && service.get().getServiceRegistrar() != null) {
            LOG.infof("Registering %s with Eureka on port %d", serviceName, port);
            Map<EurekaMetadataKey, Object> map = new EnumMap<>(EurekaMetadataKey.class);
            map.put(EurekaMetadataKey.META_EUREKA_SERVICE_ID, serviceName + "-" + port);
            Metadata<EurekaMetadataKey> storkMetadata = Metadata.of(EurekaMetadataKey.class, map);
           return service.get().getServiceRegistrar()
                    .registerServiceInstance(serviceName, storkMetadata,"localhost" , port);
        } else {
            LOG.error("No Stork registrar found for " + serviceName);
            return Uni.createFrom().voidItem();
        }
    }

    private String getHostIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }*/
}