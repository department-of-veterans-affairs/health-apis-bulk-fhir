package gov.va.api.health.bulkfhir.idsmapping;

import gov.va.api.health.ids.client.EncryptingIdEncoder.Codebook;
import gov.va.api.health.ids.client.EncryptingIdEncoder.Codebook.Mapping;
import gov.va.api.health.ids.client.EncryptingIdEncoder.CodebookSupplier;

/** Codebook mapping for BulkFhir encoding. */
public class BulkFhirIdsCodebookSupplier implements CodebookSupplier {

  @Override
  public Codebook get() {
    return Codebook.builder()
        /* Systems */
        .map(Mapping.of("BULK", "B"))
        .map(Mapping.of("UNKNOWN", "U"))
        /* Supported output formats */
        .map(Mapping.of("/Patient/$export?_outputFormat=application/fhir+ndjson", "AFN"))
        .map(Mapping.of("/Patient/$export?_outputFormat=application/ndjson", "AND"))
        .map(Mapping.of("/Patient/$export?_outputFormat=ndjson", "NDJ"))
        .map(Mapping.of("/Patient/$export", "NOF"))
        .build();
  }
}
