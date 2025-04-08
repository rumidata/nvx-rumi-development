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
import java.nio.file.*;
import java.util.*;

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
