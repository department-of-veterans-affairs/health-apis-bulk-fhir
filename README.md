# health-apis-bulk-fhir

This application is th Bulk FHIR layer that sits on top of 
[Data Query](https://github.com/department-of-veterans-affairs/health-apis-data-query)
to provide _anonymous_ data.

Read more
- https://build.fhir.org/ig/HL7/bulk-data/
- https://bit.ly/fhir-bulk-api

#### Supported Resources
- Patient

### Caveats
Limitations of the current MVP include:
- Data is currently DSTU2 format and not R4 as dictated by the specification.
- Security is implemented with access tokens and not SMART Authorization.
  `system/*.read` scopes are not currently available.
- `Patient/$export` does not support the optional `_type` and `_since` parameters.



# Concept: Publication
The VA houses the largest medical history database in the US.
To support a data set this large, some deviations from the specification have been made.
The kick off request (`/Patient/$export`) does not actually initiate the bulk packaging.
Instead, bulk data is prepared in advance on a periodic basis, e.g. monthly.
A _Publication_ is the periodic collection of data.
The Bulk FHIR application makes one Publication available to all consumers.

The Bulk FHIR endpoints still function as specified.
- The `/Patient/$export` endpoint will return the location of the Status endpoint.
- The Status endpoint will _always_ return a _Complete_ response. 
  It is is never _In-Progress_.
  
Publications are very large.
Depending on the resource type, the number of records can range from tens of millions to billions.
Publications are made of many files, which are identified in the _Complete_ status response.
A Publication can have thousands of files, each file containing tens of thousands of records.

Publications are created in an _rolling wave_.
For example, the _January_ publication is made available in February. 
The _February_ publication will be built automatically in the background over the month and made
available in March.



# Concept: Anonymization
Personally identifiable information (PII) data is removed or synthesized.
The following generalizations apply:
- Optional data that is considered PII is removed
- Dates are truncated to the year, e.g. `2005-01-01T12:34:56Z`

#### Patient
- Remove `.address`, `.contact[]`, `.id`, `.identifier[]`, `.photo`, `.telecom`.
- Remove `.multipleBirthInteger` and populate `.multipleBirthBoolean` if applicable.
- Synthesize `.name` using generated values.
  Only `.name.given`, `.name.family`, and `.name.text` will be populated.
- Synthesize  `.birthDate`.
  Patients that are greater than 90 years old will have their birth date adjusted such that
  they appear 90. 
  For example, if the current year is 2019 and the patient is 92, their birth date will be `1929-01-01T12:34:56Z` 
- Synthesize `.deceasedDateTime`

Read more
- https://www.hhs.gov/hipaa/for-professionals/privacy/special-topics/de-identification/index.html
- https://privacyruleandresearch.nih.gov/pr_08.asp
- https://www.law.cornell.edu/cfr/text/45/164.514
- https://www.hhs.gov/sites/default/files/ocr/privacy/hipaa/understanding/coveredentities/minimumnecessary.pdf



# Architecture
![Architecture](/src/plantuml/bulk-search.png)

#### Data Flow
![Data Flow](/src/plantuml/data-flow.png)

##### Notes
- _Data Query_ is responsible for enabling access to bulk FHIR compliant records through VA internal APIs that are protected from general access.
- _The Incredible Bulk_ communicates with _Data Query_ through internal, protected APIs.
- _The Incredible Bulk_ is responsible for Publication management and anonymization.
- Publication files are created by _The Incredible Bulk_ but served to consumers directly from S3 (via Kong)
- Timers are implemented using Kubernetes batch CronJob containers that periodically poke Publication endpoints.
  
When building files, _The Incredible Bulk_ will gather data from _Data Query_ where it will be anonymized and written to S3.

# Publication Lifecycle
- A Publication is created using `POST /internal/publication`
  - Data Query will be interrogated to determine records that are available.
  - The number of files required will be determined and groups of records will be associated to each file.
  - The status of each file will be `NOT_STARTED`
- A timer will trigger file building using `POST /internal/publication/any/file/next`
  - The first file that has a status of `NOT_STARTED` for the oldest Publication will be chosen.
  - Records will be extracted from Data Query, anonymized, and written to S3 for storage.
- Once all files are created (status is `COMPLETE`) for the Publication, the entire Publication will 
  be considered `COMPLETE` and made immediately available to consumers on future _status_ calls.
  (The _status_ endpoint is returned as part of the `/Patient/$export` call.)
  
Notes
- A second timer will periodically check for incomplete Publication files.
  For example, if an instance of The Incredible Bulk is building a file, but were to crash, then
  the file would have been marked as `IN_PROGRESS`, but cannot complete.
  This timer will look for such instances and update the file status as `NOT_STARTED` so that it can be re-attempted.
- Specific files can be built using `POST /internal/publication/{id}/file/{fileId}`
- Publications can be listed using `GET /internal/publication`
- Status can be queried using `GET /internal/publication/{id}`
