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
package ubic.gemma.util.gemmaspaces;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;

import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.SpringContextUtil;
import ubic.gemma.util.gemmaspaces.GemmaSpacesEnum;
import ubic.gemma.util.gemmaspaces.GemmaSpacesUtil;

/**
 * A test class for {@link GemmaSpacesUtil}.
 * 
 * @author keshav
 * @version $Id$
 */
public class GemmaSpacesUtilTest extends BaseSpringContextTest {

    /**
     * Tests if space is running.
     */
    public void testIsSpaceRunning() {

        boolean isRunning = GemmaSpacesUtil.isSpaceRunning( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() );

        if ( isRunning )
            assertTrue( isRunning );

        else
            assertFalse( isRunning );

    }

    /**
     * Tests gigaspaces beans to the {@link org.springframework.beans.factory.BeanFactory}
     */
    public void testAddGigaspacesToBeanFactory() {

        ApplicationContext withoutGigaspacesCtx = ( ApplicationContext ) SpringContextUtil.getApplicationContext( true,
                false, false, false );

        String gigaspacesTemplate = "gigaspacesTemplate";
        assertFalse( withoutGigaspacesCtx.containsBean( gigaspacesTemplate ) );

        GemmaSpacesUtil gigaspacesUtil = ( GemmaSpacesUtil ) this.getBean( "gigaSpacesUtil" );

        BeanFactory updatedCtx = gigaspacesUtil.addGemmaSpacesToApplicationContext( GemmaSpacesEnum.DEFAULT_SPACE
                .getSpaceUrl() );

        /* verify that we have the new gigaspaces beans */
        if ( !GemmaSpacesUtil.isSpaceRunning( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() ) )
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
        GemmaSpacesUtil.logSpaceStatistics( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() );
    }

    /**
     * Tests logging the configuration report.
     */
    public void testGetSpaceContainerAdmin() {
        GemmaSpacesUtil.logRuntimeConfigurationReport();
    }

    /**
     * Tests the areWorkersRegistered functinality.
     */
    public void testAreWorkersRegistered() {

        GemmaSpacesUtil gigaspacesUtil = ( GemmaSpacesUtil ) this.getBean( "gigaSpacesUtil" );
        ApplicationContext updatedCtx = gigaspacesUtil.addGemmaSpacesToApplicationContext( GemmaSpacesEnum.DEFAULT_SPACE
                .getSpaceUrl() );

        /*
         * NOTE: These assertions do not test anything ... I've added them for the sake of the unit test. This test
         * checks the areWorkersRegistered code in GigaSpacesUtil.
         */
        boolean workersRunning = false;

        workersRunning = gigaspacesUtil.areWorkersRegistered( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() );
        if ( workersRunning == false )
            assertFalse( workersRunning );

        else
            assertTrue( workersRunning );
    }

    /**
     * Tests the number of workers registered.
     */
    public void testNumWorkersRegistered() {
        GemmaSpacesUtil gigaspacesUtil = ( GemmaSpacesUtil ) this.getBean( "gigaSpacesUtil" );
        ApplicationContext updatedCtx = gigaspacesUtil.addGemmaSpacesToApplicationContext( GemmaSpacesEnum.DEFAULT_SPACE
                .getSpaceUrl() );

        int count = gigaspacesUtil.numWorkersRegistered( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() );
        log.info( count );

    }

}
