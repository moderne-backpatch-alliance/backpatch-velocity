package org.apache.velocity.test;

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

import junit.framework.TestCase;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.util.introspection.SecureIntrospectorImpl;
import org.apache.velocity.util.introspection.SecureUberspector;
import org.slf4j.helpers.NOPLogger;

import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;

// test written by us, not from upstream
public class SecureIntrospectorClassLoaderThreadTestCase extends TestCase
{
    public void testPermissionRefusesClassLoaderSubclass()
    {
        assertFalse(permission().checkObjectExecutePermission(URLClassLoader.class, "getURLs"));
    }

    public void testPermissionRefusesThreadSubclass()
    {
        assertFalse(permission().checkObjectExecutePermission(new Thread("unstarted") { }.getClass(), "getName"));
    }

    public void testPermissionRefusesClassLoaderAndThreadWithoutConfiguredList()
    {
        assertFalse(permission().checkObjectExecutePermission(ClassLoader.class, "getParent"));
        assertFalse(permission().checkObjectExecutePermission(Thread.class, "getName"));
    }

    public void testDefaultConfigRefusesMethodOnClassLoaderSubclass() throws Exception
    {
        assertRefused("$v.getURLs()", new URLClassLoader(new URL[0], null));
    }

    public void testDefaultConfigRefusesPropertyOnClassLoaderSubclass() throws Exception
    {
        assertRefused("$v.URLs", new URLClassLoader(new URL[0], null));
    }

    public void testDefaultConfigRefusesMethodOnThreadSubclass() throws Exception
    {
        assertRefused("$v.getName()", new Thread("unstarted") { });
    }

    public void testOtherObjectsStillResolve() throws Exception
    {
        assertEquals("3", render("$v.length()", "abc"));
    }

    private static SecureIntrospectorImpl permission()
    {
        return new SecureIntrospectorImpl(new String[0], new String[0], NOPLogger.NOP_LOGGER);
    }

    private static void assertRefused(String template, Object value) throws Exception
    {
        assertEquals(template, render(template, value));
    }

    private static String render(String template, Object value) throws Exception
    {
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME, SecureUberspector.class.getName());
        ve.init();
        VelocityContext c = new VelocityContext();
        c.put("v", value);
        StringWriter w = new StringWriter();
        ve.evaluate(c, w, "test", template);
        return w.toString();
    }
}
