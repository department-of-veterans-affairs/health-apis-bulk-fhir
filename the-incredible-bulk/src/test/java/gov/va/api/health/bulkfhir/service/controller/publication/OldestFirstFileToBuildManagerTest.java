package gov.va.api.health.bulkfhir.service.controller.publication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildRequest;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuilderExceptions.FindFileToBuildFailed;
import gov.va.api.health.bulkfhir.service.status.StatusEntity;
import gov.va.api.health.bulkfhir.service.status.StatusRepository;
import java.time.Instant;
import java.util.List;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OldestFirstFileToBuildManagerTest {

  @Mock StatusRepository repository;

  /**
   * @return A list of status entities: [ {pTime: 15, f: c} {pTime: 15, f: b} {pTime: 15, f: a}
   *     {pTime: 1, f: x} {pTime: 2, f: a} {pTime: 20, f: c} {pTime: 20, f: a} ]
   */
  List<StatusEntity> availableFiles() {
    return List.of(
        StatusEntity.builder()
            .publicationId("1")
            .publicationEpoch(Instant.EPOCH.plusSeconds(15).toEpochMilli())
            .fileName("c")
            .build(),
        StatusEntity.builder()
            .publicationId("1")
            .publicationEpoch(Instant.EPOCH.plusSeconds(15).toEpochMilli())
            .fileName("b")
            .build(),
        StatusEntity.builder()
            .publicationId("1")
            .publicationEpoch(Instant.EPOCH.plusSeconds(15).toEpochMilli())
            .fileName("a")
            .build(),
        StatusEntity.builder()
            .publicationId("2")
            .publicationEpoch(Instant.EPOCH.plusSeconds(1).toEpochMilli())
            .fileName("x")
            .build(),
        StatusEntity.builder()
            .publicationId("3")
            .publicationEpoch(Instant.EPOCH.plusSeconds(2).toEpochMilli())
            .fileName("a")
            .build(),
        StatusEntity.builder()
            .publicationId("4")
            .publicationEpoch(Instant.EPOCH.plusSeconds(20).toEpochMilli())
            .fileName("c")
            .build(),
        StatusEntity.builder()
            .publicationId("4")
            .publicationEpoch(Instant.EPOCH.plusSeconds(20).toEpochMilli())
            .fileName("a")
            .build());
  }

  OldestFirstFileToBuildManager buildManager() {
    return OldestFirstFileToBuildManager.builder().repository(repository).build();
  }

  @Test
  public void filesAreSortedByPublicationDateThenFileName() {
    List<StatusEntity> files = buildManager().sortAvailableFiles(availableFiles());
    List<StatusEntity> expectedFiles =
        List.of(
            StatusEntity.builder()
                .publicationId("2")
                .publicationEpoch(Instant.EPOCH.plusSeconds(1).toEpochMilli())
                .fileName("x")
                .build(),
            StatusEntity.builder()
                .publicationId("3")
                .publicationEpoch(Instant.EPOCH.plusSeconds(2).toEpochMilli())
                .fileName("a")
                .build(),
            StatusEntity.builder()
                .publicationId("1")
                .publicationEpoch(Instant.EPOCH.plusSeconds(15).toEpochMilli())
                .fileName("a")
                .build(),
            StatusEntity.builder()
                .publicationId("1")
                .publicationEpoch(Instant.EPOCH.plusSeconds(15).toEpochMilli())
                .fileName("b")
                .build(),
            StatusEntity.builder()
                .publicationId("1")
                .publicationEpoch(Instant.EPOCH.plusSeconds(15).toEpochMilli())
                .fileName("c")
                .build(),
            StatusEntity.builder()
                .publicationId("4")
                .publicationEpoch(Instant.EPOCH.plusSeconds(20).toEpochMilli())
                .fileName("a")
                .build(),
            StatusEntity.builder()
                .publicationId("4")
                .publicationEpoch(Instant.EPOCH.plusSeconds(20).toEpochMilli())
                .fileName("c")
                .build());
    assertThat(files).isEqualTo(expectedFiles);
  }

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void noFileIsReturnedWhenNoFilesHaveYetToBeStartedWithEmptyList() {
    when(repository.findByStatusNotStarted()).thenReturn(Lists.newArrayList());
    FileBuildRequest result = buildManager().getNextFileToBuild();
    assertThat(result).isNull();
  }

  @Test
  public void noFileIsReturnedWhenNoFilesHaveYetToBeStartedWithNull() {
    when(repository.findByStatusNotStarted()).thenReturn(null);
    FileBuildRequest result = buildManager().getNextFileToBuild();
    assertThat(result).isNull();
  }

  @Test(expected = FindFileToBuildFailed.class)
  public void noFileIsReturnedWhenQueryFails() {
    when(repository.findByStatusNotStarted()).thenThrow(new IllegalArgumentException("NOPE"));
    /*
     * We expect an explosion here, the IllegalArgumentException should be converted into a FileToBuildFailed
     * exception.
     */
    buildManager().getNextFileToBuild();
  }

  @Test
  public void theCorrectFileIsSelectedAsTheNextToBeBuilt() {
    when(repository.findByStatusNotStarted()).thenReturn(availableFiles());
    FileBuildRequest result = buildManager().getNextFileToBuild();
    assertThat(result).isEqualTo(FileBuildRequest.builder().publicationId("2").fileId("x").build());
  }
}
