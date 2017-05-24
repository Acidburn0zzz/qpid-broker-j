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
package org.apache.qpid.server.model;

import java.util.Collection;
import java.util.Date;

@ManagedObject( creatable = false, amqpName = "org.apache.qpid.Connection")
public interface Connection<X extends Connection<X>> extends ConfiguredObject<X>
{

    // Attributes

    String STATE = "state";

    String CLIENT_ID = "clientId";
    String CLIENT_VERSION = "clientVersion";
    String INCOMING = "incoming";
    String LOCAL_ADDRESS = "localAddress";
    String PRINCIPAL = "principal";
    String PROPERTIES = "properties";
    String REMOTE_ADDRESS = "remoteAddress";
    String REMOTE_PROCESS_NAME = "remoteProcessName";
    String REMOTE_PROCESS_PID = "remoteProcessPid";
    String SESSION_COUNT_LIMIT = "sessionCountLimit";
    String TRANSPORT = "transport";
    String PORT = "port";


    String MAX_UNCOMMITTED_IN_MEMORY_SIZE = "connection.maxUncommittedInMemorySize";

    @ManagedContextDefault(name = MAX_UNCOMMITTED_IN_MEMORY_SIZE,
            description = "Defines the maximum limit of total messages sizes (in bytes) from uncommitted transactions"
                          + " which connection can hold in memory. If limit is breached, all messages from"
                          + " connection in-flight transactions are flowed to disk including those arriving"
                          + " after breaching the limit.")
    long DEFAULT_MAX_UNCOMMITTED_IN_MEMORY_SIZE = 10l * 1024l * 1024l;


    String CLOSE_RESPONSE_TIMEOUT = "connection.closeResponseTimeout";
    @ManagedContextDefault(name = CLOSE_RESPONSE_TIMEOUT)
    long DEFAULT_CLOSE_RESPONSE_TIMEOUT = 2000L;

    String MAX_MESSAGE_SIZE = "qpid.max_message_size";
    @ManagedContextDefault(name = MAX_MESSAGE_SIZE)
    int DEFAULT_MAX_MESSAGE_SIZE = 100 * 1024 * 1024;

    @DerivedAttribute
    String getClientId();

    @DerivedAttribute
    String getClientVersion();

    @DerivedAttribute
    String getClientProduct();

    @DerivedAttribute
    boolean isIncoming();

    @DerivedAttribute
    String getLocalAddress();

    @DerivedAttribute
    String getPrincipal();

    @DerivedAttribute
    String getRemoteAddress();

    @DerivedAttribute
    String getRemoteProcessName();

    @DerivedAttribute
    String getRemoteProcessPid();

    @DerivedAttribute
    long getSessionCountLimit();

    @DerivedAttribute
    Transport getTransport();

    @DerivedAttribute
    String getTransportInfo();

    @DerivedAttribute
    Protocol getProtocol();

    @DerivedAttribute
    NamedAddressSpace getAddressSpace();

    @DerivedAttribute
    Port<?> getPort();

    @DerivedAttribute(description = "The maximum size in bytes that uncommitted transactions associated with this connection"
                                    + " may grow before the messages contained within the transactions will be flowed to disk. "
                                    + " Disabled if negative.")
    long getMaxUncommittedInMemorySize();

    // currently this reports inbound message content size without header.
    // See also QPID-7689: https://issues.apache.org/jira/browse/QPID-7689?focusedCommentId=16022923#comment-16022923
    @ManagedStatistic(statisticType = StatisticType.CUMULATIVE, units = StatisticUnit.BYTES, label = "Inbound")
    long getBytesIn();

    // currently this reports outbound  message content size without header.
    // See also QPID-7689: https://issues.apache.org/jira/browse/QPID-7689?focusedCommentId=16022923#comment-16022923
    @ManagedStatistic(statisticType = StatisticType.CUMULATIVE, units = StatisticUnit.BYTES, label = "Outbound")
    long getBytesOut();

    @ManagedStatistic(statisticType = StatisticType.CUMULATIVE, units = StatisticUnit.MESSAGES, label = "Inbound")
    long getMessagesIn();

    @ManagedStatistic(statisticType = StatisticType.CUMULATIVE, units = StatisticUnit.MESSAGES, label = "Outbound")
    long getMessagesOut();

    @ManagedStatistic(statisticType = StatisticType.POINT_IN_TIME, units = StatisticUnit.ABSOLUTE_TIME, label = "Last I/O time")
    Date getLastIoTime();

    @ManagedStatistic(statisticType = StatisticType.POINT_IN_TIME, units = StatisticUnit.COUNT, label = "Sessions")
    int getSessionCount();

    //children
    Collection<Session> getSessions();

}
