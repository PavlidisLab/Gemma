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
package ubic.gemma.core.analysis.report;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.auditAndSecurity.eventType.AlignmentBasedGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceUpdateEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author pavlidis
 */
public class ArrayDesignReportServiceTest extends BaseSpringContextTest {

    private static ArrayDesign ad;
    private static boolean persisted = false;
    @Autowired
    AuditTrailService ads;

    @Autowired
    ArrayDesignReportService arrayDesignReportService;

    @Before
    public void setUp() throws Exception {
        if ( !ArrayDesignReportServiceTest.persisted ) {
            ArrayDesignReportServiceTest.ad = this
                    .getTestPersistentArrayDesign( 5, true, false ); // not read only.

            ads.addUpdateEvent( ArrayDesignReportServiceTest.ad, ArrayDesignSequenceUpdateEvent.class, "sequences");

            ads.addUpdateEvent( ArrayDesignReportServiceTest.ad, ArrayDesignSequenceAnalysisEvent.class, "alignment" );

            ads.addUpdateEvent( ArrayDesignReportServiceTest.ad, AlignmentBasedGeneMappingEvent.class, "mapping" );

            Thread.sleep( 100 );

            ads.addUpdateEvent( ArrayDesignReportServiceTest.ad, ArrayDesignSequenceAnalysisEvent.class,
                    "alignment 2" );

            ads.addUpdateEvent( ArrayDesignReportServiceTest.ad, AlignmentBasedGeneMappingEvent.class, "mapping 2" );
            Thread.sleep( 100 );
            ArrayDesignReportServiceTest.persisted = true;
        }

    }

    @Test
    public void testGenerateArrayDesignGeneMappingEvent() {

        String report = arrayDesignReportService.getLastGeneMappingEvent( ArrayDesignReportServiceTest.ad.getId() );

        log.info( report );
        assertTrue( !report.equals( "[None]" ) );
        assertNotNull( report );
    }

    @Test
    public void testGenerateArrayDesignSequenceAnalysisEvent() {

        String report = arrayDesignReportService.getLastSequenceAnalysisEvent( ArrayDesignReportServiceTest.ad.getId() );

        log.info( report );
        assertTrue( !report.equals( "[None]" ) );
        assertNotNull( report );
    }

    @Test
    public void testGenerateArrayDesignSequenceUpdateEvent() {

        String report = arrayDesignReportService.getLastSequenceUpdateEvent( ArrayDesignReportServiceTest.ad.getId() );

        log.info( report );
        assertTrue( !report.equals( "[None]" ) );
        assertNotNull( report );
    }

}
