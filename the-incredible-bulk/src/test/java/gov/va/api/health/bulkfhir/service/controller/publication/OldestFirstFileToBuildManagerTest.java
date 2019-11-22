package gov.va.api.health.bulkfhir.service.controller.publication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildRequest;
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

  OldestFirstFileToBuildManager buildManager() {
    return OldestFirstFileToBuildManager.builder().repository(repository).build();
  }

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void noFileIsReturnedWhenNoFilesHaveYetToBeStartedWithEmptyList() {
    when(repository.findByStatusNotStarted(any())).thenReturn(Lists.newArrayList());
    FileBuildRequest result = buildManager().getNextFileToBuild();
    assertThat(result).isNull();
  }

  @Test
  public void noFileIsReturnedWhenNoFilesHaveYetToBeStartedWithNull() {
    when(repository.findByStatusNotStarted(any())).thenReturn(null);
    FileBuildRequest result = buildManager().getNextFileToBuild();
    assertThat(result).isNull();
  }

  @Test(expected = IllegalArgumentException.class)
  public void noFileIsReturnedWhenQueryFails() {
    when(repository.findByStatusNotStarted(any())).thenThrow(new IllegalArgumentException("NOPE"));
    /*
     * We expect an explosion here, the IllegalArgumentException should be converted into a FileToBuildFailed
     * exception.
     */
    buildManager().getNextFileToBuild();
  }

  @Test
  public void theCorrectFileIsSelectedAsTheNextToBeBuilt() {
    when(repository.findByStatusNotStarted(any()))
        .thenReturn(
            List.of(
                StatusEntity.builder()
                    .publicationId("2")
                    .publicationEpoch(Instant.EPOCH.plusSeconds(1).toEpochMilli())
                    .fileName("x")
                    .build()));
    FileBuildRequest result = buildManager().getNextFileToBuild();
    assertThat(result).isEqualTo(FileBuildRequest.builder().publicationId("2").fileId("x").build());
  }
}
