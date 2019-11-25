package gov.va.api.health.bulkfhir.anonymizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import gov.va.api.health.dstu2.api.datatypes.HumanName;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResourceBasedSyntheticDataTest {

  /** Let's mock the names implementation here, and only worry about synthesis. */
  @Mock Names names;

  LocalDate now = LocalDate.now();

  @Test
  void synthesizeDate() {
    assertThat(syntheticData().synthesizeDate(now.toString()))
        .isEqualTo(now.withMonth(1).withDayOfMonth(1).toString());
  }

  @Test
  void synthesizeDateReturnsDefaultValueOnParseException() {
    assertThat(syntheticData().synthesizeDate("GARBAGE")).isEqualTo("2000-01-01");
  }

  @Test
  void synthesizeDateReturnsNullWhenNoDate() {
    assertThat(syntheticData().synthesizeDate(null)).isNull();
    assertThat(syntheticData().synthesizeDate("")).isNull();
  }

  @Test
  void synthesizeDateTime() {
    assertThat(syntheticData().synthesizeDateTime(now.getYear() + "-09-29T11:11:11Z"))
        .isEqualTo(now.getYear() + "-01-01T12:34:56Z");
  }

  @Test
  void synthesizeDateTimeReturnsDefaultValueOnParseException() {
    assertThat(syntheticData().synthesizeDateTime("GARBAGE")).isEqualTo("2000-01-01T01:01:01Z");
  }

  @Test
  void synthesizeDateTimeReturnsNullWhenNoDateTime() {
    assertThat(syntheticData().synthesizeDateTime(null)).isNull();
    assertThat(syntheticData().synthesizeDateTime("")).isNull();
  }

  @Test
  void synthesizeDateTimeWithAgeGreaterThanNinety() {
    assertThat(syntheticData().synthesizeDateTime("1900-12-12T11:11:11Z"))
        .isEqualTo(now.minusYears(90).getYear() + "-01-01T12:34:56Z");
  }

  @Test
  void synthesizeDateWithAgeGreaterThanNinety() {
    assertThat(syntheticData().synthesizeDate("1900-12-12"))
        .isEqualTo(now.minusYears(90).getYear() + "-01-01");
  }

  @Test
  void synthesizeName() {
    when(names.getName(1)).thenReturn("Aaron");
    when(names.getName(1001)).thenReturn("Blake");
    var expected =
        List.of(
            HumanName.builder()
                .given(List.of("Aaron"))
                .family(List.of("Blake"))
                .text("Aaron Blake")
                .build());
    assertThat(syntheticData().synthesizeName(1)).isEqualTo(expected);
  }

  ResourceBasedSyntheticData syntheticData() {
    return ResourceBasedSyntheticData.builder().names(names).build();
  }
}
