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
package org.apache.sling.osgi.installer.impl.config;

import org.apache.sling.osgi.installer.impl.EntityResourceList;
import org.apache.sling.osgi.installer.impl.OsgiInstallerTask;
import org.apache.sling.osgi.installer.impl.RegisteredResource;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Task creator for configurations.
 */
public class ConfigTaskCreator {

    public static final String ALIAS_KEY = "org.apache.sling.installer.osgi.factoryaliaspid";
    public static final String CONFIG_PATH_KEY = "org.apache.sling.installer.osgi.path";


    /** Interface of the config admin */
    private static String CONFIG_ADMIN_SERVICE_NAME = ConfigurationAdmin.class.getName();

    /** Service tracker for the configuration admin. */
    private final ServiceTracker configAdminServiceTracker;

    /**
     * Constructor
     */
    public ConfigTaskCreator(final BundleContext bc) {
        this.configAdminServiceTracker = new ServiceTracker(bc, CONFIG_ADMIN_SERVICE_NAME, null);
        this.configAdminServiceTracker.open();
    }

    /**
     * Deactivate this creator.
     */
    public void deactivate() {
        this.configAdminServiceTracker.close();
    }

	/**
     * Create a task to install or uninstall a configuration.
	 */
	public OsgiInstallerTask createTask(final EntityResourceList toActivate) {
	    // if there is no config admin, just return
	    if ( this.configAdminServiceTracker.getService() == null ) {
            return null;
	    }
	    final OsgiInstallerTask result;
		if (toActivate.getActiveResource().getState() == RegisteredResource.State.UNINSTALL) {
		    result = new ConfigRemoveTask(toActivate, this.configAdminServiceTracker);
		} else {
	        result = new ConfigInstallTask(toActivate, this.configAdminServiceTracker);
		}
		return result;
	}
}