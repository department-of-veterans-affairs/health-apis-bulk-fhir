package gov.va.api.health.bulkfhir.service.controller.status;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PublicationExceptions {

  static void assertPublicationFound(boolean found, String publicationId) {
    if (!found) {
      throw new PublicationNotFound(publicationId);
    }
  }

  static void assertDoesNotExist(boolean exists, String publicationId) {
    if (exists) {
      throw new PublicationAlreadyExists(publicationId);
    }
  }

  public static class PublicationAlreadyExists extends PublicationException {
    public PublicationAlreadyExists(String publicationId) {
      super(publicationId);
    }
  }

  public static class PublicationException extends RuntimeException {
    PublicationException(String message) {
      super(message);
    }

    PublicationException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static class PublicationNotFound extends PublicationException {
    public PublicationNotFound(String publicationId) {
      super(publicationId);
    }
  }
}
