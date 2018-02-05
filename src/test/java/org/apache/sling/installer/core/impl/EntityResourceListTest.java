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

import org.apache.sling.installer.api.event.InstallationListener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.internal.matchers.Any;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.Version;

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class EntityResourceListTest {
	
	private static final String OLD_URI = "old-uri";
	private static final String OLD_DIGEST = "old-digest";
	private static final String NEW_URI = "new-uri";
	private static final String NEW_DIGEST = "new-digest";
	
	private static final String MOCK_URL = "mock-url";
	private static final String RESOURCE_ID = "res-id";
	private static final Version VERSION = new Version("1.0.0");
	
	@Mock
	InstallationListener listener;
	
    @Test
    public void testAddExistingResourceWithDifferentDataUriAndDigest() {
    	EntityResourceList erl = new EntityResourceList(RESOURCE_ID, listener);
    	RegisteredResourceImpl r1 = mock(RegisteredResourceImpl.class);
    	when(r1.getURL()).thenReturn(MOCK_URL);
    	when(r1.getVersion()).thenReturn((VERSION));
    	when(r1.getDataURI()).thenReturn(OLD_URI);
    	when(r1.getDigest()).thenReturn(OLD_DIGEST);
    	erl.addOrUpdate(r1);
    	assertEquals(OLD_URI, ((RegisteredResourceImpl)erl.getFirstResource()).getDataURI());

    	RegisteredResourceImpl r2 = mock(RegisteredResourceImpl.class);
    	when(r2.getURL()).thenReturn(MOCK_URL);
    	when(r2.getVersion()).thenReturn(VERSION);
    	when(r2.getDataURI()).thenReturn(NEW_URI);
    	when(r2.getDigest()).thenReturn(NEW_DIGEST);
    	erl.addOrUpdate(r2);
    	ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    	verify(r1).updateResourceUri(argument.capture());
    	assertEquals(NEW_URI, argument.getValue());
    }

}
