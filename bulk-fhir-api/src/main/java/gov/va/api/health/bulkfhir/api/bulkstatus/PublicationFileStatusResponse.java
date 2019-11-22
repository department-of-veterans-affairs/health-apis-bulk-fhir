package gov.va.api.health.bulkfhir.api.bulkstatus;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

@Value
@Builder
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PublicationFileStatusResponse {
  @NotNull
  boolean requiresAccessToken;

  @NotNull
  String request;

  @NotNull Instant creationDate;

  @NotEmpty List<FileLocation> output;

  @NotEmpty List<FileLocation> error;

  @Value
  @Builder
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public static class FileLocation {
    @NotNull
    String type;

    @NotNull
    String url;
  }
}
