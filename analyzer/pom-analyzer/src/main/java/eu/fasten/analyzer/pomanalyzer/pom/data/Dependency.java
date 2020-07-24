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

package eu.fasten.analyzer.pomanalyzer.pom.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class Dependency {

    public final String artifactId;
    public final String groupId;
    public final List<VersionConstraint> versionConstraints;
    public final List<Exclusion> exclusions;
    public final String scope;
    public final boolean optional;
    public final String type;
    public final String classifier;

    /**
     * Constructor for Dependency object.
     * (From https://maven.apache.org/ref/3.6.3/maven-model/maven.html#class_dependency)
     *
     * @param artifactId         artifactId of dependency Maven coordinate
     * @param groupId            groupId of dependency Maven coordinate
     * @param versionConstraints List of version constraints of the dependency
     * @param exclusions         List of exclusions
     * @param scope              Scope of the dependency
     * @param optional           Is dependency optional
     * @param type               Type of the dependency
     * @param classifier         Classifier for dependency
     */
    public Dependency(final String artifactId, final String groupId,
                      final List<VersionConstraint> versionConstraints,
                      final List<Exclusion> exclusions, final String scope, final boolean optional,
                      final String type, final String classifier) {
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.versionConstraints = versionConstraints;
        this.exclusions = exclusions;
        this.scope = scope;
        this.optional = optional;
        this.type = type;
        this.classifier = classifier;
    }

    public Dependency(final String artifactId, final String groupId, final String version,
                      final List<Exclusion> exclusions, final String scope, final boolean optional,
                      final String type, final String classifier) {
        this(artifactId, groupId, VersionConstraint.resolveMultipleVersionConstraints(version),
                exclusions, scope, optional, type, classifier);
    }

    public Dependency(final String artifactId, final String groupId, final String version) {
        this(artifactId, groupId, version, new ArrayList<>(), "", false, "", "");
    }

    /**
     * Turns list of version constraints into string array of specifications.
     *
     * @return String array representation of the dependency version constraints
     */
    public String[] getVersionConstraints() {
        var constraints = new String[this.versionConstraints.size()];
        for (int i = 0; i < versionConstraints.size(); i++) {
            constraints[i] = versionConstraints.get(i).toString();
        }
        return constraints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Dependency that = (Dependency) o;
        if (optional != that.optional) {
            return false;
        }
        if (!artifactId.equals(that.artifactId)) {
            return false;
        }
        if (!groupId.equals(that.groupId)) {
            return false;
        }
        if (!Objects.equals(versionConstraints, that.versionConstraints)) {
            return false;
        }
        if (!Objects.equals(exclusions, that.exclusions)) {
            return false;
        }
        if (!Objects.equals(scope, that.scope)) {
            return false;
        }
        if (!Objects.equals(type, that.type)) {
            return false;
        }
        return Objects.equals(classifier, that.classifier);
    }

    /**
     * Converts Dependency object into JSON.
     *
     * @return JSONObject representation of dependency
     */
    public JSONObject toJSON() {
        final var json = new JSONObject();
        json.put("artifactId", this.artifactId);
        json.put("groupId", this.groupId);
        final var constraintsJson = new JSONArray();
        for (var constraint : this.versionConstraints) {
            constraintsJson.put(constraint.toJSON());
        }
        json.put("versionConstraints", constraintsJson);
        final var exclusionsJson = new JSONArray();
        for (var exclusion : this.exclusions) {
            exclusionsJson.put(exclusion.toJSON());
        }
        json.put("exclusions", exclusionsJson);
        json.put("scope", this.scope);
        json.put("optional", this.optional);
        json.put("type", this.type);
        json.put("classifier", this.classifier);
        return json;
    }

    /**
     * Creates a Dependency object from JSON.
     *
     * @param json JSONObject representation of dependency
     * @return Dependency object
     */
    public static Dependency fromJSON(JSONObject json) {
        var artifactId = json.getString("artifactId");
        var groupId = json.getString("groupId");
        var versionConstraints = new ArrayList<VersionConstraint>();
        if (json.has("versionConstraints")) {
            var constraintsJson = json.getJSONArray("versionConstraints");
            for (var i = 0; i < constraintsJson.length(); i++) {
                versionConstraints.add(VersionConstraint.fromJSON(constraintsJson.getJSONObject(i)));
            }
        }
        var exclusions = new ArrayList<Exclusion>();
        if (json.has("exclusions")) {
            var exclusionsJson = json.getJSONArray("exclusions");
            for (var i = 0; i < exclusionsJson.length(); i++) {
                exclusions.add(Exclusion.fromJSON(exclusionsJson.getJSONObject(i)));
            }
        }
        var scope = json.optString("scope");
        var optional = json.optBoolean("optional", false);
        var type = json.optString("type");
        var classifier = json.optString("classifier");
        return new Dependency(artifactId, groupId, versionConstraints, exclusions, scope,
                optional, type, classifier);
    }


    public static class VersionConstraint {

        public final String lowerBound;
        public final boolean isLowerHardRequirement;
        public final String upperBound;
        public final boolean isUpperHardRequirement;

        /**
         * Constructor for VersionConstraint object.
         *
         * @param lowerBound             Lower bound on the version range
         * @param isLowerHardRequirement Is lower bound a hard requirement
         * @param upperBound             Upper bound on the version range
         * @param isUpperHardRequirement Is upper bound a hard requirement
         */
        public VersionConstraint(final String lowerBound, final boolean isLowerHardRequirement,
                                 final String upperBound, final boolean isUpperHardRequirement) {
            this.lowerBound = lowerBound;
            this.isLowerHardRequirement = isLowerHardRequirement;
            this.upperBound = upperBound;
            this.isUpperHardRequirement = isUpperHardRequirement;
        }

        /**
         * Constructs a VersionConstraint object from specification.
         * (From https://maven.apache.org/pom.html#Dependency_Version_Requirement_Specification)
         *
         * @param spec String specification of version constraint
         */
        public VersionConstraint(final String spec) {
            this.isLowerHardRequirement = spec.startsWith("[");
            this.isUpperHardRequirement = spec.endsWith("]");
            if (!spec.contains(",")) {
                var version = spec;
                if (version.startsWith("[") && version.endsWith("]")) {
                    version = version.substring(1, spec.length() - 1);
                }
                this.upperBound = version;
                this.lowerBound = version;

            } else {
                final var versionSplit = spec.substring(1, spec.length() - 1).split(",");
                this.lowerBound = versionSplit[0];
                this.upperBound = (versionSplit.length > 1) ? versionSplit[1] : "";
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            VersionConstraint that = (VersionConstraint) o;
            if (isLowerHardRequirement != that.isLowerHardRequirement) {
                return false;
            }
            if (isUpperHardRequirement != that.isUpperHardRequirement) {
                return false;
            }
            if (!lowerBound.equals(that.lowerBound)) {
                return false;
            }
            return upperBound.equals(that.upperBound);
        }

        /**
         * Turns version constraint back into string specification.
         *
         * @return String representation of the version constraint
         */
        @Override
        public String toString() {
            var constraintBuilder = new StringBuilder();
            if (this.lowerBound.equals(this.upperBound)) {
                if (this.isLowerHardRequirement && this.isUpperHardRequirement) {
                    constraintBuilder.append("[");
                    constraintBuilder.append(this.lowerBound);
                    constraintBuilder.append("]");
                } else {
                    constraintBuilder.append(this.lowerBound);
                }
            } else {
                if (this.isLowerHardRequirement) {
                    constraintBuilder.append("[");
                } else {
                    constraintBuilder.append("(");
                }
                constraintBuilder.append(this.lowerBound);
                constraintBuilder.append(",");
                constraintBuilder.append(this.upperBound);
                if (this.isUpperHardRequirement) {
                    constraintBuilder.append("]");
                } else {
                    constraintBuilder.append(")");
                }
            }
            return constraintBuilder.toString();
        }

        /**
         * Converts VersionConstraint object into JSON.
         *
         * @return JSONObject representation of version constraint
         */
        public JSONObject toJSON() {
            var json = new JSONObject();
            json.put("lowerBound", this.lowerBound);
            json.put("isLowerHardRequirement", this.isLowerHardRequirement);
            json.put("upperBound", this.upperBound);
            json.put("isUpperHardRequirement", this.isUpperHardRequirement);
            return json;
        }

        /**
         * Creates a VersionConstraint object from JSON.
         *
         * @param json JSONObject representation of version constraint
         * @return VersionConstraint object
         */
        public static VersionConstraint fromJSON(JSONObject json) {
            var lowerBound = json.getString("lowerBound");
            var upperBound = json.getString("upperBound");
            var isLowerHardRequirement = json.getBoolean("isLowerHardRequirement");
            var isUpperHardRequirement = json.getBoolean("isUpperHardRequirement");
            return new VersionConstraint(lowerBound, isLowerHardRequirement,
                    upperBound, isUpperHardRequirement);
        }

        /**
         * Creates full list of version constraints from specification.
         * (From https://maven.apache.org/pom.html#Dependency_Version_Requirement_Specification)
         *
         * @param spec String specification of version constraints
         * @return List of Version Constraints
         */
        public static List<VersionConstraint> resolveMultipleVersionConstraints(String spec) {
            if (spec == null) {
                return List.of(new VersionConstraint("*"));
            }
            if (spec.startsWith("$")) {
                return List.of(new VersionConstraint(spec));
            }
            final var versionRangesCount = (StringUtils.countMatches(spec, ",") + 1) / 2;
            var versionConstraints = new ArrayList<VersionConstraint>(versionRangesCount);
            int count = 0;
            for (int i = 0; i < spec.length(); i++) {
                if (spec.charAt(i) == ',') {
                    count++;
                    if (count % 2 == 0) {
                        var specBuilder = new StringBuilder(spec);
                        specBuilder.setCharAt(i, ';');
                        spec = specBuilder.toString();
                    }
                }
            }
            var versionRanges = spec.split(";");
            for (var versionRange : versionRanges) {
                versionConstraints.add(new VersionConstraint(versionRange));
            }
            return versionConstraints;
        }
    }


    public static class Exclusion {

        public final String artifactId;
        public final String groupId;

        /**
         * Constructor for Exclusion object.
         * Exclusion defines a dependency which must be excluded from transitive dependencies.
         *
         * @param artifactId artifactId of excluded Maven coordinate
         * @param groupId    groupId of excluded Maven coordinate
         */
        public Exclusion(final String artifactId, final String groupId) {
            this.artifactId = artifactId;
            this.groupId = groupId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Exclusion exclusion = (Exclusion) o;
            if (!artifactId.equals(exclusion.artifactId)) {
                return false;
            }
            return groupId.equals(exclusion.groupId);
        }

        /**
         * Converts Exclusion object into JSON.
         *
         * @return JSONObject representation of exclusion
         */
        public JSONObject toJSON() {
            final var json = new JSONObject();
            json.put("artifactId", this.artifactId);
            json.put("groupId", this.groupId);
            return json;
        }

        /**
         * Creates a Exclusion object from JSON.
         *
         * @param json JSONObject representation of exclusion
         * @return Exclusion object
         */
        public static Exclusion fromJSON(JSONObject json) {
            var artifactId = json.getString("artifactId");
            var groupId = json.getString("groupId");
            return new Exclusion(artifactId, groupId);
        }
    }
}