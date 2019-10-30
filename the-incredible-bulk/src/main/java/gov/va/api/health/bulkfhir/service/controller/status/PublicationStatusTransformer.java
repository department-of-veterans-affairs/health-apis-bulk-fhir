package gov.va.api.health.bulkfhir.service.controller.status;

import gov.va.api.health.bulkfhir.api.internal.PublicationStatus;
import java.util.function.Function;

interface PublicationStatusTransformer
    extends Function<Iterable<StatusEntity>, PublicationStatus> {}
