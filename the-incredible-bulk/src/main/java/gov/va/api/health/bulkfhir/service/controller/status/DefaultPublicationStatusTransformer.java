package gov.va.api.health.bulkfhir.service.controller.status;

import gov.va.api.health.bulkfhir.api.internal.PublicationStatus;

public class DefaultPublicationStatusTransformer implements PublicationStatusTransformer {

  @Override
  public PublicationStatus apply(Iterable<StatusEntity> statusEntities) {
    return null;
  }
}
