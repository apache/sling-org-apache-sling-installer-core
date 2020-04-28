/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.installer.api.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ConfigurationSerializerTest {

    @Parameters(name = "Serializer:{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { ConfigurationSerializerFactory.Format.JSON, "config1.json" },
                { ConfigurationSerializerFactory.Format.CONFIG, "config1.config" },
                { ConfigurationSerializerFactory.Format.PROPERTIES, "config1.cfg" },
                { ConfigurationSerializerFactory.Format.PROPERTIES_XML, "config1.xml" }
        });
    }

    private final ConfigurationSerializer serializer;
    private final LineIterator lineIterator;

    public ConfigurationSerializerTest(ConfigurationSerializerFactory.Format format, String resource) throws IOException {
        this.serializer = ConfigurationSerializerFactory.create(format);
        InputStream input = this.getClass().getResourceAsStream(resource);
        lineIterator = IOUtils.lineIterator(input, StandardCharsets.UTF_8);
    }

    @After
    public void tearDown() throws IOException {
        lineIterator.close();
    }

    @Test
    public void testSerializer() throws IOException {
        Dictionary<String, Object> dictionary = new Hashtable<>();

        dictionary.put("String-value", "test");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        serializer.serialize(dictionary, output);
        output.close();

        try (ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray())) {
            LineIterator actualLineIterator = IOUtils.lineIterator(input, StandardCharsets.UTF_8);
            // compare line by line
            while (lineIterator.hasNext()) {
                String expectedLine = lineIterator.nextLine();
                String actualLine = actualLineIterator.nextLine();
                // ignore lines starting with "*"
                if (!expectedLine.equals("*")) {
                    // XML output differs between Java 1.8 and Java 11
                    if ( expectedLine.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"") &&
                         actualLine.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"") ) {
                        continue;
                    }
                    Assert.assertEquals(expectedLine, actualLine);
                }
            }
        }
    }
}
