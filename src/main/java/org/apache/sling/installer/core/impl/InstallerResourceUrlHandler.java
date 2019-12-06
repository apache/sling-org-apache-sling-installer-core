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
package org.apache.sling.installer.core.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.ProtectionDomain;

import org.apache.sling.installer.api.info.InfoProvider;
import org.apache.sling.installer.api.info.ResourceGroup;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.core.impl.tasks.AbstractBundleTask;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerService;

/**
 * URL Handler for schemes used by the UpdateHandlers<br/>
 * 
 * This is for example used by Apache Felix in the context of populating the {@link ProtectionDomain} of a bundle's class
 * by evaluating the bundle location.
 * 
 * @see <a href="https://osgi.org/specification/osgi.core/7.0.0/service.url.html#d0e42987">OSGi URL Handlers</a>
 */
public class InstallerResourceUrlHandler extends AbstractURLStreamHandlerService implements URLStreamHandlerService {

    private final InfoProvider installerInfo;

    public InstallerResourceUrlHandler(InfoProvider installerInfo) {
        this.installerInfo = installerInfo;
    }

    private InputStream getInputStreamFromInstallerResourceUrl(URL url) throws IOException {
        for (ResourceGroup resourceGroup : installerInfo.getInstallationState().getInstalledResources()) {
            for (org.apache.sling.installer.api.info.Resource resource : resourceGroup.getResources()) {
                String bundleLocation = AbstractBundleTask.getBundleLocation(resource);
                if (url.toString().equals(bundleLocation) && resource.getState().equals(ResourceState.INSTALLED)) {
                    return resource.getInputStream();
                }
            }
        }
        throw new IOException("Could not find OSGi installer resource with location " + url);
    }

    @Override
    public URLConnection openConnection(URL url) throws IOException {
        return new InputStreamConnection(url, getInputStreamFromInstallerResourceUrl(url));
    }

    private static final class InputStreamConnection extends URLConnection {

        private final InputStream input;
        
        protected InputStreamConnection(URL url, InputStream input) {
            super(url);
            this.input = input;
        }

        @Override
        public void connect() throws IOException {
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return input;
        }
    }

}
