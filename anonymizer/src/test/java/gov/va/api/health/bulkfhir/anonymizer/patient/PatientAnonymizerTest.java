package gov.va.api.health.bulkfhir.anonymizer.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import gov.va.api.health.bulkfhir.anonymizer.SyntheticData;
import gov.va.api.health.bulkfhir.anonymizer.patient.AnonymizerPatientSamples.Anonymized;
import gov.va.api.health.bulkfhir.anonymizer.patient.AnonymizerPatientSamples.Fhir;
import gov.va.api.health.dstu2.api.datatypes.HumanName;
import gov.va.api.health.dstu2.api.datatypes.HumanName.NameUse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PatientAnonymizerTest {

  @Mock SyntheticData syntheticData;

  PatientAnonymizer anonymizer() {
    return PatientAnonymizer.builder().syntheticData(syntheticData).build();
  }

  /**
   * Let's mock the synthesis process. We only want to test that the anonymizer is acting
   * correctly on each patient field.
   */
  @Test
  void apply() {
    when(syntheticData.synthesizeName(Mockito.anyLong()))
        .thenReturn(
            List.of(
                HumanName.builder()
                    .use(NameUse.usual)
                    .text("Mr. Sam Nobody")
                    .family(List.of("Nobody"))
                    .given(List.of("Sam"))
                    .build()));
    when(syntheticData.synthesizeDate("1970-11-14")).thenReturn("1970-10-01");
    when(syntheticData.synthesizeDateTime("2001-03-03T15:08:09Z"))
        .thenReturn("2001-02-01T00:00:00Z");
    assertThat(anonymizer().apply(Fhir.create().patient()))
        .isEqualTo(Anonymized.create().patient());
  }

  @Test
  void icnBasedSeedReturnsHashWhenIcnIsGarbage() {
    assertThat(anonymizer().icnBasedSeed("GARBAGE")).isEqualTo("GARBAGE".hashCode());
  }

  @Test
  void icnBasedSeedReturnsLongWhenIcnIs10V6Format() {
    Long expected = Long.valueOf("0123456789012345");
    assertThat(anonymizer().icnBasedSeed("0123456789V012345")).isEqualTo(expected);
  }

  @Test
  void sanitizeMultipleBirthBoolean() {
    assertThat(anonymizer().sanitizeMultipleBirthBoolean(true, null)).isTrue();
    assertThat(anonymizer().sanitizeMultipleBirthBoolean(false, null)).isFalse();
    assertThat(anonymizer().sanitizeMultipleBirthBoolean(null, 2)).isTrue();
    assertThat(anonymizer().sanitizeMultipleBirthBoolean(null, 0)).isFalse();
    assertThat(anonymizer().sanitizeMultipleBirthBoolean(null, -1)).isFalse();
  }
}
