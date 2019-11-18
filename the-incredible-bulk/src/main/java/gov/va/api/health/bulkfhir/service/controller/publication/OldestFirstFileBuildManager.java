package gov.va.api.health.bulkfhir.service.controller.publication;

import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildManager;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildRequest;
import gov.va.api.health.bulkfhir.service.status.StatusEntity;
import gov.va.api.health.bulkfhir.service.status.StatusRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Builder
@AllArgsConstructor(onConstructor = @__({@Autowired}))
public class OldestFirstFileBuildManager implements FileBuildManager {

  private final StatusRepository repository;

  @Override
  public FileBuildRequest getNextFileToBuild() {
    try {
      List<StatusEntity> availableFiles = repository.findByStatusNotStarted();
      if (availableFiles == null || availableFiles.isEmpty()) {
        /*
         * No files that haven't been started yet have been found.
         */
        log.debug("Didn't find any files available to be started.");
        return null;
      }

      /*
       * Sort the files
       */
      List<StatusEntity> sortedFiles = sortAvailableFiles(availableFiles);

      StatusEntity fileToBuild = sortedFiles.get(0);

      return FileBuildRequest.builder()
          .publicationId(fileToBuild.publicationId())
          .fileId(fileToBuild.fileName())
          .build();
    } catch (Exception e) {
      log.error("Failed to get not yet started publication files.", e);
      return null;
    }
  }

  /**
   * Sort the given list of files by earliest publication date, then by file name.
   *
   * @param availableFiles The list of available files to sort
   * @return The sorted list of files
   */
  public List<StatusEntity> sortAvailableFiles(final List<StatusEntity> availableFiles) {
    List<StatusEntity> sortedFiles = new ArrayList<>(availableFiles);
    sortedFiles.sort(
        Comparator.<StatusEntity>comparingLong(StatusEntity::publicationEpoch)
            .thenComparing((a, b) -> a.fileName().compareTo(b.fileName())));
    return sortedFiles;
  }
}
