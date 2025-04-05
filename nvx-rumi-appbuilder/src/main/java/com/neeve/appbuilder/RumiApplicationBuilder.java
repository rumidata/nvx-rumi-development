/**
 * Copyright 2022 N5 Technologies, Inc
 *
 * This product includes software developed at N5 Technologies, Inc
 * (http://www.n5corp.com/) as well as software licenced to N5 Technologies,
 * Inc under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding
 * copyright ownership.
 *
 * N5 Technologies licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.neeve.appbuilder;

import java.io.InputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;

class TemplateProcessor {
    public static void applyTemplate(Path templateDir, Path targetDir, Map<String, String> tokens) throws IOException {
        Files.walk(templateDir).forEach(source -> {
            try {
                Path relative = templateDir.relativize(source);
                String replacedPath = applyTokens(relative.toString(), tokens);
                Path target = targetDir.resolve(replacedPath);

                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                }
                else {
                    String content = Files.readString(source);
                    String replacedContent = applyTokens(content, tokens);
                    Files.createDirectories(target.getParent());
                    Files.writeString(target, replacedContent);
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static String applyTokens(String input, Map<String, String> tokens) {
        String result = input;
        for (Map.Entry<String, String> entry: tokens.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}

public class RumiApplicationBuilder {
    public enum BuildTool {
        MAVEN("maven"),
        GRADLE("gradle");

        private final String name;

        BuildTool(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static BuildTool fromString(String value) {
            for (BuildTool tool: BuildTool.values()) {
                if (tool.name.equalsIgnoreCase(value)) {
                    return tool;
                }
            }
            throw new IllegalArgumentException("Unsupported build tool: " + value);
        }
    }

    public enum EncodingType {
        QUARK("quark"),
        PROTOBUF("protobuf");

        private final String name;

        EncodingType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static EncodingType fromString(String value) {
            for (EncodingType tool: EncodingType.values()) {
                if (tool.name.equalsIgnoreCase(value)) {
                    return tool;
                }
            }
            throw new IllegalArgumentException("Unsupported encoding type: " + value);
        }
    }

    public enum MessagingProvider {
        SOLACE("solace"),
        ACTIVEMQ("activemq"),
        KAFKA("kafka");

        private final String name;

        MessagingProvider(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static MessagingProvider fromString(String value) {
            for (MessagingProvider provider : MessagingProvider.values()) {
                if (provider.name.equalsIgnoreCase(value)) {
                    return provider;
                }
            }
            throw new IllegalArgumentException("Unsupported messaging provider: " + value);
        }
    }

    public static class AppParams {
        private final String appName;
        private final String packageName;
        private final String groupId;
        private final String artifactPrefix;
        private final String rumiVersion;
        private final String rumiBindingsVersion;
        private final String rumiMgmtVersion;
        private final EncodingType encodingType;
        private final MessagingProvider messagingProvider;
        private final String appTokenName;

        public AppParams(String appName,
                         String packageName,
                         String groupId,
                         String artifactPrefix,
                         String rumiVersion,
                         String rumiBindingsVersion,
                         String rumiMgmtVersion,
                         EncodingType encodingType,
                         MessagingProvider messagingProvider) {
            if (rumiMgmtVersion == null) {
                throw new IllegalArgumentException("Rumi management version cannot be null");
            }
            if (rumiBindingsVersion == null) {
                throw new IllegalArgumentException("Rumi bindings version cannot be null");
            }
            if (rumiVersion == null) {
                throw new IllegalArgumentException("Rumi runtime version cannot be null");
            }
            if (artifactPrefix == null) {
                throw new IllegalArgumentException("artifact prefix  cannot be null");
            }
            if (groupId == null) {
                throw new IllegalArgumentException("group id cannot be null");
            }
            if (packageName == null) {
                throw new IllegalArgumentException("package name cannot be null");
            }
            if (appName == null) {
                throw new IllegalArgumentException("app name cannot be null");
            }
            if (encodingType == null) {
                encodingType = EncodingType.QUARK;
            }
            if (messagingProvider == null) {
                messagingProvider = MessagingProvider.ACTIVEMQ;
            }
            this.appName = appName;
            this.packageName = packageName;
            this.groupId = groupId;
            this.artifactPrefix = artifactPrefix;
            this.rumiVersion = rumiVersion;
            this.rumiBindingsVersion = rumiBindingsVersion;
            this.rumiMgmtVersion = rumiMgmtVersion;
            this.encodingType = encodingType;
            this.messagingProvider = messagingProvider;
            this.appTokenName = appName.toLowerCase().replaceAll("\\s+", "");
        }

        private String getConnectionStringForProvider(MessagingProvider provider) {
            switch (provider) {
                case SOLACE:
                    return "solace://solace.rumi.local:55555";
                case ACTIVEMQ:
                    return "activemq://activemq.rumi.local:61616?wireFormat.maxInactivityDuration=0";
                case KAFKA:
                    return "kafka://kafka.rumi.local:9092";
                default:
                    throw new IllegalStateException("Unhandled messaging provider: " + provider);
            }
        }

        private String getMessagingDependencySnippet(MessagingProvider provider) {
            String indent = "        "; // 8 spaces to match existing <dependency> blocks
            switch (provider) {
                case SOLACE:
                    return indent + "\n" +
                           indent + "<dependency>\n" +
                           indent + "    <groupId>com.neeve</groupId>\n" +
                           indent + "    <artifactId>nvx-rumi-solace</artifactId>\n" +
                           indent + "</dependency>\n";
                case KAFKA:
                    return indent + "\n" +
                           indent + "<dependency>\n" +
                           indent + "    <groupId>com.neeve</groupId>\n" +
                           indent + "    <artifactId>nvx-rumi-kafka</artifactId>\n" +
                           indent + "</dependency>\n";
                case ACTIVEMQ:
                    return ""; // nothing needed
                default:
                    throw new IllegalStateException("Unhandled messaging provider: " + provider);
            }
        }

        public Map<String, String> toTokenMap() {
            Map<String, String> map = new HashMap<>();
            map.put("{{AppDisplayName}}", appName);
            map.put("{{appTokenName}}", appTokenName);
            map.put("{{PackageName}}", packageName);
            map.put("{{PackagePath}}", packageName.replace('.', '/'));
            map.put("{{GroupId}}", groupId);
            map.put("{{ArtifactPrefix}}", artifactPrefix);
            map.put("{{ParentArtifactId}}", artifactPrefix + "-" + appTokenName);
            map.put("{{RoeArtifactId}}", artifactPrefix + "-" + appTokenName + "-roe");
            map.put("{{SystemArtifactId}}", artifactPrefix + "-" + appTokenName + "-system");
            map.put("{{BusName}}", appTokenName);
            map.put("{{RumiVersion}}", rumiVersion);
            map.put("{{RumiBindingsVersion}}", rumiBindingsVersion);
            map.put("{{RumiMgmtVersion}}", rumiMgmtVersion);
            map.put("{{EncodingType}}", encodingType.getName());
            map.put("{{MessagingConnectionString}}", getConnectionStringForProvider(messagingProvider));
            map.put("{{MessagingProviderDependency}}", getMessagingDependencySnippet(messagingProvider));
            return map;
        }
    }

    final private Path extractTemplateDirectory(String templatePath) throws IOException {
        Path tempDir = Files.createTempDirectory("rumi-template-");
        try (ScanResult scanResult = new ClassGraph().acceptPaths(templatePath).scan()) {
            for (Resource resource: scanResult.getAllResources()) {
                String relativePath = resource.getPath().substring(templatePath.length() + 1);
                Path targetPath = tempDir.resolve(relativePath);
                Files.createDirectories(targetPath.getParent());
                try (InputStream in = resource.open()) {
                    Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        return tempDir;
    }

    final public void createApplication(AppParams params, Path targetDir, BuildTool buildTool) throws IOException {
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }
        if (targetDir == null) {
            targetDir = Paths.get("").toAbsolutePath();
        }
        if (buildTool == null) {
            buildTool = BuildTool.MAVEN;
        }
        Path templateDir;
        try {
            String templatePath = String.format("templates/%s/app", buildTool.getName());
            templateDir = extractTemplateDirectory(templatePath);
        }
        catch (IOException e) {
            throw new IOException("Failed to extract template for build tool: " + buildTool, e);
        }
        TemplateProcessor.applyTemplate(templateDir, targetDir, params.toTokenMap());
    }
}
