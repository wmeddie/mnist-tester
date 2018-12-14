package io.skymind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AppTest {
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    @Test
    public void testEndpointLoginUrlExtraction() {
        String endpoint = "http://localhost:9008/endpoints/bar/models/foo/default";

        SkilClient skilClient = new SkilClient("admin", "admin");
        String hostAndPort = skilClient.extractHostAndPort(endpoint);

        assertEquals("http://localhost:9008", hostAndPort);
    }

    @Test
    public void testTokenExtraction() {
        String token = "{\"token\": \"abc123\"}";
        SkilClient skilClient = new SkilClient("admin", "admin");
        String extracted = skilClient.extractToken(token);

        assertEquals("abc123", extracted);
    }

    @Test
    public void testArrayToStringIsJsony() {
        String s = Arrays.toString(new float[]{1.0f, 2.0f, 3.0f});


        assertEquals("[1.0, 2.0, 3.0]", s);
    }

    @Test
    public void testResultsFromJson() {
        String json = "{\"results\":[1, 2, 3]}";
        SkilClient skilClient = new SkilClient("admin", "admin");

        List<Integer> integers = skilClient.extractResults(json);
        List<Integer> expected = Arrays.asList(1, 2, 3);

        assertEquals(expected, integers);

        json = "{\"results\":[1]}";
        integers = skilClient.extractResults(json);
        expected = Collections.singletonList(1);

        assertEquals(expected, integers);
    }

    @Test
    public void testProbabilitiesJson() {
        String json = "{\"results\":[1, 2, 3],\"probabilities\":[1.0]}";
        SkilClient skilClient = new SkilClient("admin", "admin");

        List<Float> integers = skilClient.extractProbs(json);
        List<Float> expected = Collections.singletonList(1.0f);

        assertEquals(expected, integers);
    }
}
