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
package ubic.gemma.analysis.preprocess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueService;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ProcessedExpressionDataCreateServiceTest extends AbstractGeoServiceTest {

    @Autowired
    ExpressionExperimentService eeService;

    @Autowired
    GeoService geoService;

    @Autowired
    ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Autowired
    ExperimentalFactorService experimentalFactorService;

    @Autowired
    ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    BioMaterialService bioMaterialService;

    @Autowired
    BioAssayService bioAssayService;

    @Autowired
    FactorValueService factorValueService;

    ExpressionExperiment ee = null;

    @Test
    public void testComputeDevRankForExpressionExperimentB() throws Exception {

        String path = getTestFileBasePath();

        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                    + "GSE5949short" ) );
            Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService.fetchAndLoad(
                    "GSE5949", false, true, false, false );
            this.ee = results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            this.ee = ( ( Collection<ExpressionExperiment> ) e.getData() ).iterator().next();
        }

        ee = eeService.thawLite( ee );

        Collection<ProcessedExpressionDataVector> preferredVectors = processedExpressionDataVectorCreateService
                .computeProcessedExpressionData( ee );

        ee = eeService.load( ee.getId() );
        ee = eeService.thawLite( ee );

        for ( ProcessedExpressionDataVector d : preferredVectors ) {
            assertTrue( d.getQuantitationType().getIsMaskedPreferred() );
            assertTrue( ee.getQuantitationTypes().contains( d.getQuantitationType() ) );
            assertNotNull( d.getRankByMean() );
            assertNotNull( d.getRankByMax() );
        }
    }

    @Test
    public void testReorder() throws Exception {

        String path = getTestFileBasePath();

        ExpressionExperiment old = eeService.findByShortName( "GSE404" );
        if ( old != null ) {
            eeService.delete( old );
        }

        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                    + "gse404Short" ) );
            Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService.fetchAndLoad(
                    "GSE404", false, true, false, false );
            this.ee = results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            this.ee = ( ExpressionExperiment ) e.getData();
        }

        ee = eeService.thawLite( ee );
        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );

        ExperimentalFactor factor = ExperimentalFactor.Factory.newInstance();
        factor.setType( FactorType.CATEGORICAL );
        factor.setName( ee.getShortName() + " design" );
        factor.setExperimentalDesign( ee.getExperimentalDesign() );
        factor = experimentalFactorService.create( factor );

        ee.getExperimentalDesign().getExperimentalFactors().add( factor );
        eeService.update( ee );

        FactorValue fv1 = FactorValue.Factory.newInstance();
        FactorValue fv2 = FactorValue.Factory.newInstance();
        fv1.setValue( "foo" );
        fv1.setExperimentalFactor( factor );
        fv2.setValue( "bar" );
        fv2.setIsBaseline( true );
        fv2.setExperimentalFactor( factor );

        fv1 = factorValueService.create( fv1 );
        fv2 = factorValueService.create( fv2 );

        factor.getFactorValues().add( fv1 );
        factor.getFactorValues().add( fv2 );

        experimentalFactorService.update( factor );

        ee = eeService.thaw( ee );

        List<BioAssay> basInOrder = new ArrayList<BioAssay>( ee.getBioAssays() );

        Collections.sort( basInOrder, new Comparator<BioAssay>() {

            @Override
            public int compare( BioAssay o1, BioAssay o2 ) {
                return o1.getId().compareTo( o2.getId() );
            }
        } );

        int i = 0;
        for ( BioAssay ba : basInOrder ) {
            // bioAssayService.thaw( ba );
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                assert fv1.getId() != null;
                if ( !bm.getFactorValues().isEmpty() ) {
                    continue;
                }
                if ( i % 2 == 0 ) {
                    bm.getFactorValues().add( fv1 );
                    // log.info( bm + " " + bm.getId() + " => " + fv1 );
                } else {
                    bm.getFactorValues().add( fv2 );
                    // log.info( bm + " " + bm.getId() + " => " + fv2 );
                }

                bioMaterialService.update( bm );
            }
            i++;
        }

        assertEquals( 2, factor.getFactorValues().size() );

        ee.getExperimentalDesign().getExperimentalFactors().add( factor );

        /*
         * All that was setup. Now do the interesting bit
         */

        processedExpressionDataVectorCreateService.reorderByDesign( ee );

        /*
         * Now check the vectors...
         */
        Collection<ProcessedExpressionDataVector> resortedVectors = processedExpressionDataVectorService
                .getProcessedDataVectors( ee );

        // ExpressionDataDoubleMatrix newMat = new ExpressionDataDoubleMatrix( resortedVectors );
        // log.info( newMat );

        boolean foundVector = false;

        assertTrue( resortedVectors.size() > 0 );

        for ( ProcessedExpressionDataVector vector : resortedVectors ) {
            i = 0;
            log.debug( vector.getDesignElement().getName() + " ........................." );

            for ( BioAssay ba : vector.getBioAssayDimension().getBioAssays() ) {

                assertEquals( 1, ba.getSamplesUsed().size() );

                for ( BioMaterial bm : ba.getSamplesUsed() ) {
                    assertEquals( 1, bm.getFactorValues().size() );

                    FactorValue fv = bm.getFactorValues().iterator().next();

                    assertNotNull( fv.getId() );
                    log.debug( ba.getId() + " " + fv.getId() + " " + fv );
                    if ( i < 10 ) {
                        assertEquals( fv2, fv ); // first because it is baseline;
                    }

                }
                i++;

            }

            /*
             * spot check the data, same place as before.
             */
            if ( vector.getDesignElement().getName().equals( "40" ) ) {
                foundVector = true;
                ByteArrayConverter conv = new ByteArrayConverter();
                Double[] d = ArrayUtils.toObject( conv.byteArrayToDoubles( vector.getData() ) );
                assertEquals( 20, d.length );
                assertEquals( -0.08, d[1], 0.001 );
                assertEquals( 0.45, d[10], 0.001 );
                assertEquals( Double.NaN, d[19], 0.001 );
            }
        }

        assertTrue( "test vector not found", foundVector );

    }
}
