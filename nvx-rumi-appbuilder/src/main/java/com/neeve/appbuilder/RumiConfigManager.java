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

import com.google.gson.Gson;
import java.io.*;
import java.nio.file.*;

public class RumiConfigManager {
    private static final String CONFIG_FILE_NAME = ".rumi";
    private static final Gson gson = new Gson();

    public static void writeAppParams(Path appRoot, RumiApplicationBuilder.AppParams params) throws IOException {
        Path configFile = appRoot.resolve(CONFIG_FILE_NAME);
        try (BufferedWriter writer = Files.newBufferedWriter(configFile)) {
            gson.toJson(params, writer);
        }
    }

    public static RumiApplicationBuilder.AppParams readAppParams(Path appRoot) throws IOException {
        Path configFile = appRoot.resolve(CONFIG_FILE_NAME);
        if (!Files.exists(configFile)) {
            throw new FileNotFoundException("No .rumi config file found at " + configFile);
        }
        try (BufferedReader reader = Files.newBufferedReader(configFile)) {
            return gson.fromJson(reader, RumiApplicationBuilder.AppParams.class);
        }
    }
}

