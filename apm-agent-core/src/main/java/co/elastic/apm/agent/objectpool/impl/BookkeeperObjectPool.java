/*-
 * #%L
 * Elastic APM Java agent
 * %%
 * Copyright (C) 2018 - 2020 Elastic and contributors
 * %%
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * #L%
 */
package co.elastic.apm.agent.objectpool.impl;

import co.elastic.apm.agent.objectpool.ObjectPool;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * {@link ObjectPool} wrapper implementation that keeps track of all created object instances, and thus allows to check
 * for any pooled object leak. Should only be used for testing as it keeps a reference to all in-flight pooled objects.
 *
 * @param <T> pooled object type
 */
public class BookkeeperObjectPool<T> implements ObjectPool<T> {

    private ObjectPool<T> pool;
    private Set<T> toReturn = Collections.<T>newSetFromMap(new IdentityHashMap<T, Boolean>());

    static {
        boolean isTest = false;
        assert isTest = true;
        if (!isTest) {
            throw new IllegalStateException("this object pool should not be used outside tests");
        }
    }

    /**
     * @param pool pool to wrap
     */
    public BookkeeperObjectPool(ObjectPool<T> pool) {
        this.pool = pool;
    }

    @Override
    public T createInstance() {
        T instance = pool.createInstance();
        toReturn.add(instance);
        return instance;
    }

    @Override
    public void recycle(T obj) {
        if (!toReturn.contains(obj)) {
            throw new IllegalStateException("trying to recycle object that has not been taken from this pool or has already been returned");
        }
        pool.recycle(obj);
        toReturn.remove(obj);
    }

    @Override
    public int getObjectsInPool() {
        return pool.getObjectsInPool();
    }

    @Override
    public long getGarbageCreated() {
        return pool.getGarbageCreated();
    }

    /**
     * @return objects that have been created by pool but haven't been returned yet
     */
    public Collection<T> getRecyclablesToReturn() {
        return toReturn;
    }
}
