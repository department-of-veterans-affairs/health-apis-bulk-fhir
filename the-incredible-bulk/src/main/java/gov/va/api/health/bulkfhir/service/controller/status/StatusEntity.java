package gov.va.api.health.bulkfhir.service.controller.status;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@Table(name = "Status", schema = "app")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class StatusEntity {
  @Id
  @Column(name = "id", updatable = false, nullable = false)
  @EqualsAndHashCode.Include
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private String id;

  /** The name of the publication that this file is part of. */
  @Column(name = "publicationId")
  private String publicationId;

  /** Publication date in epoch milliseconds. */
  @Column(name = "publicationEpoch")
  private long publicationEpoch;

  /** The maximum number of records that can exist per file. */
  @Column(name = "recordsPerFile")
  private int recordsPerFile;

  /** The file name with out extension, e.g. 'patient-5' */
  @Column(name = "fileName")
  private String fileName;

  /** The row or record number of the first item in this file. */
  @Column(name = "page")
  private int page;

  /** The row or record number of the last item in this file. */
  @Column(name = "count")
  private int count;

  /**
   * 0 if this file has not been started, otherwise the epoch milliseconds that building started.
   */
  @Column(name = "buildStartEpoch")
  private long buildStartEpoch;

  /**
   * 0 if this file has not been completed, otherwise the epoch milliseconds that building
   * completed.
   */
  @Column(name = "buildCompleteEpoch")
  private long buildCompleteEpoch;

  /**
   * `null` if this file has not been attempted, otherwise the instance name of the application
   * building the file.
   */
  @Column(name = "buildProcessorId")
  private String buildProcessorId;
}
