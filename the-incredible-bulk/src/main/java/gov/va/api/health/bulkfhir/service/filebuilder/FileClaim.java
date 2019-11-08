package gov.va.api.health.bulkfhir.service.filebuilder;

import lombok.Builder;
import lombok.Value;

/**
 * A claim indicates that this file has been successfully claimed by this application for exclusive
 * rights to build it.
 */
@Value
@Builder
public class FileClaim {

  /** The original request. */
  FileBuildRequest request;

  /** The file name with out extension, e.g. 'patient-5' */
  String fileName;

  /** The row or record number of the first item in this file. */
  int page;

  /** The row or record number of the last item in this file. */
  int count;
}
