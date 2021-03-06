package gov.va.api.health.bulkfhir.anonymizer.patient;

import gov.va.api.health.argonaut.api.resources.Patient;
import gov.va.api.health.bulkfhir.anonymizer.AnonymizedIdGenerator;
import gov.va.api.health.bulkfhir.anonymizer.SyntheticData;
import java.util.function.Function;
import lombok.Builder;

@Builder
public class PatientAnonymizer implements Function<Patient, Patient> {

  private final SyntheticData syntheticData;

  private final AnonymizedIdGenerator idGenerator;

  /** Anonymize a Patient Record. */
  public Patient apply(Patient resource) {
    String anonymizedId = idGenerator.generateIdFrom(resource.id());
    /*
     * Lets get a repeatable seed from our patient record, so that we can create replicable
     * Synthetic data. For our seed, we will strip the V out of the ICN, and use the resulting long.
     * If we cannot parse the ICN, we create our seed from the string hash.
     */
    long idBasedSeed = Integer.toUnsignedLong(anonymizedId.hashCode());
    return Patient.builder()
        .id(anonymizedId)
        .birthDate(syntheticData.synthesizeDate(resource.birthDate()))
        .careProvider(resource.careProvider())
        .communication(resource.communication())
        .contained(resource.contained())
        .deceasedBoolean(resource.deceasedBoolean())
        .deceasedDateTime(syntheticData.synthesizeDateTime(resource.deceasedDateTime()))
        .extension(resource.extension())
        .gender(resource.gender())
        .implicitRules(resource.implicitRules())
        .language(resource.language())
        .link(resource.link())
        .managingOrganization(resource.managingOrganization())
        .maritalStatus(resource.maritalStatus())
        .meta(resource.meta())
        .modifierExtension(resource.modifierExtension())
        .multipleBirthBoolean(
            sanitizeMultipleBirthBoolean(
                resource.multipleBirthBoolean(), resource.multipleBirthInteger()))
        .name(syntheticData.synthesizeName(idBasedSeed))
        .resourceType(resource.resourceType())
        .text(resource.text())
        .build();
  }

  /**
   * MBI and MBB are a one of choice. If MBI is provided, anonymization must drop it, and instead
   * provide the MBB corresponding the value.
   */
  Boolean sanitizeMultipleBirthBoolean(Boolean multipleBirthBoolean, Integer multipleBirthInteger) {
    if (multipleBirthInteger != null) {
      return multipleBirthInteger > 0;
    }
    return multipleBirthBoolean;
  }
}
