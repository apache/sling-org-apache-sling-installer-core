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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;

import org.apache.felix.cm.json.Configurations;
import org.apache.sling.installer.api.serializer.ConfigurationSerializer;

/** Serializes dictionary configuration objects (as specified in the configuration admin) into JSON format.
 *
 * @see <a href="https://osgi.org/specification/osgi.cmpn/7.0.0/service.configurator.html">OSGi Configurator Spec</a>
 */
public class JsonConfigurationSerializer implements ConfigurationSerializer {

    @Override
    public void serialize(Dictionary<String, Object> dictionary, OutputStream outputStream) throws IOException {
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        Configurations.buildWriter().build(writer).writeConfiguration(dictionary);
        writer.flush();
        // do not close the writer to prevent closing the underlying outputstream
    }
}
