package gov.va.api.health.bulkfhir.service.status;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface StatusRepository extends JpaRepository<StatusEntity, String> {

  String DISTINCT_PUBLICATION_IDS_BY_CREATION_TIME_DESC_QUERY =
      "select s.publicationId from StatusEntity s"
          + " group by s.publicationId order by s.publicationEpoch desc";

  int countByPublicationId(String publicationId);

  @Transactional
  int deleteByPublicationId(String publicationId);

  List<StatusEntity> findByPublicationId(String publicationId);

  List<StatusEntity> findByPublicationIdAndFileName(String publicationId, String fileName);

  @Query("select s from StatusEntity s where s.buildCompleteEpoch = 0 and s.buildStartEpoch > 0")
  List<StatusEntity> findByStatusInProgress();

  @Query("select s from StatusEntity s where s.buildCompleteEpoch = 0 and s.buildStartEpoch = 0")
  List<StatusEntity> findByStatusNotStarted();

  @Query(DISTINCT_PUBLICATION_IDS_BY_CREATION_TIME_DESC_QUERY)
  List<String> findDistinctPublicationIds();
}
