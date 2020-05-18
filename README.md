# iDAAS-Connect
iDAAS Connectors for Inboud Data Processing

Powered by [Apache Camel](https://camel.apache.org/)

## MVP Features
- Consolidate inbound processing routes aka "connectors" defined in [iDAAS Connect Clinical Third Party](https://github.com/RedHat-Healthcare/iDAAS-Connect-Clinical-ThirdParty), [iDAAS Connect Clinical Industry Standards](https://github.com/RedHat-Healthcare/iDAAS-Connect-Clinical-IndustryStandards), [iDAAS Connect Finanacial Industry Standards](https://github.com/RedHat-Healthcare/iDAAS-Connect-Financial-IndustryStandards), and [iDAAS Connect Financial Third Party](https://github.com/RedHat-Healthcare/iDAAS-Connect-Financial-ThirdParty).
- YML configuration for dynamic route configuration/feature enablement.
- Minimal dependency build using build properties.
- Container build support targeting [Red Hat Universal Base Image](https://developers.redhat.com/products/rhel/ubi/)
- Build plugin support for K8s and OpenShift based deployments