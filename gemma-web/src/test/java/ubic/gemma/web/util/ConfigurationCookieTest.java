/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.web.util;

import junit.framework.TestCase;

import javax.servlet.http.Cookie;

/**
 * @author pavlidis
 *
 */
public class ConfigurationCookieTest extends TestCase {

    public void testConfigurationCookieA() {
        ConfigurationCookie cookie = new ConfigurationCookie( "foo" );
        cookie.setProperty( "arg", "bla" );
        String actualValue = cookie.getString( "arg" );
        assertEquals( "bla", actualValue );
    }

    public void testConfigurationCookieB() throws Exception {
        Cookie plainCookie = new Cookie( "foo", "arg=bla\nbork=snork\n" );
        ConfigurationCookie cookie = new ConfigurationCookie( plainCookie );
        String actualValue = cookie.getString( "arg" );
        assertEquals( "bla", actualValue );
    }

    public void testConfigurationCookieC() throws Exception {
        Cookie plainCookie = new Cookie( "foo", "arg=bla\nbork=1\n" );
        ConfigurationCookie cookie = new ConfigurationCookie( plainCookie );
        int actualValue = cookie.getInt( "bork" );
        assertEquals( 1, actualValue );
    }

    public void testConfigurationCookieD() {
        ConfigurationCookie cookie = new ConfigurationCookie( "foo" );
        cookie.addProperty( "arg", "bla" );
        String actualValue = cookie.getValue();
        assertTrue( "Got " + actualValue, actualValue.contains( "arg = bla" ) );
    }

    public void testConfigurationCookieE() throws Exception {
        String value = "# written by PropertiesConfiguration # Tue Oct 17 23:30:31 PDT 2006 dummy=foo @@ sequenceType = AFFY_COLLAPSED @@ taxon = Rattus norvegicus";
        Cookie plainCookie = new Cookie( "foo", value );
        ConfigurationCookie cookie = new ConfigurationCookie( plainCookie );
        String actualValue = cookie.getString( "sequenceType" );
        assertEquals( "AFFY_COLLAPSED", actualValue );
    }

}
