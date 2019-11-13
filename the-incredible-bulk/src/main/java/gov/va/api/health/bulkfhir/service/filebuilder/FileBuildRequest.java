package gov.va.api.health.bulkfhir.service.filebuilder;

import lombok.Builder;
import lombok.Value;

/** This represents the file that should be created. */
@Value
@Builder
public class FileBuildRequest {
  String publicationId;
  String fileId;
}
