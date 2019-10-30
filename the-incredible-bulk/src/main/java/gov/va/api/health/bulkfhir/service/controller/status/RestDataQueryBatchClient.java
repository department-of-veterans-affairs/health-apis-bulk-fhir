package gov.va.api.health.bulkfhir.service.controller.status;

import gov.va.api.health.argonaut.api.resources.Patient;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * REST implementation of the batch client that accesses Data Query by providing the access token.
 */
@Component
public class RestDataQueryBatchClient implements DataQueryBatchClient {

  private final RestTemplate restTemplate;
  private final String baseUrl;
  private final String accessKey;
  private final String accessKeyHeader;
  private final String internalBulkPath;

  /** Spring constructor that allows customization from application properties. */
  @Builder
  public RestDataQueryBatchClient(
      @Value("${dataquery.url}") String baseUrl,
      @Value("${dataquery.internal-bulk-path:/internal/bulk}") String internalBulkPath,
      @Value("${dataquery.access-key-header:bulk}") String accessKeyHeader,
      @Value("${dataquery.access-key}") String accessKey,
      @Autowired RestTemplate restTemplate) {
    this.baseUrl = baseUrl;
    this.internalBulkPath = internalBulkPath;
    this.accessKeyHeader = accessKeyHeader;
    this.accessKey = accessKey;
    this.restTemplate = restTemplate;
  }

  private <T> ResponseEntity<T> callTo(String url, Supplier<ResponseEntity<T>> call) {
    try {
      return call.get();
    } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.Unauthorized e) {
      throw new AccessDenied(url);
    } catch (HttpClientErrorException.NotFound e) {
      throw new NotFound(url);
    } catch (HttpClientErrorException.BadRequest e) {
      throw new BadRequest(url);
    } catch (HttpStatusCodeException e) {
      throw new RequestFailed(url);
    }
  }

  private HttpHeaders headers() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(accessKeyHeader, accessKey);
    return headers;
  }

  @Override
  public ResourceCount requestPatientCount() {
    String url = urlOf("/Patient/count");
    return callTo(
            url,
            () ->
                restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<Void>(headers()),
                    ParameterizedTypeReference.<ResourceCount>forType(ResourceCount.class)))
        .getBody();
  }

  @Override
  public List<Patient> requestPatients(int page, int count) {
    String url = urlOf("/Patient?page={page}&_count={count}");
    return callTo(
            url,
            () ->
                restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<Void>(headers()),
                    new ParameterizedTypeReference<List<Patient>>() {},
                    Map.of("page", page, "_count", count)))
        .getBody();
  }

  private String urlOf(String subPath) {
    return baseUrl + internalBulkPath + subPath;
  }
}
