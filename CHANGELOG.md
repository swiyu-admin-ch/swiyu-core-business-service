# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 3.42.2

### Changed

- Update demo data

## 3.42.1

### Fixed

- Add CVE overrides

## 3.42.0

### Added

- Add state & identifier specific limit endpoints

## 3.41.3

### Changed

- add more unit test
- fix validation of POP
- fix tiny bugs in the conversion of TrustOnboardingSubmission

## 3.41.2

### Fix

- Add signing rule for test data
- Add public resolve url template for profile combination local,test-data-injection

## 3.41.1

### Changed

- Re-apply migration of `business_entity` records with type `UNKNOWN` or `INDIVIDUAL` to `BUSINESS` (migration
  `V1_22_0`),
  covering any partners created between the initial `V1_21_0` migration and the enforcement of the type constraint.
- Sync `requested_partner_type` on all `TrustOnboardingSubmission` records to match the current type of their
  associated `business_entity`, ensuring submissions that were created with `UNKNOWN` or `INDIVIDUAL` are updated to
  `BUSINESS`.

## 3.41.0

### Added

- Explicit check for existence of the BP ID in the ecosystem before interacting with the status lists.

## 3.40.7

### Changed

- Update demo data to align with feature set (Disable INDIVIDUAL BPs)

## 3.40.6

### Changed

- Format address in doi pdf correctly if street is missing

## 3.40.5

### Changed

- refactored discrete validator into maven sub-module

## 3.40.4

### Changed

- Marked UNKNOWN business partner type as to be removed with contract story EID-6656

## 3.40.3

### Changed

- Migrate all `business_entity` records with type `UNKNOWN` or `INDIVIDUAL` to `BUSINESS` (migration `V1_21_0`). The new
  v2 onboarding flow (`/api/v2/internal/management/business-partners/`) always requires an explicit partner type, so
  `UNKNOWN` is no longer produced for new partners.
- Deprecated `BusinessPartnerType.UNKNOWN` and `BusinessPartnerTypeDto.UNKNOWN`. All existing occurrences have been
  migrated; this value must not be used in new onboarding requests via the v2 controller.

## 3.40.2

### Changed

- Update confluentinc/cp-enterprise-control-center from 7.9.5 to 7.9.8
- Update confluentinc/cp-enterprise-kafka from 7.9.5 to 7.9.8
- Update confluentinc/cp-schema-registry from 7.9.5 to 7.9.8
- Update confluentinc/cp-zookeeper from 7.9.5 to 7.9.8
- Update maven from 3.9.12 to 3.9.16
- Update repo.bit.admin.ch:8444/postgres from 17.8 to 17.10
- Update at.yawk.lz4:lz4-java from 1.10.1 to 1.11.1
- Update net.java.dev.jna:jna from 5.18.1 to 5.19.1
- Update org.jetbrains.kotlin:kotlin-stdlib from 2.2.21 to 2.4.0
- Update org.apache.commons:commons-fileupload2-jakarta-servlet6 from 2.0.0-M4 to 2.0.0-M5
- Update org.apache.commons:commons-fileupload2-core from 2.0.0-M4 to 2.0.0-M5
- Update commons-logging:commons-logging from 1.2 to 1.4.0
- Update commons-io:commons-io from 2.19.0 to 2.22.0
- Update commons-codec:commons-codec from 1.11 to 1.22.0
- Update jakarta.json:jakarta.json-api from 2.1.2 to 2.1.3
- Update jakarta.activation:jakarta.activation-api from 2.1.2 to 2.1.4
- Update jakarta.xml.bind:jakarta.xml.bind-api from 4.0.0 to 4.0.5
- Update com.fasterxml.jackson.module:jackson-module-jakarta-xmlbind-annotations from 2.17.2 to 2.22.1
- Update com.fasterxml.jackson.jakarta.rs:jackson-jakarta-rs-base from 2.17.2 to 2.22.1
- Update com.fasterxml.jackson.jakarta.rs:jackson-jakarta-rs-json-provider from 2.17.2 to 2.22.1
- Update org.sonarsource.scanner.maven:sonar-maven-plugin from 5.5.0.6356 to 5.7.0.6970
- Update org.apache.maven.plugins:maven-surefire-plugin from 3.5.5 to 3.5.6
- Update ch.qos.logback:logback-core from 1.5.36 to 1.5.38
- Update ch.qos.logback:logback-classic from 1.5.36 to 1.5.38
- Update org.glassfish.jersey.connectors:jersey-apache-connector from 3.1.10 to 3.1.12
- Update org.glassfish.jersey.inject:jersey-hk2 from 3.1.10 to 3.1.12
- Update org.glassfish.jersey.core:jersey-common from 3.1.10 to 3.1.12
- Update org.glassfish.jersey.core:jersey-client from 3.1.10 to 3.1.12
- Update io.github.openfeign.querydsl:querydsl-jpa-spring from 7.1 to 7.4.0
- Update io.github.openfeign.querydsl:querydsl-apt from 7.1 to 7.4.0
- Update org.openapitools:openapi-generator-maven-plugin from 7.20.0 to 7.23.0
- Update com.diffplug.spotless:spotless-maven-plugin from 3.2.1 to 3.8.0
- Update org.openapitools:jackson-databind-nullable from 0.2.9 to 0.2.10
- Update com.nimbusds:nimbus-jose-jwt from 10.8 to 10.9.1
- Update com.tngtech.archunit:archunit-junit5 from 1.4.1 to 1.4.2
- Update com.tngtech.archunit:archunit-junit5 from 1.4.1 to 1.4.2
- Update com.tngtech.archunit:archunit-junit5 from 1.4.1 to 1.4.2

## 3.40.1

### Changed

- Update demo data to align with sonar ruleset

## 3.40.0

### Changed

- Identifier ID generation now explicit

### Added

- Support for local E2E tests

## 3.39.7

### Fixed

- Fix identifier update returning 500 instead of 400 for a did log with an unrecognized DID method

## 3.39.6

### Changed

- removed EIDARTFE_754_VC_SCHEMA feature toggle from VqpsSubmissionB2BController. They are unrelated
- removed unused feature toggle EID_5540_UPDATE_IS_GOVERNMENT

## 3.39.5

### Changed

- Update README.md file

## 3.39.4

### Changed

- Fix missing tracing span when loading demo data and during integration tests

## 3.39.3

### Changed

- Version bump logback to 1.5.36 and org.glassfish.jersey to 3.1.10 for security fixes

## 3.39.2

### Added

- Add declaration of intent templates in it,fr,en,de for actor type gov and biz

## 3.39.1

### Changed

- Add some details in the failure responses during the AddDitSubmission creation process

## 3.39.0

### Added

- Add multi-language entity names for business partner V2 responses and persist new trust onboarding organization name
  translations.

## 3.38.2

### Fixed

- Add iss claim into messaging system security context

## 3.38.1

### Changed

- Bump bouncycastle version to 1.84 to fix HIGH vulnerabilities

## 3.38.0

### Added

- Send portal audit commands for business partner registration/updates, trust onboarding document uploads and trust
  onboarding submissions

## 3.37.6

### Fixed

- fixed sonar issue

## 3.37.5

### Changed

- version bump ch.admin.swiyu:didresolver to 2.8.2 (security fix, no CVE)

## 3.37.4

### Changed

- version bump jeap-spring-boot-parent to 33.11.0

## 3.37.3

### Changed

- version bump org.glassfish.jersey to 3.1.10 (CVE-2025-12383)

## 3.37.2

### Changed

- version bump spring-kafka to 3.3.16 (CVE-2026-41731)
- version bump jeap-spring-boot-parent to 33.9.0

## 3.37.1

### Changed

- refactored scheduled jobs into a dedicated module

## 3.37.0

### Changed

- Add validation of declaration of intent with external diskreter validator service

## 3.36.0

### Added

- Send registry audit commands for identifier and status registry operations

## 3.35.0

### Changed

- Enable virtual threads (Project Loom) for Tomcat, @Async, and @Scheduled tasks

## 3.34.3

### Fixed

- fixed issue with not executing flyway migrations on openshift

## 3.34.2

### Fixed

- Resolve race condition on concurrent PUT identifier entry requests causing duplicate-key 500 error

## 3.34.1

### Fixed

- Add `@ValidLocalizedMap` validation to `VqpsSubmissionInternalDto.purposeName` and `purposeDescription` so the
  internal API contract enforces the same localized-map rules (default key required, BCP-47 locale keys) as the
  create-request DTO and domain entity

## 3.34.0

### Changed

- Add download/generation of filled doi document in requested language based current trust onboarding submission content

## 3.33.0

### Added

- Allow business partners to delete trust onboarding submission documents when the submission is in state UNSUBMITTED or
  INFORMATION_REQUESTED

## 3.32.0

### Added

- Allow attribute "profile_version" in DIDDoc

## 3.31.0

### Added

- Validate the DCQL Queries that are submitted for vqPS

## 3.30.0

### Changed

- waiting for publication of a VQPS is now optional when creating a VqpsSubmission

## 3.29.1

### Changed

- Improve validation error messages for trust onboarding and base onboarding to include which field is missing or
  invalid, and what the expected format is.

## 3.29.0

### Changed

- Align pattern validation for trust onboarding and base onboarding with swiyu-ecosystem-portal. This includes:
    - static uid validation if present
    - static swiss zip code validation
    - other mandatory fields

## 3.28.0

### Added

- Added Submission of Verification Query Public Statement (vqPS)

## 3.27.0

### Changed

- Add missing attributes apping for trust onboarding submission

## 3.26.0

### Added

- Added dummy declaration of intent templates for all doi-variants and languages.
- Draft implementation of the filling service for the pdfs using PDFBox.

## 3.25.0

### Added

- New B2B API endpoint `PUT /api/v2/status/.../status-list-entries/{id}` with Swiss profile conformity validation: `typ`
  header (`statuslist+jwt`), `profile_version` header (`swiss-profile-vc:1.0.0`), `exp` claim, decompressed `lst` size
  limit (200KB, zip-bomb protected), and bit-alignment checks.

### Deprecated

- `PUT /api/v1/status/.../status-list-entries/{id}` is deprecated in favour of the v2 endpoint.

## 3.24.0

### Added

- Added signingRule and signatories to TrustOnboardingSubmission

## 3.23.11

### Changed

- bump parent to fix some vulnerabilities.

## 3.23.10

### Changed

- StatusListValidator now derives the DID from kid header instead of iss claim when validating status list ownership

## 3.23.9

### Changed

- Removed dependency requirement on DIDDocs to contain an "authentication" key

## 3.23.8

### Changed

- Removed dependency on iss claim during statuslist issuer validation

## 3.23.7

### Changed

- Removed constraint that a status lists needs to contain an iss claim

## 3.23.6

### Changed

- Removed constraint that a JWTs kid needs to match its iss claim

## 3.23.5

### Changed

- Sanitize filename of partner document before uploading to S3

## 3.23.4

### Fixed

- Fixed `AuditObject.type` value in `CreateAuditRecordCommand` from `StatusList` to `STATUS_LIST` to match the expected
  naming convention

## 3.23.3

### Changed

- Removed deprecated `isGovActor` field from `TrustOnboardingSubmissionDto`
- `type` field in `CreateBusinessEntityDto` is now required (`@NotNull`)

## 3.23.2

### Changed

- Updated setVersions script to ensure -SNAPSHOT suffix and CHANGELOG entry

## 3.23.1

### Changed

- Updated to JEAP 33.6.0 which fixes broken health indicator metrics for multi db

## 3.23.0

### Changed

- Removed @context requirement for DIDDoc & DIDLogs
- Updated DIDResolver to 2.7, allowing for JSON Representation of DIDDoc & DIDLogs

## 3.22.9

### Fixed

- Exclude business partner role required to access API self-service-portal apimgmt%selfservice from token by using
  custom converter. Change was introduced by jeap with jeap-spring-boot-starters version 21.0.0.
  See https://bitbucket.bit.admin.ch/projects/JEAP/repos/jeap-spring-boot-starters/browse/CHANGELOG.md

## 3.22.8

### Fixed

- Updated jeap-spring-boot-parent to 33.2.0 to resolve tomcat CVEs

## 3.22.7

### Changed

- Align API error responses to use `BusinessExceptionErrorCode` directly and extend schemas with possible business error
  codes.

## 3.22.6

### Changed

- Enhance exception message for identifier registry url mismatch

## 3.22.5

### Fixed

- patch CVE-2026-33871

## 3.22.4

### Fixed

- patch CVE-2026-33870

## 3.22.3

### Fixed

- Update MessageContractsConfig to register TiTrustAddDidSubmissionAcceptedEvent and
  TiTrustAddDidSubmissionRejectedEvent

## 3.22.2

### Changed

- Disable kafka message encryption temporarily until we have a solution for the encryption issue related to vault
  transit engine

## 3.22.1

### Fixed

- Update jeap-spring-boot-parent to 31.4.0 to resolve CVE CVE-2026-22732

## 3.22.0

### Added

- Added message encryption

## 3.21.0

### Added

- Scheduled cleanup task for TrustAdditionalDidsSubmission that sets UNSUBMITTED entries older than 90 days to
  UNSUBMITTED_TIMEOUT.

## 3.20.1

### Changed

- Handle TrustAddDidSubmissionAcceptedEvent and TrustAddDidSubmissionRejectedEvent

## 3.20.0

### Added

- Integrated status- and identifier-registries authoring capabilities. Mutations to those registries are now directly
  stored to DB intead via API to authoring services.

## 3.19.12

### Changed

- Use the correct `client-id` for token inspection.

## 3.19.11

### Changed

- aligned audit-id for changes in audit metadata fields and auditing commands
- improved testability with kafka

## 3.19.10

### Changed

- Add B2B gateway introspection configuration.

## 3.19.9

### Added

- Added auditing for status list changes

## 3.19.8

### Changed

- Use lightweight token mode to inspect pruned tokens therefore removing a technical limitation on roles per user.

## 3.19.7

### Changed

- Move to aligned messaging error handling topic

## 3.19.6

### Added

- Added API to add additional DIDs to a trusted partner identity

## 3.19.5

### Changed

- upgraded to latest jeap-spring-boot-parent version 30.18.0 (fixes GHSA-72hv-8253-57qq)

## 3.19.4

### Changed

- introduced new maven structure for merging status- and identifier-authoring-service

## 3.19.3

### Changed

- Update confluentinc/cp-enterprise-control-center from 7.9.4 to 7.9.5
- Update confluentinc/cp-enterprise-kafka from 7.9.4 to 7.9.5
- Update confluentinc/cp-schema-registry from 7.9.4 to 7.9.5
- Update confluentinc/cp-zookeeper from 7.9.4 to 7.9.5
- Update repo.bit.admin.ch:8444/postgres from 15.15 to 17.8
- Update org.apache.maven.plugins:maven-surefire-plugin from 3.5.4 to 3.5.5
- Update at.yawk.lz4:lz4-java from 1.10.1 to 1.10.3
- Update org.jetbrains.kotlin:kotlin-stdlib from 2.2.21 to 2.3.10
- Update com.diffplug.spotless:spotless-maven-plugin from 3.1.0 to 3.2.1
- Update io.github.swiyu-admin-ch:didresolver from 2.4.0 to 2.6.0
- Update org.openapitools:openapi-generator-maven-plugin from 7.17.0 to 7.20.0
- Update org.openapitools:jackson-databind-nullable from 0.2.8 to 0.2.9
- Update com.nimbusds:nimbus-jose-jwt from 10.6 to 10.8
- Update ch.admin.bit.jeap:jeap-spring-boot-parent from 30.15.0 to 30.16.0

## 3.19.2

### Changed

- Removed deprecated URL string constructor usage

## 3.19.1

### Added

- Identifiers now also provide the URL required by the DIDToolbox to create the DIDLog in the GET endpoint

## 3.19.0

### Added

- Identifiers are now validated to point to a known base registry

## 3.18.3

### Changed

- Added permission check to BP creation to only create governmental BPs if user is allowed to do so

## 3.18.2

### Changed

- added requestedPartnerType attribute to TrustOnboardingSubmission

## 1.18.1

### Changed

- Removed IsGovernmental controller which only been used for testing purpose

## 1.18.0

### Added

- Identifier upload now allows only for updated identifiers to protect from DID forking

## 1.17.0

### Added

- Add support for TrustOnboardingSubmission list endpoint to filter for Business Partners

## 1.16.2

### Changed

- Re-allow authorized users to create new business partners

## 1.16.1

### Changed

- enabled REST request tracing by default

## 1.16.0

### Changed

- Updated java version from 21 to 25 and jeap-spring-boot-parent to 30.15.0

## 1.15.10

### Added

- added log in case of BusinessException

## 1.15.9

### Changed

- added requestedPartnerType attribute to TrustOnboardingSubmission

## 1.15.8

### Changed

- cleaned up API identifiers in OpenAPI spec

## 1.15.7

### Changed

- Fixed issue which occurred while saving did detail description

## 1.15.6

### Changed

- added null-check for UID when creating trust onboarding submission.

## 1.15.5

### Changed

- default resource id is now bj-swiyu-ecosystem as configured in mock and real oauth server

## 1.15.4

### Changed

- updated jeap-spring-boot-parent to 30.10.0
- enabled support for detailed health metrics

## 1.15.3

### Added

- integrated support for validating whether a user is member of the "Swiyu Governmental Allowlist" which allows the
  creation of governmental business partners.

## 1.15.2

### Changed

- REST Endpoints are now secured by checking against the following roles:
    - ti_@trustonboardingsubmission_@read
    - ti_@trustonboardingsubmission_@write
    - ti_@vcschemasubmission_@read
    - ti_@vcschemasubmission_@write
    - ti_@businesspartner_#read
    - ti_@businesspartner_#write
    - ti_@identifier_#read
    - ti_@identifier_#write
    - ti_@status_#read
    - ti_@status_#write

## 1.15.1

### Added

- Added commercial register property to trust onboarding submission

## 1.15.0

### Added

- Added aggregation field for trust status to business partner

## 1.14.1

### Added

- Did Detail page

## 1.14.0

### Added

- new business partner internal V2 API
- deprecated old business partner APIs

## 1.13.38

### Changed

- Add sonar ignore for deprecation warning while in use / till contract

## 1.13.37

### Changed

- Add missing deprecated as part of 1.3.20 business partner type extension

## 1.13.36

### Changed

- Fixed logging output to be JSON again

## 1.13.35

### Changed

- Upgraded to JEAP 30.2.0
- Re-Enabled Pact Test

## 1.13.34

### Changed

- Remove PostAuthorize from trust onboarding submit endpoint because authorization is already done at start of method

## 1.13.33

### Changed

- Pop of TrustOnboardingSubmission is only replaced during update if the did selection changed

## 1.13.32

### Changed

- disabled pact test while it still has bugs

## 1.13.31

### Added

- Add missing POP validation on submit

## 1.13.30

### Changed

- removed migrateAddMissingDidToIdentifierEntries on startup (not needed any longer and leads to flaky tests)
- improved tests (less verbose logging, fixed certain warnings)

## 1.13.29

### Changed

- do not allow arbitrary properties/items in document root of did:tdw and did:webvh schema validations

## 1.13.28

### Changed

- Improved Pact Test for Trust Onboarding Submission
- Upgraded to Jeap 30.1.0
- fixed CVE-2025-66566 by updating "at.yawk.lz4:lz4-java" to "1.10.1"

## 1.13.27

### Changed

- Fixed by CVE-2025-12183 by updating to lz4-java 1.8.1

## 1.13.26

### Changed

- improved authorization handling for trust onboarding endpoints

## 1.13.25

### Changed

- fixed TrustOnboardingPactProviderTest, improved onboarding test data

## 1.13.24

### Changed

- changed authentication for trust onboarding endpoints. removed post authorize and added token check at start of method

## 1.13.23

### Changed

- refactor to enforce partner document module boundaries

## 1.13.22

### Changed

- Added sonar plugin dependency in pom.xml to fix build failure in pipeline

## 1.13.21

### Changed

- Update confluentinc/cp-enterprise-control-center from 7.9.2 to 7.9.4
- Update confluentinc/cp-enterprise-kafka from 7.9.2 to 7.9.4
- Update confluentinc/cp-schema-registry from 7.9.2 to 7.9.4
- Update confluentinc/cp-zookeeper from 7.9.2 to 7.9.4
- Update repo.bit.admin.ch:8444/postgres from 15.14 to 15.15
- Update net.java.dev.jna:jna from 5.18.0 to 5.18.1
- Update org.jetbrains.kotlin:kotlin-stdlib from 2.2.20 to 2.2.21
- Update com.diffplug.spotless:spotless-maven-plugin from 3.0.0 to 3.1.0
- Update io.github.swiyu-admin-ch:didresolver from 2.2.0 to 2.3.0
- Update org.openapitools:openapi-generator-maven-plugin from 7.15.0 to 7.17.0
- Update org.openapitools:jackson-databind-nullable from 0.2.7 to 0.2.8
- Update com.nimbusds:nimbus-jose-jwt from 10.5 to 10.6
- Update ch.admin.bit.jeap:jeap-spring-boot-parent from 27.2.0 to 28.3.0

## 1.13.20

### Added

- Introduced new BusinessPartnerType values: BUSINESS, INDIVIDUAL, UNKNOWN (with UNKNOWN as migration/default)
- Added businessPartnerType field to TrustOnboardingSubmissionDto (enum), keeping deprecated isGovActor for transition
- updateBusinessEntityIsGovernment now sets UNKNOWN instead of null when unsetting governmental type

### Deprecated

- isGovActor field in TrustOnboardingSubmissionDto marked for removal when contracting

## 1.13.19

### Added

- Authorization for Trust Onboarding Submissions
- phase out old DIDs by setting their status to deactivated

## 1.13.18

### Changed

- align kafka usage in local/shared profile

## 1.13.17

### Added

- GET endpoint to retrieve single business partner

### Changed

- validate version of trust onboarding submission on submit

## 1.13.16

### Changed

- Fixed wrong kafka topic configuration

## 1.13.15

### Changed

- Update demo data to mimic prod data

## 1.13.14

### Changed

- Demo data import can now reset itself

## 1.13.13

### Changed

- system user context is now set during did migration of identfier entries (no errors should occur anymore)
- replaced Page with PagedModel for paged GET Identifier APIs
- introduced querydsl for a generic search example

## 1.13.12

### Changed

- fixed local kafka startup in docker-compose

### Added

- demo data importer now creates for each partner one identifier entry

## 1.13.11

### Changed

- Removed optimistic locking handling for trustonboardingsubmission since causes issues with status updates (will be
  fixed later)

## 1.13.10

### Changed

- Fixed did resolving on identifier entries

## 1.13.9

### Changed

- Add test for new identifier internal control to pass sonar gate

## 1.13.8

### Changed

- Add identifier internal endpoint so bp identifier registry data are available for ecosystem portal

## 1.13.7

### Changed

- Finish B2B endpoint for trust onboarding submissions

## 1.13.6

### Changed

- Rename previous migration script V1_10_1_ which hasn't been applied yet and therefore lead to error

## 1.13.5

### Changed

- Align migration script file name with pom version

## 1.13.4

### Added

- Full DID is now saved on identifier entry.
- Add migration for all old identifier entries with valid DID:TDW stored on base registry

## 1.13.3

### Added

- Proof of Possession (PoP) validation for trust onboarding submissions
- Bare for b2b endpoint for trust onboarding submissions

## 1.13.2

### Changed

- Virus scan utilizes basic authorization

## 1.13.1

### Changed

- Virus scan base url can now be suffixed by /scan

## 1.13.0

### Added

- Add virus scan module
- TrustOnboardingSubmission now auto timeout after 90 days (all associated documents get deleted)

### Changed

- Virus scan uploaded TrustOnboardingSubmission documents

## 1.12.4

### Changed

- simplified feature toggle handling
- added feature toggle EID_5540_UPDATE_IS_GOVERNMENT in order to allow setting the partner type governmental via rest
  endpoint

## 1.12.3

### Changed

- Update confluentinc/cp-enterprise-control-center from 5.3.1 to 7.9.2
- Update confluentinc/cp-enterprise-kafka from 5.3.1 to 7.9.2
- Update confluentinc/cp-schema-registry from 5.3.1 to 7.9.2
- Update confluentinc/cp-zookeeper from 5.3.1 to 7.9.2
- Update maven from 3.9.10 to 3.9.11
- Update repo.bit.admin.ch:8444/postgres from 15.13 to 15.14
- Update org.apache.maven.plugins:maven-surefire-plugin from 3.5.2 to 3.5.4
- Update net.java.dev.jna:jna from 5.17.0 to 5.18.0
- Update org.jetbrains.kotlin:kotlin-stdlib from 2.2.0 to 2.2.20
- Update com.diffplug.spotless:spotless-maven-plugin from 2.45.0 to 3.0.0
- Update io.github.swiyu-admin-ch:didresolver from 2.0.1 to 2.2.0
- Update org.openapitools:openapi-generator-maven-plugin from 7.14.0 to 7.15.0
- Update com.networknt:json-schema-validator from 1.5.8 to 1.5.9
- Update org.openapitools:jackson-databind-nullable from 0.2.6 to 0.2.7
- Update com.nimbusds:nimbus-jose-jwt from 10.3.1 to 10.5

## 1.12.2

### Changed

- Fix S3 jeap client
- Fix Document not found exception

## 1.12.2

### Changed

- Fix S3 jeap property import

## 1.12.0

### Added

- Add endpoint to set if a business partner is government or not

## 1.11.0

### Added

- Conformity check for deactivated DID

## 1.10.0

### Added

- Added document upload capabilities

## 1.9.7

### Changed

- submit requires version as parameter, only succeeds if user tries to submit latest version
- change to listItemDto for getting all trust onboarding submissions

## 1.9.6

### Changed

- Update jeap-spring-boot-parent dependency

## 1.9.5

### Added

- Added Demo test data injector capabilities

## 1.9.4

### Added

- Added TrustOnboardingSubmission endpoints

## 1.9.3

### Changed

- Update to latest jeap-spring-boot-parent

## 1.9.2

### Changed

- enforce software architecture requirement to differentiate between internal api and external api for VCSchema classes

## 1.9.1

### Changed

- Add missing Health Indicator for StatusListData service

## 1.9.0

### Changed

- Updated message dependencies to latest version
- Updated event handling for TiTrustOnboardingRejectedEvent, TiTrustOnboardingInformationRequestedEvent

### Added

- Added reject/decline reasons and partner note to TrustOnboardingSubmission

## 1.8.13

### Changed

- Update message dependencies to latest versions

## 1.8.12

### Changed

- Handle trust onboarding status update events

## 1.8.11

### Changed

- Upgrade message dependency for failed and succeeded event because topic name was wrong

## 1.8.10

### Changed

- Configure kafka related settings to handle events and retrieve data via api client

## 1.8.9

### Changed

- Use the newest event type for submission accepted event

## 1.8.8

### Changed

- Fixing message reference key for event messages keys

## 1.8.7

### Changed

- Using a message reference key for event messages as key

## 1.8.6

### Changed

- Using a business id for event messages as key

## 1.8.5

### Changed

- changed recently added API endpoint "/api/v1/internal/business-entities" to "
  /api/v1/internal/management/business-partners"
- aligned namings/descriptions of APIs

### Added

- Consuming and processing TiVcSchemaPublicationSucceeded and TiVcSchemaPublicationFailed events

## 1.8.4

### Changed

- Fix startup property validation

## 1.8.3

### Changed

- Update maven from 3.9.9 to 3.9.10
- Update repo.bit.admin.ch:8444/confluentinc/cp-kafka from 7.5.0 to 8.0.0
- Update repo.bit.admin.ch:8444/confluentinc/cp-kafka from 7.5.0 to 8.0.0
- Update repo.bit.admin.ch:8444/confluentinc/cp-schema-registry from 7.5.0 to 8.0.0
- Update org.jetbrains.kotlin:kotlin-stdlib from 2.1.21 to 2.2.0
- Update com.diffplug.spotless:spotless-maven-plugin from 2.44.4 to 2.45.0
- Update org.openapitools:openapi-generator-maven-plugin from 7.13.0 to 7.14.0
- Update com.networknt:json-schema-validator from 1.5.6 to 1.5.8
- Update com.nimbusds:nimbus-jose-jwt from 10.3 to 10.3.1
- Update ch.admin.bit.jeap:jeap-spring-boot-parent from 26.64.1 to 26.68.0

## 1.8.2

### Added

- Status list validation failures now contain more additional details as to why the validation failed

## 1.8.1

### Added

- The service now also validates that an uploaded status list is not older than 24 hours
- The service now also validates that the issuer of a status list belongs to the same business partner
- The service now also validates that the issuer of a status list is the same during its lifetime

## 1.8.0

### Added

- The service now also validates that an uploaded status list is a valid VC.

## 1.7.0

### Added

- Added VcSchemaSubmission controller for internal communication (service to service)

## 1.6.4

### Changed

- Updated jeap-spring-boot-parent version to 26.61.0

## 1.6.3

### Changed

- Updated jeap-spring-boot-parent version to 26.60.0

## 1.6.2

### Changed

- Fixed sonar code smells (S3740, S5778)

## 1.6.1

### Changed

- Fixed sonar code smells (S1161, S1118, S5976)

## 1.6.0

### Added

- Add create, read and list endpoints for VcSchema

## 1.5.1

### Added

- Implemented missing validations for DID Log, DID Document, and Status List to ensure specification compliance.

## 1.5.0

### Other

- Update Interface Summaries

## 1.4.9

### Changed

- Downgrade maven-sunfire-plugin due to archunit incompatibility

## 1.4.8

### Changed

- Update org.springframework.security:spring-security-crypto from 6.4.5 to 6.5.0
- Update org.jetbrains.kotlin:kotlin-stdlib from 1.9.25 to 2.1.21
- Update com.nimbusds:nimbus-jose-jwt from 9.48 to 10.3
- Update ch.admin.bit.jeap:jeap-spring-boot-parent from 26.50.0 to 26.50.1

# 1.4.6

### Other

- Fix audit metadata author for both user and api-gw call

## 1.4.5

### Other

- Fixed major or critical sonar issues

## 1.4.4

### Other

- Added spotless plugin

## 1.4.3

### Changed

- Fix sonar issues which occurred after main merge

## 1.4.2

### Changed

- Align audit metadata usage with other services

# 1.4.1

### Changed

- Actually use the documented enum constants in the management endpoint

# 1.4.0

### Added

- Now supports update of organisation name property.

# 1.3.2

### Changed

- Updated jeap spring boot parent version to 26.41.0
- Set version of spring-security-crypto specifically to 6.4.4 to resolve vulnerability CVE-2025-22228

# 1.3.1

### Changed

- Updated DID Resolver to 2.0.1

### Changed

- Update dependencies due to CVE-2025-24813

# 1.3.0

### Added

- add new context for diddoc to conform to didtoolbox 1.2.0 changes

# 1.2.4

### Changed

- Removed link to team e-mail which is then used by the openapi client generator

# 1.2.3

### Changed

- Upgrade did-resolver library from version 1.0.1 to 2.0.0

## 1.2.2

### Changed

- Changed mail pattern for mgmt object to be conformant with HTML5 email
  validation https://html.spec.whatwg.org/multipage/input.html#valid-e-mail-address

## 1.2.1

### Changed

- Changed mail pattern for mgmt object to be RFC2822 conform

## 1.2.0

### Added

- Management now reports current limits per business partner

### Changed

- DID creation returns in error if limit is surpassed
- Statuslist creation returns error if limit is surpassed
- ApiError now explicitly declares which error codes exists

# 1.1.2

### Changed

- Set connection timeout, read timeout and max redirects for rest client.

## 1.1.1

### Changed

- unhandled errors are now logged with stack trace

## 1.1.0

### Added

- Extending prometheus export with metrics for build

## 1.0.1

### Added

- Check health endpoints of status authoring, identifier authoring and PAMS api

## 1.0.0

- Initial Release
