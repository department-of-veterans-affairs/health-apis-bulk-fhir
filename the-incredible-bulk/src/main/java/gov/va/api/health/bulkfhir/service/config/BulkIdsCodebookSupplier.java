package gov.va.api.health.bulkfhir.service.config;

import gov.va.api.health.ids.client.EncryptingIdEncoder;

public class BulkIdsCodebookSupplier implements EncryptingIdEncoder.CodebookSupplier {

  @Override
  public EncryptingIdEncoder.Codebook get() {
    return EncryptingIdEncoder.Codebook.builder()
        /* Systems */
        .map(EncryptingIdEncoder.Codebook.Mapping.of("CDW", "C"))
        .map(EncryptingIdEncoder.Codebook.Mapping.of("MVI", "M"))
        .map(EncryptingIdEncoder.Codebook.Mapping.of("UNKNOWN", "U"))
        /* Data Query Resources */
        .map(EncryptingIdEncoder.Codebook.Mapping.of("ALLERGY_INTOLERANCE", "AI"))
        .map(EncryptingIdEncoder.Codebook.Mapping.of("APPOINTMENT", "AP"))
        .map(EncryptingIdEncoder.Codebook.Mapping.of("CONDITION", "CO"))
        .map(EncryptingIdEncoder.Codebook.Mapping.of("DIAGNOSTIC_REPORT", "DR"))
        .map(EncryptingIdEncoder.Codebook.Mapping.of("ENCOUNTER", "EN"))
        .map(EncryptingIdEncoder.Codebook.Mapping.of("IMMUNIZATION", "IM"))
        .map(EncryptingIdEncoder.Codebook.Mapping.of("LOCATION", "LO"))
        .map(EncryptingIdEncoder.Codebook.Mapping.of("MEDICATION", "ME"))
        .map(EncryptingIdEncoder.Codebook.Mapping.of("MEDICATION_DISPENSE", "MD"))
        .map(EncryptingIdEncoder.Codebook.Mapping.of("MEDICATION_ORDER", "MO"))
        .map(EncryptingIdEncoder.Codebook.Mapping.of("MEDICATION_STATEMENT", "MS"))
        .map(EncryptingIdEncoder.Codebook.Mapping.of("OBSERVATION", "OB"))
        .map(EncryptingIdEncoder.Codebook.Mapping.of("ORGANIZATION", "OG"))
        .map(EncryptingIdEncoder.Codebook.Mapping.of("PATIENT", "PA"))
        .map(EncryptingIdEncoder.Codebook.Mapping.of("PRACTITIONER", "PC"))
        .map(EncryptingIdEncoder.Codebook.Mapping.of("PROCEDURE", "PR"))
        .build();
  }
}
