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
package org.apache.qpid.systest.rest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.qpid.server.model.AlternateBinding;
import org.apache.qpid.server.model.Exchange;

public class ExchangeRestTest extends QpidRestTestCase
{
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        getRestTestHelper().createTestQueues();
    }

    public void testGet() throws Exception
    {
        List<Map<String, Object>> exchanges = getRestTestHelper().getJsonAsList("exchange");
        assertNotNull("Exchanges cannot be null", exchanges);
        assertTrue("Unexpected number of exchanges", exchanges.size() >= EXPECTED_VIRTUALHOSTS.length * EXPECTED_EXCHANGES.length);
        for (Map<String, Object> exchange : exchanges)
        {
            Asserts.assertExchange((String) exchange.get(Exchange.NAME), (String) exchange.get(Exchange.TYPE), exchange);
        }
    }

    public void testGetHostExchanges() throws Exception
    {
        List<Map<String, Object>> exchanges = getRestTestHelper().getJsonAsList("exchange/test");
        assertNotNull("Users cannot be null", exchanges);
        assertEquals("Unexpected number of exchanges", exchanges.size(), EXPECTED_EXCHANGES.length);
        for (String exchangeName : EXPECTED_EXCHANGES)
        {
            Map<String, Object> exchange = getRestTestHelper().find(Exchange.NAME, exchangeName, exchanges);
            assertExchange(exchangeName, exchange);
        }
    }

    public void testGetHostExchangeByName() throws Exception
    {
        for (String exchangeName : EXPECTED_EXCHANGES)
        {
            Map<String, Object> exchange = getRestTestHelper().getJsonAsMap("exchange/test/test/"
                                                                            + getRestTestHelper().encodeAsUTF(
                    exchangeName));
            assertExchange(exchangeName, exchange);
        }
    }

    public void testSetExchangeSupported() throws Exception
    {
        String exchangeName = getTestName();
        String exchangeUrl = "exchange/test/test/" + exchangeName;

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(Exchange.NAME, exchangeName);
        attributes.put(Exchange.TYPE, "direct");
        getRestTestHelper().submitRequest(exchangeUrl, "PUT", attributes, 201);

        Map<String, Object> exchange = getRestTestHelper().getJsonAsMap(exchangeUrl);
        assertNotNull("Exchange not found", exchange);

        attributes = new HashMap<>();
        attributes.put(Exchange.NAME, exchangeName);
        attributes.put(Exchange.ALTERNATE_BINDING,
                       Collections.singletonMap(AlternateBinding.DESTINATION, "amq.direct"));

        getRestTestHelper().submitRequest(exchangeUrl, "PUT", attributes, 200);
        exchange = getRestTestHelper().getJsonAsMap(exchangeUrl);
        assertNotNull("Exchange not found", exchange);
        assertEquals(new HashMap<>(Collections.singletonMap(AlternateBinding.DESTINATION, "amq.direct")),
                     new HashMap<>(((Map<String, Object>) exchange.get(Exchange.ALTERNATE_BINDING))));
    }

    private void assertExchange(String exchangeName, Map<String, Object> exchange)
    {
        assertNotNull("Exchange with name " + exchangeName + " is not found", exchange);
        String type = (String) exchange.get(Exchange.TYPE);
        Asserts.assertExchange(exchangeName, type, exchange);
        if ("direct".equals(type))
        {
            assertBindings(exchange);
        }
    }

    private void assertBindings(Map<String, Object> exchange)
    {
        List<Map<String, Object>> bindings = (List<Map<String, Object>>) exchange.get("bindings");
        assertEquals(RestTestHelper.EXPECTED_QUEUES.length, bindings.size());
        for (Map<String, Object> binding : bindings)
        {
            String destination = (String) binding.get("destination");
            assertTrue(Arrays.asList(RestTestHelper.EXPECTED_QUEUES).contains(destination));
        }
    }

}
