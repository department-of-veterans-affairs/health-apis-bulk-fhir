package gov.va.api.health.bulkfhir.service.controller.publication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atMostOnce;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.va.api.health.argonaut.api.resources.Patient;
import gov.va.api.health.bulkfhir.service.controller.JsonStringConverter;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JsonStringConverterTest {

  @Mock ObjectMapper jacksonMapper;

  JsonStringConverter converter() {
    return JsonStringConverter.builder().jacksonMapper(jacksonMapper).build();
  }

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @SneakyThrows
  public void jsonStringConverterDoesNotAttemptToConvertNull() {
    assertThat(converter().apply(null)).isNull();
    verify(jacksonMapper, never()).writeValueAsString(any());
  }

  @Test
  @SneakyThrows
  public void jsonStringConverterProperlyConvertsString() {
    when(jacksonMapper.writeValueAsString(any())).thenReturn("HI");
    assertThat(converter().apply(Patient.builder().build())).isEqualTo("HI");
    verify(jacksonMapper, atMostOnce()).writeValueAsString(any());
  }

  @Test
  @SneakyThrows
  public void jsonStringConverterReturnsNullOnFailure() {
    when(jacksonMapper.writeValueAsString(any()))
        .thenThrow(new JsonProcessingException("YOU CANT DO THAT") {});
    assertThat(converter().apply(Patient.builder().build())).isNull();
    verify(jacksonMapper, atMostOnce()).writeValueAsString(any());
  }
}
