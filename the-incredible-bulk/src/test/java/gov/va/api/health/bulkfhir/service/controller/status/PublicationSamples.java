package gov.va.api.health.bulkfhir.service.controller.status;

import static java.time.temporal.ChronoUnit.HOURS;

import gov.va.api.health.bulkfhir.api.internal.BuildStatus;
import gov.va.api.health.bulkfhir.api.internal.PublicationStatus;
import gov.va.api.health.bulkfhir.api.internal.PublicationStatus.FileStatus;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import lombok.NoArgsConstructor;

class PublicationSamples {

  private static final Instant THEN = Instant.parse("2005-01-21T07:57:00Z");

  @NoArgsConstructor(staticName = "create")
  static class Api {
    PublicationStatus status() {
      return PublicationStatus.builder()
          .publicationId("HokeyPokey123")
          .recordsPerFile(100)
          .creationDate(THEN.minus(24, HOURS))
          .overallStatus(BuildStatus.IN_PROGRESS)
          .file(
              FileStatus.builder()
                  .fileId("f1")
                  .firstRecord(0)
                  .lastRecord(99)
                  .buildStartTime(THEN.minus(23, HOURS))
                  .buildCompleteTime(THEN.minus(22, HOURS))
                  .status(BuildStatus.COMPLETE)
                  .buildProcessorId("incredible-bulk-1")
                  .build())
          .file(
              FileStatus.builder()
                  .fileId("f2")
                  .firstRecord(100)
                  .lastRecord(199)
                  .buildStartTime(THEN.minus(2, HOURS))
                  .buildCompleteTime(null)
                  .status(BuildStatus.IN_PROGRESS)
                  .buildProcessorId("incredible-bulk-2")
                  .build())
          .file(
              FileStatus.builder()
                  .fileId("f3")
                  .firstRecord(200)
                  .lastRecord(232)
                  .buildStartTime(null)
                  .buildCompleteTime(null)
                  .status(BuildStatus.NOT_STARTED)
                  .buildProcessorId(null)
                  .build())
          .build();
    }
  }

  @NoArgsConstructor(staticName = "create")
  static class Entity {
    List<StatusEntity> entities(Supplier<String> ids) {
      String publicationId = "HokeyPokey123";
      int recordsPerFile = 100;
      Instant creationDate = THEN.minus(24, HOURS);
      return List.of(
          StatusEntity.builder()
              .id(ids.get())
              .publicationId(publicationId)
              .recordsPerFile(recordsPerFile)
              .publicationEpoch(creationDate.toEpochMilli())
              .fileName("f1")
              .page(1)
              .count(100)
              .buildStartEpoch(THEN.minus(23, HOURS).toEpochMilli())
              .buildCompleteEpoch(THEN.minus(22, HOURS).toEpochMilli())
              .buildProcessorId("incredible-bulk-1")
              .build(),
          StatusEntity.builder()
              .id(ids.get())
              .publicationId(publicationId)
              .recordsPerFile(recordsPerFile)
              .publicationEpoch(creationDate.toEpochMilli())
              .fileName("f2")
              .page(2)
              .count(100)
              .buildStartEpoch(THEN.minus(2, HOURS).toEpochMilli())
              .buildCompleteEpoch(0)
              .buildProcessorId("incredible-bulk-2")
              .build(),
          StatusEntity.builder()
              .id(ids.get())
              .publicationId(publicationId)
              .recordsPerFile(recordsPerFile)
              .publicationEpoch(creationDate.toEpochMilli())
              .fileName("f3")
              .page(3)
              .count(33)
              .buildStartEpoch(0)
              .buildCompleteEpoch(0)
              .buildProcessorId(null)
              .build());
    }

    List<StatusEntity> entitiesWithIds() {
      AtomicInteger id = new AtomicInteger(0);
      return entities(() -> "" + id.incrementAndGet());
    }

    List<StatusEntity> entitiesWithoutIds() {
      return entities(() -> null);
    }
  }
}
