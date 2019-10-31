package gov.va.api.health.bulkfhir.api.internal;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PublicationRequest {
  @NotNull
  @Pattern(regexp = "[-A-Za-z0-9]{8,64}")
  String publicationId;

  @Min(1)
  @Max(500_000)
  int recordsPerFile;
}
