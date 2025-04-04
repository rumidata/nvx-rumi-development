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
    public void createApplication(RumiAppParams params, Path targetDir) throws IOException {
        Path templateDir;
        try {
            templateDir = Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource("templates/app")).toURI());
        }
        catch (URISyntaxException | NullPointerException e) {
            throw new IOException("Template directory not found in resources", e);
        }
        TemplateProcessor.applyTemplate(templateDir, targetDir, params.toTokenMap());
    }
}
