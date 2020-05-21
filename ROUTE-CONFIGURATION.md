# iDAAS-Connect Data Processing Route Configuration

# Properties based configuration
iDAAS-Connect data processing routes are configured in the [application.properties](src/main/resources/application.properties) file.
Properties related to iDAAS-Connect routes use the prefix, "idaas.connect".
"idaas.connect" properties support iDAAS components, iDAAS consumers, and additional iDAAS producers.

# iDAAS-Connect Component Format
The format used for iDAAS-Connect component properties is:
```sh
idaas.connect.component.[component name]=[component class]
```
A "component" is a custom data processor used in message processing. Available components include components within the Camel 
framework, and it's supported libraries, or components authored within iDAAS-Connect.

Example: specifying HL7 encoders/decoders for MLLP processing
```sh
idaas.connect.component.hl7decoder=org.apache.camel.component.hl7.HL7MLLPNettyDecoderFactory
idaas.connect.component.hl7encoder=org.apache.camel.component.hl7.HL7MLLPNettyEncoderFactory
```

# iDAAS-Connect Consumer Format
The format used for iDAAS-Connect consumer properties is:
```sh
idaas.connect.consumer.[route id].[scheme|context|option]=[scheme|context|option value]
```
The route id is the logical name for the iDAAS-Connect data processing route, which includes the consumer, optional processors,
and producer(s).

The scheme, context, and option "fields" are used to build the consumer's URI. The resulting URI has the format:
```sh
[scheme]://[context]?[option1=value&option2=value]
```
Options are "optional", and are separated using `&`. If options are not included the `?` separator is excluded.

Example: the properties used to support a HL7-MLLP based consumer
```sh
idaas.connect.consumer.hl7-mllp.scheme=netty:tcp
idaas.connect.consumer.hl7-mllp.context=localhost:2575
idaas.connect.consumer.hl7-mllp.options=sync=true,encoders=#hl7encoder,decoders=#hl7decoder
```

The generated consumer URI is
 ```sh
 netty:tcp://localhost:2575?sync=true&encoders=#hl7encoder&decoders=#hl7decoder
```

# iDAAS-Connect Producer Format
iDAAS-Connect provides a single Kafka producer, used to store incoming data messages. Additional producers may be included
in the application.properties file and included in the data route.

The format used for iDAAS-Connect producer properties is similar to consumer properties:
```sh
idaas.connect.producer.[route id].[producer number][scheme|context|option]=[scheme|context|option value]
```

The route id is used to associate the producer with a configured consumer.
The `producer number` is a numeric identifier used to associate the property configurations for the producer URI.

```sh
idaas.connect.producer.hl7-mllp.0.scheme=stub
idaas.connect.producer.hl7-mllp.0.context=hl7-stub
```

The generated producer URI is
 ```sh
 stub://hl7-stub
 ```

