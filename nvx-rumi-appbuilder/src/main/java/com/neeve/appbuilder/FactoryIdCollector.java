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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

class FactoryIdCollector {
    private static final String MODELS_DIR = "src/main/models";
    private static final String MODEL_NAMESPACE = "http://www.neeveresearch.com/schema/x-adml";
    private static final int MAX_FACTORY_ID = Short.MAX_VALUE;

    /**
     * Collects available factory IDs not currently used in the app.
     *
     * @param appRoot Path to the Rumi app root
     *  
     * @param minAvailable Minimum number of factory IDs to return
     *  
     * @return A sorted list of available factory IDs
     *  
     * @throws IOException If parsing or traversal fails
     */
    static List<Integer> collectAvailableFactoryIds(Path appRoot, int minAvailable) throws IOException {
        // prepare set of used ids
        Set<Integer> usedIds = new HashSet<>();
        Files.walk(appRoot)
            .filter(path -> path.toString().endsWith(".xml"))
            .filter(path -> path.toString().contains(MODELS_DIR))
            .forEach(path -> {
                try {
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    dbFactory.setNamespaceAware(true);
                    DocumentBuilder builder = dbFactory.newDocumentBuilder();
                    Document doc = builder.parse(path.toFile());

                    Element root = doc.getDocumentElement();
                    if (!MODEL_NAMESPACE.equals(root.getNamespaceURI())) return;

                    NodeList factories = doc.getElementsByTagNameNS(MODEL_NAMESPACE, "factory");
                    for (int i = 0; i < factories.getLength(); i++) {
                        Element factory = (Element) factories.item(i);
                        String id = factory.getAttribute("id");
                        if (id != null && !id.isBlank()) {
                            usedIds.add(Integer.parseInt(id));
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Skipping invalid model file: " + path + " — " + e.getMessage());
                }
            });

        // prepare list of available IDs (all gaps and, optionally, maxUsed +... depending on how many additional we need)
        int maxUsed = usedIds.stream().max(Integer::compareTo).orElse(0);
        List<Integer> available = new ArrayList<>();
        for (int i = 1; i < maxUsed; i++) {
            if (!usedIds.contains(i)) {
                available.add(i);
            }
        }
        int nextId = maxUsed + 1;
        while (available.size() < minAvailable) {
            if (nextId > MAX_FACTORY_ID) {
                throw new IllegalStateException("Exceeded maximum allowable factory-id (" + MAX_FACTORY_ID + ")");
            }
            available.add(nextId++);
        }
        Collections.sort(available);

        // done
        return available;
    }
}

