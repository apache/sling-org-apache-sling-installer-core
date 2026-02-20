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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.core.impl.EntityResourceList;
import org.apache.sling.installer.core.impl.MockBundleContext;
import org.apache.sling.installer.core.impl.MockBundleResource;
import org.apache.sling.installer.core.impl.PersistentResourceList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RestartActiveBundlesTaskTest {

    private static final String ATTR_BUNDLES = "bundles";

    @Mock
    private InstallationContext installationContext;

    // -------------------------------------------------------------------------
    // Helper: build a task + its EntityResourceList using a given bundle list
    // -------------------------------------------------------------------------

    private static final String ENTITY_ID =
            PersistentResourceList.RESTART_ACTIVE_BUNDLES_TYPE + ':' + PersistentResourceList.RESTART_ACTIVE_BUNDLES_ID;

    // -------------------------------------------------------------------------
    // Helper: build a non-fragment, non-system-bundle mock
    // -------------------------------------------------------------------------

    private Bundle mockBundle(long id, int state) {
        Bundle b = mock(Bundle.class);
        when(b.getBundleId()).thenReturn(id);
        when(b.getState()).thenReturn(state);
        Dictionary<String, String> headers = new Hashtable<>();
        when(b.getHeaders("")).thenReturn(headers);
        return b;
    }

    /** Mock a fragment bundle (has Fragment-Host header). */
    private Bundle mockFragmentBundle(long id, int state) {
        Bundle b = mockBundle(id, state);
        Dictionary<String, String> headers = new Hashtable<>();
        headers.put(Constants.FRAGMENT_HOST, "some.host.bundle");
        when(b.getHeaders("")).thenReturn(headers);
        return b;
    }

    // =========================================================================
    // Constructor tests
    // =========================================================================

    @Test
    public void testConstructor_CollectsActiveBundles() throws IOException {
        Bundle active1 = mockBundle(1L, Bundle.ACTIVE);
        Bundle active2 = mockBundle(2L, Bundle.ACTIVE);

        List<Bundle> bundles = new ArrayList<>();
        bundles.add(active1);
        bundles.add(active2);

        MockBundleResource resource = new MockBundleResource("dummy", "1.0");
        EntityResourceList erl = new EntityResourceList(ENTITY_ID, new MockInstallationListener());
        erl.addOrUpdate(resource.getRegisteredResourceImpl());

        MockBundleContext ctx = new MockBundleContext(bundles);
        TaskSupport support = new TaskSupport(ctx);
        new RestartActiveBundlesTask(erl, support);

        @SuppressWarnings("unchecked")
        Set<Long> ids = (Set<Long>) erl.getActiveResource().getAttribute(ATTR_BUNDLES);
        assertNotNull("Bundle id set should be stored as attribute", ids);
        assertTrue("Bundle 1 should be in set", ids.contains(1L));
        assertTrue("Bundle 2 should be in set", ids.contains(2L));
    }

    @Test
    public void testConstructor_SkipsSystemBundle() throws IOException {
        // Bundle with id == 0 is the system bundle and must be skipped
        Bundle systemBundle = mockBundle(0L, Bundle.ACTIVE);

        List<Bundle> bundles = new ArrayList<>();
        bundles.add(systemBundle);

        MockBundleResource resource = new MockBundleResource("dummy", "1.0");
        EntityResourceList erl = new EntityResourceList(ENTITY_ID, new MockInstallationListener());
        erl.addOrUpdate(resource.getRegisteredResourceImpl());

        MockBundleContext ctx = new MockBundleContext(bundles);
        TaskSupport support = new TaskSupport(ctx);
        new RestartActiveBundlesTask(erl, support);

        @SuppressWarnings("unchecked")
        Set<Long> ids = (Set<Long>) erl.getActiveResource().getAttribute(ATTR_BUNDLES);
        assertNotNull(ids);
        assertTrue("System bundle (id 0) must not be in set", ids.isEmpty());
    }

    @Test
    public void testConstructor_SkipsFragmentBundles() throws IOException {
        // Fragment bundles must not be added to the restart set
        Bundle fragment = mockFragmentBundle(3L, Bundle.ACTIVE);

        List<Bundle> bundles = new ArrayList<>();
        bundles.add(fragment);

        MockBundleResource resource = new MockBundleResource("dummy", "1.0");
        EntityResourceList erl = new EntityResourceList(ENTITY_ID, new MockInstallationListener());
        erl.addOrUpdate(resource.getRegisteredResourceImpl());

        MockBundleContext ctx = new MockBundleContext(bundles);
        TaskSupport support = new TaskSupport(ctx);
        new RestartActiveBundlesTask(erl, support);

        @SuppressWarnings("unchecked")
        Set<Long> ids = (Set<Long>) erl.getActiveResource().getAttribute(ATTR_BUNDLES);
        assertNotNull(ids);
        assertTrue("Fragment bundle must not be in set", ids.isEmpty());
    }

    @Test
    public void testConstructor_SkipsNonActiveBundles() throws IOException {
        Bundle resolved = mockBundle(4L, Bundle.RESOLVED);
        Bundle installed = mockBundle(5L, Bundle.INSTALLED);

        List<Bundle> bundles = new ArrayList<>();
        bundles.add(resolved);
        bundles.add(installed);

        MockBundleResource resource = new MockBundleResource("dummy", "1.0");
        EntityResourceList erl = new EntityResourceList(ENTITY_ID, new MockInstallationListener());
        erl.addOrUpdate(resource.getRegisteredResourceImpl());

        MockBundleContext ctx = new MockBundleContext(bundles);
        TaskSupport support = new TaskSupport(ctx);
        new RestartActiveBundlesTask(erl, support);

        @SuppressWarnings("unchecked")
        Set<Long> ids = (Set<Long>) erl.getActiveResource().getAttribute(ATTR_BUNDLES);
        assertNotNull(ids);
        assertTrue("Non-active bundles must not be in set", ids.isEmpty());
    }

    // =========================================================================
    // execute() tests
    // =========================================================================

    @Test
    public void testExecute_StartsResolvedBundle() throws Exception {
        Bundle bundle = mockBundle(1L, Bundle.RESOLVED);

        List<Bundle> bundles = new ArrayList<>();
        bundles.add(bundle);

        MockBundleResource resource = new MockBundleResource("dummy", "1.0");
        EntityResourceList erl = new EntityResourceList(ENTITY_ID, new MockInstallationListener());
        erl.addOrUpdate(resource.getRegisteredResourceImpl());

        // Pre-seed ids with bundle id 1
        Set<Long> preIds = new HashSet<>();
        preIds.add(1L);
        erl.getActiveResource().setAttribute(ATTR_BUNDLES, preIds);

        MockBundleContext ctx = new MockBundleContext(bundles);
        TaskSupport support = new TaskSupport(ctx);
        RestartActiveBundlesTask task = new RestartActiveBundlesTask(erl, support);

        task.execute(installationContext);

        verify(bundle).start();
    }

    @Test
    public void testExecute_DoesNotStartActiveBundle() throws Exception {
        Bundle bundle = mockBundle(1L, Bundle.ACTIVE);

        List<Bundle> bundles = new ArrayList<>();
        bundles.add(bundle);

        MockBundleResource resource = new MockBundleResource("dummy", "1.0");
        EntityResourceList erl = new EntityResourceList(ENTITY_ID, new MockInstallationListener());
        erl.addOrUpdate(resource.getRegisteredResourceImpl());

        // Seed the ids – the bundle is already ACTIVE so execute must not call start()
        Set<Long> preIds = new HashSet<>();
        preIds.add(1L);
        erl.getActiveResource().setAttribute(ATTR_BUNDLES, preIds);

        MockBundleContext ctx = new MockBundleContext(bundles);
        TaskSupport support = new TaskSupport(ctx);
        // Constructor will also add bundle 1 because it is ACTIVE, but that is expected
        RestartActiveBundlesTask task = new RestartActiveBundlesTask(erl, support);

        task.execute(installationContext);

        verify(bundle, never()).start();
    }

    @Test
    public void testExecute_DoesNotStartStartingBundle() throws Exception {
        Bundle bundle = mockBundle(2L, Bundle.STARTING);

        List<Bundle> bundles = new ArrayList<>();
        bundles.add(bundle);

        MockBundleResource resource = new MockBundleResource("dummy", "1.0");
        EntityResourceList erl = new EntityResourceList(ENTITY_ID, new MockInstallationListener());
        erl.addOrUpdate(resource.getRegisteredResourceImpl());

        Set<Long> preIds = new HashSet<>();
        preIds.add(2L);
        erl.getActiveResource().setAttribute(ATTR_BUNDLES, preIds);

        MockBundleContext ctx = new MockBundleContext(bundles);
        TaskSupport support = new TaskSupport(ctx);
        RestartActiveBundlesTask task = new RestartActiveBundlesTask(erl, support);

        task.execute(installationContext);

        verify(bundle, never()).start();
    }

    @Test
    public void testExecute_DoesNotStartStoppingBundle() throws Exception {
        Bundle bundle = mockBundle(3L, Bundle.STOPPING);

        List<Bundle> bundles = new ArrayList<>();
        bundles.add(bundle);

        MockBundleResource resource = new MockBundleResource("dummy", "1.0");
        EntityResourceList erl = new EntityResourceList(ENTITY_ID, new MockInstallationListener());
        erl.addOrUpdate(resource.getRegisteredResourceImpl());

        Set<Long> preIds = new HashSet<>();
        preIds.add(3L);
        erl.getActiveResource().setAttribute(ATTR_BUNDLES, preIds);

        MockBundleContext ctx = new MockBundleContext(bundles);
        TaskSupport support = new TaskSupport(ctx);
        RestartActiveBundlesTask task = new RestartActiveBundlesTask(erl, support);

        task.execute(installationContext);

        verify(bundle, never()).start();
    }

    @Test
    public void testExecute_SkipsBundleNotFoundInContext() throws Exception {
        // Bundle id 99 is in the set but getBundleContext().getBundle(99) returns null
        List<Bundle> bundles = new ArrayList<>(); // empty context

        MockBundleResource resource = new MockBundleResource("dummy", "1.0");
        EntityResourceList erl = new EntityResourceList(ENTITY_ID, new MockInstallationListener());
        erl.addOrUpdate(resource.getRegisteredResourceImpl());

        MockBundleContext ctx = new MockBundleContext(bundles);
        TaskSupport support = new TaskSupport(ctx);
        // Construct first so the constructor runs with an empty context (no NPE)
        RestartActiveBundlesTask task = new RestartActiveBundlesTask(erl, support);

        // Inject the unknown id AFTER construction so execute() encounters it
        @SuppressWarnings("unchecked")
        Set<Long> ids = (Set<Long>) erl.getActiveResource().getAttribute(ATTR_BUNDLES);
        ids.add(99L);

        // Should not throw even when bundle is null
        task.execute(installationContext);

        assertTrue("Unknown bundle id should be removed from set after execute", !ids.contains(99L));
    }

    @Test
    public void testExecute_HandlesNullAttributeGracefully() throws Exception {
        List<Bundle> bundles = new ArrayList<>();

        MockBundleResource resource = new MockBundleResource("dummy", "1.0");
        EntityResourceList erl = new EntityResourceList(ENTITY_ID, new MockInstallationListener());
        erl.addOrUpdate(resource.getRegisteredResourceImpl());

        // Do NOT set the ATTR_BUNDLES attribute – it remains null
        MockBundleContext ctx = new MockBundleContext(bundles);
        TaskSupport support = new TaskSupport(ctx);
        RestartActiveBundlesTask task = new RestartActiveBundlesTask(erl, support);

        // Manually clear the attribute set by the constructor so execute sees null
        erl.getActiveResource().setAttribute(ATTR_BUNDLES, null);

        // Should complete without NPE
        task.execute(installationContext);

        // Attribute must still be null – execute() must not create a new set when the stored value is null
        assertNull(
                "ATTR_BUNDLES must remain null when execute() receives a null bundle set",
                erl.getActiveResource().getAttribute(ATTR_BUNDLES));
    }

    @Test
    public void testExecute_HandlesBundleException() throws Exception {
        Bundle bundle = mockBundle(5L, Bundle.RESOLVED);
        doThrow(new BundleException("start failed")).when(bundle).start();

        List<Bundle> bundles = new ArrayList<>();
        bundles.add(bundle);

        MockBundleResource resource = new MockBundleResource("dummy", "1.0");
        EntityResourceList erl = new EntityResourceList(ENTITY_ID, new MockInstallationListener());
        erl.addOrUpdate(resource.getRegisteredResourceImpl());

        Set<Long> preIds = new HashSet<>();
        preIds.add(5L);
        erl.getActiveResource().setAttribute(ATTR_BUNDLES, preIds);

        MockBundleContext ctx = new MockBundleContext(bundles);
        TaskSupport support = new TaskSupport(ctx);
        RestartActiveBundlesTask task = new RestartActiveBundlesTask(erl, support);

        // Should NOT throw; BundleException is caught and logged
        task.execute(installationContext);

        verify(bundle).start();
        // id remains in the set because start failed
        @SuppressWarnings("unchecked")
        Set<Long> ids = (Set<Long>) erl.getActiveResource().getAttribute(ATTR_BUNDLES);
        assertTrue("Id should remain in set when start throws BundleException", ids.contains(5L));
    }

    @Test
    public void testExecute_HandlesIllegalStateException() throws Exception {
        Bundle bundle = mockBundle(6L, Bundle.RESOLVED);
        doThrow(new IllegalStateException("bundle uninstalled")).when(bundle).start();

        List<Bundle> bundles = new ArrayList<>();
        bundles.add(bundle);

        MockBundleResource resource = new MockBundleResource("dummy", "1.0");
        EntityResourceList erl = new EntityResourceList(ENTITY_ID, new MockInstallationListener());
        erl.addOrUpdate(resource.getRegisteredResourceImpl());

        Set<Long> preIds = new HashSet<>();
        preIds.add(6L);
        erl.getActiveResource().setAttribute(ATTR_BUNDLES, preIds);

        MockBundleContext ctx = new MockBundleContext(bundles);
        TaskSupport support = new TaskSupport(ctx);
        RestartActiveBundlesTask task = new RestartActiveBundlesTask(erl, support);

        // Should NOT throw; IllegalStateException is caught and the id is removed
        task.execute(installationContext);

        verify(bundle).start();
        @SuppressWarnings("unchecked")
        Set<Long> ids = (Set<Long>) erl.getActiveResource().getAttribute(ATTR_BUNDLES);
        assertTrue("Id should be removed from set when start throws IllegalStateException", !ids.contains(6L));
    }

    @Test
    public void testExecute_RemovesIdAfterSuccessfulStart() throws Exception {
        Bundle bundle = mockBundle(7L, Bundle.RESOLVED);

        List<Bundle> bundles = new ArrayList<>();
        bundles.add(bundle);

        MockBundleResource resource = new MockBundleResource("dummy", "1.0");
        EntityResourceList erl = new EntityResourceList(ENTITY_ID, new MockInstallationListener());
        erl.addOrUpdate(resource.getRegisteredResourceImpl());

        Set<Long> preIds = new HashSet<>();
        preIds.add(7L);
        erl.getActiveResource().setAttribute(ATTR_BUNDLES, preIds);

        MockBundleContext ctx = new MockBundleContext(bundles);
        TaskSupport support = new TaskSupport(ctx);
        RestartActiveBundlesTask task = new RestartActiveBundlesTask(erl, support);

        task.execute(installationContext);

        @SuppressWarnings("unchecked")
        Set<Long> ids = (Set<Long>) erl.getActiveResource().getAttribute(ATTR_BUNDLES);
        assertTrue("Successfully started bundle's id should be removed from set", !ids.contains(7L));
    }

    /**
     * Regression test for the execute() fragment-guard fix.
     * <p>
     * If a bundle id that is stored in the restart set has become a fragment by
     * the time {@code execute()} runs (e.g. persisted from a previous cycle),
     * the old code would call {@code bundle.start()} on it, producing the
     * "Fragment bundles can not be started" error in the logs.
     * The fix detects fragments early and removes them silently without ever
     * calling {@code start()}.
     */
    @Test
    public void testExecute_SkipsAndRemovesFragmentBundle() throws Exception {
        Bundle fragment = mockFragmentBundle(20L, Bundle.RESOLVED);

        List<Bundle> bundles = new ArrayList<>();
        bundles.add(fragment);

        MockBundleResource resource = new MockBundleResource("dummy", "1.0");
        EntityResourceList erl = new EntityResourceList(ENTITY_ID, new MockInstallationListener());
        erl.addOrUpdate(resource.getRegisteredResourceImpl());

        MockBundleContext ctx = new MockBundleContext(bundles);
        TaskSupport support = new TaskSupport(ctx);
        RestartActiveBundlesTask task = new RestartActiveBundlesTask(erl, support);

        // Inject the fragment id directly so execute() encounters it even if the
        // constructor already cleaned it out (simulates a persisted stale entry)
        @SuppressWarnings("unchecked")
        Set<Long> ids = (Set<Long>) erl.getActiveResource().getAttribute(ATTR_BUNDLES);
        ids.add(20L);

        task.execute(installationContext);

        // start() must never be called on a fragment
        verify(fragment, never()).start();
        // The id must be cleaned up from the set
        assertTrue("Fragment id must be removed from set after execute", !ids.contains(20L));
    }

    @Test
    public void testExecute_EmptyBundleSet_NoStartCalled() throws Exception {
        List<Bundle> bundles = new ArrayList<>();

        MockBundleResource resource = new MockBundleResource("dummy", "1.0");
        EntityResourceList erl = new EntityResourceList(ENTITY_ID, new MockInstallationListener());
        erl.addOrUpdate(resource.getRegisteredResourceImpl());

        // Empty set – constructor finds no active bundles in an empty context
        MockBundleContext ctx = new MockBundleContext(bundles);
        TaskSupport support = new TaskSupport(ctx);
        RestartActiveBundlesTask task = new RestartActiveBundlesTask(erl, support);

        // Should complete without any start() calls
        task.execute(installationContext);

        // The stored id set must be empty – nothing to restart in an empty context
        @SuppressWarnings("unchecked")
        Set<Long> ids = (Set<Long>) erl.getActiveResource().getAttribute(ATTR_BUNDLES);
        assertNotNull("Bundle id set must be initialised even when empty", ids);
        assertTrue("Bundle id set must be empty when no active bundles exist", ids.isEmpty());
    }
}
