package com.redhat.idaas.connect.configuration;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;


/**
 * Parses iDAAS Connect Application Properties used to generate Camel components, routes, and processors.
 * iDAAS properties
 * iDAAS property format
 * Components:
 * - [idaas namespace].component.[component name]=[component class]
 * - idaas.connect.component.hl7decoder=org.apache.camel.component.hl7.HL7MLLPNettyDecoderFactory
 *
 * Consumer Endpoints:
 * - [idaas namespace].consumer.[route id].[scheme|context|options]
 * - idaas.connect.consumer.hl7-mllp.scheme=netty:tcp
 *
 * Producer Endpoints:
 * - [idaas namespace].producer.[route id].[zero based producer id].[scheme|context|options]
 * - idaas.connect.producer.hl7-mllp.0.scheme=stub
 */
public final class PropertyParser {

    // constants used to parse property keys
    private static final String IDAAS_NAMESPACE_PROPERTY = "idaas.connect.namespace";
    private static final String DEFAULT_IDAAS_PROPERTY_NAMESPACE = "idaas.connect";
    private static final String IDAAS_COMPONENT_NAMESPACE = "component";
    private static final String IDAAS_CONSUMER_NAMESPACE = "consumer";
    private static final String IDAAS_PRODUCER_NAMESPACE = "producer";

    /**
     * the namespace, or prefix, used to filter idaas connect properties
     */
    private final String idaasPropertyNamespace;

    /**
     * properties object containing idaas connect properties
     */
    private final Properties idaasProperties;

    /**
     * Maps a component name to it's fully qualified class name
     */
    private final Map<String, String> idaasComponents = new HashMap<>();

    /**
     * Maps a route id to a camel route
     */
    private final Map<String, CamelRoute> idaasRoutes = new HashMap<>();

    /**
     * Camel {@link RouteBuilder} instances used to construct a {@link RouteDefinition}
     */
    private final List<RouteBuilder> idaasRouteDefinitions = new ArrayList<>();

    /**
     * Returns a map of iDAAS Components
     * Key - component name
     * Value - component class name
     * @return iDAAS Components as {@link Map}
     */
    public Map<String, String> getIdaasComponents() {
        return idaasComponents;
    }

    /**
     * Returns a map of iDAAS Routes
     * Key - route id
     * Value - {@link CamelRoute}
     * @return iDAAS Route as {@link Map}
     */
    private Map<String, CamelRoute> getIdaasRoutes() { return idaasRoutes; }

    /**
     * Returns the Camel {@link RouteBuilder} used to construct the processing routes declared
     * in iDAAS {@link Properties}
     * @return {@link List}
     */
    public List<RouteBuilder> getIdaasRouteDefinitions() { return idaasRouteDefinitions; }

    /**
     * Parses the component identifier and value (component class name) from a property name and value.
     * @param propertyName The property name
     * @param propertyValue The property value (component class name)
     */
    private void parseComponent(String propertyName, String propertyValue) {
        int componentNameIndex = propertyName.lastIndexOf('.') + 1;
        String componentKey = propertyName.substring(componentNameIndex);
        idaasComponents.put(componentKey, propertyValue);
    }

    /**
     * Parses a route id from a consumer or producer endpoint.
     * @param propertyName The idaas property name/key
     * @param propertyValue The idaas property value
     */
    private void parseRouteId(String propertyName, String propertyValue) {
        String routeId =  propertyName
                .substring(idaasPropertyNamespace.length() + 1)
                .split("\\.")[1];

        if (!idaasRoutes.containsKey(routeId)) {
            CamelRoute camelRoute = new CamelRoute();
            camelRoute.setRouteId(routeId);
            idaasRoutes.put(routeId, camelRoute);
        }
    }

    /**
     * Sets a {@link CamelEndpoint} field
     * @param camelEndpoint The target CamelEndpoint
     * @param endpointField The field to set
     * @param fieldValue The field value
     */
    private void setEndpointField(CamelEndpoint camelEndpoint, String endpointField, String fieldValue) {

        if (endpointField.equalsIgnoreCase("scheme")) {
            camelEndpoint.setScheme(fieldValue);
        } else if (endpointField.equalsIgnoreCase("context")) {
            camelEndpoint.setContextPath(fieldValue);
        } else if (endpointField.equalsIgnoreCase("options")) {
            camelEndpoint.setOptions(fieldValue);
        }
    }

    /**
     * Parses a consumer endpoint property
     * @param propertyName The property name/key
     * @param propertyValue The property value
     */
    private void parseConsumer(String propertyName, String propertyValue) {
        String consumerNamespace = idaasPropertyNamespace
                .concat(".")
                .concat(IDAAS_CONSUMER_NAMESPACE);

        String[] consumerFields = propertyName
                .substring(consumerNamespace.length() + 1)
                .split("\\.");

        String routeId = consumerFields[0];
        String consumerField = consumerFields[1];

        CamelEndpoint consumer = idaasRoutes.get(routeId).getConsumer();
        setEndpointField(consumer, consumerField, propertyValue);
    }

    /**
     * Parses a producer endpoint property
     * @param propertyName The property name/key
     * @param propertyValue The property value
     */
    private void parseProducer(String propertyName, String propertyValue) {
        String producerNamespace = idaasPropertyNamespace
                .concat(".")
                .concat(IDAAS_PRODUCER_NAMESPACE);

        String[] producerFields = propertyName
                .substring(producerNamespace.length() + 1)
                .split("\\.");

        String routeId = producerFields[0];
        int producerIndex = Integer.parseInt(producerFields[1]);
        String producerField = producerFields[2];

        List<CamelEndpoint> producers = idaasRoutes.get(routeId).getProducers();
        CamelEndpoint producer;

        if (producerIndex != producers.size() -1) {
            producer = new CamelEndpoint();
            producers.add(producer);
        } else {
            producer = idaasRoutes.get(routeId).getProducers().get(producerIndex);
        }

        setEndpointField(producer, producerField, propertyValue);
    }

    /**
     * Parses Camel {@link RouteBuilder} from property metadata
     */
    private void parseRoutes() {
        for (Entry<String, CamelRoute> routeEntry: idaasRoutes.entrySet()) {
            CamelRoute camelRoute = routeEntry.getValue();

            idaasRouteDefinitions.add(new RouteBuilder() {
                @Override
                public void configure() {
                    CamelEndpoint consumer = camelRoute.getConsumer();

                    if (camelRoute.getProducers().size() == 1) {
                        CamelEndpoint producer = camelRoute.getProducers().get(0);
                        from(consumer.toString())
                                .routeId(camelRoute.getRouteId())
                                .log(LoggingLevel.INFO, "${body}")
                                .to(producer.toString());
                    } else {
                        String producerUris = camelRoute.getProducers()
                                .stream()
                                .map(CamelEndpoint::toString)
                                .collect(Collectors.joining(","));
                        from(consumer.toString())
                                .routeId(camelRoute.getRouteId())
                                .log(LoggingLevel.INFO, "${body}")
                                .multicast()
                                .to(producerUris);
                    }
                }
            });
        }
    }

    /**
     * Parses application properties, generating application metadata for Camel components.
     */
    private void parseProperties() {
        for (String propertyName : idaasProperties.stringPropertyNames()) {
            String propertyValue = idaasProperties.getProperty(propertyName);

            int namespaceStartIndex = idaasPropertyNamespace.length() + 1;
            int namespaceEndIndex = namespaceStartIndex + propertyName.substring(namespaceStartIndex).indexOf('.');
            String propertyNamespace = propertyName.substring(namespaceStartIndex, namespaceEndIndex);

            switch (propertyNamespace) {
                case IDAAS_COMPONENT_NAMESPACE:
                    parseComponent(propertyName, propertyValue);
                    break;
                case IDAAS_CONSUMER_NAMESPACE:
                    parseRouteId(propertyName, propertyValue);
                    parseConsumer(propertyName, propertyValue);
                    break;
                case IDAAS_PRODUCER_NAMESPACE:
                    parseRouteId(propertyName, propertyValue);
                    parseProducer(propertyName, propertyValue);
                    break;
            }
        }
    }

    /**
     * Returns a property value from the underlying properties object
     *
     * @param propertyKey The property key to lookup
     * @return The property value if found, otherwise returns null
     */
    public String getPropertyValue(String propertyKey) {
        return idaasProperties.getProperty(propertyKey);
    }

    /**
     * Filters properties by matching on a string prefix.
     *
     * @param properties   The source Properties
     * @param filterPrefix The prefix used to match the properties
     * @return a Properties instance containing properties which match the string prefix
     */
    private Properties filterProperties(Properties properties, String filterPrefix) {
        Map<Object, Object> filteredProperties = properties
                .entrySet()
                .stream()
                .filter(p -> p.getKey().toString().startsWith(filterPrefix))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Properties idaasProperties = new Properties();
        idaasProperties.putAll(filteredProperties);
        return idaasProperties;
    }

    /**
     * Creates a new PropertyParser instance.
     * Incoming properties are filtered to include properties which are used to generate Camel
     * components, routes, and processors.
     * @param properties The source application properties
     */
    public PropertyParser(Properties properties) {
        idaasPropertyNamespace = properties.getProperty(IDAAS_NAMESPACE_PROPERTY, DEFAULT_IDAAS_PROPERTY_NAMESPACE);
        idaasProperties = filterProperties(properties, idaasPropertyNamespace);
        parseProperties();
        parseRoutes();
        idaasRoutes.clear();
    }
}