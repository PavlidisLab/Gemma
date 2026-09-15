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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.common.auditAndSecurity.eventType.AlignmentBasedGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceUpdateEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author pavlidis
 */
public class ArrayDesignReportServiceTest extends BaseSpringContextTest5 {

    private static ArrayDesign ad;
    private static boolean persisted = false;
    @Autowired
    AuditTrailService ads;

    @Autowired
    ArrayDesignReportService arrayDesignReportService;

    @BeforeEach
    public void setUp() throws Exception {
        if ( !ArrayDesignReportServiceTest.persisted ) {
            ArrayDesignReportServiceTest.ad = this
                    .getTestPersistentArrayDesign( 5, true, false, false ); // not read only.

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

    /**
     * A report is one file per database id, so an id that comes to mean a different platform hands that platform
     * the previous occupant's counts — silently, because they are plausible numbers either way.
     * <p>
     * Not hypothetical. On 2026-08-26 {@code PlatformsWebServiceTest.testMicroarrayGeneCountsAreNullWithoutAReport}
     * asserted null gene counts for a freshly seeded microarray and read 0, out of a two-day-old report written
     * for a different test platform that had held the same id in a previous {@code gemdtest}. The report
     * directory lives under {@code gemma.appdata.home} and outlives the database; the ids do not.
     */
    @Test
    public void testAReportWrittenForADifferentPlatformIsNotApplied() {
        assertNotNull( arrayDesignReportService.generateArrayDesignReport(
                ArrayDesignReportServiceTest.ad.getId() ) );

        // same id, different platform — which is exactly what a recycled id looks like from here
        ArrayDesignValueObject impostor = new ArrayDesignValueObject( ArrayDesignReportServiceTest.ad.getId() );
        impostor.setShortName( "GPL_NOT_THE_ONE_THE_REPORT_IS_ABOUT" );
        arrayDesignReportService.fillInValueObjects( Collections.singleton( impostor ) );

        assertThat( impostor.getNumGenes() )
                .withFailMessage( "another platform's counts were applied to this one" )
                .isNull();
        assertThat( impostor.getDateCached() ).isNull();
    }

    /** The other half: the report still reaches the platform it was written for. */
    @Test
    public void testAReportIsAppliedToThePlatformItWasWrittenFor() {
        ArrayDesignValueObject report = arrayDesignReportService.generateArrayDesignReport(
                ArrayDesignReportServiceTest.ad.getId() );
        assertNotNull( report );

        ArrayDesignValueObject vo = new ArrayDesignValueObject( ArrayDesignReportServiceTest.ad.getId() );
        vo.setShortName( ArrayDesignReportServiceTest.ad.getShortName() );
        arrayDesignReportService.fillInValueObjects( Collections.singleton( vo ) );

        assertThat( vo.getNumGenes() ).isEqualTo( report.getNumGenes() );
        assertThat( vo.getDateCached() ).isEqualTo( report.getDateCached() );
    }
}
