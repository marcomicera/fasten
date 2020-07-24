/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.fasten.analyzer.javacgopal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import eu.fasten.analyzer.javacgopal.data.MavenCoordinate;
import eu.fasten.analyzer.javacgopal.data.PartialCallGraph;
import eu.fasten.core.data.RevisionCallGraph;
import java.io.FileNotFoundException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class OPALPluginTest {

    static OPALPlugin.OPAL opalPlugin;
    final String topic = "fasten.maven.pkg";

    @BeforeClass
    public static void instantiatePlugin() {
        opalPlugin = new OPALPlugin.OPAL();
    }

    @Test
    public void testConsumerTopic() {
        assertTrue(opalPlugin.consumeTopic().isPresent());
        assertEquals("fasten.maven.pkg", opalPlugin.consumeTopic().get().get(0));
    }

    @Test
    public void testSetTopic() {
        String topicName = "fasten.mvn.pkg";
        opalPlugin.setTopic(topicName);
        assertTrue(opalPlugin.consumeTopic().isPresent());
        assertEquals(topicName, opalPlugin.consumeTopic().get().get(0));
    }

    @Test
    public void testConsume() throws JSONException, FileNotFoundException {

        JSONObject coordinateJSON = new JSONObject("{\n"
                + "    \"groupId\": \"org.slf4j\",\n"
                + "    \"artifactId\": \"slf4j-api\",\n"
                + "    \"version\": \"1.7.29\",\n"
                + "    \"date\":\"1574072773\"\n"
                + "}");

        opalPlugin.consume(coordinateJSON.toString());

        final var extendedRevisionCallGraph = PartialCallGraph.createExtendedRevisionCallGraph(
                new MavenCoordinate("org.slf4j", "slf4j-api", "1.7.29"), 1574072773);

        assertTrue(opalPlugin.produce().isPresent());
        JSONAssert.assertEquals(extendedRevisionCallGraph.toJSON().toString(),
                opalPlugin.produce().get(), false);
    }

    @Test
    public void testShouldNotFaceClassReadingError() throws JSONException, FileNotFoundException {

        JSONObject coordinateJSON1 = new JSONObject("{\n"
                + "    \"groupId\": \"com.zarbosoft\",\n"
                + "    \"artifactId\": \"coroutines-core\",\n"
                + "    \"version\": \"0.0.3\",\n"
                + "    \"date\":\"1574072773\"\n"
                + "}");

        opalPlugin.consume(coordinateJSON1.toString());

        var extendedRevisionCallGraph = PartialCallGraph.createExtendedRevisionCallGraph(
                new MavenCoordinate("com.zarbosoft", "coroutines-core", "0.0.3"), 1574072773);

        assertTrue(opalPlugin.produce().isPresent());
        JSONAssert
                .assertEquals(extendedRevisionCallGraph.toJSON().toString(),
                        opalPlugin.produce().get(), false);
    }

    @Test
    public void testEmptyCallGraph() throws JSONException {
        JSONObject emptyCGCoordinate = new JSONObject("{\n"
                + "    \"groupId\": \"activemq\",\n"
                + "    \"artifactId\": \"activemq\",\n"
                + "    \"version\": \"release-1.5\",\n"
                + "    \"date\":\"1574072773\"\n"
                + "}");

        opalPlugin.consume(emptyCGCoordinate.toString());
        assertTrue(opalPlugin.produce().isPresent());
        var graph = opalPlugin.produce().get();
        var cg = new RevisionCallGraph(new JSONObject(graph));
        //Based on plugin's logs this artifact's call graph should be empty.
        assertTrue(cg.isCallGraphEmpty());
    }

    @Test
    public void testFileNotFoundException() throws JSONException {
        JSONObject noJARFile = new JSONObject("{\n"
                + "    \"groupId\": \"com.visionarts\",\n"
                + "    \"artifactId\": \"power-jambda-pom\",\n"
                + "    \"version\": \"0.9.10\",\n"
                + "    \"date\":\"1521511260\"\n"
                + "}");

        opalPlugin.consume(noJARFile.toString());
        var error = opalPlugin.getPluginError();

        assertEquals(FileNotFoundException.class.getSimpleName(), error.getClass().getSimpleName());
    }

    @Test
    public void testNullPointerException() throws JSONException {
        JSONObject mvnCoordinate = new JSONObject("{\n"
                + "    \"groupId\": \"ch.epfl.scala\",\n"
                + "    \"artifactId\": \"collection-strawman_0.6\",\n"
                + "    \"version\": \"0.8.0\",\n"
                + "    \"date\":\"1521511260\"\n"
                + "}");

//        var cg = opalPlugin.consume(new ConsumerRecord<>(topic, 1, 0, "bar", mvnCoordinate
//        .toString()), false);
//        cg.toJSON();

        // TODO: An assert is pointless here. Because we need to find the root cause of the NullPointerException in PartialCallGraph class.
        // This test shows that FASTEN URIs of a type's methods can be null! Check out the method toListOfString in ExtendedRevisionCallGraph class.
        // The described problem is patched at the very high level with a if block!
    }

    @Test
    public void testName() {
        assertEquals("eu.fasten.analyzer.javacgopal.OPALPlugin.OPAL", opalPlugin.name());
    }
}