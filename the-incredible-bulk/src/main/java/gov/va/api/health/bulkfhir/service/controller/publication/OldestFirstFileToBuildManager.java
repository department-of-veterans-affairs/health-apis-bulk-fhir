package gov.va.api.health.bulkfhir.service.controller.publication;

import gov.va.api.health.autoconfig.logging.Loggable;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildRequest;
import gov.va.api.health.bulkfhir.service.filebuilder.FileToBuildManager;
import gov.va.api.health.bulkfhir.service.status.StatusEntity;
import gov.va.api.health.bulkfhir.service.status.StatusRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Builder
@Loggable
@AllArgsConstructor(onConstructor = @__({@Autowired}))
public class OldestFirstFileToBuildManager implements FileToBuildManager {

  private final StatusRepository repository;

  @Override
  public FileBuildRequest getNextFileToBuild() {
    List<StatusEntity> availableFiles = repository.findByStatusNotStarted(PageRequest.of(0, 1));
    if (availableFiles == null || availableFiles.isEmpty()) {
      /*
       * No files that haven't been started yet have been found.
       */
      log.info("Didn't find any files available to be started.");
      return null;
    }

    /*
     * The results should have been pre-sorted and only one should have been returned.
     */
    StatusEntity fileToBuild = availableFiles.get(0);

    log.info("Found file to build next {}", fileToBuild);
    return FileBuildRequest.builder()
        .publicationId(fileToBuild.publicationId())
        .fileId(fileToBuild.fileName())
        .build();
  }
}
