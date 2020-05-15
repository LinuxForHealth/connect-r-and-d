# iDAAS-Connect
iDAAS Connectors for Inboud Data Processing

## MVP Features
- Consolidate inbound processing routes aka "connectors" defined in [iDAAS Connect Clinical Third Party](https://github.com/RedHat-Healthcare/iDAAS-Connect-Clinical-ThirdParty), [iDAAS Connect Clinical Industry Standards](https://github.com/RedHat-Healthcare/iDAAS-Connect-Clinical-IndustryStandards), [iDAAS Connect Finanacial Industry Standards](https://github.com/RedHat-Healthcare/iDAAS-Connect-Financial-IndustryStandards), and [iDAAS Connect Financial Third Party](https://github.com/RedHat-Healthcare/iDAAS-Connect-Financial-ThirdParty)
- Setup wizard used to generate an iDAAS Connect app with minimal dependencies
- YML configuration for inbound processing/route configuration and feature enablement
- Container build support targeting [Red Hat Universal Base Image](https://developers.redhat.com/products/rhel/ubi/)
- Utilize Red Hat/JBoss curated dependencies for Camel 3.x support