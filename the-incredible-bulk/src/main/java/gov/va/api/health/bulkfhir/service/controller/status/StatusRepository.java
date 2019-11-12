package gov.va.api.health.bulkfhir.service.controller.status;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

interface StatusRepository extends PagingAndSortingRepository<StatusEntity, String> {

  int countByPublicationId(String publicationId);

  @Transactional
  int deleteByPublicationId(String publicationId);

  List<StatusEntity> findByPublicationId(String publicationId);

  @Query("select s from StatusEntity s where s.buildCompleteEpoch = 0 and s.buildStartEpoch > 0")
  List<StatusEntity> findByStatusInProgress();

  @Query("select distinct s.publicationId from StatusEntity s")
  List<String> findDistinctPublicationIds();
}
