package gov.va.api.health.bulkfhir.anonymizer;

/** This interface will be used to generate an anonymized ID given the original resources id. */
public interface AnonymizedIdGenerator {

  String generateIdFrom(String identifier);
}
