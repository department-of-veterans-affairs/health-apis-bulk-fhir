package gov.va.api.health.bulkfhir.service.controller.status;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PublicationExceptions {

  public class PublicationAlreadyExists extends PublicationException {
    public PublicationAlreadyExists(String publicationId) {
      super(publicationId);
    }
  }

  public class PublicationException extends RuntimeException {
    PublicationException(String message) {
      super(message);
    }

    PublicationException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public class PublicationNotFound extends PublicationException {
    public PublicationNotFound(String publicationId) {
      super(publicationId);
    }
  }
}
