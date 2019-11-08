package gov.va.api.health.bulkfhir.service.controller.publication;

import static gov.va.api.health.bulkfhir.service.controller.publication.PublicationExceptions.assertPublicationFileFound;

import gov.va.api.health.bulkfhir.service.filebuilder.FileBuildRequest;
import gov.va.api.health.bulkfhir.service.filebuilder.FileBuilderExceptions.AlreadyClaimed;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaim;
import gov.va.api.health.bulkfhir.service.filebuilder.FileClaimant;
import gov.va.api.health.bulkfhir.service.status.StatusEntity;
import gov.va.api.health.bulkfhir.service.status.StatusRepository;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Optimistic File Claimant works by leverating JPA's Optimistic locking mechanism. It will
 * attempt to update {@link StatusEntity} instances in a new, isolated transaction. Optimistic
 * locking prevents multiple apps from claiming the same record. This interpret optimistic lock
 * exceptions as "already claimed" status.
 */
@Service
@Slf4j
@Builder
@AllArgsConstructor(onConstructor = @__({@Autowired}))
public class OptimisticFileClaimant implements FileClaimant {

  private final StatusRepository repository;

  @Override
  public void completeClaim(FileBuildRequest request) {
    StatusEntity entity = findStatusEntity(request);
    entity.buildCompleteEpoch(System.currentTimeMillis());
  }

  private StatusEntity findStatusEntity(FileBuildRequest request) {
    List<StatusEntity> entities =
        repository.findByPublicationIdAndFileName(request.publicationId(), request.fileId());
    assertPublicationFileFound(!entities.isEmpty(), request.publicationId(), request.fileId());
    return entities.get(0);
  }

  private String hostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      var assumedIdentity = System.getenv("HOST");
      if (assumedIdentity == null) {
        assumedIdentity = "Unknown-" + System.identityHashCode(this);
      }
      log.info("Could not determine hostname, assuming identity: {}", assumedIdentity);
      return assumedIdentity;
    }
  }

  /**
   * This claim attempt defines it's own transaction boundary and explicitly updates and flushes.
   * JPA Optimistic Locking behavior is expected to prevent overwriting.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Override
  public FileClaim tryClaim(FileBuildRequest request) {
    log.info("Claiming {}/{}", request.publicationId(), request.fileId());
    StatusEntity entity = findStatusEntity(request);
    entity.buildStartEpoch(System.currentTimeMillis());
    entity.buildCompleteEpoch(0);
    entity.buildProcessorId(hostName());
    log.info("BEFORE: {}", entity);
    try {
      entity = repository.saveAndFlush(entity);
      return FileClaim.builder()
          .request(request)
          .fileName(entity.fileName())
          .page(entity.page())
          .count(entity.count())
          .build();
    } catch (ObjectOptimisticLockingFailureException e) {
      throw new AlreadyClaimed(request.publicationId(), request.fileId());
    }
  }
}
