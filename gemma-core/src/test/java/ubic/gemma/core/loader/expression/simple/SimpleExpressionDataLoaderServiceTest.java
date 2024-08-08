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
package ubic.gemma.core.loader.expression.simple;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.*;

/**
 * @author pavlidis
 */
@Category(SlowTest.class)
public class SimpleExpressionDataLoaderServiceTest extends BaseSpringContextTest {

    private ExpressionExperiment ee;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private SimpleExpressionDataLoaderService service;

    @After
    public void after() {
        if ( ee != null ) {
            eeService.remove( ee );
        }
    }

    @Test
    public final void testLoad() throws Exception {

        Taxon taxon = this.getTaxon( "mouse" );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setShortName( RandomStringUtils.insecure().nextAlphabetic( 5 ) );
        ad.setName( RandomStringUtils.insecure().nextAlphabetic( 5 ) );
        ad.setPrimaryTaxon( taxon );
        ad.setTechnologyType( TechnologyType.ONECOLOR );

        Collection<ArrayDesign> ads = new HashSet<>();
        ads.add( ad );
        metaData.setArrayDesigns( ads );

        metaData.setTaxon( taxon );
        metaData.setShortName( RandomStringUtils.insecure().nextAlphabetic( 5 ) );
        metaData.setName( RandomStringUtils.insecure().nextAlphabetic( 5 ) );
        metaData.setDescription( "Simple expression data loader service test - load" );
        metaData.setQuantitationTypeName( "testing" );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.AMOUNT );
        metaData.setIsRatio( true );

        try (InputStream data = this.getClass().getResourceAsStream( "/data/testdata.txt" )) {

            ee = service.create( metaData, data );
        }
        ee = eeService.thaw( ee );

        assertNotNull( ee );
        assertEquals( 30, ee.getRawExpressionDataVectors().size() );
        assertEquals( 12, ee.getBioAssays().size() );
    }

    @Test
    public final void testLoadB() throws Exception {

        Taxon taxon = this.getTaxon( "mouse" );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setShortName( RandomStringUtils.insecure().nextAlphabetic( 5 ) );

        ad.setName( RandomStringUtils.insecure().nextAlphabetic( 5 ) );
        ad.setPrimaryTaxon( taxon );
        ad.setTechnologyType( TechnologyType.ONECOLOR );
        Collection<ArrayDesign> ads = new HashSet<>();
        ads.add( ad );
        metaData.setArrayDesigns( ads );

        metaData.setTaxon( taxon );
        metaData.setName( RandomStringUtils.insecure().nextAlphabetic( 5 ) );
        metaData.setShortName( metaData.getName() );
        metaData.setDescription( "Simple expression data loader service test - load B" );
        metaData.setQuantitationTypeName( "testing" );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.AMOUNT );
        metaData.setIsRatio( true );

        try (InputStream data = this.getClass()
                .getResourceAsStream( "/data/loader/aov.results-2-monocyte-data-bytime.bypat.data.sort" )) {

            ee = service.create( metaData, data );
        }

        ee = eeService.thaw( ee );

        assertNotNull( ee );
        assertEquals( 200, ee.getRawExpressionDataVectors().size() );
        assertEquals( 59, ee.getBioAssays().size() );
        //
    }

    @Test(expected = IllegalArgumentException.class)
    public final void testLoadDuplicatedRow() throws Exception {

        Taxon taxon = this.getTaxon( "mouse" );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setShortName( RandomStringUtils.insecure().nextAlphabetic( 5 ) );

        ad.setName( RandomStringUtils.insecure().nextAlphabetic( 5 ) );
        ad.setPrimaryTaxon( taxon );
        ad.setTechnologyType( TechnologyType.ONECOLOR );
        Collection<ArrayDesign> ads = new HashSet<>();

        ads.add( ad );
        metaData.setArrayDesigns( ads );

        metaData.setTaxon( taxon );
        metaData.setName( RandomStringUtils.insecure().nextAlphabetic( 5 ) );
        metaData.setShortName( metaData.getName() );
        metaData.setQuantitationTypeName( "testing" );
        metaData.setDescription( "Simple expression data loader service test - load duplicate row" );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );
        metaData.setScale( ScaleType.LOG2 );
        metaData.setType( StandardQuantitationType.AMOUNT );
        metaData.setIsRatio( true );

        try (InputStream data = this.getClass().getResourceAsStream( "/data/testdata.duprow.txt" )) {

            ee = service.create( metaData, data );
            fail( "Should have gotten an exception about duplicated row" );

        }
    }

}
