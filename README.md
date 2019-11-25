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

Publications are creating an _rolling wave_.
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
- Synthesized `.name` using generated values.
  Only `.name.given`, `.name.family`, and `.name.text` will be populated.
- Synthesized  `.birthDate`.
  Patients that are greater than 90 years old will have their birth date adjusted such that
  they appear 90. 
  For example, if the current year is 2019 and the patient is 92, their birth date will be `1929-01-01T12:34:56Z` 
- Synthesized `.deceasedDateTime`

Read more
- https://www.hhs.gov/hipaa/for-professionals/privacy/special-topics/de-identification/index.html
- https://privacyruleandresearch.nih.gov/pr_08.asp
- https://www.law.cornell.edu/cfr/text/45/164.514
- https://www.hhs.gov/sites/default/files/ocr/privacy/hipaa/understanding/coveredentities/minimumnecessary.pdf



# Architecture
![Architecture](/src/plantuml/bulk-search.png)