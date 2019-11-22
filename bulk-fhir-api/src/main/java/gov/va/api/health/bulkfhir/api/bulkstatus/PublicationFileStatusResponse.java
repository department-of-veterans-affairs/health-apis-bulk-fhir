package gov.va.api.health.bulkfhir.api.bulkstatus;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.time.Instant;
import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PublicationFileStatusResponse {
  @NotNull boolean requiresAccessToken;

  @NotNull String request;

  @NotNull Instant creationDate;

  @NotEmpty List<FileLocation> output;

  @NotEmpty List<FileLocation> error;

  @NotNull Extension extension;

  @Value
  @Builder
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public static class FileLocation {
    @NotNull String type;

    @NotNull String url;
  }

  @Value
  @Builder
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public static class Extension {
    @NotNull
    @Pattern(regexp = "[-A-Za-z0-9]{8,64}")
    String id;

    @Min(1)
    @Max(500_000)
    int recordsPerFile;

    @NotNull Instant creationDate;
  }
}
