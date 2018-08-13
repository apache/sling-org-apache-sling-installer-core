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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This is a group of resources all pointing to the same artifact,
 * but maybe in different versions or locations.
 * This object allows to update the alias and the resource id of
 * the group as a whole.
 * @since 1.5.0
 */
@ProviderType
public interface UpdatableResourceGroup {

    /**
     * Get the current alias for this group.
     * @return The alias or {@code null}.
     */
    @Nullable String getAlias();

    /**
     * Set the current alias for this group.
     * @param value A new alias or {@code null}.
     */
    void setAlias(@Nullable String value);

    /**
     * Get the resource type of the group
     * @return The resource type.
     */
    @NotNull String getResourceType();

    /**
     * Get the unique id
     * @return The unique id
     */
    @NotNull String getId();

    /**
     * Set a new unique id.
     * @param id The unique id
     * @throws IllegalArgumentException If {@code id} is {@code null}.
     */
    void setId(@NotNull String id);

    /**
     * Update the OSGi installer with the new information
     * If this method is not called, changed made through the setter methods
     * are discarded.
     */
    void update();
}
