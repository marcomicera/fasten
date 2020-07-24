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

package eu.fasten.analyzer.metadataplugin;

import eu.fasten.server.connectors.PostgresConnector;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.SQLException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(name = "MetadataPlugin")
public class Main implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @CommandLine.Option(names = {"-f", "--file"},
            paramLabel = "JSON",
            description = "Path to JSON file which contains the callgraph")
    String jsonFile;

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
        var metadataPlugin = new MetadataDatabasePlugin.MetadataDBExtension();
        try {
            metadataPlugin.setDBConnection(PostgresConnector.getDSLContext(dbUrl, dbUser));
        } catch (IllegalArgumentException | SQLException e) {
            logger.error("Could not connect to the database", e);
            return;
        }
        final FileReader reader;
        try {
            reader = new FileReader(jsonFile);
        } catch (FileNotFoundException e) {
            logger.error("Could not find the JSON file at " + jsonFile, e);
            return;
        }
        final JSONObject jsonCallgraph = new JSONObject(new JSONTokener(reader));
        metadataPlugin.consume(jsonCallgraph.toString());
        metadataPlugin.produce().ifPresent(System.out::println);
    }
}
