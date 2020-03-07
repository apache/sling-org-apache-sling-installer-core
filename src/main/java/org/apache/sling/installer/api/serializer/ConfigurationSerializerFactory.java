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

import org.apache.sling.installer.core.impl.serializer.ConfigConfigurationSerializer;
import org.apache.sling.installer.core.impl.serializer.JsonConfigurationSerializer;
import org.apache.sling.installer.core.impl.serializer.PropertiesConfigurationSerializer;
import org.jetbrains.annotations.NotNull;

public class ConfigurationSerializerFactory {
    public enum Format {
        JSON,
        CONFIG,
        PROPERTIES,
        PROPERTIES_XML
    }
    
    private ConfigurationSerializerFactory() {
    }

    public static @NotNull ConfigurationSerializer create(@NotNull Format format) {
        final ConfigurationSerializer serializer;
        switch (format) {
            case CONFIG:
                serializer = new ConfigConfigurationSerializer();
                break;
            case PROPERTIES:
                serializer = new PropertiesConfigurationSerializer(false);
                break;
            case PROPERTIES_XML:
                serializer = new PropertiesConfigurationSerializer(true);
                break;
            default:
                serializer = new JsonConfigurationSerializer();
                break;
        }
        return serializer;
    }
}
