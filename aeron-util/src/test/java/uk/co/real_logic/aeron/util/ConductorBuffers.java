/*
 * Copyright 2014 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.aeron.util;

import org.junit.rules.ExternalResource;
import uk.co.real_logic.aeron.util.concurrent.AtomicBuffer;
import uk.co.real_logic.aeron.util.concurrent.ringbuffer.ManyToOneRingBuffer;
import uk.co.real_logic.aeron.util.concurrent.ringbuffer.RingBuffer;

import java.io.File;
import java.nio.ByteBuffer;

import static uk.co.real_logic.aeron.util.concurrent.ringbuffer.BufferDescriptor.TRAILER_LENGTH;

public class ConductorBuffers extends ExternalResource
{
    public static final int BUFFER_SIZE = 512 + TRAILER_LENGTH;

    private final String adminDir;
    private final int bufferSize;

    private ConductorBufferStrategy creatingStrategy;
    private ConductorBufferStrategy mappingStrategy;
    private ByteBuffer toMediaDriver;
    private ByteBuffer toApi;

    public ConductorBuffers()
    {
        this(BUFFER_SIZE);
    }

    public ConductorBuffers(int bufferSize)
    {
        this.bufferSize = bufferSize;
        adminDir = IoUtil.tmpDir() + "/conductor";
    }

    protected void before() throws Exception
    {
        final File dir = new File(adminDir);
        if (dir.exists())
        {
            IoUtil.delete(dir, false);
        }

        IoUtil.ensureDirectoryExists(dir, "conductor dir");
        creatingStrategy = new CreatingConductorBufferStrategy(adminDir, bufferSize);
        mappingStrategy = new MappingConductorBufferStrategy(adminDir);
        toMediaDriver = creatingStrategy.toMediaDriver();
        toApi = creatingStrategy.toApi();
    }

    protected void after()
    {
        // Force unmapping of byte buffers to allow deletion
        creatingStrategy.close();
    }

    public RingBuffer toMediaDriver()
    {
        return ringBuffer(toMediaDriver);
    }

    public RingBuffer toApi()
    {
        return ringBuffer(toApi);
    }

    private ManyToOneRingBuffer ringBuffer(final ByteBuffer buffer)
    {
        return new ManyToOneRingBuffer(new AtomicBuffer(buffer));
    }

    public ConductorBufferStrategy strategy()
    {
        return creatingStrategy;
    }

    public String adminDir()
    {
        return adminDir;
    }

    public RingBuffer mappedToMediaDriver()
    {
        return suppress(() -> ringBuffer(toMediaDriver));
    }

    public RingBuffer mappedToApi()
    {
        return suppress(() -> ringBuffer(toApi));
    }

    private static interface ExceptionalSupplier<T>
    {
        public T supply() throws Exception;
    }

    private <T> T suppress(ExceptionalSupplier<T> supplier)
    {
        try
        {
            return supplier.supply();
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }
}