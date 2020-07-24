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

package eu.fasten.analyzer.pomanalyzer;

import eu.fasten.server.connectors.PostgresConnector;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.SQLException;
import org.json.JSONObject;
import org.json.JSONTokener;
import picocli.CommandLine;

@CommandLine.Command(name = "POMAnalyzer")
public class Main implements Runnable {

    @CommandLine.Option(names = {"-f", "--file"},
            paramLabel = "JSON",
            description = "Path to JSON file which contains the Maven coordinate")
    String jsonFile;

    @CommandLine.Option(names = {"-a", "--artifactId"},
            paramLabel = "ARTIFACT",
            description = "artifactId of the Maven coordinate")
    String artifact;

    @CommandLine.Option(names = {"-g", "--groupId"},
            paramLabel = "GROUP",
            description = "groupId of the Maven coordinate")
    String group;

    @CommandLine.Option(names = {"-v", "--version"},
            paramLabel = "VERSION",
            description = "version of the Maven coordinate")
    String version;

    @CommandLine.Option(names = {"-d", "--database"},
            paramLabel = "dbURL",
            description = "Database URL for connection",
            defaultValue = "jdbc:postgresql:postgres")
    String dbUrl;

    @CommandLine.Option(names = {"-u", "--user"},
            paramLabel = "dbUser",
            description = "Database user name",
            defaultValue = "postgres")
    String dbUser;

    public static void main(String[] args) {
        final int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        var pomAnalyzer = new POMAnalyzerPlugin.POMAnalyzer();
        try {
            pomAnalyzer.setDBConnection(PostgresConnector.getDSLContext(dbUrl, dbUser));
        } catch (SQLException e) {
            System.err.println("Error connecting to the database:");
            e.printStackTrace(System.err);
            return;
        }
        if (artifact != null && group != null && version != null) {
            var mvnCoordinate = new JSONObject();
            mvnCoordinate.put("artifactId", artifact);
            mvnCoordinate.put("groupId", group);
            mvnCoordinate.put("version", version);
            var record = new JSONObject();
            record.put("payload", mvnCoordinate);
            pomAnalyzer.consume(record.toString());
            pomAnalyzer.produce().ifPresent(System.out::println);
        } else if (jsonFile != null) {
            FileReader reader;
            try {
                reader = new FileReader(jsonFile);
            } catch (FileNotFoundException e) {
                System.err.println("Could not find the JSON file at " + jsonFile);
                return;
            }
            var record = new JSONObject(new JSONTokener(reader));
            pomAnalyzer.consume(record.toString());
            pomAnalyzer.produce().ifPresent(System.out::println);
        } else {
            System.err.println("You need to specify Maven coordinate either by providing its "
                    + "artifactId ('-a'), groupId ('-g') and version ('-v') or by providing path "
                    + "to JSON file that contains that Maven coordinate as payload.");
        }
    }
}
