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
        // remove address
        // synth birthdate
        .birthDate(syntheticData.synthesizeDate(resource.birthDate()))
        .careProvider(resource.careProvider())
        .communication(resource.communication())
        // remove contact
        .contained(resource.contained())
        .deceasedBoolean(resource.deceasedBoolean())
        // synth deceasedDateTime
        .deceasedDateTime(syntheticData.synthesizeDateTime(resource.deceasedDateTime()))
        .extension(resource.extension())
        .gender(resource.gender())
        // remove id
        // remove identifier
        .implicitRules(resource.implicitRules())
        .language(resource.language())
        .link(resource.link())
        .managingOrganization(resource.managingOrganization())
        .maritalStatus(resource.maritalStatus())
        .meta(resource.meta())
        .modifierExtension(resource.modifierExtension())
        .multipleBirthBoolean(resource.multipleBirthBoolean())
        // Remove multipleBirthInteger
        // synth name
        .name(syntheticData.synthesizeName(icnBasedSeed))
        // remove photo
        .resourceType(resource.resourceType())
        // remove telecom
        .text(resource.text())
        .build();
  }

  public long icnBasedSeed(String id) {
    long icnBasedSeed;
    try {
      icnBasedSeed = Long.parseLong(id.replace("V", ""));
    } catch (NumberFormatException e) {
      log.info("Failed to generate ICN Based seed from: {}. Using default hash instead.", id);
      icnBasedSeed = id.hashCode();
    }
    return icnBasedSeed;
  }
}
