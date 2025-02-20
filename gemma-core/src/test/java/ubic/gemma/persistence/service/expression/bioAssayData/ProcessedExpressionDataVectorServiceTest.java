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

package ubic.gemma.persistence.service.expression.bioAssayData;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.persistence.util.IdentifiableUtils;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeNoException;

/**
 * @author Paul
 */
public class ProcessedExpressionDataVectorServiceTest extends AbstractGeoServiceTest {

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
    private TwoChannelMissingValues tcmv;
    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    /* fixtures */
    private final Collection<ExpressionExperiment> ees = new ArrayList<>();
    private final Collection<ArrayDesign> ads = new ArrayList<>();

    @After
    public void after() {
        for ( ExpressionExperiment ee : ees ) {
            expressionExperimentService.remove( ee );
        }
        for ( ArrayDesign ad : ads ) {
            arrayDesignService.remove( ad );
        }
    }

    @Test
    public void testReplaceProcessedDataVectors() throws QuantitationTypeConversionException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee = expressionExperimentService.create( ee );
        ees.add( ee );
        ArrayDesign platform = new ArrayDesign();
        platform.setPrimaryTaxon( getTaxon( "human" ) );
        for ( int i = 0; i < 10; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "cs" + i, platform );
            platform.getCompositeSequences().add( cs );
        }
        platform = arrayDesignService.create( platform );
        ads.add( platform );
        assertThat( platform.getCompositeSequences() ).hasSize( 10 );
        BioAssayDimension bad = new BioAssayDimension();
        bad = bioAssayDimensionService.create( bad );
        QuantitationType rawQt = QuantitationType.Factory.newInstance();
        rawQt.setGeneralType( GeneralType.QUANTITATIVE );
        rawQt.setType( StandardQuantitationType.AMOUNT );
        rawQt.setScale( ScaleType.LOG2 );
        rawQt.setRepresentation( PrimitiveType.DOUBLE );
        rawQt.setIsPreferred( true );
        ee.getQuantitationTypes().add( rawQt );
        for ( CompositeSequence cs : platform.getCompositeSequences() ) {
            RawExpressionDataVector vec = new RawExpressionDataVector();
            vec.setExpressionExperiment( ee );
            vec.setDesignElement( cs );
            vec.setBioAssayDimension( bad );
            vec.setQuantitationType( rawQt );
            vec.setData( new byte[0] );
            ee.getRawExpressionDataVectors().add( vec );
        }
        expressionExperimentService.update( ee );
        assertEquals( 10, processedDataVectorService.createProcessedDataVectors( ee, false ) );
        Set<ProcessedExpressionDataVector> createdVectors = ee.getProcessedExpressionDataVectors();
        assertThat( createdVectors ).hasSize( 10 );
        ee = expressionExperimentService.thaw( ee );
        assertThat( ee.getNumberOfDataVectors() )
                .isEqualTo( 10 );
        assertThat( ee.getProcessedExpressionDataVectors() )
                .containsExactlyInAnyOrderElementsOf( createdVectors );
        // effectively transform the processed vectors
        List<ProcessedExpressionDataVector> newVectors = new ArrayList<>();
        for ( ProcessedExpressionDataVector v : ee.getProcessedExpressionDataVectors() ) {
            ProcessedExpressionDataVector newVector = new ProcessedExpressionDataVector();
            newVector.setExpressionExperiment( ee );
            newVector.setBioAssayDimension( bad );
            newVector.setQuantitationType( v.getQuantitationType() );
            newVector.setDesignElement( v.getDesignElement() );
            newVector.setData( v.getData() );
            newVectors.add( newVector );
        }
        processedDataVectorService.replaceProcessedDataVectors( ee, newVectors, false );
        ee = expressionExperimentService.thaw( ee );
        assertThat( ee.getProcessedExpressionDataVectors() )
                .containsExactlyInAnyOrderElementsOf( newVectors );
        assertThat( ee.getNumberOfDataVectors() )
                .isEqualTo( 10 );
    }

    /**
     * Test method for
     * {@link ProcessedExpressionDataVectorService#getProcessedDataArrays(java.util.Collection, java.util.Collection)}
     * .
     */
    @Test
    @Category(SlowTest.class)
    public void testGetProcessedDataMatrices() throws Exception {
        ExpressionExperiment ee;
        try {
            ee = this.getDataset();
        } catch ( Exception e ) {
            if ( e.getCause() instanceof IOException && e.getCause().getMessage().contains( "502" ) ) {
                assumeNoException( "Test skipped because of failure to fetch data.", e );
                return;
            } else {
                throw e;
            }
        }

        assertEquals( 40, processedDataVectorService.createProcessedDataVectors( ee, false ) );
        Collection<DoubleVectorValueObject> v = processedDataVectorService.getProcessedDataArrays( ee );
        assertEquals( 40, v.size() );

        Collection<Gene> genes = this.getGeneAssociatedWithEe( ee );
        tableMaintenanceUtil.disableEmail();
        tableMaintenanceUtil.updateGene2CsEntries();

        assertEquals( 100, genes.size() );
        v = processedDataVectorService.getProcessedDataArrays( Collections.singleton( ee ), IdentifiableUtils.getIds( genes ) );
        assertTrue( "got " + v.size() + ", expected at least 40", 40 <= v.size() );
    }

    private ExpressionExperiment getDataset() throws Exception {
        // Dataset uses spotted arrays, 11 samples.

        ExpressionExperiment ee;
        try {
            geoService.setGeoDomainObjectGenerator(
                    new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath( "gse432Short" ) ) );
            //noinspection unchecked
            Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService
                    .fetchAndLoad( "GSE432", false, true, false );
            ees.addAll( results );
            ee = results.iterator().next();
            tcmv.computeMissingValues( ee, 1.5, null );
            // No masked preferred computation.
        } catch ( AlreadyExistsInSystemException e ) {
            if ( e.getData() instanceof List ) {
                ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).iterator().next();
            } else {
                ee = ( ExpressionExperiment ) e.getData();
            }
        }

        ee.setShortName( RandomStringUtils.randomAlphabetic( 12 ) );
        expressionExperimentService.update( ee );
        ee = expressionExperimentService.thaw( ee );
        processedDataVectorService.createProcessedDataVectors( ee, false );
        return ee;
    }

    private Collection<Gene> getGeneAssociatedWithEe( ExpressionExperiment ee ) {
        Collection<ArrayDesign> ads = this.expressionExperimentService.getArrayDesignsUsed( ee );
        Collection<Gene> genes = new HashSet<>();
        ads = arrayDesignService.thaw( ads );
        for ( ArrayDesign ad : ads ) {
            Taxon taxon = this.getTaxon( "mouse" );
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
                assertNotNull( cs );

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
