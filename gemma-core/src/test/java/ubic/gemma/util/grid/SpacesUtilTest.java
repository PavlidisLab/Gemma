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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;

import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.SpringContextUtil;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.util.grid.javaspaces.SpacesUtil;

/**
 * A test class for {@link SpacesUtil}.
 * 
 * @author keshav
 * @version $Id$
 */
public class SpacesUtilTest extends BaseSpringContextTest {

    /**
     * Tests if space is running.
     */
    public void testIsSpaceRunning() {

        boolean isRunning = SpacesUtil.isSpaceRunning( SpacesEnum.DEFAULT_SPACE.getSpaceUrl() );

        if ( isRunning ) {
            assertTrue( isRunning );
        } else {
            assertFalse( isRunning );
        }

    }

    /**
     * Tests gigaspaces beans to the {@link org.springframework.beans.factory.BeanFactory}
     */
    public void testAddGigaspacesToBeanFactory() {

        ApplicationContext withoutGigaspacesCtx = ( ApplicationContext ) SpringContextUtil.getApplicationContext( true,
                true, false, false );

        String gigaspacesTemplate = "gigaspacesTemplate";
        assertFalse( withoutGigaspacesCtx.containsBean( gigaspacesTemplate ) );

        SpacesUtil gigaspacesUtil = ( SpacesUtil ) this.getBean( "spacesUtil" );

        BeanFactory updatedCtx = gigaspacesUtil.addGemmaSpacesToApplicationContext( SpacesEnum.DEFAULT_SPACE
                .getSpaceUrl() );

        /* verify that we have the new gigaspaces beans */
        if ( !SpacesUtil.isSpaceRunning( SpacesEnum.DEFAULT_SPACE.getSpaceUrl() ) )
            assertFalse( updatedCtx.containsBean( gigaspacesTemplate ) );
        else {
            assertTrue( updatedCtx.containsBean( gigaspacesTemplate ) );
        }
        /* make sure we haven't lost the other beans */
        assertTrue( updatedCtx.containsBean( "sessionFactory" ) );
    }

    /**
     * Test logging space statistics.
     */
    public void testLogSpaceStatistics() {
        SpacesUtil.logSpaceStatistics( SpacesEnum.DEFAULT_SPACE.getSpaceUrl() );
    }

    /**
     * Tests logging the configuration report.
     */
    public void testGetSpaceContainerAdmin() {
        SpacesUtil.logRuntimeConfigurationReport();
    }

    /**
     * Tests the number of idle workers.
     */
    public void testNumIdle() {
        SpacesUtil gigaspacesUtil = ( SpacesUtil ) this.getBean( "spacesUtil" );
        gigaspacesUtil.addGemmaSpacesToApplicationContext( SpacesEnum.DEFAULT_SPACE.getSpaceUrl() );

        int count = gigaspacesUtil.numIdleWorkers( SpacesEnum.DEFAULT_SPACE.getSpaceUrl() );
        assertTrue( count >= 0 );

    }

}
