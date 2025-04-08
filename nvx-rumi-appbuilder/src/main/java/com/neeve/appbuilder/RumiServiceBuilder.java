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

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RumiServiceBuilder {
    public enum ServiceType {
        DRIVER("driver"),
        CONNECTOR("connector"),
        PROCESSOR("processor");

        private final String name;

        ServiceType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static ServiceType fromString(String value) {
            for (ServiceType type : ServiceType.values()) {
                if (type.name.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unsupported service type: " + value);
        }
    }

    public enum ServiceHAModel {
        STATE_REPLICATION("sr"),
        EVENT_SOURCING("es");

        private final String name;

        ServiceHAModel(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static ServiceHAModel fromString(String value) {
            for (ServiceHAModel model : ServiceHAModel.values()) {
                if (model.name.equalsIgnoreCase(value)) {
                    return model;
                }
            }
            throw new IllegalArgumentException("Unsupported service HA model: " + value);
        }
    }

    public static class ServiceParams {
        private final String serviceName;
        private final ServiceType serviceType;
        private final ServiceHAModel serviceHAModel;
        private final RumiApplicationBuilder.AppParams appParams;
        private final Map<String, String> tokenMap;

        public ServiceParams(String serviceName,
                             ServiceType serviceType,
                             ServiceHAModel serviceHAModel,
                             RumiApplicationBuilder.AppParams appParams) {
            if (serviceName == null || serviceName.isEmpty()) {
                throw new IllegalArgumentException("Service name cannot be null or empty");
            }
            if (appParams == null) {
                throw new IllegalArgumentException("AppParams cannot be null");
            }
            this.serviceName = serviceName;
            this.serviceType = serviceType != null ? serviceType : ServiceType.PROCESSOR;
            this.serviceHAModel = serviceHAModel;
            if (this.serviceType == ServiceType.PROCESSOR && this.serviceHAModel == null) {
                throw new IllegalArgumentException("service HA model must be specified for the '" + this.serviceType.getName() + "' service type.");
            }
            this.appParams = appParams;
            this.tokenMap = toTokenMap();
        }

        private Map<String, String> toTokenMap() {
            Map<String, String> map = new HashMap<>(appParams.getTokenMap());
            String kebabCase = TokenUtils.toKebabCase(serviceName);
            String slashCase = TokenUtils.toSlashCase(kebabCase);
            String dottedCase = TokenUtils.toPackagePath(kebabCase);
            String parentArtifactId = map.get(TokenUtils.toToken("ParentArtifactId"));
            String serviceArtifactId = parentArtifactId + "-" + kebabCase;
            map.put(TokenUtils.toToken("ServiceDisplayName"), TokenUtils.forDisplay(serviceName));
            map.put(TokenUtils.toToken("ServiceTokenName"), kebabCase);     // e.g., "order-processor"
            map.put(TokenUtils.toToken("ServiceName"), map.get(TokenUtils.toToken("AppTokenName")) + "-" + kebabCase);     // e.g., "order-processor"
            map.put(TokenUtils.toToken("ServicePackageName"), dottedCase);  // e.g., "order.processor"
            map.put(TokenUtils.toToken("ServicePackagePath"), slashCase);      // e.g., "order/processor"
            map.put(TokenUtils.toToken("ServiceType"), serviceType.getName());
            map.put(TokenUtils.toToken("ServiceHAModel"), serviceHAModel == null || serviceHAModel == ServiceHAModel.STATE_REPLICATION ? "StateReplication" : "EventSourcing");
            map.put(TokenUtils.toToken("ServiceArtifactId"), serviceArtifactId);
            return map;
        }

        public String getServiceName() {
            return serviceName;
        }

        public RumiApplicationBuilder.AppParams getAppParams() {
            return appParams;
        }

        public ServiceType getServiceType() {
            return serviceType;
        }

        public ServiceHAModel getServiceHAModel() {
            return serviceHAModel;
        }

        public Map<String, String> getTokenMap() {
            return tokenMap;
        }
    }

    private void updateParentPom(Path appRoot, ServiceParams params) throws IOException {
        Path pomPath = appRoot.resolve("pom.xml");
        List<String> lines = Files.readAllLines(pomPath);
        String serviceModuleLine = "        <module>" + params.getTokenMap().get(TokenUtils.toToken("ServiceArtifactId")) + "</module>";
        String systemModuleLine = "<module>" + params.getTokenMap().get(TokenUtils.toToken("SystemArtifactId")) + "</module>";

        // skip if it's already present
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equals(serviceModuleLine.trim())) {
                return;
            }
        }

        // find the line to insert before
        int insertIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equals(systemModuleLine.trim())) {
                insertIndex = i;
                break;
            }
        }

        // insert
        if (insertIndex != -1) {
            lines.add(insertIndex, serviceModuleLine);
            Files.write(pomPath, lines);
        } 
        else {
            throw new IOException("Could not find system module in pom.xml");
        }
    }

    public void createService(ServiceParams params) throws IOException {
        // validate
        if (params == null) {
            throw new IllegalArgumentException("params cannot be null");
        }

        // get params
        RumiApplicationBuilder.AppParams appParams = params.getAppParams();
        Path appRoot = Paths.get(appParams.getAppRoot()).toAbsolutePath();
        String serviceTypeName = params.getServiceType().getName();
        String buildToolName = appParams.getBuildTool().getName();

        // extract template to a temp directory
        Path templateDir;
        try {
            String templatePath = String.format("templates/%s/service/%s", buildToolName, serviceTypeName);
            if (params.getServiceHAModel() != null) {
                templatePath = templatePath + "/" + params.getServiceHAModel().getName();
            }
            templateDir = extractTemplateDirectory(templatePath);
        }
        catch (IOException e) {
            throw new IOException("Failed to extract template for build tool '" + buildToolName + "' and service type '" + serviceTypeName + "'", e);
        }

        // generate the service skeleton
        TemplateProcessor.applyTemplate(templateDir, appRoot, params.getTokenMap());

        // update the parent pom
        updateParentPom(appRoot, params);
    }

    private Path extractTemplateDirectory(String templatePath) throws IOException {
        Path tempDir = Files.createTempDirectory("rumi-service-template-");
        try (ScanResult scanResult = new ClassGraph().acceptPaths(templatePath).scan()) {
            var resources = scanResult.getAllResources();
            if (resources.isEmpty()) {
                throw new InternalError("Template path not found or is empty: " + templatePath);
            }
            for (Resource resource : resources) {
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
}

