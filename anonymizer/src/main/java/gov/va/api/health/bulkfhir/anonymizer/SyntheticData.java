package gov.va.api.health.bulkfhir.anonymizer;

import gov.va.api.health.dstu2.api.datatypes.HumanName;
import java.util.List;

/**
 * The SyntheticData interface defines the set of FHIR PII fields that must be anonymized.
 * Implementations will define the details of synthesis. For example, class path resource based
 * implementation exists in ResourceBasedSyntheticData.java
 */
public interface SyntheticData {

  String synthesizeDate(String rawBirthdate);

  String synthesizeDateTime(String rawDateTime);

  List<HumanName> synthesizeName(long seed);
}
