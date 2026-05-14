package org.sihsalus.module.serialization.xstream;

import org.openmrs.module.serialization.xstream.XStreamShortSerializer;
import org.openmrs.module.serialization.xstream.converter.CollectionCompatibility;
import org.openmrs.module.serialization.xstream.converter.CollectionCompatibilityConverter;
import org.openmrs.serialization.SerializationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "org.openmrs.module.serialization.xstream")
public class SihsalusSerializationXstreamConfiguration {

    @Bean
    CollectionCompatibility serializationXstreamCollectionCompatibility() {
        return new CollectionCompatibilityConverter();
    }

    @Bean
    XStreamShortSerializer xstreamShortSerializer() throws SerializationException {
        return new XStreamShortSerializer();
    }
}
