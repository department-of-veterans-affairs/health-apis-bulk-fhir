FROM vasdvp/health-apis-maven:3.6-jdk-12

COPY / /bulk-fhir-tests
RUN chmod 755 /bulk-fhir-tests/entrypoint.sh
ENTRYPOINT ["/bulk-fhir-tests/entrypoint.sh"]
CMD []
