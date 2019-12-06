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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.installer.api.UpdateHandler;
import org.apache.sling.installer.api.info.InfoProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

public class UpdateHandlerTracker extends SortingServiceTracker<UpdateHandler> {

    private final InfoProvider infoProvider;
    private final Map<ServiceReference, ServiceRegistration<URLStreamHandlerService>> urlHandlers;
    public UpdateHandlerTracker(BundleContext ctx, InfoProvider infoProvider) {
        super(ctx, UpdateHandler.class.getName(), null);
        this.infoProvider = infoProvider;
        this.urlHandlers = new HashMap<>();
    }

    @Override
    public Object addingService(ServiceReference reference) {
        final String[] schemes = PropertiesUtil.toStringArray(reference.getProperty(UpdateHandler.PROPERTY_SCHEMES));
        if (schemes != null && schemes.length > 0) {
            addUrlHandler(schemes);
        }
        return super.addingService(reference);
    }

    private ServiceRegistration<URLStreamHandlerService> addUrlHandler(String[] schemes) {
        InstallerResourceUrlHandler service = new InstallerResourceUrlHandler(infoProvider);
        Dictionary<String, String[]> properties = new Hashtable<>();
        properties.put(URLConstants.URL_HANDLER_PROTOCOL, schemes);
        return context.registerService(URLStreamHandlerService.class, service, properties);
        
    }
    
    private void removeUrlHandler(ServiceReference serviceReference) {
        ServiceRegistration<URLStreamHandlerService> urlHandler = urlHandlers.get(serviceReference);
        if (urlHandler != null) {
            urlHandler.unregister();
        }
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        removeUrlHandler(reference);
        super.removedService(reference, service);
    }

    @Override
    public void close() {
        for (ServiceReference serviceReference : urlHandlers.keySet()) {
            removeUrlHandler(serviceReference);
        }
        super.close();
    }
}
