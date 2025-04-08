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
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.core.loader.expression.simple.model.*;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author pavlidis
 */
@Category(SlowTest.class)
public class SimpleExpressionDataLoaderServiceTest extends BaseSpringContextTest {

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private SimpleExpressionDataLoaderService service;

    private ExpressionExperiment ee;

    @After
    public void after() {
        if ( ee != null ) {
            eeService.remove( ee );
        }
    }

    @Test
    public final void testLoad() throws Exception {
        SimpleExpressionExperimentMetadata metaData = new SimpleExpressionExperimentMetadata();
        SimplePlatformMetadata ad = new SimplePlatformMetadata();
        ad.setShortName( RandomStringUtils.randomAlphabetic( 5 ) );
        ad.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        ad.setTechnologyType( TechnologyType.ONECOLOR );

        Collection<SimplePlatformMetadata> ads = new HashSet<>();
        ads.add( ad );
        metaData.setArrayDesigns( ads );

        metaData.setTaxon( SimpleTaxonMetadata.forName( "mouse" ) );
        metaData.setShortName( RandomStringUtils.randomAlphabetic( 5 ) );
        metaData.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        metaData.setDescription( "Simple expression data loader service test - load" );
        SimpleQuantitationTypeMetadata qtMetadata = new SimpleQuantitationTypeMetadata();
        qtMetadata.setName( "testing" );
        qtMetadata.setGeneralType( GeneralType.QUANTITATIVE );
        qtMetadata.setScale( ScaleType.LOG2 );
        qtMetadata.setType( StandardQuantitationType.AMOUNT );
        qtMetadata.setIsRatio( true );
        metaData.setQuantitationType( qtMetadata );

        DoubleMatrix<String, String> matrix;
        try ( InputStream data = this.getClass().getResourceAsStream( "/data/testdata.txt" ) ) {
            matrix = new DoubleMatrixReader().read( data );
        }
        ee = service.create( metaData, matrix );
        ee = eeService.thaw( ee );

        assertThat( ee ).isNotNull();
        assertThat( ee.getRawExpressionDataVectors() ).hasSize( 30 );
        assertThat( ee.getBioAssays() ).hasSize( 12 );
    }

    @Test
    public final void testLoadB() throws Exception {
        SimpleExpressionExperimentMetadata metaData = new SimpleExpressionExperimentMetadata();
        SimplePlatformMetadata ad = new SimplePlatformMetadata();
        ad.setShortName( RandomStringUtils.randomAlphabetic( 5 ) );

        ad.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        ad.setTechnologyType( TechnologyType.ONECOLOR );
        Collection<SimplePlatformMetadata> ads = new HashSet<>();
        ads.add( ad );
        metaData.setArrayDesigns( ads );

        metaData.setTaxon( SimpleTaxonMetadata.forName( "mouse" ) );
        metaData.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        metaData.setShortName( metaData.getName() );
        metaData.setDescription( "Simple expression data loader service test - load B" );
        SimpleQuantitationTypeMetadata qtMetadata = new SimpleQuantitationTypeMetadata();
        qtMetadata.setName( "testing" );
        qtMetadata.setGeneralType( GeneralType.QUANTITATIVE );
        qtMetadata.setScale( ScaleType.LOG2 );
        qtMetadata.setType( StandardQuantitationType.AMOUNT );
        qtMetadata.setIsRatio( true );
        metaData.setQuantitationType( qtMetadata );

        DoubleMatrix<String, String> matrix;
        try ( InputStream data = this.getClass()
                .getResourceAsStream( "/data/loader/aov.results-2-monocyte-data-bytime.bypat.data.sort" ) ) {
            matrix = new DoubleMatrixReader().read( data );
        }

        ee = service.create( metaData, matrix );

        ee = eeService.thaw( ee );

        assertThat( ee ).isNotNull();
        assertThat( ee.getRawExpressionDataVectors() ).hasSize( 200 );
        assertThat( ee.getBioAssays() ).hasSize( 59 );
    }

    @Test
    public final void testLoadDuplicatedRow() throws Exception {
        SimpleExpressionExperimentMetadata metaData = new SimpleExpressionExperimentMetadata();
        SimplePlatformMetadata ad = new SimplePlatformMetadata();
        ad.setShortName( RandomStringUtils.randomAlphabetic( 5 ) );

        ad.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        ad.setTechnologyType( TechnologyType.ONECOLOR );
        Collection<SimplePlatformMetadata> ads = new HashSet<>();

        ads.add( ad );
        metaData.setArrayDesigns( ads );

        metaData.setTaxon( SimpleTaxonMetadata.forName( "mouse" ) );
        metaData.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        metaData.setShortName( metaData.getName() );
        SimpleQuantitationTypeMetadata qtMetadata = new SimpleQuantitationTypeMetadata();
        qtMetadata.setName( "testing" );
        qtMetadata.setDescription( "Simple expression data loader service test - load duplicate row" );
        qtMetadata.setGeneralType( GeneralType.QUANTITATIVE );
        qtMetadata.setScale( ScaleType.LOG2 );
        qtMetadata.setType( StandardQuantitationType.AMOUNT );
        qtMetadata.setIsRatio( true );
        metaData.setQuantitationType( qtMetadata );

        assertThatThrownBy( () -> {
            DoubleMatrix<String, String> matrix;
            try ( InputStream data = this.getClass().getResourceAsStream( "/data/testdata.duprow.txt" ) ) {
                matrix = new DoubleMatrixReader().read( data );
            }
            service.create( metaData, matrix );
        } )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "Duplicate row name gene14_at" );
    }

    @Test
    public void testLoadWithSampleMetadata() {
        ArrayDesign ad = getTestPersistentArrayDesign( 10, true );
        SimpleExpressionExperimentMetadata metaData = new SimpleExpressionExperimentMetadata();
        metaData.setShortName( RandomStringUtils.randomAlphabetic( 5 ) );
        metaData.setName( RandomStringUtils.randomAlphabetic( 5 ) );
        metaData.setTaxon( SimpleTaxonMetadata.forName( "mouse" ) );
        metaData.setAccession( SimpleDatabaseEntry.fromAccession( "GSE109291", "GEO" ) );

        metaData.getArrayDesigns().add( SimplePlatformMetadata.forId( ad.getId() ) );

        for ( int i = 0; i < 8; i++ ) {
            SimpleSampleMetadata sampleMetadata = new SimpleSampleMetadata();
            sampleMetadata.setName( "sample" + i );
            sampleMetadata.setDescription( "sample description " + i );
            sampleMetadata.setPlatformUsed( SimplePlatformMetadata.forId( ad.getId() ) );
            sampleMetadata.setAccession( SimpleDatabaseEntry.fromAccession( "GSM0000" + i, "GEO" ) );
            metaData.getSamples().add( sampleMetadata );
        }

        ee = service.create( metaData, null );

        ee = eeService.loadAndThaw( ee.getId() );
        assertThat( ee ).isNotNull();
        assertThat( ee.getAccession() ).isNotNull();
        assertThat( ee.getAccession().getAccession() ).isEqualTo( "GSE109291" );
        assertThat( ee.getAccession().getExternalDatabase().getName() ).isEqualTo( "GEO" );
        assertThat( ee.getBioAssays() )
                .hasSize( 8 )
                .allSatisfy( ba -> {
                    assertThat( ba.getName() ).startsWith( "sample" );
                    assertThat( ba.getDescription() ).startsWith( "sample description " );
                    assertThat( ba.getAccession() ).isNotNull();
                    assertThat( ba.getArrayDesignUsed() ).isEqualTo( ad );
                    assertThat( ba.getSampleUsed().getName() ).startsWith( "sample" );
                    assertThat( ba.getSampleUsed().getDescription() ).startsWith( "Generated by Gemma for: " );
                } );
        assertThat( ee.getNumberOfSamples() ).isEqualTo( 8 );
    }
}
