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
package org.apache.qpid.server.management.plugin.servlet.rest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.Session;

import org.apache.qpid.client.AMQConnection;
import org.apache.qpid.server.model.Exchange;
import org.apache.qpid.server.model.Queue;
import org.apache.qpid.server.model.VirtualHost;
import org.apache.qpid.server.queue.AMQQueueFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;

public class VirtualHostRestTest extends QpidRestTestCase
{
    private static final String VIRTUALHOST_EXCHANGES_ATTRIBUTE = "exchanges";
    public static final String VIRTUALHOST_QUEUES_ATTRIBUTE = "queues";
    public static final String VIRTUALHOST_CONNECTIONS_ATTRIBUTE = "connections";

    private AMQConnection _connection;

    public void testGet() throws Exception
    {
        List<Map<String, Object>> hosts = getRestTestHelper().getJsonAsList("/rest/virtualhost/");
        assertNotNull("Hosts data cannot be null", hosts);
        assertEquals("Unexpected number of hosts", EXPECTED_HOSTS.length, hosts.size());
        for (String hostName : EXPECTED_HOSTS)
        {
            Map<String, Object> host = getRestTestHelper().find("name", hostName, hosts);
            Asserts.assertVirtualHost(hostName, host);
        }
    }

    public void testGetHost() throws Exception
    {
        // create AMQP connection to get connection JSON details
        _connection = (AMQConnection) getConnection();
        _connection.createSession(true, Session.SESSION_TRANSACTED);

        Map<String, Object> hostDetails = getRestTestHelper().getJsonAsSingletonList("/rest/virtualhost/test");
        Asserts.assertVirtualHost("test", hostDetails);

        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) hostDetails.get(Asserts.STATISTICS_ATTRIBUTE);
        assertEquals("Unexpected number of exchanges in statistics", EXPECTED_EXCHANGES.length, statistics.get(VirtualHost.EXCHANGE_COUNT));
        assertEquals("Unexpected number of queues in statistics", EXPECTED_QUEUES.length, statistics.get(VirtualHost.QUEUE_COUNT));
        assertEquals("Unexpected number of connections in statistics", 1, statistics.get(VirtualHost.CONNECTION_COUNT));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> exchanges = (List<Map<String, Object>>) hostDetails.get(VIRTUALHOST_EXCHANGES_ATTRIBUTE);
        assertEquals("Unexpected number of exchanges", EXPECTED_EXCHANGES.length, exchanges.size());
        Asserts.assertDurableExchange("amq.fanout", "fanout", getRestTestHelper().find(Exchange.NAME, "amq.fanout", exchanges));
        Asserts.assertDurableExchange("amq.topic", "topic", getRestTestHelper().find(Exchange.NAME, "amq.topic", exchanges));
        Asserts.assertDurableExchange("amq.direct", "direct", getRestTestHelper().find(Exchange.NAME, "amq.direct", exchanges));
        Asserts.assertDurableExchange("amq.match", "headers", getRestTestHelper().find(Exchange.NAME, "amq.match", exchanges));
        Asserts.assertDurableExchange("<<default>>", "direct", getRestTestHelper().find(Exchange.NAME, "<<default>>", exchanges));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> queues = (List<Map<String, Object>>) hostDetails.get(VIRTUALHOST_QUEUES_ATTRIBUTE);
        assertEquals("Unexpected number of queues", EXPECTED_QUEUES.length, queues.size());
        Map<String, Object> queue = getRestTestHelper().find(Queue.NAME,  "queue", queues);
        Map<String, Object> ping = getRestTestHelper().find(Queue.NAME, "ping", queues);
        Asserts.assertQueue("queue", "standard", queue);
        Asserts.assertQueue("ping", "standard", ping);
        assertEquals("Unexpected value of queue attribute " + Queue.DURABLE, Boolean.FALSE, queue.get(Queue.DURABLE));
        assertEquals("Unexpected value of queue attribute " + Queue.DURABLE, Boolean.FALSE, ping.get(Queue.DURABLE));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> connections = (List<Map<String, Object>>) hostDetails
                .get(VIRTUALHOST_CONNECTIONS_ATTRIBUTE);
        assertEquals("Unexpected number of connections", 1, connections.size());
        Asserts.assertConnection(connections.get(0), _connection);
    }

    public void testPutCreateQueue() throws Exception
    {
        String queueName = getTestQueueName();

        createQueue(queueName + "-standard", "standard", null);

        Map<String, Object> sortedQueueAttributes = new HashMap<String, Object>();
        sortedQueueAttributes.put(Queue.SORT_KEY, "sortme");
        createQueue(queueName + "-sorted", "sorted", sortedQueueAttributes);

        Map<String, Object> priorityQueueAttributes = new HashMap<String, Object>();
        priorityQueueAttributes.put(Queue.PRIORITIES, 10);
        createQueue(queueName + "-priority", "priority", priorityQueueAttributes);

        Map<String, Object> lvqQueueAttributes = new HashMap<String, Object>();
        lvqQueueAttributes.put(Queue.LVQ_KEY, "LVQ");
        createQueue(queueName + "-lvq", "lvq", lvqQueueAttributes);

        Map<String, Object> hostDetails = getRestTestHelper().getJsonAsSingletonList("/rest/virtualhost/test");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> queues = (List<Map<String, Object>>) hostDetails.get(VirtualHostRestTest.VIRTUALHOST_QUEUES_ATTRIBUTE);
        Map<String, Object> standardQueue = getRestTestHelper().find(Queue.NAME, queueName + "-standard" , queues);
        Map<String, Object> sortedQueue = getRestTestHelper().find(Queue.NAME, queueName + "-sorted" , queues);
        Map<String, Object> priorityQueue = getRestTestHelper().find(Queue.NAME, queueName + "-priority" , queues);
        Map<String, Object> lvqQueue = getRestTestHelper().find(Queue.NAME, queueName + "-lvq" , queues);

        Asserts.assertQueue(queueName + "-standard", "standard", standardQueue);
        Asserts.assertQueue(queueName + "-sorted", "sorted", sortedQueue);
        Asserts.assertQueue(queueName + "-priority", "priority", priorityQueue);
        Asserts.assertQueue(queueName + "-lvq", "lvq", lvqQueue);

        assertEquals("Unexpected value of queue attribute " + Queue.DURABLE, Boolean.TRUE, standardQueue.get(Queue.DURABLE));
        assertEquals("Unexpected value of queue attribute " + Queue.DURABLE, Boolean.TRUE, sortedQueue.get(Queue.DURABLE));
        assertEquals("Unexpected value of queue attribute " + Queue.DURABLE, Boolean.TRUE, priorityQueue.get(Queue.DURABLE));
        assertEquals("Unexpected value of queue attribute " + Queue.DURABLE, Boolean.TRUE, lvqQueue.get(Queue.DURABLE));

        assertEquals("Unexpected sorted key attribute", "sortme", sortedQueue.get(Queue.SORT_KEY));
        assertEquals("Unexpected lvq key attribute", "LVQ", lvqQueue.get(Queue.LVQ_KEY));
        assertEquals("Unexpected priorities key attribute", 10, priorityQueue.get(Queue.PRIORITIES));
    }

    public void testPutCreateExchange() throws Exception
    {
        String exchangeName = getTestName();

        createExchange(exchangeName + "-direct", "direct");
        createExchange(exchangeName + "-topic", "topic");
        createExchange(exchangeName + "-headers", "headers");
        createExchange(exchangeName + "-fanout", "fanout");

        Map<String, Object> hostDetails = getRestTestHelper().getJsonAsSingletonList("/rest/virtualhost/test");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> exchanges = (List<Map<String, Object>>) hostDetails.get(VirtualHostRestTest.VIRTUALHOST_EXCHANGES_ATTRIBUTE);
        Map<String, Object> directExchange = getRestTestHelper().find(Queue.NAME, exchangeName + "-direct" , exchanges);
        Map<String, Object> topicExchange = getRestTestHelper().find(Queue.NAME, exchangeName + "-topic" , exchanges);
        Map<String, Object> headersExchange = getRestTestHelper().find(Queue.NAME, exchangeName + "-headers" , exchanges);
        Map<String, Object> fanoutExchange = getRestTestHelper().find(Queue.NAME, exchangeName + "-fanout" , exchanges);

        Asserts.assertDurableExchange(exchangeName + "-direct", "direct", directExchange);
        Asserts.assertDurableExchange(exchangeName + "-topic", "topic", topicExchange);
        Asserts.assertDurableExchange(exchangeName + "-headers", "headers", headersExchange);
        Asserts.assertDurableExchange(exchangeName + "-fanout", "fanout", fanoutExchange);

        assertEquals("Unexpected value of queue attribute " + Queue.DURABLE, Boolean.TRUE, directExchange.get(Queue.DURABLE));
        assertEquals("Unexpected value of queue attribute " + Queue.DURABLE, Boolean.TRUE, topicExchange.get(Queue.DURABLE));
        assertEquals("Unexpected value of queue attribute " + Queue.DURABLE, Boolean.TRUE, headersExchange.get(Queue.DURABLE));
        assertEquals("Unexpected value of queue attribute " + Queue.DURABLE, Boolean.TRUE, fanoutExchange.get(Queue.DURABLE));

    }

    public void testPutCreateLVQWithoutKey() throws Exception
    {
        String queueName = getTestQueueName()+ "-lvq";
        createQueue(queueName, "lvq", null);

        Map<String, Object> hostDetails = getRestTestHelper().getJsonAsSingletonList("/rest/virtualhost/test");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> queues = (List<Map<String, Object>>) hostDetails.get(VirtualHostRestTest.VIRTUALHOST_QUEUES_ATTRIBUTE);
        Map<String, Object> lvqQueue = getRestTestHelper().find(Queue.NAME, queueName  , queues);

        Asserts.assertQueue(queueName , "lvq", lvqQueue);
        assertEquals("Unexpected value of queue attribute " + Queue.DURABLE, Boolean.TRUE, lvqQueue.get(Queue.DURABLE));
        assertEquals("Unexpected lvq key attribute", AMQQueueFactory.QPID_LVQ_KEY, lvqQueue.get(Queue.LVQ_KEY));
    }

    public void testPutCreateSortedQueueWithoutKey() throws Exception
    {
        String queueName = getTestQueueName() + "-sorted";
        int responseCode = tryCreateQueue(queueName, "sorted", null);
        assertEquals("Unexpected response code", 409, responseCode);

        Map<String, Object> hostDetails = getRestTestHelper().getJsonAsSingletonList("/rest/virtualhost/test");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> queues = (List<Map<String, Object>>) hostDetails.get(VirtualHostRestTest.VIRTUALHOST_QUEUES_ATTRIBUTE);
        Map<String, Object> testQueue = getRestTestHelper().find(Queue.NAME, queueName  , queues);

        assertNull("Sorted queue without a key was created ", testQueue);
    }

    public void testPutCreatePriorityQueueWithoutKey() throws Exception
    {
        String queueName = getTestQueueName()+ "-priority";
        createQueue(queueName, "priority", null);

        Map<String, Object> hostDetails = getRestTestHelper().getJsonAsSingletonList("/rest/virtualhost/test");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> queues = (List<Map<String, Object>>) hostDetails.get(VirtualHostRestTest.VIRTUALHOST_QUEUES_ATTRIBUTE);
        Map<String, Object> priorityQueue = getRestTestHelper().find(Queue.NAME, queueName  , queues);

        Asserts.assertQueue(queueName , "priority", priorityQueue);
        assertEquals("Unexpected value of queue attribute " + Queue.DURABLE, Boolean.TRUE, priorityQueue.get(Queue.DURABLE));
        assertEquals("Unexpected number of priorities", 10, priorityQueue.get(Queue.PRIORITIES));
    }

    public void testPutCreateStandardQueueWithoutType() throws Exception
    {
        String queueName = getTestQueueName();
        createQueue(queueName, null, null);

        Map<String, Object> hostDetails = getRestTestHelper().getJsonAsSingletonList("/rest/virtualhost/test");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> queues = (List<Map<String, Object>>) hostDetails.get(VirtualHostRestTest.VIRTUALHOST_QUEUES_ATTRIBUTE);
        Map<String, Object> queue = getRestTestHelper().find(Queue.NAME, queueName  , queues);

        Asserts.assertQueue(queueName , "standard", queue);
    }

    public void testPutCreateQueueOfUnsupportedType() throws Exception
    {
        String queueName = getTestQueueName();
        int responseCode = tryCreateQueue(queueName, "unsupported", null);
        assertEquals("Unexpected response code", 409, responseCode);

        Map<String, Object> hostDetails = getRestTestHelper().getJsonAsSingletonList("/rest/virtualhost/test");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> queues = (List<Map<String, Object>>) hostDetails.get(VirtualHostRestTest.VIRTUALHOST_QUEUES_ATTRIBUTE);
        Map<String, Object> queue = getRestTestHelper().find(Queue.NAME, queueName  , queues);

        assertNull("Queue of unsupported type was created", queue);
    }

    public void testDeleteQueue() throws Exception
    {
        String queueName = getTestQueueName();
        createQueue(queueName, null, null);

        HttpURLConnection connection = getRestTestHelper().openManagementConnection("/rest/queue/test/" + queueName, "DELETE");
        connection.connect();
        assertEquals("Unexpected response code", 200, connection.getResponseCode());
        List<Map<String, Object>> queues = getRestTestHelper().getJsonAsList("/rest/queue/test/" + queueName);
        assertEquals("Queue should be deleted", 0, queues.size());
    }

    public void testDeleteQueueById() throws Exception
    {
        String queueName = getTestQueueName();
        createQueue(queueName, null, null);
        Map<String, Object> queueDetails = getRestTestHelper().getJsonAsSingletonList("/rest/queue/test/" + queueName);

        HttpURLConnection connection = getRestTestHelper().openManagementConnection("/rest/queue/test?id=" + queueDetails.get(Queue.ID), "DELETE");
        connection.connect();
        assertEquals("Unexpected response code", 200, connection.getResponseCode());
        List<Map<String, Object>> queues = getRestTestHelper().getJsonAsList("/rest/queue/test/" + queueName);
        assertEquals("Queue should be deleted", 0, queues.size());
    }

    public void testDeleteExchange() throws Exception
    {
        String exchangeName = getTestName();
        createExchange(exchangeName, "direct");

        HttpURLConnection connection = getRestTestHelper().openManagementConnection("/rest/exchange/test/" + exchangeName, "DELETE");
        connection.connect();
        assertEquals("Unexpected response code", 200, connection.getResponseCode());
        List<Map<String, Object>> queues = getRestTestHelper().getJsonAsList("/rest/exchange/test/" + exchangeName);
        assertEquals("Exchange should be deleted", 0, queues.size());
    }

    public void testDeleteExchangeById() throws Exception
    {
        String exchangeName = getTestName();
        createExchange(exchangeName, "direct");
        Map<String, Object> echangeDetails = getRestTestHelper().getJsonAsSingletonList("/rest/exchange/test/" + exchangeName);

        HttpURLConnection connection = getRestTestHelper().openManagementConnection("/rest/exchange/test?id=" + echangeDetails.get(Exchange.ID), "DELETE");
        connection.connect();
        assertEquals("Unexpected response code", 200, connection.getResponseCode());
        List<Map<String, Object>> queues = getRestTestHelper().getJsonAsList("/rest/exchange/test/" + exchangeName);
        assertEquals("Exchange should be deleted", 0, queues.size());
    }

    public void testPutCreateQueueWithAttributes() throws Exception
    {
        String queueName = getTestQueueName();

        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(Queue.ALERT_REPEAT_GAP, 1000);
        attributes.put(Queue.ALERT_THRESHOLD_MESSAGE_AGE, 3600000);
        attributes.put(Queue.ALERT_THRESHOLD_MESSAGE_SIZE, 1000000000);
        attributes.put(Queue.ALERT_THRESHOLD_QUEUE_DEPTH_MESSAGES, 800);
        attributes.put(Queue.MAXIMUM_DELIVERY_ATTEMPTS, 15);
        attributes.put(Queue.QUEUE_FLOW_CONTROL_SIZE_BYTES, 2000000000);
        attributes.put(Queue.QUEUE_FLOW_RESUME_SIZE_BYTES, 1500000000);

        createQueue(queueName + "-standard", "standard", attributes);

        Map<String, Object> sortedQueueAttributes = new HashMap<String, Object>();
        sortedQueueAttributes.putAll(attributes);
        sortedQueueAttributes.put(Queue.SORT_KEY, "sortme");
        createQueue(queueName + "-sorted", "sorted", sortedQueueAttributes);

        Map<String, Object> priorityQueueAttributes = new HashMap<String, Object>();
        priorityQueueAttributes.putAll(attributes);
        priorityQueueAttributes.put(Queue.PRIORITIES, 10);
        createQueue(queueName + "-priority", "priority", priorityQueueAttributes);

        Map<String, Object> lvqQueueAttributes = new HashMap<String, Object>();
        lvqQueueAttributes.putAll(attributes);
        lvqQueueAttributes.put(Queue.LVQ_KEY, "LVQ");
        createQueue(queueName + "-lvq", "lvq", lvqQueueAttributes);

        Map<String, Object> hostDetails = getRestTestHelper().getJsonAsSingletonList("/rest/virtualhost/test");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> queues = (List<Map<String, Object>>) hostDetails.get(VirtualHostRestTest.VIRTUALHOST_QUEUES_ATTRIBUTE);
        Map<String, Object> standardQueue = getRestTestHelper().find(Queue.NAME, queueName + "-standard" , queues);
        Map<String, Object> sortedQueue = getRestTestHelper().find(Queue.NAME, queueName + "-sorted" , queues);
        Map<String, Object> priorityQueue = getRestTestHelper().find(Queue.NAME, queueName + "-priority" , queues);
        Map<String, Object> lvqQueue = getRestTestHelper().find(Queue.NAME, queueName + "-lvq" , queues);

        attributes.put(Queue.DURABLE, Boolean.TRUE);
        Asserts.assertQueue(queueName + "-standard", "standard", standardQueue, attributes);
        Asserts.assertQueue(queueName + "-sorted", "sorted", sortedQueue, attributes);
        Asserts.assertQueue(queueName + "-priority", "priority", priorityQueue, attributes);
        Asserts.assertQueue(queueName + "-lvq", "lvq", lvqQueue, attributes);

        assertEquals("Unexpected sorted key attribute", "sortme", sortedQueue.get(Queue.SORT_KEY));
        assertEquals("Unexpected lvq key attribute", "LVQ", lvqQueue.get(Queue.LVQ_KEY));
        assertEquals("Unexpected priorities key attribute", 10, priorityQueue.get(Queue.PRIORITIES));
    }

    @SuppressWarnings("unchecked")
    public void testCreateQueueWithDLQEnabled() throws Exception
    {
        String queueName = getTestQueueName();

        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(AMQQueueFactory.X_QPID_DLQ_ENABLED, true);

        //verify the starting state
        Map<String, Object> hostDetails = getRestTestHelper().getJsonAsSingletonList("/rest/virtualhost/test");
        List<Map<String, Object>> queues = (List<Map<String, Object>>) hostDetails.get(VirtualHostRestTest.VIRTUALHOST_QUEUES_ATTRIBUTE);
        List<Map<String, Object>> exchanges = (List<Map<String, Object>>) hostDetails.get(VirtualHostRestTest.VIRTUALHOST_EXCHANGES_ATTRIBUTE);

        assertNull("queue should not have already been present", getRestTestHelper().find(Queue.NAME, queueName , queues));
        assertNull("queue should not have already been present", getRestTestHelper().find(Queue.NAME, queueName + "_DLQ" , queues));
        assertNull("exchange should not have already been present", getRestTestHelper().find(Exchange.NAME, queueName + "_DLE" , exchanges));

        //create the queue
        createQueue(queueName, "standard", attributes);

        //verify the new queue, as well as the DLQueue and DLExchange have been created
        hostDetails = getRestTestHelper().getJsonAsSingletonList("/rest/virtualhost/test");
        queues = (List<Map<String, Object>>) hostDetails.get(VirtualHostRestTest.VIRTUALHOST_QUEUES_ATTRIBUTE);
        exchanges = (List<Map<String, Object>>) hostDetails.get(VirtualHostRestTest.VIRTUALHOST_EXCHANGES_ATTRIBUTE);

        Map<String, Object> queue = getRestTestHelper().find(Queue.NAME, queueName , queues);
        Map<String, Object> dlqQueue = getRestTestHelper().find(Queue.NAME, queueName + "_DLQ" , queues);
        Map<String, Object> dlExchange = getRestTestHelper().find(Exchange.NAME, queueName + "_DLE" , exchanges);
        assertNotNull("queue should not have been present", queue);
        assertNotNull("queue should not have been present", dlqQueue);
        assertNotNull("exchange should not have been present", dlExchange);

        //verify that the alternate exchange is set as expected on the new queue
        Map<String, Object> queueAttributes = new HashMap<String, Object>();
        queueAttributes.put(Queue.ALTERNATE_EXCHANGE, queueName + "_DLE");

        Asserts.assertQueue(queueName, "standard", queue, queueAttributes);
        Asserts.assertQueue(queueName, "standard", queue, null);
    }

    private void createExchange(String exchangeName, String exchangeType) throws IOException
    {
        HttpURLConnection connection = getRestTestHelper().openManagementConnection("/rest/exchange/test/" + exchangeName, "PUT");

        Map<String, Object> queueData = new HashMap<String, Object>();
        queueData.put(Exchange.NAME, exchangeName);
        queueData.put(Exchange.DURABLE, Boolean.TRUE);
        queueData.put(Exchange.TYPE, exchangeType);

        getRestTestHelper().writeJsonRequest(connection, queueData);
        assertEquals("Unexpected response code", 201, connection.getResponseCode());

        connection.disconnect();
    }

    private void createQueue(String queueName, String queueType, Map<String, Object> attributes) throws IOException,
            JsonGenerationException, JsonMappingException
    {
        int responseCode = tryCreateQueue(queueName, queueType, attributes);
        assertEquals("Unexpected response code", 201, responseCode);
    }

    private int tryCreateQueue(String queueName, String queueType, Map<String, Object> attributes) throws IOException,
            JsonGenerationException, JsonMappingException
    {
        HttpURLConnection connection = getRestTestHelper().openManagementConnection("/rest/queue/test/" + queueName, "PUT");

        Map<String, Object> queueData = new HashMap<String, Object>();
        queueData.put(Queue.NAME, queueName);
        queueData.put(Queue.DURABLE, Boolean.TRUE);
        if (queueType != null)
        {
            queueData.put(Queue.TYPE, queueType);
        }
        if (attributes != null)
        {
            queueData.putAll(attributes);
        }

        getRestTestHelper().writeJsonRequest(connection, queueData);
        int responseCode = connection.getResponseCode();
        connection.disconnect();
        return responseCode;
    }

}
