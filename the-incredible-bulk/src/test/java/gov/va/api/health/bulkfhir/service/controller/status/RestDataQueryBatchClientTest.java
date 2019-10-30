package gov.va.api.health.bulkfhir.service.controller.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.va.api.health.argonaut.api.resources.Patient;
import gov.va.api.health.bulkfhir.service.controller.status.DataQueryBatchClient.AccessDenied;
import gov.va.api.health.bulkfhir.service.controller.status.DataQueryBatchClient.BadRequest;
import gov.va.api.health.bulkfhir.service.controller.status.DataQueryBatchClient.NotFound;
import gov.va.api.health.bulkfhir.service.controller.status.DataQueryBatchClient.RequestFailed;
import gov.va.api.health.bulkfhir.service.controller.status.DataQueryBatchClient.ResourceCount;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class RestDataQueryBatchClientTest {

  @Mock RestTemplate rt;

  RestDataQueryBatchClient client() {
    return RestDataQueryBatchClient.builder()
        .baseUrl("http://awesome.com")
        .internalBulkPath("/i/b")
        .accessKeyHeader("secret")
        .accessKey("open")
        .restTemplate(rt)
        .build();
  }

  @SuppressWarnings("unchecked")
  @Test
  void count200() {
    var resourceCount =
        ResourceCount.builder().resourceType("Patient").count(123).maxRecordsPerPage(50).build();
    whenCountRequest().thenReturn(new ResponseEntity<ResourceCount>(resourceCount, HttpStatus.OK));
    var actual = client().requestPatientCount();
    assertThat(actual).isEqualTo(resourceCount);
    ArgumentCaptor<HttpEntity> args = ArgumentCaptor.forClass(HttpEntity.class);
    verify(rt)
        .exchange(
            anyString(), eq(HttpMethod.GET), args.capture(), any(ParameterizedTypeReference.class));
    assertThat(args.getValue().getHeaders().get("secret")).containsExactly("open");
  }

  @Test
  void countThrowsAccessDeniedWhenForbidden() {
    whenCountRequest()
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.FORBIDDEN, "x", new HttpHeaders(), null, null));
    assertThrows(AccessDenied.class, () -> client().requestPatientCount());
  }

  @Test
  void countThrowsAccessDeniedWhenUnauthorized() {
    whenCountRequest()
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "x", new HttpHeaders(), null, null));
    assertThrows(AccessDenied.class, () -> client().requestPatientCount());
  }

  @Test
  void countThrowsBadRequestWhenBadRequest() {
    whenCountRequest()
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "x", new HttpHeaders(), null, null));
    assertThrows(BadRequest.class, () -> client().requestPatientCount());
  }

  @Test
  void countThrowsNotFoundWhenNotFound() {
    whenCountRequest()
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "x", new HttpHeaders(), null, null));
    assertThrows(NotFound.class, () -> client().requestPatientCount());
  }

  @Test
  void countThrowsRequestFailedWhenGenericFailureOccurs() {
    whenCountRequest()
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.BAD_GATEWAY, "x", new HttpHeaders(), null, null));
    assertThrows(RequestFailed.class, () -> client().requestPatientCount());
  }

  @SuppressWarnings("unchecked")
  @Test
  void requestPatients200() {
    var patients = List.of(Patient.builder().id("1").build(), Patient.builder().id("2").build());
    whenPatientRequest().thenReturn(new ResponseEntity<List<Patient>>(patients, HttpStatus.OK));
    var actual = client().requestPatients(1, 50);
    assertThat(actual).isEqualTo(patients);
    ArgumentCaptor<HttpEntity> args = ArgumentCaptor.forClass(HttpEntity.class);
    verify(rt)
        .exchange(
            anyString(),
            eq(HttpMethod.GET),
            args.capture(),
            any(ParameterizedTypeReference.class),
            eq(Map.of("page", 1, "_count", 50)));
    assertThat(args.getValue().getHeaders().get("secret")).containsExactly("open");
  }

  private OngoingStubbing<ResponseEntity> whenCountRequest() {
    return when(
        rt.exchange(
            eq("http://awesome.com/i/b/Patient/count"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)));
  }

  private OngoingStubbing<ResponseEntity> whenPatientRequest() {
    return when(
        rt.exchange(
            eq("http://awesome.com/i/b/Patient?page={page}&_count={count}"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class),
            anyMap()));
  }
}
