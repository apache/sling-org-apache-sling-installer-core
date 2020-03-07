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
package org.apache.sling.installer.core.impl.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Dictionary;
import java.util.Properties;

import org.apache.sling.installer.api.serializer.ConfigurationSerializer;
import org.jetbrains.annotations.NotNull;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

public class PropertiesConfigurationSerializer implements ConfigurationSerializer {

    private final boolean isXml;
    
    public PropertiesConfigurationSerializer(boolean isXml) {
        this.isXml = isXml;
    }

    @Override
    public void serialize(@NotNull Dictionary<String, Object> dictionary, @NotNull OutputStream outputStream) throws IOException {
        // convert to properties object
        Converter converter = Converters.standardConverter();
        Properties properties = converter.convert(dictionary).to(Properties.class);
        if (!isXml) {
            properties.store(outputStream, null);
        } else {
            properties.storeToXML(outputStream, null);
        }
    }

}
