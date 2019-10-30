package gov.va.api.health.bulkfhir.api.internal;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.time.Instant;
import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PublicationStatus {
  @NotNull
  @Pattern(regexp = "[-A-Za-z0-9]{8,64}")
  String publicationId;

  @Min(1)
  @Max(500_000)
  int recordsPerFile;

  @NotNull Instant creationDate;

  @NotNull BuildStatus overallStatus;

  @Singular @NotEmpty List<FileStatus> files;

  @Value
  @Builder
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public static class FileStatus {
    @NotNull
    @Pattern(regexp = "[-A-Za-z0-9]{8,64}")
    String fileId;

    @Min(0)
    int firstRecord;

    @Min(0)
    int lastRecord;

    @NotNull BuildStatus status;
    @NotNull Instant buildStartTime;
    @NotNull Instant buildCompleteTime;

    @NotBlank
    @Max(64)
    String buildProcessorId;
  }
}
