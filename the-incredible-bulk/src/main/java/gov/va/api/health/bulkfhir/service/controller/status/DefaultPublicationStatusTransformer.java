package gov.va.api.health.bulkfhir.service.controller.status;

import gov.va.api.health.bulkfhir.api.internal.BuildStatus;
import gov.va.api.health.bulkfhir.api.internal.PublicationStatus;
import gov.va.api.health.bulkfhir.api.internal.PublicationStatus.FileStatus;
import gov.va.api.health.bulkfhir.api.internal.PublicationStatus.PublicationStatusBuilder;
import java.time.Instant;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultPublicationStatusTransformer implements PublicationStatusTransformer {

  @Override
  public PublicationStatus apply(Iterable<StatusEntity> statusEntities) {
    log.info("{}", statusEntities);
    Counter counter = new Counter();
    PublicationStatusBuilder publication = PublicationStatus.builder();
    for (var entity : statusEntities) {
      if (counter.total() == 0) {
        publication
            .publicationId(entity.publicationId())
            .recordsPerFile(entity.recordsPerFile())
            .creationDate(entity.publicationTime().orElse(null));
      }
      FileStatus fileStatus = toStatus(entity);
      counter.add(fileStatus.status());
      publication.file(fileStatus);
    }
    return publication.overallStatus(counter.overallStatus()).build();
  }

  private BuildStatus statusOf(Optional<Instant> start, Optional<Instant> complete) {
    if (complete.isPresent()) {
      return BuildStatus.COMPLETE;
    }
    if (start.isPresent()) {
      return BuildStatus.IN_PROGRESS;
    }
    return BuildStatus.NOT_STARTED;
  }

  private FileStatus toStatus(StatusEntity entity) {
    int firstRecord = (entity.page() - 1) * entity.recordsPerFile();
    Optional<Instant> start = entity.buildStartTime();
    Optional<Instant> complete = entity.buildCompleteTime();
    return FileStatus.builder()
        .fileId(entity.fileName())
        .firstRecord(firstRecord)
        .lastRecord(firstRecord + entity.count() - 1)
        .status(statusOf(start, complete))
        .buildStartTime(start.orElse(null))
        .buildCompleteTime(complete.orElse(null))
        .buildProcessorId(entity.buildProcessorId())
        .build();
  }

  static class Counter {
    int notStarted;
    int inProgress;
    int completed;

    void add(BuildStatus status) {
      if (status == BuildStatus.IN_PROGRESS) {
        inProgress++;
      }
      if (status == BuildStatus.COMPLETE) {
        completed++;
      }
      notStarted++;
    }

    BuildStatus overallStatus() {
      /* Something is in progress. */
      if (inProgress != 0) {
        return BuildStatus.IN_PROGRESS;
      }
      /*
       * Nothing is actively in progress, but some files have not been started and some have
       * already completed.
       */
      if (notStarted != 0 && completed != 0) {
        return BuildStatus.IN_PROGRESS;
      }
      /* Everything has completed. */
      if (notStarted == 0 && completed != 0) {
        return BuildStatus.COMPLETE;
      }
      return BuildStatus.NOT_STARTED;
    }

    int total() {
      return notStarted + inProgress + completed;
    }
  }
}
