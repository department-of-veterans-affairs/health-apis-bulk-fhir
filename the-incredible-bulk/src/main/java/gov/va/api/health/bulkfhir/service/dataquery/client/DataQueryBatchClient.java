package gov.va.api.health.bulkfhir.service.dataquery.client;

import gov.va.api.health.argonaut.api.resources.Patient;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

public interface DataQueryBatchClient {

  ResourceCount requestPatientCount();

  List<Patient> requestPatients(int page, int count);

  /** The resource requested was not allowed because we do not have the correct access token. */
  class AccessDenied extends DataQueryBatchClientException {
    public AccessDenied(String url) {
      super(url);
    }
  }

  /** A request to Data Query was malformed, such as missing required search parameters. */
  class BadRequest extends DataQueryBatchClientException {
    public BadRequest(String url) {
      super(url);
    }
  }

  /** The generic exception for working with Data Query. */
  class DataQueryBatchClientException extends RuntimeException {
    public DataQueryBatchClientException(String url) {
      super(url);
    }
  }

  /** The resource requested was not found. */
  class NotFound extends DataQueryBatchClientException {
    public NotFound(String url) {
      super(url);
    }
  }

  /** An unspecified error occurred while performing a search. */
  class RequestFailed extends DataQueryBatchClientException {
    public RequestFailed(String url) {
      super(url);
    }
  }

  @Value
  @Builder
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class ResourceCount {
    String resourceType;
    int count;
    int maxRecordsPerPage;
  }
}
