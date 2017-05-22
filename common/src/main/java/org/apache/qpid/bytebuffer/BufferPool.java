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
package org.apache.qpid.bytebuffer;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

class BufferPool
{
    private final int _maxSize;
    private final ConcurrentLinkedQueue<ByteBuffer> _pooledBuffers = new ConcurrentLinkedQueue<>();
    private final AtomicInteger _size = new AtomicInteger();

    BufferPool(final int maxSize)
    {
        _maxSize = maxSize;
    }

    ByteBuffer getBuffer()
    {
        final ByteBuffer buffer = _pooledBuffers.poll();
        if (buffer != null)
        {
            _size.decrementAndGet();
        }
        return buffer;
    }

    void returnBuffer(ByteBuffer buf)
    {
        buf.clear();
        if (size() < _maxSize)
        {
            _pooledBuffers.add(buf);
            _size.incrementAndGet();
        }
    }

    public int getMaxSize()
    {
        return _maxSize;
    }

    public int size()
    {
        return _size.get();
    }
}