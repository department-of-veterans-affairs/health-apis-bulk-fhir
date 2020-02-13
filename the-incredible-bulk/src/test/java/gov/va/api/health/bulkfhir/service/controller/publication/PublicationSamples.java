package gov.va.api.health.bulkfhir.service.controller.publication;

import static java.time.temporal.ChronoUnit.HOURS;

import gov.va.api.health.bulkfhir.api.internal.BuildStatus;
import gov.va.api.health.bulkfhir.api.internal.PublicationStatus;
import gov.va.api.health.bulkfhir.api.internal.PublicationStatus.FileStatus;
import gov.va.api.health.bulkfhir.service.status.StatusEntity;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
          .files(
              List.of(
                  FileStatus.builder()
                      .fileId("f1")
                      .firstRecord(0)
                      .lastRecord(99)
                      .buildStartTime(THEN.minus(23, HOURS))
                      .buildCompleteTime(THEN.minus(22, HOURS))
                      .status(BuildStatus.COMPLETE)
                      .buildProcessorId("incredible-bulk-1")
                      .build(),
                  FileStatus.builder()
                      .fileId("f2")
                      .firstRecord(100)
                      .lastRecord(199)
                      .buildStartTime(THEN.minus(2, HOURS))
                      .buildCompleteTime(null)
                      .status(BuildStatus.IN_PROGRESS)
                      .buildProcessorId("incredible-bulk-2")
                      .build(),
                  FileStatus.builder()
                      .fileId("f3")
                      .firstRecord(200)
                      .lastRecord(299)
                      .buildStartTime(null)
                      .buildCompleteTime(null)
                      .status(BuildStatus.NOT_STARTED)
                      .buildProcessorId(null)
                      .build(),
                  FileStatus.builder()
                      .fileId("f4")
                      .firstRecord(300)
                      .lastRecord(343)
                      .buildStartTime(THEN.minus(4, HOURS))
                      .buildCompleteTime(null)
                      .status(BuildStatus.IN_PROGRESS)
                      .buildProcessorId("incredible-bulk-4")
                      .build()))
          .build();
    }
  }

  @NoArgsConstructor(staticName = "create")
  static class Entity {
    List<StatusEntity> entities(Supplier<String> ids, Instant when) {
      String publicationId = "HokeyPokey123";
      int recordsPerFile = 100;
      Instant creationDate = when.minus(24, HOURS);
      return List.of(
          StatusEntity.builder()
              .id(ids.get())
              .publicationId(publicationId)
              .recordsPerFile(recordsPerFile)
              .publicationEpoch(creationDate.toEpochMilli())
              .fileName("f1")
              .page(1)
              .count(100)
              .buildStartEpoch(when.minus(23, HOURS).toEpochMilli())
              .buildCompleteEpoch(when.minus(22, HOURS).toEpochMilli())
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
              .buildStartEpoch(when.minus(2, HOURS).toEpochMilli())
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
              .count(100)
              .buildStartEpoch(0)
              .buildCompleteEpoch(0)
              .buildProcessorId(null)
              .build(),
          StatusEntity.builder()
              .id(ids.get())
              .publicationId(publicationId)
              .recordsPerFile(recordsPerFile)
              .publicationEpoch(creationDate.toEpochMilli())
              .fileName("f4")
              .page(4)
              .count(44)
              .buildStartEpoch(when.minus(4, HOURS).toEpochMilli())
              .buildCompleteEpoch(0)
              .buildProcessorId("incredible-bulk-4")
              .build());
    }

    List<StatusEntity> entitiesInProgress() {
      AtomicInteger id = new AtomicInteger(0);
      return entities(() -> "IP" + id.incrementAndGet(), Instant.now()).stream()
          // Imitate the database query
          .filter(e -> e.buildStartEpoch() > 0 && e.buildCompleteEpoch() == 0)
          .collect(Collectors.toList());
    }

    List<StatusEntity> entitiesWithIds() {
      AtomicInteger id = new AtomicInteger(0);
      return entities(() -> "" + id.incrementAndGet(), THEN);
    }

    List<StatusEntity> entitiesWithoutIds() {
      return entities(() -> null, THEN);
    }
  }
}
