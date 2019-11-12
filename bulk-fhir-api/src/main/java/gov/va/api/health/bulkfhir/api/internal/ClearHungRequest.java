package gov.va.api.health.bulkfhir.api.internal;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

/**
 * The `/internal/publication/hung` endpoint will be automatically triggered using a timer. This is
 * the post body for the endpoint should someone wish to run it manually (testing purposes, etc.).
 */
@Value
@Builder
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ClearHungRequest {

  @NotNull String hangTime;

  /** Convert the input string to a Duration when getting hangTime. */
  public Duration hangTime() {
    String[] duration = hangTime.split(" ");
    if (duration.length != 2) {
      throw new RuntimeException("String hangTime is incorrectly formatted: " + hangTime);
    }
    return Duration.of(Long.parseLong(duration[0]), ChronoUnit.valueOf(duration[1].toUpperCase()));
  }
}
