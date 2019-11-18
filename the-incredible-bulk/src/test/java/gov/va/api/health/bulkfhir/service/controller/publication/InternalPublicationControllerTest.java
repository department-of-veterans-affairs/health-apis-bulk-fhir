package gov.va.api.health.bulkfhir.service.controller.publication;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import gov.va.api.health.bulkfhir.api.internal.ClearHungRequest;
import gov.va.api.health.bulkfhir.api.internal.FileBuildResponse;
import gov.va.api.health.bulkfhir.api.internal.PublicationRequest;
import gov.va.api.health.bulkfhir.service.controller.publication.PublicationExceptions.PublicationAlreadyExists;
import gov.va.api.health.bulkfhir.service.controller.publication.PublicationExceptions.PublicationNotFound;
import gov.va.api.health.bulkfhir.service.controller.publication.PublicationExceptions.PublicationRecordsPerFileTooBig;
import gov.va.api.health.bulkfhir.service.controller.publication.PublicationSamples.Api;
import gov.va.api.health.bulkfhir.service.dataquery.client.DataQueryBatchClient;
import gov.va.api.health.bulkfhir.service.dataquery.client.DataQueryBatchClient.ResourceCount;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildManager;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildRequest;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuilder;
import gov.va.api.health.bulkfhir.service.status.StatusEntity;
import gov.va.api.health.bulkfhir.service.status.StatusRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InternalPublicationControllerTest {

  @Mock StatusRepository repo;

  @Mock PublicationStatusTransformer tx;

  @Mock DataQueryBatchClient dq;

  @Mock FileBuilder fileBuilder;

  @Mock FileBuildManager fileBuildManager;

  private void assertStatusEntityCreated(
      StatusEntity entity,
      String publicationId,
      int recordsPerFile,
      int page,
      int count,
      String fileName) {
    assertThat(entity.publicationId()).isEqualTo(publicationId);
    assertThat(entity.recordsPerFile()).isEqualTo(recordsPerFile);
    assertThat(entity.fileName()).isEqualTo(fileName);
    assertThat(entity.page()).isEqualTo(page);
    assertThat(entity.count()).isEqualTo(count);
    assertThat(entity.buildStartEpoch()).isZero();
    assertThat(entity.buildCompleteEpoch()).isZero();
    assertThat(entity.buildProcessorId()).isNull();
  }

  @Test
  void buildForwardsCallsToBuildManager() {
    var response = FileBuildResponse.builder().publicationId("x").fileId("a").build();
    when(fileBuilder.buildFile(FileBuildRequest.builder().publicationId("x").fileId("a").build()))
        .thenReturn(response);
    assertThat(controller().buildFile("x", "a")).isSameAs(response);
    verify(fileBuilder)
        .buildFile(FileBuildRequest.builder().publicationId("x").fileId("a").build());
    verifyNoMoreInteractions(fileBuilder);
  }

  @Test
  void buildNextFileCallsFileBuilderWhenManagerReturnsFile() {
    when(fileBuildManager.getNextFileToBuild())
        .thenReturn(FileBuildRequest.builder().publicationId("x").fileId("a").build());
    var response = FileBuildResponse.builder().publicationId("x").fileId("a").build();
    when(fileBuilder.buildFile(FileBuildRequest.builder().publicationId("x").fileId("a").build()))
        .thenReturn(response);
    assertThat(controller().buildNextFile()).isSameAs(response);
    verify(fileBuilder)
        .buildFile(FileBuildRequest.builder().publicationId("x").fileId("a").build());
    verifyNoMoreInteractions(fileBuilder);
  }

  @Test
  void buildNextFileDoesNothingWhenManagerDoesNotReturnsFile() {
    when(fileBuildManager.getNextFileToBuild()).thenReturn(null);
    assertThat(controller().buildNextFile()).isNull();
    verifyNoMoreInteractions(fileBuilder);
  }

  InternalPublicationController controller() {
    return InternalPublicationController.builder()
        .repository(repo)
        .dataQuery(dq)
        .transformer(tx)
        .fileBuilder(fileBuilder)
        .fileBuildManager(fileBuildManager)
        .build();
  }

  @Test
  void deletePublicationDeletesPublication() {
    when(repo.deleteByPublicationId("x")).thenReturn(5);
    controller().deletePublication("x");
    verify(repo).deleteByPublicationId("x");
  }

  @Test
  void deletePublicationThrowsExceptionForUnknownPublication() {
    when(repo.deleteByPublicationId("x")).thenReturn(0);
    assertThrows(PublicationNotFound.class, () -> controller().deletePublication("x"));
  }

  @Test
  void getPublicationIdReturnsOverallStatus() {
    var entities = PublicationSamples.Entity.create().entitiesWithoutIds();
    var status = Api.create().status();
    var publicationId = entities.get(0).publicationId();
    when(repo.findByPublicationId(publicationId)).thenReturn(entities);
    when(tx.apply(entities)).thenReturn(status);
    assertThat(controller().getPublicationStatus(publicationId)).isEqualTo(status);
  }

  @Test
  void getPublicationIdThrowsNotFoundForUnknownPublication() {
    when(repo.findByPublicationId("x")).thenReturn(emptyList());
    assertThrows(PublicationNotFound.class, () -> controller().getPublicationStatus("x"));
  }

  @Test
  void getPublicationReturnsAllPublicationIds() {
    var publications = List.of("a", "b", "c");
    when(repo.findDistinctPublicationIds()).thenReturn(publications);
    assertThat(controller().getPublicationIds()).isEqualTo(publications);
  }

  @Test
  void getPublicationReturnsEmptyListWhenNoPublicationsExist() {
    when(repo.findDistinctPublicationIds()).thenReturn(emptyList());
    assertThat(controller().getPublicationIds()).isEmpty();
  }

  @Test
  void manuallyClearHungPublication() {
    // Assert that we start with two IN_PROGRESS entities
    List<StatusEntity> inProgress = PublicationSamples.Entity.create().entitiesInProgress();
    assertThat(inProgress.size()).isEqualTo(2);
    when(repo.findByStatusInProgress()).thenReturn(inProgress);
    controller()
        .manuallyClearHungPublications(
            ClearHungRequest.builder().hangTime(Duration.parse("PT3H")).build());
    ArgumentCaptor<List<StatusEntity>> captor = ArgumentCaptor.forClass(List.class);
    verify(repo).saveAll(captor.capture());
    /* 3 hours will reset one of the two in progress entity
     * the start epoch should have been reset to 0 */
    assertThat(captor.getValue().size()).isEqualTo(1);
    assertThat(captor.getValue().get(0).buildStartEpoch()).isEqualTo(0);
  }

  @Test
  void postPublicationCreatesNewPublication() {
    when(repo.countByPublicationId("p")).thenReturn(0);
    when(dq.requestPatientCount())
        .thenReturn(
            ResourceCount.builder()
                .resourceType("Patient")
                .count(333)
                .maxRecordsPerPage(100)
                .build());
    controller()
        .createPublication(
            PublicationRequest.builder().publicationId("p").recordsPerFile(100).build());
    ArgumentCaptor<List<StatusEntity>> args = ArgumentCaptor.forClass(List.class);
    verify(repo).saveAll(args.capture());
    var entities = args.getValue();
    assertStatusEntityCreated(entities.get(0), "p", 100, 1, 100, "Patient-0001");
    assertStatusEntityCreated(entities.get(1), "p", 100, 2, 100, "Patient-0002");
    assertStatusEntityCreated(entities.get(2), "p", 100, 3, 100, "Patient-0003");
    assertStatusEntityCreated(entities.get(3), "p", 100, 4, 33, "Patient-0004");
  }

  @Test
  void postPublicationThrowsErrorIfRequestedPageSizeIsTooBig() {
    when(repo.countByPublicationId("p")).thenReturn(0);
    when(dq.requestPatientCount())
        .thenReturn(
            ResourceCount.builder()
                .resourceType("Patient")
                .count(1000)
                .maxRecordsPerPage(99)
                .build());
    assertThrows(
        PublicationRecordsPerFileTooBig.class,
        () ->
            controller()
                .createPublication(
                    PublicationRequest.builder().publicationId("p").recordsPerFile(100).build()));
  }

  @Test
  void postPublicationThrowsExceptionForDuplicatePublication() {
    var request = PublicationRequest.builder().publicationId("p").recordsPerFile(100).build();
    when(repo.countByPublicationId(request.publicationId())).thenReturn(3);
    assertThrows(PublicationAlreadyExists.class, () -> controller().createPublication(request));
  }
}
