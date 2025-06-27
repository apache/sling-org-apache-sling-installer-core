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
package org.apache.sling.installer.core.impl.tasks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.core.impl.EntityResourceList;
import org.apache.sling.installer.core.impl.MockBundleResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.startlevel.BundleStartLevel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BundleUpdateTaskTest {

    private static final String BUNDLE_SYMBOLIC_NAME = "test.bundle";
    private static final String BUNDLE_VERSION = "1.1.0";

    @Mock
    private BundleContext bundleContext;

    @Mock
    private Bundle bundle;

    @Mock
    private BundleStartLevel bundleStartLevel;

    @Mock
    private InstallationContext installationContext;

    @Mock
    private TaskSupport taskSupport;

    @Test
    public void testBundleUpdateExecute_BundleNotFound() throws Exception {
        List<Bundle> bundles = new ArrayList<>();
        bundles.add(bundle);
        // Setup bundle context
        when(bundleContext.getBundles()).thenReturn(bundles.toArray(new Bundle[0]));
        // Setup task support
        when(taskSupport.getBundleContext()).thenReturn(bundleContext);
        // Setup resource with proper InputStream
        MockBundleResource resource = new MockBundleResource(BUNDLE_SYMBOLIC_NAME, BUNDLE_VERSION) {
            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream("test bundle content".getBytes());
            }
        };
        EntityResourceList resourceList =
                new EntityResourceList(resource.getEntityId(), new MockInstallationListener());
        try {
            resourceList.addOrUpdate(resource.getRegisteredResourceImpl());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Create task
        BundleUpdateTask task = new BundleUpdateTask(resourceList, taskSupport);
        // Given: Bundle not found in context
        when(bundleContext.getBundles()).thenReturn(new Bundle[0]);

        // When: Execute task
        task.execute(installationContext);

        // Then: Task should be ignored
        assertEquals(ResourceState.IGNORED, resourceList.getFirstResource().getState());
        assertTrue(resourceList
                .getFirstResource()
                .getError()
                .contains("Bundle to update (" + BUNDLE_SYMBOLIC_NAME + ") not found"));
    }

    @Test
    public void testBundleUpdateExecute_SameVersionNonSnapshotSameStartLevel() throws Exception {
        List<Bundle> bundles = new ArrayList<>();
        // Setup basic bundle mock
        when(bundle.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC_NAME);
        when(bundle.getVersion()).thenReturn(new Version(BUNDLE_VERSION));
        when(bundle.adapt(BundleStartLevel.class)).thenReturn(bundleStartLevel);
        bundles.add(bundle);
        // Setup bundle context
        when(bundleContext.getBundles()).thenReturn(bundles.toArray(new Bundle[0]));
        // Setup task support
        when(taskSupport.getBundleContext()).thenReturn(bundleContext);
        // Setup resource with proper InputStream
        MockBundleResource resource = new MockBundleResource(BUNDLE_SYMBOLIC_NAME, BUNDLE_VERSION) {
            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream("test bundle content".getBytes());
            }
        };
        EntityResourceList resourceList =
                new EntityResourceList(resource.getEntityId(), new MockInstallationListener());
        try {
            resourceList.addOrUpdate(resource.getRegisteredResourceImpl());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Create task
        BundleUpdateTask task = new BundleUpdateTask(resourceList, taskSupport);

        // When: Execute task
        task.execute(installationContext);

        assertEquals(ResourceState.INSTALLED, resourceList.getFirstResource().getState());
        assertTrue(resourceList
                .getFirstResource()
                .getError()
                .contains("Same version is already installed, and not a snapshot, ignoring update"));
    }
}
