package gov.va.api.health.bulkfhir.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import gov.va.api.health.bulkfhir.api.internal.ClearHungRequest;
import gov.va.api.health.bulkfhir.api.internal.FileBuildResponse;
import gov.va.api.health.bulkfhir.api.internal.PublicationRequest;
import gov.va.api.health.bulkfhir.api.internal.PublicationStatus;
import gov.va.api.health.bulkfhir.service.dataquery.client.DataQueryBatchClient.ResourceCount;
import gov.va.api.health.sentinel.ExpectedResponse;
import gov.va.api.health.sentinel.categories.Local;
import io.restassured.http.Header;
import io.restassured.http.Method;
import java.time.Duration;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockserver.integration.ClientAndServer;

@Slf4j
public class InternalPublicationIT {

  @Test
  @Category({Local.class})
  public void errorCodes() {
    try (MockDataQuery dq = MockDataQuery.create()) {
      dq.count(
          ResourceCount.builder().resourceType("Patient").maxRecordsPerPage(100).count(88).build());
      PublicationEndpoint endpoint = PublicationEndpoint.create();
      /* Does not exist */
      endpoint.getPublication("nope-" + System.currentTimeMillis()).expect(404);
      /* Publication ID not ok */
      endpoint
          .create(PublicationRequest.builder().publicationId("no").recordsPerFile(100).build())
          .expect(400);
      /* Try to create same publication twice */
      endpoint
          .create(
              PublicationRequest.builder()
                  .publicationId("errorCodes-1")
                  .recordsPerFile(100)
                  .build())
          .expect(201);
      endpoint
          .create(
              PublicationRequest.builder()
                  .publicationId("errorCodes-1")
                  .recordsPerFile(100)
                  .build())
          .expect(400);
      /* Delete it, then try to delete it again */
      endpoint.deletePublication("errorCodes-1").expect(200);
      endpoint.deletePublication("errorCodes-1").expect(404);
    }
  }

  @Test
  @Category({Local.class})
  public void fullCycle() {
    try (MockDataQuery dq = MockDataQuery.create()) {
      /* Nothing exists */
      PublicationEndpoint endpoint = PublicationEndpoint.create();
      assertThat(endpoint.listPublications().expect(200).expectListOf(String.class)).isEmpty();
      /* Create one */
      dq.count(
          ResourceCount.builder().resourceType("Patient").maxRecordsPerPage(100).count(88).build());
      endpoint
          .create(
              PublicationRequest.builder().publicationId("fullCycle-1").recordsPerFile(50).build())
          .expect(201);
      assertThat(endpoint.listPublications().expect(200).expectListOf(String.class))
          .containsExactly("fullCycle-1");
      /* Create another one */
      endpoint
          .create(
              PublicationRequest.builder().publicationId("fullCycle-2").recordsPerFile(50).build())
          .expect(201);
      assertThat(endpoint.listPublications().expect(200).expectListOf(String.class))
          .containsExactlyInAnyOrder("fullCycle-1", "fullCycle-2");
      /* Clear Hung Publications :lyin: */
      endpoint
          .clearHungPublications(
              ClearHungRequest.builder().hangTime(Duration.parse("PT87600H")).build())
          .expect(200);
      assertThat(endpoint.listPublications().expect(200).expectListOf(String.class))
          .containsExactlyInAnyOrder("fullCycle-1", "fullCycle-2");
      /* Get details for one */
      PublicationStatus status =
          endpoint.getPublication("fullCycle-1").expect(200).expectValid(PublicationStatus.class);
      assertThat(status.publicationId()).isEqualTo("fullCycle-1");
      /* Build next file */
      FileBuildResponse fileBuildResponse =
          endpoint.buildNextFile().expect(202).expectValid(FileBuildResponse.class);
      assertThat(fileBuildResponse.publicationId()).isEqualTo("fullCycle-1");
      assertThat(fileBuildResponse.fileId()).isEqualTo("Patient-0001");
      /* Delete both */
      endpoint.deletePublication("fullCycle-1").expect(200);
      assertThat(endpoint.listPublications().expect(200).expectListOf(String.class))
          .containsExactly("fullCycle-2");
      endpoint.deletePublication("fullCycle-2").expect(200);
      assertThat(endpoint.listPublications().expect(200).expectListOf(String.class)).isEmpty();
      /* Build next file when there are no more files to build */
      endpoint.buildNextFile().expect(204);
    }
  }

  @NoArgsConstructor(staticName = "create")
  static class MockDataQuery implements AutoCloseable {

    ClientAndServer dq = startClientAndServer(8090);

    @Override
    public void close() {
      dq.close();
    }

    void count(ResourceCount status) {
      dq.when(request().withPath("/internal/bulk/Patient/count"))
          .respond(
              response()
                  .withStatusCode(200)
                  .withHeader("Content-Type", "application/json")
                  .withBody(json(status)));
    }

    @SneakyThrows
    private String json(ResourceCount status) {
      return JacksonConfig.createMapper().writeValueAsString(status);
    }
  }

  @NoArgsConstructor(staticName = "create")
  static class PublicationEndpoint {

    ExpectedResponse buildNextFile() {
      return ExpectedResponse.of(
          TestClients.bulkFhir()
              .service()
              .requestSpecification()
              .header(internalAccessToken())
              .contentType("application/json")
              .request(Method.POST, url() + "/any/file/next"));
    }

    @SneakyThrows
    ExpectedResponse clearHungPublications(ClearHungRequest clearHungRequest) {
      return ExpectedResponse.of(
          TestClients.bulkFhir()
              .service()
              .requestSpecification()
              .header(internalAccessToken())
              .contentType("application/json")
              .body(JacksonConfig.createMapper().writeValueAsString(clearHungRequest))
              .request(Method.POST, url() + "/hung"));
    }

    @SneakyThrows
    ExpectedResponse create(PublicationRequest request) {
      return ExpectedResponse.of(
          TestClients.bulkFhir()
              .service()
              .requestSpecification()
              .header(internalAccessToken())
              .contentType("application/json")
              .body(JacksonConfig.createMapper().writeValueAsString(request))
              .request(Method.POST, url()));
    }

    ExpectedResponse deletePublication(String id) {
      return ExpectedResponse.of(
          TestClients.bulkFhir()
              .service()
              .requestSpecification()
              .header(internalAccessToken())
              .contentType("application/json")
              .request(Method.DELETE, url() + "/" + id));
    }

    ExpectedResponse get(String url) {
      return ExpectedResponse.of(
          TestClients.bulkFhir()
              .service()
              .requestSpecification()
              .header(internalAccessToken())
              .contentType("application/json")
              .request(Method.GET, url));
    }

    ExpectedResponse getPublication(String id) {
      return get(url() + "/" + id);
    }

    private Header internalAccessToken() {
      return new Header("internal", System.getProperty("internal", "not-supplied"));
    }

    ExpectedResponse listPublications() {
      return get(url());
    }

    String url() {
      return SystemDefinitions.systemDefinition().bulkFhir().urlWithApiPath()
          + "internal/publication";
    }
  }
}
