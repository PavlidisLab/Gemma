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

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.TableMaintenanceUtil;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorDaoImpl;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.EntityUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Paul
 */
public class ProcessedExpressionDataVectorServiceTest extends AbstractGeoServiceTest {

    private Collection<ExpressionExperiment> ees;
    @Autowired
    private ProcessedExpressionDataVectorService processedDataVectorService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;
    @Autowired
    private GeoService geoService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private CompositeSequenceService compositeSequenceService;
    @Autowired
    TwoChannelMissingValues tcmv;

    @After
    public void after() {
        if ( ees == null || ees.isEmpty() )
            return;

        for ( ExpressionExperiment ee : ees ) {
            expressionExperimentService.remove( ee );
        }

    }

    @Before
    public void before() throws Exception {
        ees = this.getDataset();
    }

    /**
     * Test method for
     * {@link ProcessedExpressionDataVectorDaoImpl#getProcessedDataArrays(java.util.Collection, java.util.Collection)}
     * .
     */
    @Test
    @Category(SlowTest.class)
    public void testGetProcessedDataMatrices() {

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

        Collection<Gene> genes = this.getGeneAssociatedWithEe( ee );
        tableMaintenanceUtil.disableEmail();
        tableMaintenanceUtil.updateGene2CsEntries();

        v = processedDataVectorService.getProcessedDataArrays( ees, EntityUtils.getIds( genes ) );
        assertTrue( "got " + v.size() + ", expected at least 40", 40 <= v.size() );

        processedDataVectorService.clearCache();

    }

    private Collection<ExpressionExperiment> getDataset() throws Exception {
        // Dataset uses spotted arrays, 11 samples.

        ExpressionExperiment ee;
        try {
            geoService.setGeoDomainObjectGenerator(
                    new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath( "gse432Short" ) ) );
            //noinspection unchecked
            Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService
                    .fetchAndLoad( "GSE432", false, true, false );
            ee = results.iterator().next();

            tcmv.computeMissingValues( ee, 1.5, null );
            // No masked preferred computation.
        } catch ( AlreadyExistsInSystemException e ) {
            if ( e.getData() instanceof List ) {
                ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).iterator().next();
            } else {
                ee = ( ExpressionExperiment ) e.getData();
            }
        } catch ( Exception e ) {
            if ( e.getCause() instanceof IOException && e.getCause().getMessage().contains( "502" ) ) {
                return null;
            }
            throw e;
        }

        ee.setShortName( RandomStringUtils.randomAlphabetic( 12 ) );
        expressionExperimentService.update( ee );
        ee = expressionExperimentService.thawLite( ee );
        processedDataVectorService.createProcessedDataVectors( ee );
        Collection<ExpressionExperiment> e = new HashSet<>();
        e.add( ee );
        return e;
    }

    private Collection<Gene> getGeneAssociatedWithEe( ExpressionExperiment ee ) {
        Collection<ArrayDesign> ads = this.expressionExperimentService.getArrayDesignsUsed( ee );
        Collection<Gene> genes = new HashSet<>();
        for ( ArrayDesign ad : ads ) {
            Taxon taxon = this.getTaxon( "mouse" );
            ad = this.arrayDesignService.thaw( ad );

            for ( CompositeSequence cs : ad.getCompositeSequences() ) {
                Gene g = this.getTestPersistentGene();
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
