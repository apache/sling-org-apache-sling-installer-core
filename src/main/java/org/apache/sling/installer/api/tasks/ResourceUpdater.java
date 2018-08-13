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
package org.apache.sling.installer.api.tasks;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * A resource updater can be used if the processing of resources changes
 * with regards to the entity id and the alias. This can be used by plugin
 * implementations to update this information.
 * The resource updater is called once when it is registered and picked up
 * by the OSGi installer with all known resource groups at this point of time.
 * The resource updater might be called more than once and therefore should
 * take action to either process the group as quickly as possible on further
 * invocation or unregister itself.
 * @since 1.5.0
 */
@ConsumerType
public interface ResourceUpdater {

    /**
     * Inform the resource handler about all known group
     *
     * @param groups The groups of resources to update
     */
    void update(@NotNull Collection<UpdatableResourceGroup> groups);
}
