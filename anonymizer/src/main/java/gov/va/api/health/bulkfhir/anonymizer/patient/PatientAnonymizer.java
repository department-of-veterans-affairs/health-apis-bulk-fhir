package gov.va.api.health.bulkfhir.anonymizer.patient;

import gov.va.api.health.argonaut.api.resources.Patient;
import gov.va.api.health.bulkfhir.anonymizer.SyntheticData;
import java.util.function.Function;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class PatientAnonymizer implements Function<Patient, Patient> {

  private final SyntheticData syntheticData;

  /** Anonymize a Patient Record. */
  public Patient apply(Patient resource) {
    /**
     * Lets get a repeatable seed from our patient record, so that we can create replicable
     * Synthetic data. For our seed, we will strip the V out of the ICN, and use the resulting long.
     * If we cannot parse the ICN, we create our seed from the string hash.
     */
    long icnBasedSeed = icnBasedSeed(resource.id());
    return Patient.builder()
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
        .name(syntheticData.synthesizeName(icnBasedSeed))
        .resourceType(resource.resourceType())
        .text(resource.text())
        .build();
  }

  /**
   * Generate a seed for name synthesis derived from the ICN. Fallback to a string hash if we can't
   * parse the ICN. This will allow for a unique, repeatible seed for each ICN.
   */
  long icnBasedSeed(String id) {
    long icnBasedSeed;
    try {
      icnBasedSeed = Long.parseLong(id.replace("V", ""));
    } catch (NumberFormatException e) {
      log.info("Failed to generate ICN Based seed from: {}. Using default hash instead.", id);
      icnBasedSeed = id.hashCode();
    }
    return icnBasedSeed;
  }

  /**
   * MBI and MBB are a one of choice. If MBI is provided, anonymization must drop it, and instead
   * provide the MBB corresponding the value.
   */
  private boolean sanitizeMultipleBirthBoolean(
      Boolean multipleBirthBoolean, Integer multipleBirthInteger) {
    if (multipleBirthInteger != null) {
      return multipleBirthInteger > 0;
    }
    return multipleBirthBoolean;
  }
}
