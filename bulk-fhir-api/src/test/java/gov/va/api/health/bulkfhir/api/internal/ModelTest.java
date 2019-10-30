package gov.va.api.health.bulkfhir.api.internal;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import gov.va.api.health.bulkfhir.api.internal.PublicationStatus.FileStatus;
import java.time.Instant;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class ModelTest {

  @Test
  void publicationRequest() {
    roundTrip(
        PublicationRequest.builder().publicationId("HokeyPokey123").recordsPerFile(100).build());
  }

  @Test
  void publicationStatus() {
    var now = Instant.now();
    roundTrip(
        PublicationStatus.builder()
            .publicationId("HokeyPokey123")
            .recordsPerFile(100)
            .creationDate(now.minus(24, HOURS))
            .file(
                FileStatus.builder()
                    .fileId("f1")
                    .firstRecord(0)
                    .lastRecord(99)
                    .buildStartTime(now.minus(23, HOURS))
                    .buildCompleteTime(now.minus(22, HOURS))
                    .status(BuildStatus.COMPLETE)
                    .buildProcessorId("incredible-bulk-1")
                    .build())
            .file(
                FileStatus.builder()
                    .fileId("f2")
                    .firstRecord(100)
                    .lastRecord(199)
                    .buildStartTime(now.minus(2, HOURS))
                    .buildCompleteTime(null)
                    .status(BuildStatus.IN_PROGRESS)
                    .buildProcessorId("incredible-bulk-2")
                    .build())
            .file(
                FileStatus.builder()
                    .fileId("f3")
                    .firstRecord(200)
                    .lastRecord(299)
                    .buildStartTime(null)
                    .buildCompleteTime(null)
                    .status(BuildStatus.NOT_STARTED)
                    .buildProcessorId(null)
                    .build())
            .build());
  }

  @SneakyThrows
  private <T> void roundTrip(T object) {
    ObjectMapper mapper = new JacksonConfig().objectMapper();
    String json = mapper.writeValueAsString(object);
    Object evilTwin = mapper.readValue(json, object.getClass());
    assertThat(evilTwin).isEqualTo(object);
  }
}
