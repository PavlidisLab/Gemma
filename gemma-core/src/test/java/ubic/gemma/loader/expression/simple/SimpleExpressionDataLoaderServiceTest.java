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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class SimpleExpressionDataLoaderServiceTest extends BaseSpringContextTest {

    ExpressionExperiment ee;

    @Autowired
    ExpressionExperimentService eeService;

    @Autowired
    SimpleExpressionDataLoaderService service;

    @After
    public void after() {
        if ( ee != null ) {
            ee = eeService.load( ee.getId() );
            eeService.delete( ee );
        }
    }

    @Test
    public final void testLoad() throws Exception {

        Taxon taxon = this.getTaxon( "mouse" );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        ad.setPrimaryTaxon( taxon );
        ad.setTechnologyType( TechnologyType.ONECOLOR );

        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        ads.add( ad );
        metaData.setArrayDesigns( ads );

        metaData.setTaxon( taxon );
        metaData.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        metaData.setQuantitationTypeName( "testing" );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.AMOUNT );
        metaData.setIsRatio( true );

        InputStream data = this.getClass().getResourceAsStream( "/data/testdata.txt" );

        ee = service.create( metaData, data );

        ee = eeService.thaw( ee );

        assertNotNull( ee );
        assertEquals( 30, ee.getRawExpressionDataVectors().size() );
        assertEquals( 12, ee.getBioAssays().size() );
    }

    @Test
    public final void testLoadDuplicatedRow() throws Exception {

        Taxon taxon = this.getTaxon( "mouse" );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        ad.setPrimaryTaxon( taxon );
        ad.setTechnologyType( TechnologyType.ONECOLOR );
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();

        ads.add( ad );
        metaData.setArrayDesigns( ads );

        metaData.setTaxon( taxon );
        metaData.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        metaData.setQuantitationTypeName( "testing" );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.AMOUNT );
        metaData.setIsRatio( true );

        InputStream data = this.getClass().getResourceAsStream( "/data/testdata.duprow.txt" );

        try {
            ee = service.create( metaData, data );
            fail( "Should have gotten an exception about duplicated row" );
        } catch ( IllegalArgumentException e ) {
            // expected
        }

    }

    /**
     *  
     */
    @Test
    public final void testLoadB() throws Exception {

        Taxon taxon = this.getTaxon( "mouse" );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        ad.setPrimaryTaxon( taxon );
        ad.setTechnologyType( TechnologyType.ONECOLOR );
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        ads.add( ad );
        metaData.setArrayDesigns( ads );

        metaData.setTaxon( taxon );
        metaData.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        metaData.setQuantitationTypeName( "testing" );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.AMOUNT );
        metaData.setIsRatio( true );

        InputStream data = this.getClass().getResourceAsStream(
                "/data/loader/aov.results-2-monocyte-data-bytime.bypat.data.sort" );

        ee = service.create( metaData, data );

        ee = eeService.thaw( ee );

        assertNotNull( ee );
        assertEquals( 200, ee.getRawExpressionDataVectors().size() );
        assertEquals( 59, ee.getBioAssays().size() );
        //
    }

}
