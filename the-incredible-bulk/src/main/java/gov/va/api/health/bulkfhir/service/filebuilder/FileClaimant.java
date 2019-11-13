package gov.va.api.health.bulkfhir.service.filebuilder;

/**
 * The Claimant interacts with the persistence layer to claim a file for exclusive rights. If
 * successful, the persistence store will be updated to indicate this application instance has
 * claimed it.
 */
public interface FileClaimant {

  /** Mark the file as completed. */
  void completeClaim(FileBuildRequest request);

  /**
   * Attempt to claim a file. This method will _attempt_ to throw AlreadyClaimed exceptions if a
   * claim cannot be made. However, it is possible that other exceptions could be thrown, e.g.
   * database exceptions that should already be interpreted as claim failures.
   */
  FileClaim tryClaim(FileBuildRequest request);
}
