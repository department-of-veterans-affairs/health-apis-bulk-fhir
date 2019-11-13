package gov.va.api.health.bulkfhir.service.controller.publication;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PublicationExceptions {

  static void assertDoesNotExist(boolean exists, String publicationId) {
    if (exists) {
      throw new PublicationAlreadyExists(publicationId);
    }
  }

  static void assertPublicationFileFound(boolean found, String publicationId, String fileId) {
    if (!found) {
      throw new PublicationFileNotFound(publicationId, fileId);
    }
  }

  static void assertPublicationFound(boolean found, String publicationId) {
    if (!found) {
      throw new PublicationNotFound(publicationId);
    }
  }

  static void assertRecordsPerFile(int recordsPerFile, int maxAllowed) {
    if (recordsPerFile > maxAllowed) {
      throw new PublicationRecordsPerFileTooBig(recordsPerFile, maxAllowed);
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
  }

  public static class PublicationFileNotFound extends PublicationException {

    public PublicationFileNotFound(String publicationId, String fileId) {
      super(publicationId + " file: " + fileId);
    }
  }

  public static class PublicationNotFound extends PublicationException {

    public PublicationNotFound(String publicationId) {
      super(publicationId);
    }
  }

  public static class PublicationRecordsPerFileTooBig extends PublicationException {

    public PublicationRecordsPerFileTooBig(int recordsPerFile, int maxAllowed) {
      super("Requested: " + recordsPerFile + ", max allowed: " + maxAllowed);
    }
  }
}
