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

import java.util.Collection;
import java.util.HashSet;

import net.sf.ehcache.Cache;

import org.apache.commons.lang.RandomStringUtils;

import ubic.gemma.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.AbstractGeoService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.testing.AbstractGeoServiceTest;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * @author Paul
 * @version $Id$
 */
public class ProcessedExpressionDataVectorDaoImplTest extends BaseSpringContextTest {

    ProcessedExpressionDataVectorDao processedDataVectorDao;
    ExpressionExperimentService expressionExperimentService;
    ExpressionExperiment newee = null;
    DesignElementDataVector dedv;
    protected AbstractGeoService geoService;
    ArrayDesignService arrayDesignService;
    TaxonService taxonService;
    CompositeSequenceService compositeSequenceService;

    /**
     * @param designElementDataVectorDao the designElementDataVectorDao to set
     */
    public void setProcessedExpressionDataVectorDao( ProcessedExpressionDataVectorDao designElementDataVectorDao ) {
        this.processedDataVectorDao = designElementDataVectorDao;
    }

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        dedv = RawExpressionDataVector.Factory.newInstance();
        expressionExperimentService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        arrayDesignService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        geoService = ( AbstractGeoService ) this.getBean( "geoDatasetService" );
        compositeSequenceService = ( CompositeSequenceService ) this.getBean( "compositeSequenceService" );
        taxonService = ( TaxonService ) this.getBean( "taxonService" );
    }

    protected void onTearDownAfterTransaction() throws Exception {
        super.onTearDownAfterTransaction();

        if ( newee != null && newee.getId() != null ) {
            // expressionExperimentService.delete( newee );
        }
    }

    /**
     * Test method for
     * {@link ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDaoImpl#getProcessedDataArrays(java.util.Collection, java.util.Collection)}.
     */
    @SuppressWarnings("unchecked")
    public void testGetProcessedDataMatrices() {
        endTransaction();

        Collection<ExpressionExperiment> ees = getDataset();

        Collection<Gene> genes = getGeneAssociatedWithEe();
        processedDataVectorDao.createProcessedDataVectors( ees.iterator().next() );
        Collection<DoubleVectorValueObject> v = processedDataVectorDao.getProcessedDataArrays( ees, genes );
        assertEquals( 40, v.size() );
    }

    public void testGetProcessedDataCache() {
        endTransaction();
        Collection<ExpressionExperiment> ees = getDataset();
        Collection<Gene> genes = getGeneAssociatedWithEe();
        Collection<DoubleVectorValueObject> v = processedDataVectorDao.getProcessedDataArrays( ees, genes );
        assertEquals( 40, v.size() );
        Cache cache = ProcessedDataVectorCache.getCache( ees.iterator().next() );
        cache.clearStatistics();
        processedDataVectorDao.getProcessedDataArrays( ees, genes );
        v = processedDataVectorDao.getProcessedDataArrays( ees, genes );
        assertEquals( 40, v.size() );
        long hits = cache.getStatistics().getCacheHits();

        // It really should be 86!
        assertTrue( hits > 80 );
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<ExpressionExperiment> getDataset() {
        // Dataset uses spotted arrays, 11 samples.
        String path = ConfigUtils.getString( "gemma.home" );
        assert path != null;
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path
                    + AbstractGeoServiceTest.GEO_TEST_DATA_ROOT + "gse432Short" ) );
            Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService.fetchAndLoad(
                    "GSE432", false, true, false, false, false );
            newee = results.iterator().next();
            newee.setShortName( RandomStringUtils.randomAlphabetic( 12 ) );
            expressionExperimentService.update( newee );
            TwoChannelMissingValues tcmv = ( TwoChannelMissingValues ) this.getBean( "twoChannelMissingValues" );
            tcmv.computeMissingValues( newee, 1.5, null );
            // No masked preferred computation.
        } catch ( AlreadyExistsInSystemException e ) {
            newee = ( ExpressionExperiment ) e.getData();
        }

        this.expressionExperimentService.thawLite( newee );
        processedDataVectorDao.createProcessedDataVectors( newee );
        Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
        ees.add( newee );
        return ees;
    }

    /**
     * @return
     */
    private Collection<Gene> getGeneAssociatedWithEe() {
        int i = 0;
        ArrayDesign ad = newee.getBioAssays().iterator().next().getArrayDesignUsed();
        Taxon taxon = taxonService.findByCommonName( "mouse" );
        this.arrayDesignService.thawLite( ad );
        Collection<Gene> genes = new HashSet<Gene>();
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            if ( i >= 10 ) break;
            Gene g = this.getTestPeristentGene();
            BlatAssociation blata = BlatAssociation.Factory.newInstance();
            blata.setGeneProduct( g.getProducts().iterator().next() );
            BlatResult br = BlatResult.Factory.newInstance();
            BioSequence bs = cs.getBiologicalCharacteristic();
            if ( bs == null ) {
                bs = BioSequence.Factory.newInstance();
                bs.setName( RandomStringUtils.random( 10 ) );
                bs.setTaxon( taxon );
                bs = ( BioSequence ) persisterHelper.persist( bs );
                cs.setBiologicalCharacteristic( bs );
                compositeSequenceService.update( cs );
            }

            br.setQuerySequence( bs );
            blata.setBlatResult( br );
            blata.setBioSequence( bs );
            persisterHelper.persist( blata );
            genes.add( g );
        }
        return genes;
    }

}
