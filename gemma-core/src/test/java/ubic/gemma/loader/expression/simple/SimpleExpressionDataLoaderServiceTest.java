/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.loader.expression.simple;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.RandomStringUtils;

import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class SimpleExpressionDataLoaderServiceTest extends BaseSpringContextTest {

    /**
     * Test method for
     * {@link ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService#loadPersistentModel(ubic.gemma.loader.expression.simple.model.ExpressionExperimentMetaData, java.io.InputStream)}
     * .
     */
    public final void testLoad() throws Exception {
        SimpleExpressionDataLoaderService service = ( SimpleExpressionDataLoaderService ) this
                .getBean( "simpleExpressionDataLoaderService" );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        ads.add( ad );
        metaData.setArrayDesigns( ads );

        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( "mouse" );
        metaData.setTaxon( taxon );
        metaData.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        metaData.setQuantitationTypeName( "testing" );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.AMOUNT );
        metaData.setIsRatio( true );

        InputStream data = this.getClass().getResourceAsStream( "/data/testdata.txt" );

        ExpressionExperiment ee = service.load( metaData, data );

        ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
                .getBean( "expressionExperimentService" );
        eeService.thaw( ee );

        assertNotNull( ee );
        assertEquals( 30, ee.getRawExpressionDataVectors().size() );
        assertEquals( 12, ee.getBioAssays().size() );
    }

    /**
     * @throws Exception
     *         {@link ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService#loadPersistentModel(ubic.gemma.loader.expression.simple.model.ExpressionExperimentMetaData, java.io.InputStream)}
     *         .
     */
    public final void testLoadB() throws Exception {
        SimpleExpressionDataLoaderService service = ( SimpleExpressionDataLoaderService ) this
                .getBean( "simpleExpressionDataLoaderService" );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        ads.add( ad );
        metaData.setArrayDesigns( ads );

        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( "mouse" );
        metaData.setTaxon( taxon );
        metaData.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        metaData.setQuantitationTypeName( "testing" );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.AMOUNT );
        metaData.setIsRatio( true );

        InputStream data = this.getClass().getResourceAsStream(
                "/data/loader/aov.results-2-monocyte-data-bytime.bypat.data.sort" );

        ExpressionExperiment ee = service.load( metaData, data );

        ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
                .getBean( "expressionExperimentService" );
        eeService.thaw( ee );

        assertNotNull( ee );
        assertEquals( 200, ee.getRawExpressionDataVectors().size() );
        assertEquals( 59, ee.getBioAssays().size() );
        // setComplete();
    }

    public final void testLoadImageCloneDesign() throws Exception {
        endTransaction();
        SimpleExpressionDataLoaderService service = ( SimpleExpressionDataLoaderService ) this
                .getBean( "simpleExpressionDataLoaderService" );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        ads.add( ad );
        metaData.setArrayDesigns( ads );

        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( "human" );
        metaData.setTaxon( taxon );
        metaData.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        metaData.setQuantitationTypeName( "testing" );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.AMOUNT );
        metaData.setIsRatio( true );
        metaData.setProbeIdsAreImageClones( true );

        InputStream data = this.getClass().getResourceAsStream( "/data/loader/expression/luo-prostate.sample.txt" );

        ExpressionExperiment ee = service.load( metaData, data );

        ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
                .getBean( "expressionExperimentService" );
        eeService.thaw( ee );

        assertNotNull( ee );

        DesignElementDataVectorService dedvs = ( DesignElementDataVectorService ) this
                .getBean( "designElementDataVectorService" );

        for ( RawExpressionDataVector vector : ee.getRawExpressionDataVectors() ) {
            dedvs.thaw( vector );
            assertTrue( ( ( CompositeSequence ) vector.getDesignElement() ).getBiologicalCharacteristic().getName()
                    .startsWith( "IMAGE:" ) );
        }

        assertEquals( 173, ee.getRawExpressionDataVectors().size() );
        assertEquals( 25, ee.getBioAssays().size() );
    }

    public final void testLoadSimilarDatasets() throws Exception {
        ExpressionExperiment ee1 = load();
        ExpressionExperiment ee2 = load();

        assertEquals( 8, ee1.getBioAssays().size() );
        assertEquals( ee1.getBioAssays().size(), ee2.getBioAssays().size() );
    }

    /**
     * @return
     * @throws IOException
     */
    private ExpressionExperiment load() throws IOException {
        InputStream data = this.getClass().getResourceAsStream(
                "/data/loader/expression/experimentalDesignTestData.txt" );

        SimpleExpressionDataLoaderService s = ( SimpleExpressionDataLoaderService ) this
                .getBean( "simpleExpressionDataLoaderService" );

        ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
                .getBean( "expressionExperimentService" );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();

        Taxon human = taxonService.findByCommonName( "human" );

        metaData.setShortName( RandomStringUtils.randomAlphabetic( 10 ) );
        metaData.setDescription( "bar" );
        metaData.setIsRatio( false );
        metaData.setTaxon( human );
        metaData.setQuantitationTypeName( "rma" );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.AMOUNT );

        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setShortName( "foobly" );
        ad.setName( "foobly foo" );

        metaData.getArrayDesigns().add( ad );

        ExpressionExperiment ee = s.load( metaData, data );
        eeService.thawLite( ee );
        return ee;
    }

}
