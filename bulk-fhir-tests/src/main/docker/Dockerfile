FROM vasdvp/health-apis-dev-tools:mvn-3.6-jdk-12

COPY maven/ /bulk-fhir-tests
RUN chmod 755 /bulk-fhir-tests/*.sh
ENTRYPOINT ["/bulk-fhir-tests/entrypoint.sh"]
CMD []
