package gov.va.api.health.bulkfhir.api.internal;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.time.Duration;
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

  @NotNull Duration hangTime;
  
}
