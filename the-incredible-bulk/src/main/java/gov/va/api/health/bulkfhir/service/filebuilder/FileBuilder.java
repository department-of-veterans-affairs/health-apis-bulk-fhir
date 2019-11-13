package gov.va.api.health.bulkfhir.service.filebuilder;

import gov.va.api.health.bulkfhir.api.internal.FileBuildResponse;

/**
 * The FileBuilder is the starting point to build a file. It will not do the work itself, but
 * orchestrate activities with {@link FileBuildWorker} instances.
 *
 * <p>The FileBuilder is meant to work synchronously and should respond immediately, after assigning
 * tasks to workers that will perform the work in the background.
 */
public interface FileBuilder {
  /**
   * Build this file regardless of its status. If the file has been completed or is already in the
   * progress, it will be restarted.
   *
   * <p>This method should return immediately and does not guarantee the work to build a file is
   * complete. However, it does guarantee the work has been identified and background tasks have
   * been created.
   */
  FileBuildResponse buildFile(FileBuildRequest request);
}
