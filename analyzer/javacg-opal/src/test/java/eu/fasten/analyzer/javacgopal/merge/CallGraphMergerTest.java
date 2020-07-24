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

package eu.fasten.analyzer.javacgopal.merge;

import static org.junit.Assert.assertEquals;

import eu.fasten.core.data.RevisionCallGraph;
import eu.fasten.analyzer.javacgopal.data.PartialCallGraph;
import eu.fasten.core.data.FastenJavaURI;
import java.io.File;
import java.util.Arrays;
import org.junit.BeforeClass;
import org.junit.Test;

public class CallGraphMergerTest {

    static RevisionCallGraph artifact;
    static RevisionCallGraph dependency;

    @BeforeClass
    public static void generateCallGraph() {

        /*
         * Importer is a java8 compiled bytecode of:
         *<pre>
         * package name.space;
         *
         * import depen.dency.Imported;
         *
         * public class Importer {
         *     public Importer() {
         *     }
         *
         *     public static void sourceMethod() {
         *         Imported.targetMethod();
         *     }
         * }
         * </pre>
         */
        var importerGraph = new PartialCallGraph(new File(
            Thread.currentThread().getContextClassLoader().getResource("Importer.class")
                .getFile()));

        artifact = RevisionCallGraph.extendedBuilder()
            .forge("mvn")
            .product("ImporterGroup.ImporterArtifact")
            .version("1.0.0")
            .cgGenerator(importerGraph.getGENERATOR())
            .timestamp(1574072773)
            .graph(importerGraph.getGraph())
            .classHierarchy(importerGraph.getClassHierarchy())
            .build();

        /*
         * Imported is a java8 compiled bytecode of:
         *<pre>
         * package depen.dency;
         *
         * public class Imported {
         *     public Imported() {
         *     }
         *
         *     public static void targetMethod() {
         *     }
         * }
         * </pre>
         */
        var importedGraph = new PartialCallGraph(new File(
            Thread.currentThread().getContextClassLoader().getResource("Imported.class")
                .getFile()));

        dependency = RevisionCallGraph.extendedBuilder()
            .forge("mvn")
            .product("ImportedGroup.ImportedArtifact")
            .version("1.7.29")
            .cgGenerator(importedGraph.getGENERATOR())
            .timestamp(1574072773)
            .graph(importedGraph.getGraph())
            .classHierarchy(importedGraph.getClassHierarchy())
            .build();
    }

    @Test
    public void testResolve() {

    }

    @Test
    public void testMergeCallGraphs() {

        assertEquals(new FastenJavaURI("///depen.dency/Imported.targetMethod()%2Fjava"
                + ".lang%2FVoidType"),
            artifact.getGraph().getExternalCalls().keySet().stream()
                .filter(i -> i.getRight().toString().contains(
                    "targetMethod")).findFirst().orElseThrow().getRight());

        assertEquals(new FastenJavaURI("//ImportedGroup.ImportedArtifact$1.7.29/depen"
                + ".dency/Imported"
                + ".targetMethod()%2Fjava.lang%2FVoidType"),
            CallGraphMerger.mergeCallGraph(artifact, Arrays.asList(dependency),"CHA")
                .getGraph().getExternalCalls().keySet().stream()
                .filter(i -> i.getRight().toString().contains(
                    "targetMethod")).findFirst().orElseThrow().getRight());

        assertEquals(new FastenJavaURI("//ImportedGroup.ImportedArtifact$1.7.29/depen"
                + ".dency/Imported"
                + ".targetMethod()%2Fjava.lang%2FVoidType"),
            CallGraphMerger.mergeCallGraph(artifact, Arrays.asList(dependency),"RA")
                .getGraph().getExternalCalls().keySet().stream()
                .filter(i -> i.getRight().toString().contains(
                    "targetMethod")).findFirst().orElseThrow().getRight());

    }
}