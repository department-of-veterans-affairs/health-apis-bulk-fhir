package gov.va.api.health.bulkfhir.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import gov.va.api.health.bulkfhir.api.bulkstatus.PublicationFileStatusResponse;
import gov.va.api.health.bulkfhir.api.internal.BuildStatus;
import gov.va.api.health.bulkfhir.api.internal.ClearHungRequest;
import gov.va.api.health.bulkfhir.api.internal.FileBuildResponse;
import gov.va.api.health.bulkfhir.api.internal.PublicationRequest;
import gov.va.api.health.bulkfhir.api.internal.PublicationStatus;
import gov.va.api.health.bulkfhir.service.dataquery.client.DataQueryBatchClient.ResourceCount;
import gov.va.api.health.bulkfhir.tests.categories.LabBulkFhir;
import gov.va.api.health.bulkfhir.tests.categories.ProdBulkFhir;
import gov.va.api.health.sentinel.Environment;
import gov.va.api.health.sentinel.ExpectedResponse;
import gov.va.api.health.sentinel.categories.Local;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.http.Method;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockserver.integration.ClientAndServer;

@Slf4j
public class PublicationIT {

  @Before
  @After
  public void cleanUpPublications() {
    PublicationEndpoint endpoint = PublicationEndpoint.create();
    endpoint.deletePublication(makeItPublicationName("fullCycle-1"));
    endpoint.deletePublication(makeItPublicationName("fullCycle-2"));
    endpoint.deletePublication(makeItPublicationName("errorCodes-1"));
  }

  @Test
  @Category({LabBulkFhir.class, Local.class, ProdBulkFhir.class})
  public void errorCodes() {
    /*
     * If we are in the local environment we have to mock data query, otherwise a real one already exists.
     */
    if (Environment.get() == Environment.LOCAL) {
      try (MockDataQuery dq = MockDataQuery.create()) {
        dq.count(
            ResourceCount.builder()
                .resourceType("Patient")
                .maxRecordsPerPage(25000)
                .count(88)
                .build());
        runErrorCodesTest();
      }
    } else {
      runErrorCodesTest();
    }
  }

  @Test
  @Category({Local.class})
  public void fullCycle() {
    try (MockDataQuery dq = MockDataQuery.create()) {
      String fullCycle1PublicationId = makeItPublicationName("fullCycle-1");
      String fullCycle2PublicationId = makeItPublicationName("fullCycle-2");
      /* Nothing exists */
      PublicationEndpoint endpoint = PublicationEndpoint.create();
      assertThat(endpoint.listPublications().expect(200).expectListOf(String.class)).isEmpty();
      /* Create one */
      dq.count(
          ResourceCount.builder().resourceType("Patient").maxRecordsPerPage(100).count(88).build());
      endpoint
          .create(
              PublicationRequest.builder()
                  .publicationId(fullCycle1PublicationId)
                  .recordsPerFile(50)
                  .build())
          .expect(201);
      assertThat(endpoint.listPublications().expect(200).expectListOf(String.class))
          .containsExactly(fullCycle1PublicationId);
      /* Create another one */
      endpoint
          .create(
              PublicationRequest.builder()
                  .publicationId(fullCycle2PublicationId)
                  .recordsPerFile(50)
                  .build())
          .expect(201);
      assertThat(endpoint.listPublications().expect(200).expectListOf(String.class))
          .containsExactlyInAnyOrder(fullCycle1PublicationId, fullCycle2PublicationId);
      /* Clear Hung Publications :lyin: */
      endpoint
          .clearHungPublications(
              ClearHungRequest.builder().hangTime(Duration.parse("PT87600H")).build())
          .expect(200);
      assertThat(endpoint.listPublications().expect(200).expectListOf(String.class))
          .containsExactlyInAnyOrder(fullCycle1PublicationId, fullCycle2PublicationId);
      /* Get details for one */
      PublicationStatus status =
          endpoint
              .getPublication(fullCycle1PublicationId)
              .expect(200)
              .expectValid(PublicationStatus.class);
      assertThat(status.publicationId()).isEqualTo(fullCycle1PublicationId);
      /* Build next file twice, to fully build the publication */
      FileBuildResponse fileBuildResponse =
          endpoint.buildNextFile().expect(202).expectValid(FileBuildResponse.class);
      assertThat(fileBuildResponse.publicationId()).isEqualTo(fullCycle1PublicationId);
      assertThat(fileBuildResponse.fileId()).isEqualTo("Patient-0001");
      FileBuildResponse fileBuildResponse2 =
          endpoint.buildNextFile().expect(202).expectValid(FileBuildResponse.class);
      assertThat(fileBuildResponse2.publicationId()).isEqualTo(fullCycle1PublicationId);
      assertThat(fileBuildResponse2.fileId()).isEqualTo("Patient-0002");
      /* Get the status for the completed publication */
      BulkStatusEndpoint bulkStatusEndpoint = BulkStatusEndpoint.create();
      PublicationFileStatusResponse publicationStatusResponse =
          bulkStatusEndpoint
              .getBulkStatus(
                  "I2-MCS24PV5NLA7QTLJWXT5FIUCL2YBFFE2OLTJLE644RWPXCPRFY4DT7"
                      + "SWS5BMT2AQI6P2DGSK5UFD42B4ZSATICHSZ4FOXC2MFARYFPPX5YH6PNNKEPZBTCW2BRXQQXXP")
              .expect(200)
              .expectValid(PublicationFileStatusResponse.class);
      assertThat(publicationStatusResponse.extension().isPresent()).isTrue();
      assertThat(publicationStatusResponse.extension().get().id())
          .isEqualTo(fullCycle1PublicationId);
      assertThat(publicationStatusResponse.output().size()).isEqualTo(2);
      /* Delete both */
      endpoint.deletePublication(fullCycle1PublicationId).expect(200);
      assertThat(endpoint.listPublications().expect(200).expectListOf(String.class))
          .containsExactly(fullCycle2PublicationId);
      endpoint.deletePublication(fullCycle2PublicationId).expect(200);
      assertThat(endpoint.listPublications().expect(200).expectListOf(String.class)).isEmpty();
      /* Build next file when there are no more files to build */
      endpoint.buildNextFile().expect(204);
    }
  }

  @Test
  @Category({LabBulkFhir.class, Local.class, ProdBulkFhir.class})
  public void fullCycleWithLiveData() {
    /*
     * If we are in the local environment we have to mock data query, otherwise a real one already exists.
     */
    if (Environment.get() == Environment.LOCAL) {
      try (MockDataQuery dq = MockDataQuery.create()) {
        dq.count(
            ResourceCount.builder()
                .resourceType("Patient")
                .maxRecordsPerPage(25000)
                .count(88)
                .build());
        runLiveFullCycleTest();
      }
    } else {
      runLiveFullCycleTest();
    }
  }

  @Test
  @Category({LabBulkFhir.class, ProdBulkFhir.class})
  public void fullCycleWithS3WriteCheck() throws InterruptedException, IOException {
    String fullCycle1PublicationId = makeItPublicationName("fullCycle-1");
    PublicationEndpoint endpoint = PublicationEndpoint.create();
    assertThat(endpoint.listPublications().expect(200).expectListOf(String.class))
            .doesNotContain(fullCycle1PublicationId);
    /* Create one */
    endpoint
            .create(
                    PublicationRequest.builder()
                            .publicationId(fullCycle1PublicationId)
                            .recordsPerFile(10000)
                            .automatic(false)
                            .build())
            .expect(201);
    assertThat(endpoint.listPublications().expect(200).expectListOf(String.class))
            .contains(fullCycle1PublicationId);
    /* Build the first file here */
    endpoint.buildFile(fullCycle1PublicationId, "Patient-0001").expect(202);

    int i = 0;
    while (i < 20) {
      PublicationStatus status =
              endpoint
                      .getPublication(fullCycle1PublicationId)
                      .expect(200)
                      .expectValid(PublicationStatus.class);
      /*
       * See if the Patient-0001 file is completed
       */
      if (status.files().stream().anyMatch(((file) -> file.fileId().equals("Patient-0001") && file.status() == BuildStatus.COMPLETE))) {
        break;
      }
      i++;
      Thread.sleep(1000);
    }

    /*
     * The file should have been completed verify it is in S3 if we aren't local, otherwise check the filesystem
     */
    if (Environment.get() == Environment.LOCAL) {
      Path filePath = Paths.get("Patient-0001.ndjson");
      /* Make sure the file is created */
      assertThat(Files.exists(filePath)).isTrue();
      /* Delete the file */
      Files.delete(filePath);
    } else {
      BulkStatusEndpoint statusEndpoint = BulkStatusEndpoint.create();
      statusEndpoint.getBulkFile(fullCycle1PublicationId, "Patient-0001").expect(200);
    }
  }

  private String makeItPublicationName(String baseName) {
    return PublicationIT.class.getSimpleName() + "-" + baseName;
  }

  private void runErrorCodesTest() {
    String errorCodes1PublicationId = makeItPublicationName("errorCodes-1");
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
                .publicationId(errorCodes1PublicationId)
                .recordsPerFile(25000)
                .automatic(false)
                .build())
        .expect(201);
    endpoint
        .create(
            PublicationRequest.builder()
                .publicationId(errorCodes1PublicationId)
                .recordsPerFile(25000)
                .automatic(false)
                .build())
        .expect(400);
    /* Delete it, then try to delete it again */
    endpoint.deletePublication(errorCodes1PublicationId).expect(200);
    endpoint.deletePublication(errorCodes1PublicationId).expect(404);
    /* Check error states for the bulk status endpoint */
    BulkStatusEndpoint bulkStatusEndpoint = BulkStatusEndpoint.create();
    bulkStatusEndpoint.getBulkStatus("CANTDECODEME").expect(404);
    bulkStatusEndpoint.getBulkStatusWithoutHeader("DOESNTMATTER").expect(400);
    /* Valid decodable id with no data */
    bulkStatusEndpoint
        .getBulkStatus(
            "I2-MCS24PV5NLA7QTLJWXT5FIUCL2YBFFE2OLTJLE644RWP"
                + "XCPRFY4DIYCN37E5AQY7CALSCBF3J4TEP2XMECSAX5M5FQ7UQX6U76KX76Q0")
        .expect(404);
  }

  /**
   * This is a similar version of the fullCycle test, but desensitized to not interfere when run on
   * a live database.
   */
  private void runLiveFullCycleTest() {
    String fullCycle1PublicationId = makeItPublicationName("fullCycle-1");
    String fullCycle2PublicationId = makeItPublicationName("fullCycle-2");
    PublicationEndpoint endpoint = PublicationEndpoint.create();
    assertThat(endpoint.listPublications().expect(200).expectListOf(String.class))
        .doesNotContain(fullCycle1PublicationId, fullCycle2PublicationId);
    /* Create one */
    endpoint
        .create(
            PublicationRequest.builder()
                .publicationId(fullCycle1PublicationId)
                .recordsPerFile(10000)
                .automatic(false)
                .build())
        .expect(201);
    assertThat(endpoint.listPublications().expect(200).expectListOf(String.class))
        .contains(fullCycle1PublicationId);
    /* Create another one */
    endpoint
        .create(
            PublicationRequest.builder()
                .publicationId(fullCycle2PublicationId)
                .recordsPerFile(25000)
                .automatic(false)
                .build())
        .expect(201);
    assertThat(endpoint.listPublications().expect(200).expectListOf(String.class))
        .contains(fullCycle1PublicationId, fullCycle2PublicationId);
    /* Clear Hung Publications :lyin: */
    endpoint
        .clearHungPublications(
            ClearHungRequest.builder().hangTime(Duration.parse("PT87600H")).build())
        .expect(200);
    assertThat(endpoint.listPublications().expect(200).expectListOf(String.class))
        .contains(fullCycle1PublicationId, fullCycle2PublicationId);
    /* Get details for one */
    PublicationStatus status =
        endpoint
            .getPublication(fullCycle1PublicationId)
            .expect(200)
            .expectValid(PublicationStatus.class);
    assertThat(status.publicationId()).isEqualTo(fullCycle1PublicationId);
    /* Call the next endpoint and validate a successful response is received */
    ExpectedResponse nextResponse = endpoint.buildNextFile();
    assertThat(nextResponse.response().getStatusCode()).isIn(202, 204);
    /* Get the status for a publication that doesn't exist */
    BulkStatusEndpoint bulkStatusEndpoint = BulkStatusEndpoint.create();
    bulkStatusEndpoint
        .getBulkStatus("I2-UZHTGVLJAFI4RMSCAYRQ5JEMTLTEIKKGVI6UJM7WJMMGJCB44EHA0000")
        .expect(404);
    /* Delete both */
    endpoint.deletePublication(fullCycle1PublicationId).expect(200);
    assertThat(endpoint.listPublications().expect(200).expectListOf(String.class))
        .doesNotContain(fullCycle1PublicationId);
    endpoint.deletePublication(fullCycle2PublicationId).expect(200);
    assertThat(endpoint.listPublications().expect(200).expectListOf(String.class))
        .doesNotContain(fullCycle1PublicationId, fullCycle2PublicationId);
    /* Build next file when there are no more files to build */
    endpoint.buildNextFile().expect(204);
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

    ExpectedResponse buildFile(String publicationId, String fileId) {
      return ExpectedResponse.of(
              TestClients.bulkFhir()
                      .service()
                      .requestSpecification()
                      .header(internalAccessToken())
                      .contentType("application/json")
                      .request(Method.POST, url() + "/" + publicationId + "/file/" + fileId));
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
      return SystemDefinitions.systemDefinition().internalBulkFhir().urlWithApiPath()
          + "internal/publication";
    }
  }

  @NoArgsConstructor(staticName = "create")
  static class BulkStatusEndpoint {

    private Headers acceptAndTokenHeader() {
      return new Headers(acceptHeader(), bulkToken());
    }

    private Header acceptHeader() {
      return new Header("accept", "application/json");
    }

    private Header bulkToken() {
      return new Header("client-key", System.getProperty("bulk-token", "not-supplied"));
    }

    ExpectedResponse getBulkStatus(String encodedId) {
      return ExpectedResponse.of(
          TestClients.bulkFhir()
              .service()
              .requestSpecification()
              .headers(acceptAndTokenHeader())
              .contentType("application/json")
              .request(Method.GET, url() + "/" + encodedId));
    }

    ExpectedResponse getBulkFile(String publicationId, String fileId) {
      return ExpectedResponse.of(
              TestClients.bulkFhir()
                      .service()
                      .requestSpecification()
                      .headers(acceptAndTokenHeader())
                      .contentType("application/json")
                      .request(Method.GET, url() + "/publication/" + publicationId + "/" + fileId));
    }

    ExpectedResponse getBulkStatusWithoutHeader(String encodedId) {
      return ExpectedResponse.of(
          TestClients.bulkFhir()
              .service()
              .requestSpecification()
              .header(bulkToken())
              .contentType("application/json")
              .request(Method.GET, url() + "/" + encodedId));
    }

    String url() {
      return SystemDefinitions.systemDefinition().bulkFhir().urlWithApiPath() + "bulk";
    }
  }
}
