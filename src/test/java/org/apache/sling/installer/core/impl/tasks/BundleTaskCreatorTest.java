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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.ChangeStateTask;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.core.impl.EntityResourceList;
import org.apache.sling.installer.core.impl.MockBundleResource;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.startlevel.BundleStartLevel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class BundleTaskCreatorTest {
    public static final String SN = "TestSymbolicName";

    private SortedSet<InstallTask> getTasks(TaskResource[] resources, BundleTaskCreator btc) throws IOException {
        final SortedSet<TaskResource> sortedResources = new TreeSet<TaskResource>();
        for (final TaskResource rr : resources) {
            sortedResources.add(rr);
        }
        final SortedSet<InstallTask> tasks = new TreeSet<InstallTask>();
        for (final TaskResource r : sortedResources) {
            final EntityResourceList erl = new EntityResourceList(r.getEntityId(), new MockInstallationListener());
            erl.addOrUpdate(((MockBundleResource) r).getRegisteredResourceImpl());
            assertNotNull(erl.getActiveResource());
            tasks.add(btc.createTask(erl));
        }
        return tasks;
    }

    @Test
    public void testSingleBundleNew() throws IOException {
        final TaskResource[] r = {new MockBundleResource(SN, "1.0")};
        final MockBundleTaskCreator c = new MockBundleTaskCreator();
        final SortedSet<InstallTask> s = getTasks(r, c);
        assertEquals("Expected one task", 1, s.size());
        assertTrue("Expected a BundleInstallTask", s.first() instanceof BundleInstallTask);
    }

    @Test
    public void testSingleBundleAlreadyInstalled() throws IOException {
        final TaskResource[] r = {new MockBundleResource(SN, "1.0")};

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, "1.0", Bundle.ACTIVE);
            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected one task, same version is active", 1, s.size());
            assertTrue("Change state task expected.", s.first() instanceof ChangeStateTask);
        }

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, "1.0", Bundle.RESOLVED);
            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected one tasks, same version is installed", 1, s.size());
            assertTrue("Change state task expected.", s.first() instanceof ChangeStateTask);
        }
    }

    @Test
    public void testBundleUpgrade() throws IOException {
        final TaskResource[] r = {new MockBundleResource(SN, "1.1")};

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, "1.0", Bundle.ACTIVE);
            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected one task", 1, s.size());
            assertTrue("Expected a BundleUpdateTask", s.first() instanceof BundleUpdateTask);
        }
    }

    @Test
    public void testBundleUpgradeBothRegistered() throws IOException {
        final TaskResource[] r = {new MockBundleResource(SN, "1.1"), new MockBundleResource(SN, "1.0")};

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, "1.0", Bundle.ACTIVE);
            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected two tasks", 2, s.size());
            assertTrue("Expected a ChangeStateTask", s.first() instanceof ChangeStateTask);
            assertTrue("Expected a BundleUpdateTask", s.toArray()[1] instanceof BundleUpdateTask);
        }
    }

    @Test
    public void testBundleUpgradeBothRegisteredReversed() throws IOException {
        final TaskResource[] r = {new MockBundleResource(SN, "1.0"), new MockBundleResource(SN, "1.1")};

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, "1.0", Bundle.ACTIVE);
            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected two tasks", 2, s.size());
            assertTrue("Expected a ChangeStateTask", s.first() instanceof ChangeStateTask);
            assertTrue("Expected a BundleUpdateTask", s.toArray()[1] instanceof BundleUpdateTask);
        }
    }

    @Test
    public void testBundleUpgradeSnapshot() throws IOException {
        // Need to use OSGi-compliant version number, in bundles
        // bnd and other tools generate correct numbers.
        final String v = "2.0.7.SNAPSHOT";
        final TaskResource[] r = {new MockBundleResource(SN, v)};

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, v, Bundle.ACTIVE);
            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected one task", 1, s.size());
            assertTrue("Expected a BundleUpdateTask", s.first() instanceof BundleUpdateTask);
        }
    }

    @Test
    public void testBundleRemoveSingle() throws IOException {
        final String version = "1.0";
        final MockBundleResource[] r = {new MockBundleResource(SN, version)};
        r[0].setState(ResourceState.UNINSTALL);

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, "1.0", Bundle.ACTIVE);
            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected one task, remove bundle", 1, s.size());
            assertTrue("Expected a BundleRemoveTask", s.first() instanceof BundleRemoveTask);
        }
    }

    @Test
    public void testBundleRemoveMultiple() throws IOException {
        final MockBundleResource[] r = {
            new MockBundleResource(SN, "1.0"), new MockBundleResource(SN, "1.1"), new MockBundleResource(SN, "2.0")
        };
        for (MockBundleResource x : r) {
            x.setState(ResourceState.UNINSTALL);
        }

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, "1.1", Bundle.ACTIVE);
            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected one tasks, bundle was not installed by us", 2, s.size());
            final Iterator<InstallTask> i = s.iterator();
            final InstallTask first = i.next();
            final InstallTask second = i.next();
            assertTrue("Expected a ChangeStateTask", first instanceof ChangeStateTask);
            assertTrue("Expected a BundleRemoveTask", second instanceof BundleRemoveTask);
        }
    }

    @Test
    public void testDowngradeOfRemovedResource() throws IOException {
        final MockBundleResource[] r = {
            new MockBundleResource(SN, "1.0.0"), new MockBundleResource(SN, "1.1.0"),
        };

        // Simulate V1.1 installed but resource is gone -> downgrade to 1.0
        r[1].setState(ResourceState.UNINSTALL);

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator();
            c.addBundleInfo(SN, "1.1.0", Bundle.ACTIVE);

            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected two tasks", 2, s.size());
            final Iterator<InstallTask> i = s.iterator();
            final InstallTask first = i.next();
            assertTrue("Expected a ChangeStateTask:" + first, first instanceof ChangeStateTask);
            final InstallTask second = i.next();
            assertTrue("Expected a BundleRemoveTask", second instanceof BundleRemoveTask);
            final BundleRemoveTask t = (BundleRemoveTask) second;
            assertEquals(
                    "Remove should be to V1.1",
                    r[1].getEntityId(),
                    t.getResource().getEntityId());
        }
    }

    @Test
    public void testBundleUpgradeStartLevelChanged() throws IOException {
        Dictionary<String, Object> dictionary = new Hashtable<>();
        dictionary.put(InstallableResource.INSTALLATION_HINT, 19);
        final MockBundleResource resource = new MockBundleResource(SN, "1.1");
        resource.setDictionary(dictionary);
        final TaskResource[] r = {resource};

        {
            final long bundleId = 1L;
            List<Bundle> bundles = new ArrayList<>();
            bundles.add(getMockBundle(bundleId, SN, 20));
            final MockBundleTaskCreator c = new MockBundleTaskCreator(bundles);
            c.addBundleInfo(SN, "1.1", Bundle.ACTIVE);
            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected one task", 1, s.size());
            assertTrue("Expected a BundleUpdateTask", s.first() instanceof BundleUpdateTask);
        }
    }

    @Test
    public void testBundleUpgradeStartLevelChanged_BundleInfoNull() throws IOException {
        Dictionary<String, Object> dictionary = new Hashtable<>();
        dictionary.put(InstallableResource.INSTALLATION_HINT, 19);
        final MockBundleResource resource = new MockBundleResource(SN, "1.1");
        resource.setDictionary(dictionary);
        final TaskResource[] r = {resource};

        {
            final long bundleId = 1L;
            List<Bundle> bundles = new ArrayList<>();
            bundles.add(getMockBundle(bundleId, SN, 20));
            final MockBundleTaskCreator c = new MockBundleTaskCreator(bundles);
            // Don't add bundle info, so getBundleInfo will return null
            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected one task", 1, s.size());
            assertTrue("Expected a BundleInstallTask", s.first() instanceof BundleInstallTask);
        }
    }

    @Test
    public void testBundleUpgradeStartLevelChanged_BundleNotFoundInContext() throws IOException {
        Dictionary<String, Object> dictionary = new Hashtable<>();
        dictionary.put(InstallableResource.INSTALLATION_HINT, 19);
        final MockBundleResource resource = new MockBundleResource(SN, "1.1");
        resource.setDictionary(dictionary);
        final TaskResource[] r = {resource};

        {
            final MockBundleTaskCreator c = new MockBundleTaskCreator(); // Empty bundle list
            c.addBundleInfo(SN, "1.0", Bundle.ACTIVE);
            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected one task", 1, s.size());
            // When bundle info exists with same version, it creates a ChangeStateTask
            assertTrue("Expected a BundleUpdateTask", s.first() instanceof BundleUpdateTask);
        }
    }

    @Test
    public void testBundleUpgradeStartLevelChanged_BundleStartLevelServiceNull() throws IOException {
        Dictionary<String, Object> dictionary = new Hashtable<>();
        dictionary.put(InstallableResource.INSTALLATION_HINT, 19);
        final MockBundleResource resource = new MockBundleResource(SN, "1.1");
        resource.setDictionary(dictionary);
        final TaskResource[] r = {resource};

        {
            final long bundleId = 1L;
            List<Bundle> bundles = new ArrayList<>();
            // Simulate bundle found but no BundleStartLevel service available
            bundles.add(getMockBundle(bundleId, SN, null));
            final MockBundleTaskCreator c = new MockBundleTaskCreator(bundles);
            c.addBundleInfo(SN, "1.1", Bundle.ACTIVE);
            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected one task", 1, s.size());
            assertTrue("Expected a BundleUpdateTask", s.first() instanceof BundleUpdateTask);
        }
    }

    @Test
    public void testBundleUpgradeStartLevelChanged_NoInstallationHint() throws IOException {
        final TaskResource[] r = {new MockBundleResource(SN, "1.1")};

        {
            final long bundleId = 1L;
            List<Bundle> bundles = new ArrayList<>();
            bundles.add(getMockBundle(bundleId, SN, 20));
            final MockBundleTaskCreator c = new MockBundleTaskCreator(bundles);
            c.addBundleInfo(SN, "1.1", Bundle.ACTIVE);
            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected one task", 1, s.size());
            assertTrue("Expected a BundleUpdateTask", s.first() instanceof BundleUpdateTask);
        }
    }

    @Test
    public void testBundleUpgradeStartLevelChanged_InstallationHintNull() throws IOException {
        final MockBundleResource resource = new MockBundleResource(SN, "1.1");
        resource.setDictionary(new Hashtable<>());
        final TaskResource[] r = {resource};

        {
            final long bundleId = 1L;
            List<Bundle> bundles = new ArrayList<>();
            bundles.add(getMockBundle(bundleId, SN, 20));
            final MockBundleTaskCreator c = new MockBundleTaskCreator(bundles);
            c.addBundleInfo(SN, "1.1", Bundle.ACTIVE);
            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected one task", 1, s.size());
            assertTrue("Expected a BundleUpdateTask", s.first() instanceof BundleUpdateTask);
        }
    }

    @Test
    public void testBundleUpgradeStartLevelChanged_InvalidInstallationHint() throws IOException {
        Dictionary<String, Object> dictionary = new Hashtable<>();
        dictionary.put(InstallableResource.INSTALLATION_HINT, "NOT_A_NUMBER");
        final MockBundleResource resource = new MockBundleResource(SN, "1.1");
        resource.setDictionary(dictionary);
        final TaskResource[] r = {resource};

        {
            final long bundleId = 1L;
            List<Bundle> bundles = new ArrayList<>();
            bundles.add(getMockBundle(bundleId, SN, 20));
            final MockBundleTaskCreator c = new MockBundleTaskCreator(bundles);
            c.addBundleInfo(SN, "1.1", Bundle.ACTIVE);
            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected one task", 1, s.size());
            assertTrue("Expected a BundleUpdateTask", s.first() instanceof BundleUpdateTask);
        }
    }

    @Test
    public void testBundleUpgradeStartLevelChanged_InstallationHintAsString() throws IOException {
        Dictionary<String, Object> dictionary = new Hashtable<>();
        dictionary.put(InstallableResource.INSTALLATION_HINT, "19");
        final MockBundleResource resource = new MockBundleResource(SN, "1.1");
        resource.setDictionary(dictionary);
        final TaskResource[] r = {resource};

        {
            final long bundleId = 1L;
            List<Bundle> bundles = new ArrayList<>();
            bundles.add(getMockBundle(bundleId, SN, 20));
            final MockBundleTaskCreator c = new MockBundleTaskCreator(bundles);
            c.addBundleInfo(SN, "1.1", Bundle.ACTIVE);
            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected one task", 1, s.size());
            assertTrue("Expected a BundleUpdateTask", s.first() instanceof BundleUpdateTask);
        }
    }

    @Test
    public void testBundleUpdateWithStartLevelChange_CurrentLowerThanNew() throws IOException {
        Dictionary<String, Object> dictionary = new Hashtable<>();
        dictionary.put(InstallableResource.INSTALLATION_HINT, 20);
        final MockBundleResource resource = new MockBundleResource(SN, "1.1");
        resource.setDictionary(dictionary);
        final TaskResource[] r = {resource};

        {
            final long bundleId = 1L;
            List<Bundle> bundles = new ArrayList<>();
            bundles.add(getMockBundle(bundleId, SN, 19));
            final MockBundleTaskCreator c = new MockBundleTaskCreator(bundles);
            c.addBundleInfo(SN, "1.1", Bundle.ACTIVE);
            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected one task", 1, s.size());
            assertTrue("Expected a BundleUpdateTask", s.first() instanceof BundleUpdateTask);
        }
    }

    @Test
    public void testBundleUpdateWithStartLevelChange_SameStartLevel() throws IOException {
        Dictionary<String, Object> dictionary = new Hashtable<>();
        dictionary.put(InstallableResource.INSTALLATION_HINT, 20);
        final MockBundleResource resource = new MockBundleResource(SN, "1.1");
        resource.setDictionary(dictionary);
        final TaskResource[] r = {resource};

        {
            final long bundleId = 1L;
            List<Bundle> bundles = new ArrayList<>();
            bundles.add(getMockBundle(bundleId, SN, 20));
            final MockBundleTaskCreator c = new MockBundleTaskCreator(bundles);
            c.addBundleInfo(SN, "1.1", Bundle.ACTIVE);
            final SortedSet<InstallTask> s = getTasks(r, c);
            assertEquals("Expected one task", 1, s.size());
            assertTrue("Expected a ChangeStateTask", s.first() instanceof ChangeStateTask);
        }
    }

    private Bundle getMockBundle(long bundleId, String symbolicName, Integer startLevel) {
        // Create a mock bundle with the specified symbolic name and start level
        Bundle bundle = Mockito.mock(Bundle.class);
        when(bundle.getSymbolicName()).thenReturn(symbolicName);
        when(bundle.getBundleId()).thenReturn(bundleId);
        when(bundle.getState()).thenReturn(Bundle.ACTIVE);
        if (startLevel != null) {
            BundleStartLevel bundleStartLevel = Mockito.mock(BundleStartLevel.class);
            when(bundleStartLevel.getStartLevel()).thenReturn(startLevel);
            when(bundle.adapt(BundleStartLevel.class)).thenReturn(bundleStartLevel);
        }
        return bundle;
    }
}
