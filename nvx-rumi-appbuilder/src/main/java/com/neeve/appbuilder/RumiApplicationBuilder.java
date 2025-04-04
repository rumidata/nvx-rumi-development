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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

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

    final public void createApplication(RumiAppParams params, Path targetDir, BuildTool buildTool) throws IOException {
        Path templateDir;
        try {
            String templatePath = String.format("templates/%s/app", buildTool.getName());
            templateDir = Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(templatePath)).toURI());
        }
        catch (URISyntaxException | NullPointerException e) {
            throw new IOException("Template directory for build tool '" + buildTool + "' not found in resources", e);
        }
        TemplateProcessor.applyTemplate(templateDir, targetDir, params.toTokenMap());
    }
}
