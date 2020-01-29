package gov.va.api.health.bulkfhir.anonymizer.patient;

import static java.util.Arrays.asList;

import gov.va.api.health.argonaut.api.resources.Patient;
import gov.va.api.health.argonaut.api.resources.Patient.Contact;
import gov.va.api.health.argonaut.api.resources.Patient.Gender;
import gov.va.api.health.dstu2.api.bundle.AbstractBundle.BundleType;
import gov.va.api.health.dstu2.api.bundle.AbstractEntry;
import gov.va.api.health.dstu2.api.bundle.AbstractEntry.SearchMode;
import gov.va.api.health.dstu2.api.bundle.BundleLink;
import gov.va.api.health.dstu2.api.datatypes.Address;
import gov.va.api.health.dstu2.api.datatypes.CodeableConcept;
import gov.va.api.health.dstu2.api.datatypes.Coding;
import gov.va.api.health.dstu2.api.datatypes.ContactPoint;
import gov.va.api.health.dstu2.api.datatypes.ContactPoint.ContactPointSystem;
import gov.va.api.health.dstu2.api.datatypes.ContactPoint.ContactPointUse;
import gov.va.api.health.dstu2.api.datatypes.HumanName;
import gov.va.api.health.dstu2.api.datatypes.HumanName.NameUse;
import gov.va.api.health.dstu2.api.datatypes.Identifier;
import gov.va.api.health.dstu2.api.datatypes.Identifier.IdentifierUse;
import gov.va.api.health.dstu2.api.elements.Extension;
import gov.va.api.health.dstu2.api.elements.Reference;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.experimental.UtilityClass;

@UtilityClass
class AnonymizerPatientSamples {

  /** Container class for sample anonymized patient. */
  @AllArgsConstructor(staticName = "create")
  static class Anonymized {
    static Patient.Bundle asBundle(
        String basePath, Collection<Patient> records, BundleLink... links) {
      return Patient.Bundle.builder()
          .resourceType("Bundle")
          .type(BundleType.searchset)
          .total(records.size())
          .link(asList(links))
          .entry(
              records.stream()
                  .map(
                      c ->
                          Patient.Entry.builder()
                              .fullUrl(basePath + "/Patient/" + c.id())
                              .resource(c)
                              .search(AbstractEntry.Search.builder().mode(SearchMode.match).build())
                              .build())
                  .collect(Collectors.toList()))
          .build();
    }

    static BundleLink link(BundleLink.LinkRelation relation, String base, int page, int count) {
      return BundleLink.builder()
          .relation(relation)
          .url(base + "&page=" + page + "&_count=" + count)
          .build();
    }

    Patient patient() {
      return patient("1000003");
    }

    Patient patient(String id) {
      return Patient.builder()
          // Anonymized ID
          .id("WHOISIT")
          .resourceType("Patient")
          .extension(
              List.of(
                  Extension.builder()
                      .url("http://fhir.org/guides/argonaut/StructureDefinition/argo-race")
                      .extension(
                          List.of(
                              Extension.builder()
                                  .url("ombCategory")
                                  .valueCoding(
                                      Coding.builder()
                                          .system("http://hl7.org/fhir/v3/Race")
                                          .code("1002-5")
                                          .display("American Indian or Alaska Native")
                                          .build())
                                  .build(),
                              Extension.builder()
                                  .url("text")
                                  .valueString("American Indian or Alaska Native")
                                  .build()))
                      .build(),
                  Extension.builder()
                      .url("http://fhir.org/guides/argonaut/StructureDefinition/argo-birthsex")
                      .valueCode("M")
                      .build()))
          // Remove Identifier
          // Synthesize Name from ICN seed
          .name(
              List.of(
                  HumanName.builder()
                      .use(NameUse.usual)
                      .text("Mr. Sam Nobody")
                      .family(List.of("Nobody"))
                      .given(List.of("Sam"))
                      .build()))
          // remove telecom
          // remove address
          .gender(Gender.male)
          .multipleBirthBoolean(false)
          // Synthesize birthDate to the 1st of the previous month
          .birthDate("1970-10-01")
          // Synthesize deceasedDateTime to the 1st of the previous month
          .deceasedDateTime("2001-02-01T00:00:00Z")
          .maritalStatus(
              CodeableConcept.builder()
                  .coding(
                      List.of(
                          Coding.builder()
                              .system("http://hl7.org/fhir/marital-status")
                              .code("M")
                              .display("Married")
                              .build()))
                  .build())
          // Remove Contact
          .build();
    }
  }

  /** Container class for sample FHIR patient. */
  @AllArgsConstructor(staticName = "create")
  static class Fhir {
    static Patient.Bundle asBundle(
        String basePath, Collection<Patient> records, BundleLink... links) {
      return Patient.Bundle.builder()
          .resourceType("Bundle")
          .type(BundleType.searchset)
          .total(records.size())
          .link(asList(links))
          .entry(
              records.stream()
                  .map(
                      c ->
                          Patient.Entry.builder()
                              .fullUrl(basePath + "/Patient/" + c.id())
                              .resource(c)
                              .search(AbstractEntry.Search.builder().mode(SearchMode.match).build())
                              .build())
                  .collect(Collectors.toList()))
          .build();
    }

    static BundleLink link(BundleLink.LinkRelation relation, String base, int page, int count) {
      return BundleLink.builder()
          .relation(relation)
          .url(base + "&page=" + page + "&_count=" + count)
          .build();
    }

    Patient patient() {
      return patient("1000003");
    }

    Patient patient(String id) {
      return Patient.builder()
          .id(id)
          .resourceType("Patient")
          .extension(
              List.of(
                  Extension.builder()
                      .url("http://fhir.org/guides/argonaut/StructureDefinition/argo-race")
                      .extension(
                          List.of(
                              Extension.builder()
                                  .url("ombCategory")
                                  .valueCoding(
                                      Coding.builder()
                                          .system("http://hl7.org/fhir/v3/Race")
                                          .code("1002-5")
                                          .display("American Indian or Alaska Native")
                                          .build())
                                  .build(),
                              Extension.builder()
                                  .url("text")
                                  .valueString("American Indian or Alaska Native")
                                  .build()))
                      .build(),
                  Extension.builder()
                      .url("http://fhir.org/guides/argonaut/StructureDefinition/argo-birthsex")
                      .valueCode("M")
                      .build()))
          .identifier(
              List.of(
                  Identifier.builder()
                      .use(IdentifierUse.usual)
                      .type(
                          CodeableConcept.builder()
                              .coding(
                                  List.of(
                                      Coding.builder()
                                          .system("http://hl7.org/fhir/v2/0203")
                                          .code("MR")
                                          .build()))
                              .build())
                      .system("http://va.gov/mvi")
                      .value(id)
                      .assigner(Reference.builder().display("Master Veteran Index").build())
                      .build(),
                  Identifier.builder()
                      .use(IdentifierUse.official)
                      .type(
                          CodeableConcept.builder()
                              .coding(
                                  List.of(
                                      Coding.builder()
                                          .system("http://hl7.org/fhir/v2/0203")
                                          .code("SB")
                                          .build()))
                              .build())
                      .system("http://hl7.org/fhir/sid/us-ssn")
                      .value("999-30-3951")
                      .assigner(
                          Reference.builder()
                              .display("United States Social Security Number")
                              .build())
                      .build()))
          .name(
              List.of(
                  HumanName.builder()
                      .use(NameUse.usual)
                      .text("Mr. Tobias236 Wolff180")
                      .family(List.of("Wolff180"))
                      .given(List.of("Tobias236"))
                      .build()))
          .telecom(
              List.of(
                  ContactPoint.builder()
                      .system(ContactPointSystem.phone)
                      .value("5551836103")
                      .use(ContactPointUse.mobile)
                      .build(),
                  ContactPoint.builder()
                      .system(ContactPointSystem.email)
                      .value("Tobias236.Wolff180@email.example")
                      .use(ContactPointUse.home)
                      .build()))
          .gender(Gender.male)
          .birthDate("1970-11-14")
          .deceasedDateTime("2001-03-03T15:08:09Z")
          .multipleBirthBoolean(false)
          .address(
              List.of(
                  Address.builder()
                      .line(List.of("111 MacGyver Viaduct"))
                      .city("Anchorage")
                      .state("Alaska")
                      .postalCode("99501")
                      .build()))
          .maritalStatus(
              CodeableConcept.builder()
                  .coding(
                      List.of(
                          Coding.builder()
                              .system("http://hl7.org/fhir/marital-status")
                              .code("M")
                              .display("Married")
                              .build()))
                  .build())
          .contact(
              List.of(
                  Contact.builder()
                      .address(
                          Address.builder()
                              .city("Mariana")
                              .state("Florida")
                              .line(List.of("222 Caverns Lane"))
                              .postalCode("112233")
                              .build())
                      .name(
                          HumanName.builder()
                              .text("Contact Man")
                              .given(List.of("Given"))
                              .family(List.of("Man"))
                              .build())
                      .build()))
          .build();
    }
  }
}
