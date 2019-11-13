package gov.va.api.health.bulkfhir.service.controller.publication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.va.api.health.bulkfhir.api.internal.FileBuildResponse;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildRequest;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildWorker;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuilderExceptions.AlreadyClaimed;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuilderExceptions.ClaimFailed;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaim;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaimant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NonDistributedFileBuilderTest {

  @Mock FileClaimant claimant;
  @Mock FileBuildWorker worker;

  @Test
  void claimFailedIsThrownIfClaimFails() {
    FileBuildRequest request = FileBuildRequest.builder().publicationId("p").fileId("f").build();
    doThrow(new AlreadyClaimed("p", "f")).when(claimant).tryClaim(request);
    assertThrows(AlreadyClaimed.class, () -> manager().buildFile(request));
    doThrow(new RuntimeException("fugazi")).when(claimant).tryClaim(request);
    assertThrows(ClaimFailed.class, () -> manager().buildFile(request));
  }

  @Test
  void claimFileIsWorked() {
    FileBuildRequest request = FileBuildRequest.builder().publicationId("p").fileId("f").build();
    FileClaim claim = FileClaim.builder().request(request).page(3).count(1234).build();
    when(claimant.tryClaim(request)).thenReturn(claim);
    when(worker.buildFile(claim))
        .thenReturn(CompletableFuture.completedFuture(FileBuildResponse.builder().build()));

    var response = manager().buildFile(request);
    assertThat(response)
        .isEqualTo(FileBuildResponse.builder().publicationId("p").fileId("f").build());

    verify(worker).buildFile(claim);
  }

  NonDistributedFileBuilder manager() {
    return NonDistributedFileBuilder.builder().claimant(claimant).worker(worker).build();
  }
}
