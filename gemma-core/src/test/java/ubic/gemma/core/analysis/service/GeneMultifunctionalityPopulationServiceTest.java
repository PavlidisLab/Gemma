/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.core.analysis.service;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ReleaseDetailsUpdateEvent;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author paul
 */
public class GeneMultifunctionalityPopulationServiceTest extends BaseSpringContextTest {

    private final String[] goTerms = new String[] { "GO_0047500", "GO_0051530", "GO_0051724", "GO_0004118",
            "GO_0005324" };
    @Autowired
    private GeneMultifunctionalityPopulationService s;
    @Autowired
    private Gene2GOAssociationService gene2GoService;
    @Autowired
    private GeneOntologyService goService;
    @Autowired
    private GeneService geneService;
    @Autowired
    private ExternalDatabaseService externalDatabaseService;
    private Taxon testTaxon;

    @After
    public void tearDown() {
        gene2GoService.removeAll();
        Collection<Gene> genes = geneService.loadAll( testTaxon );
        geneService.remove( genes );
    }

    @Before
    public void setUp() throws Exception {

        if ( goService.isRunning() ) {
            goService.shutDown();
        }
        gene2GoService.removeAll();

        goService.loadTermsInNameSpace( new GZIPInputStream(
                this.getClass().getResourceAsStream( "/data/loader/ontology/molecular-function.test.owl.gz" ) ) );

        testTaxon = taxonService.findOrCreate( Taxon.Factory
                .newInstance( "foobly" + RandomStringUtils.randomAlphabetic( 2 ),
                        "doobly" + RandomStringUtils.randomAlphabetic( 2 ), RandomUtils.nextInt( 0, 5000 ), true ) );

        /*
         * Create genes
         */
        for ( int i = 0; i < 120; i++ ) {
            Gene gene = this.getTestPersistentGene( testTaxon );

            // Some genes get no terms.
            if ( i >= 100 )
                continue;

            /*
             * Add up to 5 GO terms. Parents mean more will be added.
             */
            for ( int j = 0; j <= Math.floor( i / 20 ); j++ ) {
                Characteristic oe = Characteristic.Factory.newInstance();
                oe.setValueUri( GeneOntologyService.BASE_GO_URI + goTerms[j] );
                oe.setValue( goTerms[j] );
                Gene2GOAssociation g2Go1 = Gene2GOAssociation.Factory.newInstance( gene, oe, GOEvidenceCode.EXP );
                gene2GoService.create( g2Go1 );
            }
        }
    }

    @Test
    @Category(SlowTest.class)
    public void test() {
        log.info( "Updating multifunctionality" );
        Date beforeUpdateDate = new Date();
        s.updateMultifunctionality( testTaxon );

        Collection<Gene> genes = geneService.loadAll( testTaxon );

        genes = geneService.thawLite( genes );

        assertThat( genes ).hasSize( 120 );

        assertThat( genes )
                .extracting( "multifunctionality" )
                .extracting( "rank", "numGoTerms" )
                .contains( tuple( 0.9125, 5 ) );

        ExternalDatabase ed = externalDatabaseService.findByNameWithAuditTrail( ExternalDatabases.MULTIFUNCTIONALITY );
        assertThat( ed ).isNotNull();
        assertThat( ed.getLastUpdated() )
                .isBetween( beforeUpdateDate, new Date() );
        List<AuditEvent> auditEvents = ed.getAuditTrail().getEvents();
        assertThat( auditEvents ).hasSizeGreaterThanOrEqualTo( 2 );
        assertThat( auditEvents.get( auditEvents.size() - 2 ).getEventType() )
                .isInstanceOf( ReleaseDetailsUpdateEvent.class );
        assertThat( auditEvents.get( auditEvents.size() - 2 ) )
                .hasFieldOrPropertyWithValue( "action", AuditAction.UPDATE );
        assertThat( auditEvents.get( auditEvents.size() - 1 ) )
                .hasFieldOrPropertyWithValue( "eventType", null )
                .hasFieldOrPropertyWithValue( "action", AuditAction.UPDATE );
    }
}
