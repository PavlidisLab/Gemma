/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.util;

import java.net.URL;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.convert.ListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;

/**
 * Convenience methods for loading configurations.
 * <p>
 * In-tree port of {@code ubic.basecode.util.ConfigUtils} (Renovations Phase 3 baseCode util batch 3).
 * Only the methods Gemma actually calls were ported. baseCode's full ConfigUtils also exposed
 * {@code loadConfig(...)}, {@code getConfigBuilder(...)}, and {@code locateConfig(...)}, all of which
 * depended on {@code FileTools.touch} (dropped from Gemma's in-tree FileTools port) and
 * {@code commons-configuration2} file-locator APIs that Gemma doesn't use. Reintroduce here if needed.
 *
 * @author Paul
 */
public class ConfigUtils {

    private static final ListDelimiterHandler LIST_DELIMITER_HANDLER = new DefaultListDelimiterHandler( ',' );

    private ConfigUtils() {
        // block instantiation
    }

    /**
     * Load a properties configuration from a classpath resource.
     *
     * @param name the classpath location, such as "project.properties" in the base package, or
     *             org/foo/project.properties.
     * @return loaded PropertiesConfiguration
     * @throws ConfigurationException if the resource is missing or fails to load
     */
    public static PropertiesConfiguration loadClasspathConfig( String name ) throws ConfigurationException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        URL url = loader.getResource( name );
        if ( url == null ) {
            throw new ConfigurationException( "Couldn't locate: " + name );
        }

        PropertiesConfiguration pc = createConfiguration();
        FileHandler handler = new FileHandler( pc );
        handler.setURL( url );
        handler.load();
        return pc;
    }

    private static PropertiesConfiguration createConfiguration() {
        PropertiesConfiguration pc = new PropertiesConfiguration();
        pc.setListDelimiterHandler( LIST_DELIMITER_HANDLER );
        return pc;
    }
}
