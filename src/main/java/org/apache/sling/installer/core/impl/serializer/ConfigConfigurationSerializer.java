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

import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.sling.installer.api.serializer.ConfigurationSerializer;
import org.jetbrains.annotations.NotNull;

public class ConfigConfigurationSerializer implements ConfigurationSerializer {

    @Override
    public void serialize(@NotNull Dictionary<String, Object> dictionary, @NotNull OutputStream outputStream)
            throws IOException {
        ConfigurationHandler.write(outputStream, dictionary);
    }
}
