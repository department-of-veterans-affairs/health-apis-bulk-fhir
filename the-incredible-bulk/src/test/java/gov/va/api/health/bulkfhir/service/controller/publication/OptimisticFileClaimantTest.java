package gov.va.api.health.bulkfhir.service.controller.publication;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.va.api.health.bulkfhir.service.controller.publication.PublicationExceptions.PublicationFileNotFound;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildRequest;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuilderExceptions.AlreadyClaimed;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaim;
import gov.va.api.health.bulkfhir.service.status.StatusEntity;
import gov.va.api.health.bulkfhir.service.status.StatusRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class OptimisticFileClaimantTest {

  @Mock StatusRepository repo;

  @Test
  void alreadyClaimedThrownIfTransactionFails() {
    StatusEntity e = StatusEntity.builder().build();
    when(repo.findByPublicationIdAndFileName("p", "f")).thenReturn(List.of(e));
    when(repo.saveAndFlush(e))
        .thenThrow(new ObjectOptimisticLockingFailureException("fugaze", null));
    assertThrows(
        AlreadyClaimed.class,
        () ->
            claimant().tryClaim(FileBuildRequest.builder().publicationId("p").fileId("f").build()));
  }

  @Test
  void claimComplete() {
    StatusEntity e = StatusEntity.builder().build();
    when(repo.findByPublicationIdAndFileName("p", "f")).thenReturn(List.of(e));

    claimant().completeClaim(FileBuildRequest.builder().publicationId("p").fileId("f").build());
    assertThat(e.buildCompleteEpoch()).isNotZero();
  }

  OptimisticFileClaimant claimant() {
    return OptimisticFileClaimant.builder().repository(repo).build();
  }

  @Test
  void completeClaimThrowsPublicationFileNotFoundForUnknownFile() {
    when(repo.findByPublicationIdAndFileName("x", "a")).thenReturn(emptyList());
    assertThrows(
        PublicationFileNotFound.class,
        () ->
            claimant()
                .completeClaim(FileBuildRequest.builder().publicationId("x").fileId("a").build()));
  }

  @Test
  void entityClaimedIfTransactionSucceeds() {
    StatusEntity e = StatusEntity.builder().fileName("awesome").page(3).count(1234).build();
    when(repo.findByPublicationIdAndFileName("p", "f")).thenReturn(List.of(e));
    when(repo.saveAndFlush(e)).thenReturn(e);

    FileBuildRequest request = FileBuildRequest.builder().publicationId("p").fileId("f").build();
    FileClaim claim = claimant().tryClaim(request);
    /* Entity was updated. */
    assertThat(e.buildStartEpoch()).isNotZero();
    assertThat(e.buildCompleteEpoch()).isZero();
    assertThat(e.buildProcessorId()).isNotNull();
    verify(repo).saveAndFlush(e);

    /* Claim is correct */
    FileClaim expected =
        FileClaim.builder().request(request).fileName("awesome").page(3).count(1234).build();
    assertThat(claim).isEqualTo(expected);
  }

  @Test
  void tryClaimThrowsPublicationFileNotFoundForUnknownFile() {
    when(repo.findByPublicationIdAndFileName("x", "a")).thenReturn(emptyList());
    assertThrows(
        PublicationFileNotFound.class,
        () ->
            claimant().tryClaim(FileBuildRequest.builder().publicationId("x").fileId("a").build()));
  }
}
