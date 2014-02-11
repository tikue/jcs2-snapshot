package org.apache.commons.jcs.engine.behavior;

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

import java.io.IOException;
import java.io.Serializable;

/**
 * Used to register interest in receiving cache changes. <br>
 * <br>
 * Note: server which implements this interface provides a local cache event
 * notification service, whereas server which implements IRmiCacheWatch provides
 * a remote cache event notification service.
 *
 */
public interface ICacheObserver
{
    /**
     * Subscribes to the specified cache.
     *
     * @param cacheName
     *            the specified cache.
     * @param obj
     *            object to notify for cache changes.
     * @throws IOException
     */
    <K extends Serializable, V extends Serializable> void addCacheListener( String cacheName, ICacheListener<K, V> obj )
        throws IOException;

    //, CacheNotFoundException;

    /**
     * Subscribes to all caches.
     *
     * @param obj
     *            object to notify for all cache changes.
     * @throws IOException
     */
    <K extends Serializable, V extends Serializable> void addCacheListener( ICacheListener<K, V> obj )
        throws IOException;

    /**
     * Unsubscribes from the specified cache.
     * @param cacheName
     *
     * @param obj
     *            existing subscriber.
     * @throws IOException
     */
    <K extends Serializable, V extends Serializable> void removeCacheListener( String cacheName, ICacheListener<K, V> obj )
        throws IOException;

    /**
     * Unsubscribes from all caches.
     *
     * @param obj
     *            existing subscriber.
     * @throws IOException
     */
    <K extends Serializable, V extends Serializable> void removeCacheListener( ICacheListener<K, V> obj )
        throws IOException;
}
