/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.util.grid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ubic.gemma.job.grid.util.SpacesUtil;
import ubic.gemma.job.grid.util.SpacesUtilImpl;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.SpringContextUtil;

/**
 * A test class for {@link SpacesUtilImpl}.
 * 
 * @author keshav
 * @version $Id$
 */
public class SpacesUtilTest extends BaseSpringContextTest {

    @Autowired
    SpacesUtil spacesUtil;

    /**
     * Tests gigaspaces beans to the {@link org.springframework.beans.factory.BeanFactory}
     */
    @Test
    public void testAddGigaspacesToBeanFactory() {

        if ( !SpacesUtilImpl.isSpaceRunning() ) {
            return;
        }

        ApplicationContext withoutGigaspacesCtx = ( ApplicationContext ) SpringContextUtil.getApplicationContext( true,
                true, false );

        String gigaspacesTemplate = "gigaspacesTemplate";
        assertFalse( withoutGigaspacesCtx.containsBean( gigaspacesTemplate ) );

        BeanFactory updatedCtx = spacesUtil.addGemmaSpacesToApplicationContext();

        /* verify that we have the new gigaspaces beans */
        if ( !SpacesUtilImpl.isSpaceRunning() )
            assertFalse( updatedCtx.containsBean( gigaspacesTemplate ) );
        else {
            assertTrue( updatedCtx.containsBean( gigaspacesTemplate ) );
        }
        /* make sure we haven't lost the other beans */
        assertTrue( updatedCtx.containsBean( "sessionFactory" ) );
    }

    /**
     * Tests logging the configuration report.
     */
    @Test
    public void testGetSpaceContainerAdmin() {
        SpacesUtilImpl.logRuntimeConfigurationReport();
    }

    /**
     * Test logging space statistics.
     */
    @Test
    public void testLogSpaceStatistics() {
        SpacesUtilImpl.logSpaceStatistics();
    }

    /**
     * Tests the number of idle workers.
     */
    @Test
    public void testNumIdle() {

        if ( !SpacesUtilImpl.isSpaceRunning() ) {
            return;
        }

        spacesUtil.addGemmaSpacesToApplicationContext();

        int count = spacesUtil.numIdleWorkers();
        assertTrue( count >= 0 );

    }

}
