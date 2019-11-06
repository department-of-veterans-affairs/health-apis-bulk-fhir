package gov.va.api.health.bulkfhir.anonymizer;

import gov.va.api.health.argonaut.api.resources.Patient;
import java.util.function.Function;


public class PatientAnonymizer implements Function<Patient, Patient> {

  private SyntheticDataFactory syntheticDataFactory = new RandomSeedSyntheticDataFactory();

  private SyntheticData syntheticData = syntheticDataFactory.syntheticData();

  /**
   * Anonymize a Patient Recoord.
   *
   * @param resource The patient record.
   * @return The anonymized patient record.
   */
  public Patient apply(Patient resource) {
    return Patient.builder()
        .active(resource.active())
        // remove address
        // synth birthdate
        .birthDate(syntheticData.synthesizeDate(resource.birthDate()))
        .careProvider(resource.careProvider())
        .communication(resource.communication())
        // remove contact
        .contained(resource.contained())
        .deceasedBoolean(resource.deceasedBoolean())
        // synth deceasedDateTime
        .deceasedDateTime(syntheticData.synthesizeDate(resource.deceasedDateTime()))
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
        .name(syntheticData.synthesizeName(resource.name()))
        // remove photo
        .resourceType(resource.resourceType())
        // remove telecom
        .text(resource.text())
        .build();
  }
}
