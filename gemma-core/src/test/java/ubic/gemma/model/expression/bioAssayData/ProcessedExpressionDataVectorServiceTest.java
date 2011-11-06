/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

package ubic.gemma.model.expression.bioAssayData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * @author Paul
 * @version $Id$
 */
public class ProcessedExpressionDataVectorServiceTest extends BaseSpringContextTest {

    @Autowired
    private ProcessedExpressionDataVectorService processedDataVectorService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private GeoDatasetService geoService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    Collection<ExpressionExperiment> ees;

    @After
    public void after() {
        if ( ees.isEmpty() ) return;
        try {
            for ( ExpressionExperiment ee : ees ) {
                expressionExperimentService.delete( ee );
            }
        } catch ( Exception e ) {

        }
    }

    @Before
    public void before() throws Exception {
        ees = getDataset();
    }

    /**
     * Test method for
     * {@link ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDaoImpl#getProcessedDataArrays(java.util.Collection, java.util.Collection)}
     * .
     */
    @Test
    public void testGetProcessedDataMatrices() throws Exception {

        if ( ees == null ) {
            log.error( "Test skipped because of failure to fetch data." );
            return;
        }

        ExpressionExperiment ee = ees.iterator().next();

        ee = processedDataVectorService.createProcessedDataVectors( ee );

        Collection<ProcessedExpressionDataVector> createProcessedDataVectors = ee.getProcessedExpressionDataVectors();

        assertEquals( 40, createProcessedDataVectors.size() );
        Collection<DoubleVectorValueObject> v = processedDataVectorService.getProcessedDataArrays( ee );
        assertEquals( 40, v.size() );

        Collection<Gene> genes = getGeneAssociatedWithEe( ee );
        v = processedDataVectorService.getProcessedDataArrays( ees, genes );
        assertTrue( "got " + v.size() + ", expected at least 40", 40 <= v.size() );

    }

    /**
     * @return
     */
    private Collection<ExpressionExperiment> getDataset() throws Exception {
        // Dataset uses spotted arrays, 11 samples.
        String path = ConfigUtils.getString( "gemma.home" );
        assert path != null;
        ExpressionExperiment newee;
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path
                    + AbstractGeoServiceTest.GEO_TEST_DATA_ROOT + "gse432Short" ) );
            Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService.fetchAndLoad(
                    "GSE432", false, true, false, false );
            newee = results.iterator().next();

            TwoChannelMissingValues tcmv = ( TwoChannelMissingValues ) this.getBean( "twoChannelMissingValues" );
            tcmv.computeMissingValues( newee, 1.5, null );
            // No masked preferred computation.
        } catch ( AlreadyExistsInSystemException e ) {
            if ( e.getData() instanceof List ) {
                newee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).iterator().next();
            } else {
                newee = ( ExpressionExperiment ) e.getData();
            }
        } catch ( Exception e ) {
            if ( e.getCause() instanceof IOException && e.getCause().getMessage().contains( "502" ) ) {
                return null;
            }
            throw e;
        }

        newee.setShortName( RandomStringUtils.randomAlphabetic( 12 ) );
        expressionExperimentService.update( newee );

        newee = this.expressionExperimentService.thawLite( newee );

        processedDataVectorService.createProcessedDataVectors( newee );
        Collection<ExpressionExperiment> e = new HashSet<ExpressionExperiment>();
        e.add( newee );
        return e;
    }

    /**
     * @return
     */
    private Collection<Gene> getGeneAssociatedWithEe( ExpressionExperiment ee ) {
        Collection<ArrayDesign> ads = this.expressionExperimentService.getArrayDesignsUsed( ee );
        Collection<Gene> genes = new HashSet<Gene>();
        for ( ArrayDesign ad : ads ) {
            Taxon taxon = this.getTaxon( "mouse" );
            ad = this.arrayDesignService.thaw( ad );

            for ( CompositeSequence cs : ad.getCompositeSequences() ) {
                Gene g = this.getTestPeristentGene();
                BlatAssociation blata = BlatAssociation.Factory.newInstance();
                blata.setGeneProduct( g.getProducts().iterator().next() );
                BlatResult br = BlatResult.Factory.newInstance();
                BioSequence bs = BioSequence.Factory.newInstance();
                bs.setName( RandomStringUtils.random( 10 ) );
                bs.setTaxon( taxon );
                bs = ( BioSequence ) persisterHelper.persist( bs );

                assertNotNull( bs );

                cs.setBiologicalCharacteristic( bs );
                compositeSequenceService.update( cs );

                cs = compositeSequenceService.load( cs.getId() );

                assertNotNull( cs.getBiologicalCharacteristic() );

                br.setQuerySequence( bs );
                blata.setBlatResult( br );
                blata.setBioSequence( bs );
                persisterHelper.persist( blata );
                genes.add( g );
            }

        }
        return genes;
    }

}
