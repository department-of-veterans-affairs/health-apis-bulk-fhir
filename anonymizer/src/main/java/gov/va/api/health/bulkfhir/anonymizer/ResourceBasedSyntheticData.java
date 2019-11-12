package gov.va.api.health.bulkfhir.anonymizer;

import gov.va.api.health.dstu2.api.datatypes.HumanName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@Builder
public class ResourceBasedSyntheticData implements SyntheticData {

  private final Names names;

  /**
   * We synthesize Dates and DateTimes by using: month = previous month day = first of the month
   * This technique is a one way transformation and can't be reversed.
   */
  @Override
  public String synthesizeDate(String rawDate) {
    if (StringUtils.isBlank(rawDate)) {
      return null;
    }
    LocalDate date;
    try {
      date = LocalDate.parse(rawDate);
      date = date.withMonth(date.getMonthValue() - 1);
      date = date.withDayOfMonth(1);
    } catch (DateTimeParseException e) {
      log.info("Unable to parse the date [{}], using a default value.", rawDate);
      date = LocalDate.of(2000, 1, 1);
    }
    return date.toString();
  }

  @Override
  public String synthesizeDateTime(String rawDateTime) {
    if (StringUtils.isBlank(rawDateTime)) {
      return null;
    }
    LocalDateTime date;
    try {
      date = LocalDateTime.parse(rawDateTime);
      date = date.withMonth(date.getMonthValue() - 1);
      date = date.withDayOfMonth(1);
    } catch (DateTimeParseException e) {
      log.info("Unable to parse the dateTime [{}], using a defualt value.", rawDateTime);
      date = LocalDateTime.of(2000, 1, 1, 1, 1, 1);
    }
    return date.toString();
  }

  /**
   * We synthesize name using the ICN as a seed for selecting a name from a synthetic names
   * resource. By using ICN as a seed, the name transformation is repeatable.
   */
  @Override
  public List<HumanName> synthesizeName(long seed) {
    var givenName = names.getName(seed);
    var familyName = names.getName(seed + 1000);
    return List.of(
        HumanName.builder()
            .family(List.of(familyName))
            .given(List.of(givenName))
            .text(givenName + " " + familyName)
            .build());
  }
}
