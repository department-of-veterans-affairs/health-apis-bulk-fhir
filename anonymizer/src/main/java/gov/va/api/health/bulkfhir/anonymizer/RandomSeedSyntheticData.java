package gov.va.api.health.bulkfhir.anonymizer;

import gov.va.api.health.dstu2.api.datatypes.HumanName;
import java.util.List;

public class RandomSeedSyntheticData implements SyntheticData {

  @Override
  public String synthesizeDate(String rawBirthdate) {
    return null;
  }

  @Override
  public List<HumanName> synthesizeName(List<HumanName> name) {
    return null;
  }
}
