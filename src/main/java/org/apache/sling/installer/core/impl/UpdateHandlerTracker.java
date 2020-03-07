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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.installer.api.UpdateHandler;
import org.apache.sling.installer.api.info.InfoProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.converter.Converters;

/**
 * Registers one (singleton) url handler handling all registered schemes of UpdateHandler's.
 *
 */
public class UpdateHandlerTracker extends SortingServiceTracker<UpdateHandler> {

    private final InfoProvider infoProvider;
    private final Map<String, AtomicInteger> schemeUseCount;
    private ServiceRegistration<URLStreamHandlerService> urlHandler;

    public UpdateHandlerTracker(BundleContext ctx, InfoProvider infoProvider) {
        super(ctx, UpdateHandler.class.getName(), null);
        this.infoProvider = infoProvider;
        this.urlHandler = null; // initialize lazily
        this.schemeUseCount = new HashMap<>();
    }

    @Override
    public UpdateHandler addingService(ServiceReference<UpdateHandler> reference) {
        addOrRemoveService(reference, true);
        return super.addingService(reference);
    }

    @Override
    public void removedService(ServiceReference<UpdateHandler> reference, UpdateHandler service) {
        addOrRemoveService(reference, false);
        super.removedService(reference, service);
    }

    private void addOrRemoveService(ServiceReference<UpdateHandler> reference, boolean isAdd) {
        final String[] schemes = Converters.standardConverter()
                .convert(reference.getProperty(UpdateHandler.PROPERTY_SCHEMES)).to(String[].class);
        boolean hasChanged = false;
        for (String scheme : schemes) {
            if (isAdd) {
                if (addScheme(scheme)) {
                    hasChanged = true;
                }
            } else {
                if (removeScheme(scheme)) {
                    hasChanged = true;
                }
            }
        }
        if (hasChanged) {
            updateUrlStreamHandler();
        }
    }

    private synchronized void updateUrlStreamHandler() {
        if (schemeUseCount.isEmpty()) {
            if (urlHandler != null) {
                urlHandler.unregister();
                urlHandler = null;
            }
        } else {
            if (urlHandler == null) {
                InstallerResourceUrlHandler service = new InstallerResourceUrlHandler(infoProvider);
                Dictionary<String, String[]> properties = new Hashtable<>();
                properties.put(URLConstants.URL_HANDLER_PROTOCOL, schemeUseCount.keySet().toArray(new String[0]));
                urlHandler = context.registerService(URLStreamHandlerService.class, service, properties);
            } else {
                Dictionary<String, String[]> properties = new Hashtable<>();
                properties.put(URLConstants.URL_HANDLER_PROTOCOL, schemeUseCount.keySet().toArray(new String[0]));
                urlHandler.setProperties(properties);
            }
        }
    }

    private synchronized boolean addScheme(String scheme) {
        AtomicInteger useCount = schemeUseCount.get(scheme);
        if (useCount == null) {
            schemeUseCount.put(scheme, new AtomicInteger(1));
            return true;
        } else {
            useCount.incrementAndGet();
            return false;
        }
    }

    private synchronized boolean removeScheme(String scheme) {
        AtomicInteger useCount = schemeUseCount.get(scheme);
        if (useCount != null && useCount.decrementAndGet() <= 0) {
            schemeUseCount.remove(scheme);
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        schemeUseCount.clear();
        updateUrlStreamHandler();
        super.close();
    }
}
