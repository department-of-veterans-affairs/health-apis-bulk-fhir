package gov.va.api.health.bulkfhir.service.filebuilder;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FileBuilderExceptions {

  public static String asMessage(String publicationId, String fileId) {
    return "Publication: " + publicationId + " File: " + fileId;
  }

  public static class AlreadyClaimed extends ClaimFailed {
    public AlreadyClaimed(String publicationId, String fileId) {
      super(publicationId, fileId, null);
    }
  }

  public static class BuildFailed extends RuntimeException {
    public BuildFailed(String publicationId, String fileId, Throwable cause) {
      super(asMessage(publicationId, fileId), cause);
    }
  }

  public static class ClaimFailed extends RuntimeException {
    public ClaimFailed(String publicationId, String fileId, Throwable cause) {
      super(asMessage(publicationId, fileId), cause);
    }
  }
}
