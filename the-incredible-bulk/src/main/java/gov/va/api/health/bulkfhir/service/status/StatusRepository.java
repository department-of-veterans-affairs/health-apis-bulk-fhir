package gov.va.api.health.bulkfhir.service.status;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface StatusRepository extends JpaRepository<StatusEntity, String> {

  int countByPublicationId(String publicationId);

  @Transactional
  int deleteByPublicationId(String publicationId);

  List<StatusEntity> findByPublicationId(String publicationId);

  List<StatusEntity> findByPublicationIdAndFileName(String publicationId, String fileName);

  @Query("select s from StatusEntity s where s.buildCompleteEpoch = 0 and s.buildStartEpoch > 0")
  List<StatusEntity> findByStatusInProgress();

  @Query("select distinct s.publicationId from StatusEntity s")
  List<String> findDistinctPublicationIds();
}
