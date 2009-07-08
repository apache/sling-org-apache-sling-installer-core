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
package org.apache.sling.osgi.installer.impl.tasks;

import static org.apache.sling.osgi.installer.InstallResultCode.IGNORED;

import java.util.Map;

import org.apache.sling.osgi.installer.InstallResultCode;
import org.apache.sling.osgi.installer.InstallableData;
import org.apache.sling.osgi.installer.impl.InstallableDataWrapper;
import org.apache.sling.osgi.installer.impl.OsgiControllerImpl;
import org.apache.sling.osgi.installer.impl.OsgiControllerTask;
import org.apache.sling.osgi.installer.impl.OsgiControllerTaskContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base class for OsgiControllerTasks that install or
 * 	remove OSGi bundles or configs (or deployment packages, etc.)
 */
abstract class InstallRemoveTask extends OsgiControllerTask {

	protected final String uri;
	protected final InstallableData data;

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    protected InstallRemoveTask(String uri, InstallableData data) {
    	this.uri = uri;
    	this.data = data;
    }
    
	@Override
	public String toString() {
		return
			getClass().getSimpleName()
			+ ", "
			+ (isInstallOrUpdate() ? "install/update" : "uninstall")
			+ ", "
			+ uri
		;
	}

	/** {@inheritDoc} */
	public void execute(OsgiControllerTaskContext context) throws Exception {
		if(isInstallOrUpdate()) {
			executeInstallOrUpdate(context);
		} else {
			executeUninstall(context);
		}

		// Cleanup InstallableDataWrapper
		if(data instanceof InstallableDataWrapper) {
			((InstallableDataWrapper)data).cleanup();
		}
	}
	
	/** @return true if this is an install/update task, false if it is an uninstall */
	protected boolean isInstallOrUpdate() {
		return data != null;
	}
	
	protected final void executeInstallOrUpdate(OsgiControllerTaskContext context) throws Exception {
        
        // If a corresponding higher priority resource is already installed, ignore this one
        if(context.getResourceOverrideRules() != null) {
            for(String r : context.getResourceOverrideRules().getHigherPriorityResources(uri)) {
                if(context.getStorage().contains(r)) {
                    log.info("Resource {} ignored, overridden by {} which has higher priority",
                            uri, r);
                    return;
                }
            }
        }

        // If a corresponding lower priority resource is installed, uninstall it first
        if(context.getResourceOverrideRules() != null) {
            for(String r : context.getResourceOverrideRules().getLowerPriorityResources(uri)) {
                if(context.getStorage().contains(r)) {
                    log.info("Resource {} overrides {}, uninstalling the latter",
                            uri, r);
                    executeUninstall(context);
                }
            }
        }

        // process install
        final Map<String, Object> map = context.getStorage().getMap(uri);
        if(doInstallOrUpdate(context, map) != IGNORED) {
            map.put(OsgiControllerImpl.KEY_DIGEST, data.getDigest());
        }
        context.getStorage().saveToFile();
	}
	
	protected final void executeUninstall(OsgiControllerTaskContext context) throws Exception {
	    log.info("Execute uninstall " + this);
        // If a corresponding higher priority resource is installed, ignore this request
        if(context.getResourceOverrideRules() != null) {
            for(String r : context.getResourceOverrideRules().getHigherPriorityResources(uri)) {
                if(context.getStorage().contains(r)) {
                    log.info("Resource {} won't be uninstalled, overridden by {} which has higher priority",
                            uri, r);
                    return;
                }
            }
        }

        doUninstall(context, context.getStorage().getMap(uri));

    	context.getStorage().remove(uri);
    	context.getStorage().saveToFile();
		
	}

	/** Do the actual uninstall */
	protected abstract void doUninstall(OsgiControllerTaskContext ctx, Map<String, Object> attributes) throws Exception;
	
	/** Do the actual install or update */
	protected abstract InstallResultCode doInstallOrUpdate(OsgiControllerTaskContext ctx, Map<String, Object> attributes) throws Exception;
}