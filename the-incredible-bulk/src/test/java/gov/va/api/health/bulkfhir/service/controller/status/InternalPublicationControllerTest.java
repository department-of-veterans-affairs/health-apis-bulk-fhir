package gov.va.api.health.bulkfhir.service.controller.status;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.va.api.health.bulkfhir.service.controller.status.PublicationExceptions.PublicationNotFound;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InternalPublicationControllerTest {

  @Mock StatusRepository repo;

  InternalPublicationController controller() {
    return InternalPublicationController.builder().repository(repo).build();
  }

  @Test
  void deletePublicationDeletesPublication() {
    when(repo.deleteByPublicationName("x")).thenReturn(5);
    controller().deletePublication("x");
    verify(repo).deleteByPublicationName("x");
  }

  @Test
  void deletePublicationThrowsExceptionForUnknownPublication() {
    when(repo.deleteByPublicationName("x")).thenReturn(0);
    assertThrows(PublicationNotFound.class, () -> controller().deletePublication("x"));
  }

  @Test
  void getPublicationIdReturnsOverallStatus() {
    fail();
  }

  @Test
  void getPublicationIdThrowsNotFoundForUnknownPublication() {
    fail();
  }

  @Test
  void getPublicationReturnsAllPublicationIds() {
    fail();
  }

  @Test
  void getPublicationReturnsEmptyListWhenNoPublicationsExist() {
    fail();
  }

  @Test
  void postPublicationCreatesNewPublication() {
    fail();
  }

  @Test
  void postPublicationThrowsExceptionForDuplicatePublication() {
    fail();
  }
}
