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

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.TestCase;

import ubic.gemma.loader.expression.arrayExpress.ArrayExpressLoadService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.GeneServiceImpl;
import ubic.gemma.ontology.GoMetric.Metric;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author meeta
 */
public class GoMetricTest extends BaseSpringContextTest {

    private GeneOntologyService geneOntologyService;
    private GeneService geneService;
    private GoMetric goMetric;

    private OntologyTerm entry;
    private Collection<OntologyTerm> terms = new HashSet<OntologyTerm>();

    private static Log log = LogFactory.getLog( GoMetricTest.class.getName() );

    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        geneOntologyService = ( GeneOntologyService ) this.getBean( "geneOntologyService" );
        geneService = ( GeneService ) this.getBean( "geneService" );
        goMetric = ( GoMetric ) this.getBean( "goMetric" );

        while ( !geneOntologyService.isReady() ) {
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException e ) {
            }
        }
        log.info( "Ready to test" );

        entry = GeneOntologyService.getTermForId( "GO:0001963" );
        terms = geneOntologyService.getAllChildren( entry, true );
        terms.add( entry );
    }


    public final void testGetTermOccurrence() throws Exception {

        Collection<String> stringTerms = new HashSet<String>();
        for ( OntologyTerm t : terms )
            stringTerms.add( t.getUri() );

        Map<Long, Collection<String>> gene2GOMap = new HashMap<Long, Collection<String>>();
        gene2GOMap.put( ( long ) 14415, stringTerms );
        gene2GOMap.put( ( long ) 22129, stringTerms );

        Integer expected = 2;

        Integer count = goMetric.getTermOccurrence( gene2GOMap, entry.getUri() );

        assertEquals( expected, count );
    }

    public final void testGetChildrenOccurrence() throws Exception {

        Map<String, Integer> countMap = new HashMap<String, Integer>();
        for ( OntologyTerm t : terms )
            countMap.put( t.getUri(), 1 );

        Integer expected = countMap.size();
        Integer count = goMetric.getChildrenOccurrence( countMap, entry.getUri() );
        assertEquals( expected, count );
    }

    public final void testCheckParents() throws Exception {

        Map<String, Double> probMap = new HashMap<String, Double>();
        Collection<OntologyTerm> probTerms = geneOntologyService.getAllParents( entry, true );
        probTerms.add( entry );

        for ( OntologyTerm t : probTerms ) {

            if ( t.getUri().equalsIgnoreCase( entry.getUri() ) ) {
                probMap.put( t.getUri(), ( double ) 0.1 );
            } else
                probMap.put( t.getUri(), ( double ) 0.5 );
        }

        Double expected = 0.1;
        Double value = goMetric.checkParents( entry, entry, probMap );

        assertEquals( expected, value );

    }
    
    public final void testComputeSimilarityOverlap() throws Exception {

        Metric chooseMetric = Metric.jiang;

        Gene gene1 = geneService.load( 599683 );
        Gene gene2 = geneService.load( 640008 );

        log.info( "The genes retrieved: " + gene1 + gene2 );

        Collection<OntologyTerm> probTerms = geneOntologyService.getGOTerms( gene1 );
        goMetric.logIds( "computeSimilarityOverlap gene1 terms", probTerms );
        Collection<OntologyTerm> terms2 = geneOntologyService.getGOTerms( gene2 );
        goMetric.logIds( "computeSimilarityOverlap gene2 terms", terms2 );
        probTerms.addAll( terms2 );

        Map<String, Double> probMap = new HashMap<String, Double>();

        for ( OntologyTerm t : probTerms ) {

            if ( t.getUri().equalsIgnoreCase( "http://purl.org/obo/owl/GO#GO_0042592" ) )
                probMap.put( t.getUri(), 0.1 );
            else
                probMap.put( t.getUri(), 0.5 );
        }

        Double value = goMetric.computeSimilarity( gene1, gene2, probMap, chooseMetric );

        if ( chooseMetric.equals( Metric.simple ) ) assertEquals( 3.0, value );
        if ( chooseMetric.equals( Metric.resnik ) ) assertEquals( 1.4978661367769954, value );
        if ( chooseMetric.equals( Metric.lin ) ) assertEquals( 2.160964047443681, value );
        if ( chooseMetric.equals( Metric.jiang ) ) assertEquals( 0.6185149837908823, value );
    }
    
    
    /**
     * @param geneOntologyService the geneOntologyService to set
     */
    public void setGeneOntologyService( GeneOntologyService geneOntologyService ) {
        this.geneOntologyService = geneOntologyService;
    }
}
