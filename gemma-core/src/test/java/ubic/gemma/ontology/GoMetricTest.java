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

package ubic.gemma.ontology;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
 
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * FIXME Unfortunately this test requires a completely loaded database. Really a 'mini-go' with known properties must be
 * used. Tests disabled.
 * 
 * @author meeta
 * @version $Id$
 */
public class GoMetricTest extends BaseSpringContextTest {

    @Autowired
    private GeneOntologyService geneOntologyService;

    private GoMetric goMetric;

    private OntologyTerm entry;
    private Collection<OntologyTerm> terms = new HashSet<OntologyTerm>();
    private boolean enabled = true;

    /*
     * (non-Javadoc)
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @Before
    public void onSetUpInTransaction() throws Exception {

        this.enabled = false;

        // geneOntologyService.init( false );
        // if ( !GeneOntologyService.isEnabled() ) {
        // this.enabled = false;
        // return;
        // }
        // int n = 0;
        // while ( !geneOntologyService.isReady() ) {
        // try {
        // if ( ++n % 10 == 0 ) {
        // log.info( "Test is waiting for GO to load ..." );
        // }
        // Thread.sleep( 1000 );
        // } catch ( InterruptedException e ) {
        // }
        // }
        // log.info( "Ready to test" );
        //
        // entry = GeneOntologyService.getTermForId( "GO:0001963" );
        // terms = geneOntologyService.getAllChildren( entry, true );
        // terms.add( entry );
    }

    /**
     * @throws Exception
     */
    @Test
    public final void testCheckParents() throws Exception {
        if ( !enabled ) {
            // log.warn( "GO is not enabled, Test skipped" );
            log.info( "Test is disabled." );
            return;
        }
        Map<String, Double> probMap = new HashMap<String, Double>();
        Collection<OntologyTerm> probTerms = geneOntologyService.getAllParents( entry, true );
        probTerms.add( entry );

        for ( OntologyTerm t : probTerms ) {

            if ( t.getUri().equalsIgnoreCase( entry.getUri() ) ) {
                probMap.put( t.getUri(), 0.1 );
            } else
                probMap.put( t.getUri(), 0.5 );
        }

        Double expected = 0.1;
        Double value = goMetric.checkParents( entry, entry, probMap );

        assertEquals( expected, value );

    }

    /**
     * @throws Exception
     */
    @Test
    public final void testGetChildrenOccurrence() throws Exception {
        if ( !enabled ) {
            // log.warn( "GO is not enabled, Test skipped" );
            log.info( "Test is disabled." );
            return;
        }
        Map<String, Integer> countMap = new HashMap<String, Integer>();
        for ( OntologyTerm t : terms )
            countMap.put( t.getUri(), 1 );

        Integer expected = countMap.size();
        Integer count = goMetric.getChildrenOccurrence( countMap, entry.getUri() );
        assertEquals( expected, count );
    }

    /**
     * @throws Exception
     */
    @Test
    public final void testGetTermOccurrence() throws Exception {
        if ( !enabled ) {
            // log.warn( "GO is not enabled, Test skipped" );
            log.info( "Test is disabled." );
            return;
        }
        Collection<String> stringTerms = new HashSet<String>();
        for ( OntologyTerm t : terms )
            stringTerms.add( t.getUri() );

        Map<Long, Collection<String>> gene2GOMap = new HashMap<Long, Collection<String>>();
        gene2GOMap.put( ( long ) 14415, stringTerms );
        gene2GOMap.put( ( long ) 22129, stringTerms );

        Map<String, Integer> countMap = goMetric.getTermOccurrence( gene2GOMap );
        int expected = 0;

        for ( String uri : countMap.keySet() ) {
            expected += countMap.get( uri );
        }

        assertEquals( expected, ( 2 * stringTerms.size() ) );
    }

    /**
     * @throws Exception
     */
    // public final void testComputeSimilarity() throws Exception {
    // if ( !enabled ) {
    // log.warn( "GO is not enabled, Test skipped" );
    // return;
    // }
    // Metric chooseMetric = Metric.simple;
    //
    // Gene gene1 = geneService.load( 599683 );
    // Gene gene2 = geneService.load( 640008 );
    //
    // log.info( "The genes retrieved: " + gene1 + " " + gene2 );
    //
    // Collection<OntologyTerm> probTerms = geneOntologyService.getGOTerms( gene1, true );
    // // goMetric.logIds( "computeSimilarityOverlap gene1 terms", probTerms );
    // Collection<OntologyTerm> terms2 = geneOntologyService.getGOTerms( gene2, true );
    // // goMetric.logIds( "computeSimilarityOverlap gene2 terms", terms2 );
    // Collection<OntologyTerm> allTerms = new HashSet<OntologyTerm>( probTerms );
    // allTerms.addAll( terms2 );
    //
    // Map<String, Double> probMap = new HashMap<String, Double>();
    //
    // for ( OntologyTerm t : allTerms ) {
    //
    // if ( t.getUri().equalsIgnoreCase( "http://purl.org/obo/owl/GO#GO_0042592" ) )
    // probMap.put( t.getUri(), 0.1 );
    // else
    // probMap.put( t.getUri(), 0.5 );
    // }
    //
    // Double value = goMetric.computeSimilarity( gene1, gene2, probMap, chooseMetric );
    //
    // // if ( chooseMetric.equals( Metric.simple ) ) assertEquals( 4.0, value );
    // // if ( chooseMetric.equals( Metric.resnik ) ) assertEquals( 1.4978661367769954, value );
    // // if ( chooseMetric.equals( Metric.lin ) ) assertEquals( 2.160964047443681, value );
    // // if ( chooseMetric.equals( Metric.jiang ) ) assertEquals( 0.6185149837908823, value );
    // }
}
