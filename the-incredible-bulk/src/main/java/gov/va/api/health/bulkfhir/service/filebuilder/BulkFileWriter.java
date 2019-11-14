package gov.va.api.health.bulkfhir.service.filebuilder;

import java.util.stream.Stream;

/** An interface to be used to write bulk files of anonymized FHIR resource data. */
public interface BulkFileWriter {

  /**
   * Write the bulk file.
   *
   * @param claim The meta information about the file to write
   * @param resources The list of resources to write to the file
   */
  void writeFile(FileClaim claim, Stream<String> resources) throws Exception;
}
