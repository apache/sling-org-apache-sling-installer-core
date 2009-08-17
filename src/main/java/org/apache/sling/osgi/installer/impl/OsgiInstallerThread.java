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
package org.apache.sling.osgi.installer.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.osgi.service.log.LogService;

/** Worker thread where all OSGi tasks are executed.
 *  Runs cycles where the list of RegisteredResources is examined,
 *  OsgiTasks are created accordingly and executed.
 *  
 *  A separate list of RegisteredResources is kept for resources
 *  that are updated or removed during a cycle, and merged with
 *  the main list at the end of the cycle.
 */
class OsgiInstallerThread extends Thread {
    
    private final OsgiInstallerContext ctx;
    private final List<RegisteredResource> newResources = new LinkedList<RegisteredResource>();
    private final TreeSet<OsgiInstallerTask> tasks = new TreeSet<OsgiInstallerTask>();
    
    /** Group our RegisteredResource by OSGi entity */ 
    private final Map<String, TreeSet<RegisteredResource>> registeredResources = 
    	new HashMap<String, TreeSet<RegisteredResource>>();
    
    static interface TaskCreator {
    	/** Add the required OsgiInstallerTasks to the tasks collection, so that the resources reach
    	 * 	their desired states.
    	 * 	@param resources ordered set of RegisteredResource which all have the same entityId
    	 * 	@param tasks lists of tasks, to which we'll add the ones computed by this method
    	 */
    	void createTasks(TreeSet<RegisteredResource> resources, TreeSet<OsgiInstallerTask> tasks);
    }
    private final TaskCreator bundleTaskCreator = new BundleTaskCreator();
    
    OsgiInstallerThread(OsgiInstallerContext ctx) {
        setName(getClass().getSimpleName());
        this.ctx = ctx;
    }

    @Override
    public void run() {
        while(true) {
            // TODO do nothing if nothing to process!
            try {
            	mergeNewResources();
                computeTasks();
                executeTasks();
                Thread.sleep(250);
            } catch(Exception e) {
                if(ctx.getLogService() != null) {
                    ctx.getLogService().log(LogService.LOG_WARNING, e.toString(), e);
                }
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException ignored) {
                }
            }
        }
    }
    
    void addTaskToCurrentCycle(OsgiInstallerTask t) {
        if(ctx.getLogService() != null) {
            ctx.getLogService().log(LogService.LOG_DEBUG, "adding task to current cycle:" + t);
        }
        synchronized (tasks) {
            tasks.add(t);
        }
    }
    
    /** Register a new resource, will be processed on the next cycle */
    void addNewResource(RegisteredResource r) {
        synchronized (newResources) {
            newResources.add(r);
        }
    }
    
    private void addRegisteredResource(RegisteredResource r) {
        TreeSet<RegisteredResource> t = registeredResources.get(r.getEntityId());
        if(t == null) {
            t = new TreeSet<RegisteredResource>(new RegisteredResourceComparator());
            registeredResources.put(r.getEntityId(), t);
        }
        t.add(r);

    }
    
    private void mergeNewResources() {
        synchronized (newResources) {
            for(RegisteredResource r : newResources) {
            	addRegisteredResource(r);
            }
            newResources.clear();
        }
    }
    
    private void computeTasks() {
        // Walk the list of entities, and create appropriate OSGi tasks for each group
        for(TreeSet<RegisteredResource> group : registeredResources.values()) {
        	if(group.first().getResourceType().equals(RegisteredResource.ResourceType.BUNDLE)) {
        		bundleTaskCreator.createTasks(group, tasks);
        	} else {
        		throw new IllegalArgumentException("No TaskCreator for resource type "+ group.first().getResourceType());
        	} 
        }
    }
    
    private void executeTasks() throws Exception {
        while(!tasks.isEmpty()) {
            OsgiInstallerTask t = null;
            synchronized (tasks) {
                t = tasks.first();
            }
            t.execute(ctx);
            synchronized (tasks) {
                tasks.remove(t);
            }
        }
    }
    
}