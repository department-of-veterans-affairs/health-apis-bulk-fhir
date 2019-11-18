package gov.va.api.health.bulkfhir.service.filebuilder;

/** This interface provides a manager for distributing file assignments to build publications. */
public interface FileBuildManager {

  /**
   * Get the next file that should be built.
   *
   * @return A build request for the next file to build, or <code>null</code> if there are no files
   *     to build.
   */
  FileBuildRequest getNextFileToBuild();
}
