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

package eu.fasten.analyzer.pomanalyzer.pom;

import eu.fasten.analyzer.pomanalyzer.pom.data.Dependency;
import eu.fasten.analyzer.pomanalyzer.pom.data.DependencyData;
import eu.fasten.analyzer.pomanalyzer.pom.data.DependencyManagement;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataExtractor {

    private List<String> mavenRepos;
    private static final Logger logger = LoggerFactory.getLogger(DataExtractor.class);

    private String mavenCoordinate = null;
    private String pomContents = null;

    public DataExtractor() {
        var repoHost = System.getenv("MVN_REPO") != null
                ? System.getenv("MVN_REPO") : "https://repo.maven.apache.org/maven2/";
        this.mavenRepos = Collections.singletonList(repoHost);
    }

    /**
     * Extracts repository URL from POM of certain Maven coordinate.
     *
     * @param groupId    groupId of the coordinate
     * @param artifactId artifactId of the coordinate
     * @param version    version of the coordinate
     * @return Extracted repository URL as String
     */
    public String extractRepoUrl(String groupId, String artifactId, String version) {
        String repoUrl = null;
        try {
            var scm = extractScm(groupId, artifactId, version);
            if (scm != null) {
                var url = scm.selectSingleNode("./*[local-name()='url']");
                repoUrl = url.getText();
            }
        } catch (DocumentException e) {
            logger.error("Error parsing POM file for: "
                    + groupId + ":" + artifactId + ":" + version);
        } catch (FileNotFoundException e) {
            logger.error("Error downloading POM file for: "
                    + groupId + ":" + artifactId + ":" + version);
        }
        return repoUrl;
    }

    /**
     * Extracts commit tag from POM of certain Maven coordinate.
     *
     * @param groupId    groupId of the coordinate
     * @param artifactId artifactId of the coordinate
     * @param version    version of the coordinate
     * @return Extracted commit tag representing certain version in repository
     */
    public String extractCommitTag(String groupId, String artifactId, String version) {
        String commitTag = null;
        try {
            var scm = extractScm(groupId, artifactId, version);
            if (scm != null) {
                var tag = scm.selectSingleNode("./*[local-name()='tag']");
                commitTag = (tag != null) ? tag.getText() : null;
            }
        } catch (DocumentException e) {
            logger.error("Error parsing POM file for: "
                    + groupId + ":" + artifactId + ":" + version);
        } catch (FileNotFoundException e) {
            logger.error("Error downloading POM file for: "
                    + groupId + ":" + artifactId + ":" + version);
        }
        return commitTag;
    }

    private Node extractScm(String groupId, String artifactId, String version) throws FileNotFoundException, DocumentException {
        ByteArrayInputStream pomByteStream;
        if ((groupId + ":" + artifactId + ":" + version).equals(this.mavenCoordinate)) {
            pomByteStream = new ByteArrayInputStream(this.pomContents.getBytes());
        } else {
            pomByteStream = new ByteArrayInputStream(
                    this.downloadPom(artifactId, groupId, version)
                            .orElseThrow(RuntimeException::new).getBytes());
        }
        var pom = new SAXReader().read(pomByteStream);
        return pom.getRootElement().selectSingleNode("./*[local-name()='scm']");
    }

    /**
     * Extracts dependency information (dependencyManagement and list of dependencies)
     * from certain Maven coordinate.
     *
     * @param groupId    groupId of the coordinate
     * @param artifactId artifactId of the coordinate
     * @param version    version of the coordinate
     * @return Extracted dependency information as DependencyData
     */
    public DependencyData extractDependencyData(String groupId, String artifactId, String version) {
        DependencyData dependencyData = new DependencyData(null, new ArrayList<>());
        try {
            ByteArrayInputStream pomByteStream;
            if ((groupId + ":" + artifactId + ":" + version).equals(this.mavenCoordinate)) {
                pomByteStream = new ByteArrayInputStream(this.pomContents.getBytes());
            } else {
                pomByteStream = new ByteArrayInputStream(
                        this.downloadPom(artifactId, groupId, version)
                                .orElseThrow(RuntimeException::new).getBytes());
            }
            var pom = new SAXReader().read(pomByteStream);
            var versionResolutionData = extractDependencyResolutionMetadata(pom.getRootElement());
            var properties = versionResolutionData.getLeft();
            var parentDependencyManagements = versionResolutionData.getRight();
            for (int i = 0; i < parentDependencyManagements.size(); i++) {
                var depManagement = parentDependencyManagements.get(i);
                var resolvedDependencies = resolveDependencyVersions(depManagement.dependencies,
                        properties, new ArrayList<>());
                parentDependencyManagements.set(i, new DependencyManagement(resolvedDependencies));
            }
            var dependencyManagementNode = pom.getRootElement()
                    .selectSingleNode("./*[local-name()='dependencyManagement']");
            DependencyManagement dependencyManagement;
            if (dependencyManagementNode != null) {
                var dependenciesNode = dependencyManagementNode
                        .selectSingleNode("./*[local-name()='dependencies']");
                var dependencies = extractDependencies(dependenciesNode);
                dependencies = this.resolveDependencyVersions(dependencies, properties,
                        parentDependencyManagements);
                dependencyManagement = new DependencyManagement(dependencies);
            } else {
                dependencyManagement = new DependencyManagement(new ArrayList<>());
            }
            var dependenciesNode = pom.getRootElement()
                    .selectSingleNode("./*[local-name()='dependencies']");
            var dependencies = extractDependencies(dependenciesNode);
            dependencies = this.resolveDependencyVersions(dependencies, properties,
                    parentDependencyManagements);
            dependencyData = new DependencyData(dependencyManagement, dependencies);
        } catch (DocumentException e) {
            logger.error("Error parsing POM file for: "
                    + groupId + ":" + artifactId + ":" + version);
        } catch (FileNotFoundException e) {
            logger.error("Error downloading POM file for: "
                    + groupId + ":" + artifactId + ":" + version);
        }
        return dependencyData;
    }

    private List<Dependency> resolveDependencyVersions(List<Dependency> dependencies,
                                                       Map<String, String> properties,
                                                       List<DependencyManagement> depManagements) {
        var resolvedDependencies = new ArrayList<Dependency>();
        for (var dependency : dependencies) {
            if (dependency.versionConstraints.get(0).lowerBound.equals("*")) {
                for (var depManagement : depManagements) {
                    for (var parentDep : depManagement.dependencies) {
                        if (parentDep.artifactId.equals(dependency.artifactId)
                                && parentDep.groupId.equals(dependency.groupId)) {
                            resolvedDependencies.add(new Dependency(
                                    dependency.artifactId,
                                    dependency.groupId,
                                    parentDep.versionConstraints,
                                    dependency.exclusions,
                                    dependency.scope,
                                    dependency.optional,
                                    dependency.type,
                                    dependency.classifier
                            ));
                        }
                    }
                }
            } else if (dependency.versionConstraints.get(0).lowerBound.startsWith("$")) {
                var property = dependency.versionConstraints.get(0).lowerBound;
                var version = "";
                for (var entry : properties.entrySet()) {
                    if (entry.getKey().equals(property.substring(2, property.length() - 1))) {
                        version = entry.getValue();
                    }
                }
                resolvedDependencies.add(new Dependency(
                        dependency.artifactId,
                        dependency.groupId,
                        version,
                        dependency.exclusions,
                        dependency.scope,
                        dependency.optional,
                        dependency.type,
                        dependency.classifier
                ));
            } else {
                resolvedDependencies.add(dependency);
            }
        }
        return resolvedDependencies;
    }

    private Pair<Map<String, String>, List<DependencyManagement>> extractDependencyResolutionMetadata(Node pomRoot) {
        Map<String, String> properties = new HashMap<>();
        var dependencyManagements = new ArrayList<DependencyManagement>();
        var propertiesRoot = pomRoot.selectSingleNode("./*[local-name() ='properties']");
        if (propertiesRoot != null) {
            for (final var property : propertiesRoot.selectNodes("*")) {
                properties.put(property.getName(), property.getStringValue());
            }
        }
        var parentNode = pomRoot.selectSingleNode("./*[local-name() ='parent']");
        if (parentNode != null) {
            var parentGroup = parentNode
                    .selectSingleNode("./*[local-name() ='groupId']").getText();
            var parentArtifact = parentNode
                    .selectSingleNode("./*[local-name() ='artifactId']").getText();
            var parentVersion = parentNode
                    .selectSingleNode("./*[local-name() ='version']").getText();
            try {
                var parentPom = new SAXReader().read(new ByteArrayInputStream(
                        this.downloadPom(parentArtifact, parentGroup, parentVersion)
                                .orElseThrow(RuntimeException::new).getBytes())).getRootElement();
                var parentMetadata = this.extractDependencyResolutionMetadata(parentPom);
                var parentProperties = parentMetadata.getLeft();
                for (var entry : parentProperties.entrySet()) {
                    properties.put(entry.getKey(), entry.getValue());
                }
                var dependencyManagementNode = parentPom
                        .selectSingleNode("./*[local-name()='dependencyManagement']");
                DependencyManagement dependencyManagement;
                if (dependencyManagementNode != null) {
                    var dependenciesNode = dependencyManagementNode
                            .selectSingleNode("./*[local-name()='dependencies']");
                    var dependencies = extractDependencies(dependenciesNode);
                    dependencyManagement = new DependencyManagement(dependencies);
                } else {
                    dependencyManagement = new DependencyManagement(new ArrayList<>());
                }
                dependencyManagements.add(dependencyManagement);
                dependencyManagements.addAll(parentMetadata.getRight());
            } catch (DocumentException e) {
                logger.error("Error parsing POM file for: "
                        + parentGroup + ":" + parentArtifact + ":" + parentVersion);
            } catch (FileNotFoundException e) {
                logger.error("Error downloading POM file for: "
                        + parentGroup + ":" + parentArtifact + ":" + parentVersion);
            }
        }
        return new ImmutablePair<>(properties, dependencyManagements);
    }

    private List<Dependency> extractDependencies(Node dependenciesNode) {
        ArrayList<Dependency> dependencies = new ArrayList<>();
        if (dependenciesNode != null) {
            for (var dependencyNode : dependenciesNode
                    .selectNodes("./*[local-name()='dependency']")) {
                var artifactNode = dependencyNode
                        .selectSingleNode("./*[local-name()='artifactId']");
                var groupNode = dependencyNode
                        .selectSingleNode("./*[local-name()='groupId']");
                var versionNode = dependencyNode
                        .selectSingleNode("./*[local-name()='version']");
                var exclusionsNode = dependencyNode
                        .selectSingleNode("./*[local-name()='exclusions']");
                var exclusions = new ArrayList<Dependency.Exclusion>();
                if (exclusionsNode != null) {
                    for (var exclusionNode : exclusionsNode
                            .selectNodes("./*[local-name()='exclusion']")) {
                        var exclusionArtifactNode = exclusionNode
                                .selectSingleNode("./*[local-name()='artifactId']");
                        var exclusionGroupNode = exclusionNode
                                .selectSingleNode("./*[local-name()='groupId']");
                        exclusions.add(new Dependency.Exclusion(
                                exclusionArtifactNode.getText(),
                                exclusionGroupNode.getText()
                        ));
                    }
                }
                var scopeNode = dependencyNode
                        .selectSingleNode("./*[local-name()='scope']");
                var optionalNode = dependencyNode
                        .selectSingleNode("./*[local-name()='optional']");
                var typeNode = dependencyNode
                        .selectSingleNode("./*[local-name()='type']");
                var classifierNode = dependencyNode
                        .selectSingleNode("./*[local-name()='classifier']");
                String version;
                if (versionNode != null) {
                    version = versionNode.getText();
                } else {
                    version = null;
                }
                dependencies.add(new Dependency(
                        artifactNode.getText(),
                        groupNode.getText(),
                        version,
                        exclusions,
                        (scopeNode != null) ? scopeNode.getText() : "",
                        (optionalNode != null) && Boolean.parseBoolean(
                                optionalNode.getText()
                        ),
                        (typeNode != null) ? typeNode.getText() : "",
                        (classifierNode != null) ? classifierNode.getText() : ""
                ));
            }
        }
        return dependencies;
    }

    private Optional<String> downloadPom(String artifactId, String groupId, String version)
            throws FileNotFoundException {
        for (var repo : this.getMavenRepos()) {
            var pomUrl = this.getPomUrl(artifactId, groupId, version, repo);
            var pom = httpGetToFile(pomUrl).flatMap(DataExtractor::fileToString);
            if (pom.isPresent()) {
                this.mavenCoordinate = groupId + ":" + artifactId + ":" + version;
                this.pomContents = pom.get();
                return pom;
            }
        }
        return Optional.empty();
    }

    public List<String> getMavenRepos() {
        return mavenRepos;
    }

    public void setMavenRepos(List<String> mavenRepos) {
        this.mavenRepos = mavenRepos;
    }

    private String getPomUrl(String artifactId, String groupId, String version, String repo) {
        return repo + groupId.replace('.', '/') + "/" + artifactId + "/" + version
                + "/" + artifactId + "-" + version + ".pom";
    }

    /**
     * Utility function that stores the contents of GET request to a temporary file.
     */
    private static Optional<File> httpGetToFile(String url) throws FileNotFoundException {
        logger.debug("HTTP GET: " + url);
        try {
            final var tempFile = Files.createTempFile("fasten", ".pom");
            final InputStream in = new URL(url).openStream();
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            in.close();
            return Optional.of(new File(tempFile.toAbsolutePath().toString()));
        } catch (FileNotFoundException e) {
            logger.error("Could not find URL: " + url);
            throw e;
        } catch (Exception e) {
            logger.error("Error retrieving URL: " + url);
            return Optional.empty();
        }
    }

    /**
     * Utility function that reads the contents of a file to a String.
     */
    private static Optional<String> fileToString(final File f) {
        logger.trace("Loading file as string: " + f.toString());
        try {
            final var fr = new BufferedReader(new FileReader(f));
            final StringBuilder result = new StringBuilder();
            String line;
            while ((line = fr.readLine()) != null) {
                result.append(line);
            }
            fr.close();
            return Optional.of(result.toString());

        } catch (IOException e) {
            logger.error("Cannot read from file: " + f.toString(), e);
            return Optional.empty();
        }
    }
}
