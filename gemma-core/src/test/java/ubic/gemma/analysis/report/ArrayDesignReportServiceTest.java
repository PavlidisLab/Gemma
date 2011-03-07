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
package ubic.gemma.analysis.report;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEventImpl;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceAnalysisEventImpl;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceUpdateEventImpl;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignReportServiceTest extends BaseSpringContextTest {

    @Autowired
    AuditTrailService ads;

    @Autowired
    ArrayDesignReportService adrs;

    static ArrayDesign ad;
    static boolean persisted = false;

    @Before
    public void setup() throws Exception {
        if ( !persisted ) {
            ad = this.getTestPersistentArrayDesign( 5, true, false, false ); // not read only.

            ads.addUpdateEvent( ad, new ArrayDesignSequenceUpdateEventImpl(), "sequences" );

            ads.addUpdateEvent( ad, new ArrayDesignSequenceAnalysisEventImpl(), "alignment" );

            ads.addUpdateEvent( ad, new ArrayDesignGeneMappingEventImpl(), "mapping" );

            Thread.sleep( 100 );

            ads.addUpdateEvent( ad, new ArrayDesignSequenceAnalysisEventImpl(), "alignment 2" );

            ads.addUpdateEvent( ad, new ArrayDesignGeneMappingEventImpl(), "mapping 2" );
            Thread.sleep( 100 );
            persisted = true;
        }

    }

    @Test
    public void testGenerateArrayDesignGeneMappingEvent() {

        String report = adrs.getLastGeneMappingEvent( ad.getId() );

        log.info( report );
        assertTrue( !report.equals( "[None]" ) );
        assertNotNull( report );
    }

    @Test
    public void testGenerateArrayDesignSequenceAnalysisEvent() {

        String report = adrs.getLastSequenceAnalysisEvent( ad.getId() );

        log.info( report );
        assertTrue( !report.equals( "[None]" ) );
        assertNotNull( report );
    }

    @Test
    public void testGenerateArrayDesignSequenceUpdateEvent() {

        String report = adrs.getLastSequenceUpdateEvent( ad.getId() );

        log.info( report );
        assertTrue( !report.equals( "[None]" ) );
        assertNotNull( report );
    }

}
