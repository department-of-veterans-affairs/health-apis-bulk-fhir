package gov.va.api.health.bulkfhir.service.controller.publication;

import gov.va.api.health.bulkfhir.api.internal.PublicationStatus;
import gov.va.api.health.bulkfhir.service.status.StatusEntity;
import java.util.function.Function;

public interface PublicationStatusTransformer
    extends Function<Iterable<StatusEntity>, PublicationStatus> {}
