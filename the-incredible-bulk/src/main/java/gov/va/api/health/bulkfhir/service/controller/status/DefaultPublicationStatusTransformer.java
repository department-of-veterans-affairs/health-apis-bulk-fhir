package gov.va.api.health.bulkfhir.service.controller.status;

import gov.va.api.health.bulkfhir.api.internal.PublicationStatus;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultPublicationStatusTransformer implements PublicationStatusTransformer {

  @Override
  public PublicationStatus apply(Iterable<StatusEntity> statusEntities) {
    log.info("{}", statusEntities);
    return PublicationStatus.builder().creationDate(Instant.now()).build();
  }
}
