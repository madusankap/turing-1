/*
 *
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
 *
 */
package org.wso2.andes.server.virtualhost.plugins;

import java.util.concurrent.TimeUnit;

import org.wso2.andes.server.plugins.Plugin;

public interface VirtualHostPlugin extends Runnable, Plugin
{
    /**
     * Long value representing the delay between repeats
     *
     * @return
     */
    public long getDelay();

    /**
     * Option to specify what the delay value represents
     * @see java.util.concurrent.TimeUnit for valid value.
     * @return
     */
    public TimeUnit getTimeUnit();
}
