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
package org.apache.qpid.tests.protocol.v0_8;

import org.apache.qpid.server.protocol.v0_8.transport.AMQBody;
import org.apache.qpid.server.protocol.v0_8.transport.AMQDataBlock;
import org.apache.qpid.server.protocol.v0_8.transport.AMQFrame;
import org.apache.qpid.server.protocol.v0_8.transport.ConnectionOpenOkBody;
import org.apache.qpid.server.protocol.v0_8.transport.ConnectionStartBody;
import org.apache.qpid.server.protocol.v0_8.transport.ConnectionTuneBody;

public class Interaction extends org.apache.qpid.tests.protocol.Interaction<Interaction>
{

    private int _channelId;
    private int _maximumPayloadSize = 512;

    Interaction(final FrameTransport transport)
    {
        super(transport);
    }

    @Override
    protected byte[] getProtocolHeader()
    {
        return getTransport().getProtocolHeader();
    }

    @Override
    protected Interaction getInteraction()
    {
        return this;
    }

    public Interaction sendPerformative(final AMQBody amqBody) throws Exception
    {
        return sendPerformative(getChannelId(), amqBody);
    }

    public Interaction sendPerformative(int channel, final AMQBody amqBody) throws Exception
    {
        final AMQFrame frameBody = new AMQFrame(channel, amqBody);
        sendPerformativeAndChainFuture(frameBody);
        return this;
    }

    public Interaction sendPerformative(final AMQDataBlock dataBlock) throws Exception
    {
        sendPerformativeAndChainFuture(dataBlock);
        return this;
    }

    public Interaction openAnonymousConnection() throws Exception
    {
        return this.negotiateProtocol().consumeResponse(ConnectionStartBody.class)
                   .connection().startOkMechanism("ANONYMOUS").startOk().consumeResponse(ConnectionTuneBody.class)
                   .connection().tuneOk()
                   .connection().open().consumeResponse(ConnectionOpenOkBody.class);

    }

    public ConnectionInteraction connection()
    {
        return new ConnectionInteraction(this);
    }

    public ChannelInteraction channel()
    {
        return new ChannelInteraction(this);
    }

    public QueueInteraction queue()
    {
        return new QueueInteraction(this);
    }

    public int getChannelId()
    {
        return _channelId;
    }

    public int getMaximumFrameSize()
    {
        return _maximumPayloadSize;
    }

    public BasicInteraction basic()
    {
        return new BasicInteraction(this);
    }
}
