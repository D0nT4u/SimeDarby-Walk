package com.example.simedarbywalk;

import org.junit.Test;
import static org.junit.Assert.*;

public class DemoRouteTest {
    @Test public void distanceUsesSegmentRatherThanJustVertices() {
        double[][] route = {{0, 0}, {1, 0}};
        assertEquals(20, DemoStore.distance(.5, .02, route), .001);
    }
    @Test public void pointBeyondRouteUsesNearestEndpoint() {
        assertEquals(100, DemoStore.distance(1.1, 0, new double[][]{{0, 0}, {1, 0}}), .001);
    }
    @Test public void simulationFollowsDistanceAndStopsAtDestination() {
        double[][] route = {{0, 0}, {.25, 0}, {.25, .75}};
        assertArrayEquals(new double[]{.25, .25}, DemoStore.position(route, .5), .001);
        assertArrayEquals(new double[]{.25, .75}, DemoStore.position(route, 1.5), .001);
    }
    @Test public void zeroLengthRouteDoesNotProduceInvalidCoordinates() {
        assertArrayEquals(new double[]{.2, .2}, DemoStore.position(new double[][]{{.2, .2}, {.2, .2}}, .5), .001);
    }
}
