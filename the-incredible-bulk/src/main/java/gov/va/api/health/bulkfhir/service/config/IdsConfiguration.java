package gov.va.api.health.bulkfhir.service.config;

import gov.va.api.health.ids.client.EncryptingIdEncoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdsConfiguration {
  @Bean
  @ConditionalOnMissingBean
  EncryptingIdEncoder.Codebook codebook() {
    return new BulkIdsCodebookSupplier().get();
  }
}
