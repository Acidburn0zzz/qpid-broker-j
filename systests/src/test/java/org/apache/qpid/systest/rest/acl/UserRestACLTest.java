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
package org.apache.qpid.systest.rest.acl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.apache.qpid.server.security.acl.AbstractACLTestCase;
import org.apache.qpid.systest.rest.QpidRestTestCase;
import org.apache.qpid.test.utils.TestBrokerConfiguration;

public class UserRestACLTest extends QpidRestTestCase
{
    private static final String ALLOWED_GROUP = "allowedGroup";
    private static final String DENIED_GROUP = "deniedGroup";
    private static final String OTHER_GROUP = "otherGroup";

    private static final String ALLOWED_USER = "webadmin";
    private static final String DENIED_USER = "admin";
    private static final String OTHER_USER = "other";

    private File _groupFile;

    @Override
    public void startDefaultBroker() throws Exception
    {
        // starting broker in tests
    }

    @Override
    protected void customizeConfiguration() throws Exception
    {
        super.customizeConfiguration();
        _groupFile = createTemporaryGroupFile();
        final TestBrokerConfiguration brokerConfiguration = getDefaultBrokerConfiguration();
        brokerConfiguration.addGroupFileConfiguration(_groupFile.getAbsolutePath());
        brokerConfiguration.configureTemporaryPasswordFile(ALLOWED_USER, DENIED_USER, OTHER_USER);
    }

    @Override
    public void tearDown() throws Exception
    {
        super.tearDown();

        if (_groupFile != null)
        {
            if (_groupFile.exists())
            {
                _groupFile.delete();
            }
        }
    }

    private File createTemporaryGroupFile() throws Exception
    {
        File groupFile = File.createTempFile("group", "grp");
        groupFile.deleteOnExit();

        Properties props = new Properties();
        props.put(ALLOWED_GROUP + ".users", ALLOWED_USER);
        props.put(DENIED_GROUP + ".users", DENIED_USER);
        props.put(OTHER_GROUP + ".users", OTHER_USER);

        props.store(new FileOutputStream(groupFile), "test group file");

        return groupFile;
    }

    public void testAddUser() throws Exception
    {
        AbstractACLTestCase.writeACLFileUtil(this, "ACL ALLOW-LOG ALL ACCESS MANAGEMENT",
                "ACL ALLOW-LOG " + ALLOWED_GROUP + " CREATE USER",
                "ACL DENY-LOG " + DENIED_GROUP + " CREATE USER");

        super.startDefaultBroker();

        String newUser = "newUser";
        String password = "password";

        assertUserDoesNotExist(newUser);

        getRestTestHelper().setUsernameAndPassword(DENIED_USER, DENIED_USER);

        getRestTestHelper().createOrUpdateUser(newUser, password, HttpServletResponse.SC_FORBIDDEN);
        assertUserDoesNotExist(newUser);

        getRestTestHelper().setUsernameAndPassword(ALLOWED_USER, ALLOWED_USER);
        getRestTestHelper().createOrUpdateUser(newUser, password);
        assertUserExists(newUser);
    }

    public void testDeleteUser() throws Exception
    {
        AbstractACLTestCase.writeACLFileUtil(this, "ACL ALLOW-LOG ALL ACCESS MANAGEMENT",
                "ACL ALLOW-LOG " + ALLOWED_GROUP + " DELETE USER",
                "ACL DENY-LOG " + DENIED_GROUP + " DELETE USER");

        super.startDefaultBroker();

        assertUserExists(OTHER_USER);

        getRestTestHelper().setUsernameAndPassword(DENIED_USER, DENIED_USER);
        getRestTestHelper().removeUser(OTHER_USER, HttpServletResponse.SC_FORBIDDEN);
        assertUserExists(OTHER_USER);

        getRestTestHelper().setUsernameAndPassword(ALLOWED_USER, ALLOWED_USER);
        getRestTestHelper().removeUser(OTHER_USER);
        assertUserDoesNotExist(OTHER_USER);
    }

    public void testUpdateUser() throws Exception
    {
        AbstractACLTestCase.writeACLFileUtil(this, "ACL ALLOW-LOG ALL ACCESS MANAGEMENT",
                "ACL ALLOW-LOG " + ALLOWED_GROUP + " UPDATE USER",
                "ACL DENY-LOG " + DENIED_GROUP + " UPDATE USER");

        super.startDefaultBroker();

        String newPassword = "newPassword";

        checkPassword(OTHER_USER, OTHER_USER, true);

        getRestTestHelper().setUsernameAndPassword(DENIED_USER, DENIED_USER);
        getRestTestHelper().createOrUpdateUser(OTHER_USER, newPassword, HttpServletResponse.SC_FORBIDDEN);

        checkPassword(OTHER_USER, newPassword, false);

        getRestTestHelper().setUsernameAndPassword(ALLOWED_USER, ALLOWED_USER);
        getRestTestHelper().createOrUpdateUser(OTHER_USER, newPassword, HttpServletResponse.SC_OK); // expect SC_OK rather than the default SC_CREATED

        checkPassword(OTHER_USER, newPassword, true);
        checkPassword(OTHER_USER, OTHER_USER, false);
    }

    private void checkPassword(String username, String password, boolean passwordExpectedToBeCorrect) throws IOException
    {
        getRestTestHelper().setUsernameAndPassword(username, password);

        int responseCode = getRestTestHelper().submitRequest("user/"
                + TestBrokerConfiguration.ENTRY_NAME_AUTHENTICATION_PROVIDER + "/", "GET", (byte[])null);
        boolean passwordIsCorrect = responseCode == HttpServletResponse.SC_OK;

        assertEquals(passwordExpectedToBeCorrect, passwordIsCorrect);
    }

    private void assertUserDoesNotExist(String newUser) throws IOException
    {
        String path = "user/" + TestBrokerConfiguration.ENTRY_NAME_AUTHENTICATION_PROVIDER + "/" + newUser;
        getRestTestHelper().submitRequest(path, "GET", HttpServletResponse.SC_NOT_FOUND);
    }

    private void assertUserExists(String username) throws IOException
    {
        String path = "user/" + TestBrokerConfiguration.ENTRY_NAME_AUTHENTICATION_PROVIDER + "/" + username;
        Map<String, Object> userDetails = getRestTestHelper().getJsonAsMap(path);

        assertEquals(
                "User returned by " + path + " should have name=" + username + ". The returned JSON was: " + userDetails,
                username,
                userDetails.get("name"));
    }
}
