package com.redhat.idaas.connect.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link CamelRoute}
 */
public class CamelRouteTest {

    private CamelRoute camelRoute;

    /**
     * Configures the {@link CamelRoute} fixture
     */
    @BeforeEach
    public void beforeEach() {
        camelRoute = new CamelRoute();
        camelRoute.setRouteId("myRoute");

        CamelEndpoint consumer = new CamelEndpoint();
        consumer.setScheme("ftp");
        consumer.setContextPath("myftp.com:22/dropbox");
        camelRoute.setConsumer(consumer);

        CamelEndpoint producer = new CamelEndpoint();
        producer.setScheme("file");
        producer.setContextPath("home/serviceuser/documents");
        producer.addOption("fileExist", "FAIL");
        camelRoute.addProducer(producer);
    }

    /**
     * Tests {@link CamelRoute#equals} where an instance is compared to null
     */
    @Test
    public void testEqualsNull() {
        Assertions.assertNotEquals(camelRoute, null);
    }

    /**
     * Tests {@link CamelRoute#equals} where an instance is compared to a non-compatible type
     */
    @Test
    public void testEqualsDifferentClass() {
        Assertions.assertNotEquals(camelRoute, "a string");
    }

    /**
     * Tests {@link CamelRoute#equals} where an instance is compared to an equivalent object
     */
    @Test
    public void testEqualsEquivalent() {
        CamelRoute otherRoute = new CamelRoute();
        otherRoute.setRouteId(camelRoute.getRouteId());
        Assertions.assertEquals(camelRoute, otherRoute);
    }

    /**
     * Tests {@link CamelRoute#equals} where an instance is compared to a non-equivalent object
     */
    @Test
    public void testEqualsNotEquivalent() {
        CamelRoute otherRoute = new CamelRoute();
        otherRoute.setRouteId("otherRoute");
        Assertions.assertNotEquals(camelRoute, otherRoute);
    }

    /**
     * Validates that {@link CamelRoute#hashCode()} generates the same hash code for equivalent objects
     */
    @Test
    public void testHashCodeForEqualObjects() {
        CamelRoute otherRoute = new CamelRoute();
        otherRoute.setRouteId(camelRoute.getRouteId());
        Assertions.assertEquals(camelRoute.hashCode(), otherRoute.hashCode());
    }

    /**
     * Validates that {@link CamelRoute#hashCode()} generates the same hash code for non-equivalent objects
     */
    @Test
    public void testHashCodeForNonEqualObjects() {
        CamelRoute otherRoute = new CamelRoute();
        otherRoute.setRouteId("otherRoute");
        Assertions.assertNotEquals(camelRoute.hashCode(), otherRoute.hashCode());
    }

    /**
     * Tests {@link CamelRoute#toString()} with a single producer
     */
    @Test
    public void testToString() {
        String expectedRoute = "from(ftp://myftp.com:22/dropbox)\n"
                .concat(".routeId(myRoute)\n")
                .concat(".to(file://home/serviceuser/documents?fileExist=FAIL)\n");
        Assertions.assertEquals(expectedRoute, camelRoute.toString());
    }


    /**
     * Tests {@link CamelRoute#toString()} with multiple producers
     */
    @Test
    public void testToStringMultipleProducers() {
        String expectedRoute = "from(ftp://myftp.com:22/dropbox)\n"
                .concat(".routeId(myRoute)\n")
                .concat(".multicast()\n")
                .concat(".to(file://home/serviceuser/documents?fileExist=FAIL,file://home/otheruser)\n");

        CamelEndpoint producer = new CamelEndpoint();
        producer.setScheme("file");
        producer.setContextPath("home/otheruser");
        camelRoute.addProducer(producer);

        Assertions.assertEquals(expectedRoute, camelRoute.toString());
    }
}
