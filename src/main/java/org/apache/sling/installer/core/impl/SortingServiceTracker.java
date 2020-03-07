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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.sling.installer.api.tasks.RetryHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class SortingServiceTracker<T> extends ServiceTracker<T, T> {

    private final RetryHandler listener;

    private volatile int lastCount = -1;

    private volatile int lastRefCount = -1;

    private volatile List<T> sortedServiceCache;

    private volatile List<ServiceReference<T>> sortedReferences;

    /**
     * Constructor
     * 
     * @param context Bundle context
     * @param clazz   Class
     */
    public SortingServiceTracker(final BundleContext context,
            final String clazz,
            final RetryHandler listener) {
        super(context, clazz, null);
        this.listener = listener;
    }

    /**
     * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    @Override
    public void removedService(ServiceReference<T> reference, T service) {
        this.sortedServiceCache = null;
        this.sortedReferences = null;
        this.context.ungetService(reference);
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    @Override
    public void modifiedService(ServiceReference<T> reference, T service) {
        this.sortedServiceCache = null;
        this.sortedReferences = null;
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    @Override
    public T addingService(ServiceReference<T> reference) {
        this.sortedServiceCache = null;
        this.sortedReferences = null;
        T returnValue = context.getService(reference);
        if ( listener != null ) {
            // new factory or resource transformer has been added, wake up main thread
            this.listener.scheduleRetry();
        }
        return returnValue;
    }

    /**
     * Return a sorted list of the services.
     * 
     * @return Service list
     */
    public List<T> getSortedServices() {
        List<T> result = this.sortedServiceCache;
        if (result == null || this.lastCount < this.getTrackingCount()) {
            this.lastCount = this.getTrackingCount();
            final ServiceReference<T>[] references = this.getServiceReferences();
            if (references == null || references.length == 0) {
                result = Collections.emptyList();
            } else {
                Arrays.sort(references);
                result = new ArrayList<T>();
                for (int i = 0; i < references.length; i++) {
                    final T service = this.getService(references[references.length - 1 - i]);
                    if (service != null) {
                        result.add(service);
                    }
                }
            }
            this.sortedServiceCache = result;
        }
        return result;
    }

    /**
     * Return a sorted list of the services references.
     * 
     * @return Service list
     */
    public List<ServiceReference<T>> getSortedServiceReferences() {
        List<ServiceReference<T>> result = this.sortedReferences;
        if (result == null || this.lastRefCount < this.getTrackingCount()) {
            this.lastRefCount = this.getTrackingCount();
            final ServiceReference<T>[] references = this.getServiceReferences();
            if (references == null || references.length == 0) {
                result = Collections.emptyList();
            } else {
                Arrays.sort(references);
                result = new ArrayList<ServiceReference<T>>();
                for (int i = 0; i < references.length; i++) {
                    result.add(references[references.length - 1 - i]);
                }
            }
            this.sortedReferences = result;
        }
        return result;
    }

    /**
     * Check if a service with the given name is registered.
     * 
     * @param name Name
     * @return {@code true} if it exists, {@code false} otherwise.
     */
    public boolean check(final String key, final String name) {
        final ServiceReference<T>[] refs = this.getServiceReferences();
        if ( refs != null ) {
            for (final ServiceReference<T> ref : refs) {
                final Object val = ref.getProperty(key);
                if ( name.equals(val) ) {
                    return true;
                }
            }
        }
        return false;
    }
}
