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

package org.apache.ignite.internal.processors.cache;

import java.util.concurrent.atomic.AtomicLong;
import javax.cache.Cache;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.processors.cache.database.CacheDataRow;
import org.apache.ignite.internal.processors.cache.database.MetaStore;
import org.apache.ignite.internal.processors.cache.database.RowStore;
import org.apache.ignite.internal.processors.cache.database.freelist.FreeList;
import org.apache.ignite.internal.processors.cache.database.tree.reuse.ReuseList;
import org.apache.ignite.internal.processors.cache.distributed.dht.GridDhtLocalPartition;
import org.apache.ignite.internal.processors.cache.version.GridCacheVersion;
import org.apache.ignite.internal.util.lang.GridCloseableIterator;
import org.apache.ignite.internal.util.lang.GridCursor;
import org.apache.ignite.internal.util.lang.GridIterator;
import org.apache.ignite.internal.util.lang.IgniteInClosure2X;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
@SuppressWarnings("WeakerAccess")
public interface IgniteCacheOffheapManager extends GridCacheManager {
    /**
     * Partition counter update callback. May be overridden by plugin-provided subclasses.
     *
     * @param part Partition.
     * @param cntr Partition counter.
     */
    public void onPartitionCounterUpdated(int part, long cntr);

    /**
     * Partition counter provider. May be overridden by plugin-provided subclasses.
     *
     * @param part Partition ID.
     * @return Last updated counter.
     */
    public long lastUpdatedPartitionCounter(int part);

    /**
     * @return Reuse list.
     */
    public ReuseList reuseList();

    /**
     * @return Free list.
     */
    public FreeList freeList();

    /**
     * @return Meta store.
     */
    public MetaStore meta();

    /**
     * @param entry Cache entry.
     * @return Cached row, if available, null otherwise.
     * @throws IgniteCheckedException If failed.
     */
    @Nullable public CacheDataRow read(GridCacheMapEntry entry) throws IgniteCheckedException;

    /**
     * @param p Partition.
     * @param lsnr Listener.
     * @return Data store.
     * @throws IgniteCheckedException If failed.
     */
    public CacheDataStore createCacheDataStore(int p, CacheDataStore.SizeTracker lsnr) throws IgniteCheckedException;

    /**
     * @param p Partition ID.
     * @param store Data store.
     */
    public void destroyCacheDataStore(int p, CacheDataStore store) throws IgniteCheckedException;

    /**
     * TODO: GG-10884, used on only from initialValue.
     */
    public boolean containsKey(GridCacheMapEntry entry);

    /**
     * @param c Closure.
     * @throws IgniteCheckedException If failed.
     */
    public void expire(IgniteInClosure2X<GridCacheEntryEx, GridCacheVersion> c) throws IgniteCheckedException;

    /**
     * Gets the number of entries pending expire.
     *
     * @return Number of pending entries.
     * @throws IgniteCheckedException If failed to get number of pending entries.
     */
    public long expiredSize() throws IgniteCheckedException;

    /**
     * @param key  Key.
     * @param val  Value.
     * @param ver  Version.
     * @param expireTime Expire time.
     * @param partId Partition number.
     * @param part Partition.
     * @throws IgniteCheckedException If failed.
     */
    public void update(
        KeyCacheObject key,
        CacheObject val,
        GridCacheVersion ver,
        long expireTime,
        int partId,
        GridDhtLocalPartition part
    ) throws IgniteCheckedException;

    /**
     * @param key Key.
     * @param partId Partition number.
     * @param part Partition.
     * @throws IgniteCheckedException If failed.
     */
    public void remove(
        KeyCacheObject key,
        int partId,
        GridDhtLocalPartition part
    ) throws IgniteCheckedException;

    /**
     * @param ldr Class loader.
     * @return Number of undeployed entries.
     */
    public int onUndeploy(ClassLoader ldr);

    /**
     * @param primary Primary entries flag.
     * @param backup Backup entries flag.
     * @param topVer Topology version.
     * @return Rows iterator.
     * @throws IgniteCheckedException If failed.
     */
    public GridIterator<CacheDataRow> iterator(boolean primary, boolean backup, final AffinityTopologyVersion topVer)
        throws IgniteCheckedException;

    /**
     * @param part Partition.
     * @return Partition data iterator.
     * @throws IgniteCheckedException If failed.
     */
    public GridIterator<CacheDataRow> iterator(final int part) throws IgniteCheckedException;

    /**
     * @param primary Primary entries flag.
     * @param backup Backup entries flag.
     * @param topVer Topology version.
     * @param keepBinary Keep binary flag.
     * @return Entries iterator.
     * @throws IgniteCheckedException If failed.
     */
    public <K, V> GridCloseableIterator<Cache.Entry<K, V>> entriesIterator(final boolean primary,
        final boolean backup,
        final AffinityTopologyVersion topVer,
        final boolean keepBinary) throws IgniteCheckedException;

    /**
     * @param part Partition.
     * @return Iterator.
     * @throws IgniteCheckedException If failed.
     */
    public GridCloseableIterator<KeyCacheObject> keysIterator(final int part) throws IgniteCheckedException;

    /**
     * @param primary Primary entries flag.
     * @param backup Backup entries flag.
     * @param topVer Topology version.
     * @return Entries count.
     * @throws IgniteCheckedException If failed.
     */
    public long entriesCount(boolean primary, boolean backup, AffinityTopologyVersion topVer)
        throws IgniteCheckedException;

    /**
     * Clears offheap entries.
     *
     * @param readers {@code True} to clear readers.
     */
    public void clear(boolean readers);

    /**
     * @param part Partition.
     * @return Number of entries in given partition.
     */
    public long entriesCount(int part);

    /**
     * @return Offheap allocated size.
     */
    public long offHeapAllocatedSize();

    /**
     * @return Global remove ID counter.
     */
    public AtomicLong globalRemoveId();

    // TODO GG-10884: moved from GridCacheSwapManager.
    void writeAll(Iterable<GridCacheBatchSwapEntry> swapped) throws IgniteCheckedException;

    /**
     *
     */
    interface CacheDataStore {

        int partId();

        /**
         * @return Store name.
         */
        String name();

        /**
         * @param key Key.
         * @param part Partition.
         * @param val Value.
         * @param ver Version.
         * @param expireTime Expire time.
         * @throws IgniteCheckedException If failed.
         */
        void update(KeyCacheObject key,
            int part,
            CacheObject val,
            GridCacheVersion ver,
            long expireTime) throws IgniteCheckedException;

        /**
         * @param key Key.
         * @param partId Partition number.
         * @throws IgniteCheckedException If failed.
         */
        public void remove(KeyCacheObject key, int partId) throws IgniteCheckedException;

        /**
         * @param key Key.
         * @return Data row.
         * @throws IgniteCheckedException If failed.
         */
        public CacheDataRow find(KeyCacheObject key) throws IgniteCheckedException;

        /**
         * @return Data cursor.
         * @throws IgniteCheckedException If failed.
         */
        public GridCursor<? extends CacheDataRow> cursor() throws IgniteCheckedException;

        /**
         * Destroys the tree associated with the store.
         *
         * @throws IgniteCheckedException If failed.
         */
        public void destroy() throws IgniteCheckedException;

        /**
         * @return Row store.
         */
        public RowStore rowStore();

        public int size();

        public long updateCounter();

        /**
         * Data store size tracker.
         */
        interface SizeTracker {
            /**
             * On new entry inserted.
             */
            void onInsert();

            /**
             * On entry removed.
             */
            void onRemove();

            /**
             * @return Size.
             */
            int size();

            /**
             * @return Update counter.
             */
            long updateCounter();
        }
    }
}
