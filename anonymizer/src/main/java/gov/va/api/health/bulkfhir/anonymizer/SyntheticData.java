package gov.va.api.health.bulkfhir.anonymizer;

import gov.va.api.health.dstu2.api.datatypes.HumanName;
import java.util.List;

public interface SyntheticData {

  String synthesizeDate(String rawBirthdate);

  String synthesizeDateTime(String rawDateTime);

  List<HumanName> synthesizeName(long seed);
}
