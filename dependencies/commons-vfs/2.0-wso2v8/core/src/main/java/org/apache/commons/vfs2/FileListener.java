/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.vfs2;

/**
 * Listens for changes to a file.
 *
 * @author <a href="http://commons.apache.org/vfs/team-list.html">Commons VFS team</a>
 * @version $Revision: 1035154 $ $Date: 2010-11-15 11:58:06 +0530 (Mon, 15 Nov 2010) $
 */
public interface FileListener
{
    /**
     * Called when a file is created.
     * @param event The FileChangeEvent.
     * @throws Exception if an error occurs.
     */
    void fileCreated(FileChangeEvent event) throws Exception;

    /**
     * Called when a file is deleted.
     * @param event The FileChangeEvent.
     * @throws Exception if an error occurs.
     */
    void fileDeleted(FileChangeEvent event) throws Exception;

    /**
     * Called when a file is changed.<br />
     * This will only happen if you monitor the file using {@link FileMonitor}.
     * @param event The FileChangeEvent.
     * @throws Exception if an error occurs.
     */
    void fileChanged(FileChangeEvent event) throws Exception;
}
