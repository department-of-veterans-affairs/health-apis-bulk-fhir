package gov.va.api.health.bulkfhir.service.controller.status;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.va.api.health.bulkfhir.api.internal.PublicationRequest;
import gov.va.api.health.bulkfhir.service.controller.status.PublicationExceptions.PublicationAlreadyExists;
import gov.va.api.health.bulkfhir.service.controller.status.PublicationExceptions.PublicationNotFound;
import gov.va.api.health.bulkfhir.service.controller.status.PublicationSamples.Api;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InternalPublicationControllerTest {

  @Mock StatusRepository repo;
  @Mock PublicationStatusTransformer tx;

  InternalPublicationController controller() {
    return InternalPublicationController.builder().repository(repo).transformer(tx).build();
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
  void postPublicationCreatesNewPublication() {
    // TODO interact with dq here.
    controller()
        .createPublication(
            PublicationRequest.builder().publicationId("p").recordsPerFile(100).build());
    fail();
  }

  @Test
  void postPublicationThrowsExceptionForDuplicatePublication() {
    var request = PublicationRequest.builder().publicationId("p").recordsPerFile(100).build();
    when(repo.countByPublicationId(request.publicationId())).thenReturn(3);
    assertThrows(PublicationAlreadyExists.class, () -> controller().createPublication(request));
  }
}
