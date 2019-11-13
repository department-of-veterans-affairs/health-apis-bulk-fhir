package gov.va.api.health.bulkfhir.service.filebuilder;

import gov.va.api.health.bulkfhir.api.internal.FileBuildResponse;
import java.util.concurrent.CompletableFuture;

public interface FileBuildWorker {
  CompletableFuture<FileBuildResponse> buildFile(FileClaim claim);
}
