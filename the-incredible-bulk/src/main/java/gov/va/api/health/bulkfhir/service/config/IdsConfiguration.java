package gov.va.api.health.bulkfhir.service.config;

import gov.va.api.health.bulkfhir.idsmapping.BulkFhirIdsCodebookSupplier;
import gov.va.api.health.ids.client.EncryptingIdEncoder.Codebook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class IdsConfiguration {
  @Bean
  @ConditionalOnMissingBean
  Codebook codebook() {
    return new BulkFhirIdsCodebookSupplier().get();
  }
}
