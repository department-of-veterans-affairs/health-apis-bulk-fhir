package gov.va.api.health.bulkfhir.service.config;

import gov.va.api.health.bulkfhir.idsmapping.BulkFhirIdsCodebookSupplier;
import gov.va.api.health.ids.client.EncryptingIdEncoder.Codebook;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdsConfiguration {
  @Bean
  @ConditionalOnMissingBean
  Codebook codebook() {
    return new BulkFhirIdsCodebookSupplier().get();
  }
}
