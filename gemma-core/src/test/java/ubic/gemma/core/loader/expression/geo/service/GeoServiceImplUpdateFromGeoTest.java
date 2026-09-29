package ubic.gemma.core.loader.expression.geo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.BeanFactory;
import ubic.gemma.core.loader.expression.geo.GeoConverter;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link GeoServiceImpl#updateFromGEO(ExpressionExperiment, GeoService.GeoUpdateConfig)} with the GEO fetch and
 * the conversion mocked out.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GeoServiceImplUpdateFromGeoTest {

    @Mock
    private ExpressionExperimentService expressionExperimentService;
    @Mock
    private BeanFactory beanFactory;
    @Mock
    private CharacteristicService characteristicService;
    @Mock
    private BioMaterialService bioMaterialService;
    @Mock
    private BioAssayService bioAssayService;
    @Mock
    private GeoUpdateAuditService geoUpdateAuditService;

    @Mock
    private GeoDomainObjectGenerator generator;
    @Mock
    private GeoConverter geoConverter;

    @InjectMocks
    private GeoServiceImpl geoService;

    private ExternalDatabase geo;

    @BeforeEach
    void setUp() {
        geoService.setGeoDomainObjectGenerator( generator );
        when( beanFactory.getBean( "geoConverter" ) ).thenReturn( geoConverter );
        geo = ExternalDatabase.Factory.newInstance();
        geo.setName( ExternalDatabases.GEO );
    }

    /**
     * 🛑 A sample whose accession IS from GEO is exactly the one to refresh. The guard read
     * {@code accession == null || db == GEO}, so it skipped every GEO sample with "does not have a GEO
     * accession, ignoring." and no sample characteristic was ever replaced for GEO data.
     */
    @Test
    void testSampleCharacteristicsAreReplacedForAGeoSample() {
        Characteristic oldChar = characteristic( 5L, "old" );
        Characteristic newChar = characteristic( null, "new" );

        BioMaterial bm = new BioMaterial();
        bm.getCharacteristics().add( oldChar );
        ExpressionExperiment ee = gemmaExperiment( bioAssay( "GSM1", bm, true ) );
        mockFreshFromGeo( ee, "GSM1", newChar );

        geoService.updateFromGEO( ee, GeoService.GeoUpdateConfig.builder().sampleCharacteristics( true ).build() );

        verify( bioMaterialService ).update( bm );
        assertThat( bm.getCharacteristics() ).containsExactly( newChar );
        verify( geoUpdateAuditService ).recordGeoUpdate( ee, 1, false );
    }

    /**
     * 🛑 The characteristics being replaced are deleted. {@code remove()} used to be handed the set after
     * {@code clear()} had emptied it, so it deleted nothing.
     */
    @Test
    void testReplacedCharacteristicsAreDeleted() {
        Characteristic oldChar = characteristic( 5L, "old" );
        Characteristic newChar = characteristic( null, "new" );

        BioMaterial bm = new BioMaterial();
        bm.getCharacteristics().add( oldChar );
        ExpressionExperiment ee = gemmaExperiment( bioAssay( "GSM1", bm, true ) );
        mockFreshFromGeo( ee, "GSM1", newChar );

        geoService.updateFromGEO( ee, GeoService.GeoUpdateConfig.builder().sampleCharacteristics( true ).build() );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Characteristic>> removed = ArgumentCaptor.forClass( Collection.class );
        verify( characteristicService ).remove( removed.capture() );
        assertThat( removed.getValue() ).containsExactly( oldChar );
    }

    /** A sample without an accession is still skipped, and its characteristics are left alone. */
    @Test
    void testASampleWithoutAnAccessionIsLeftAlone() {
        Characteristic oldChar = characteristic( 5L, "old" );
        BioMaterial bm = new BioMaterial();
        bm.getCharacteristics().add( oldChar );
        BioAssay ba = bioAssay( "GSM1", bm, true );
        ba.setAccession( null );
        ExpressionExperiment ee = gemmaExperiment( ba );
        mockFreshFromGeo( ee, "GSM1", characteristic( null, "new" ) );

        geoService.updateFromGEO( ee, GeoService.GeoUpdateConfig.builder().sampleCharacteristics( true ).build() );

        verify( bioMaterialService, never() ).update( any( BioMaterial.class ) );
        assertThat( bm.getCharacteristics() ).containsExactly( oldChar );
    }

    /**
     * 🛑 A refresh that stored no document says so. The store returned early on a null document and the
     * caller could not tell, so updateGeoSourceMetadata recorded "Stored the GEO source metadata
     * document." for an experiment with none.
     */
    @Test
    void testASourceMetadataRefreshThatBuildsNoDocumentFails() {
        ExpressionExperiment ee = gemmaExperiment( bioAssay( "GSM1", new BioMaterial(), true ) );
        when( generator.generateSeriesMetadataOnly( "GSE1" ) ).thenReturn( null );

        assertThatThrownBy( () -> geoService.updateFromGEO( ee,
                GeoService.GeoUpdateConfig.builder().sourceMetadata( true ).build() ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "nothing was stored" );
        verify( expressionExperimentService, never() ).update( any( ExpressionExperiment.class ) );
    }

    /** ... and a failure to write it reaches the caller instead of being logged and dropped. */
    @Test
    void testASourceMetadataRefreshThatFailsToWritePropagates() {
        ExpressionExperiment ee = gemmaExperiment( bioAssay( "GSM1", new BioMaterial(), true ) );
        GeoSeries series = new GeoSeries();
        series.setGeoAccession( "GSE1" );
        when( generator.generateSeriesMetadataOnly( "GSE1" ) ).thenReturn( series );
        when( expressionExperimentService.findByAccession( "GSE1" ) ).thenReturn( Collections.singletonList( ee ) );
        doThrow( new RuntimeException( "write failed" ) ).when( expressionExperimentService ).update( ee );

        assertThatThrownBy( () -> geoService.updateFromGEO( ee,
                GeoService.GeoUpdateConfig.builder().sourceMetadata( true ).build() ) )
                .hasMessage( "write failed" );
    }

    private ExpressionExperiment gemmaExperiment( BioAssay ba ) {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        ee.setShortName( "GSE1" );
        ee.setAccession( DatabaseEntry.Factory.newInstance( "GSE1", geo ) );
        ee.getBioAssays().add( ba );
        when( expressionExperimentService.thawLite( ee ) ).thenReturn( ee );
        return ee;
    }

    private BioAssay bioAssay( String gsm, BioMaterial bm, boolean hasOriginalPlatform ) {
        BioAssay ba = new BioAssay();
        ba.setAccession( DatabaseEntry.Factory.newInstance( gsm, geo ) );
        ba.setSampleUsed( bm );
        ArrayDesign ad = new ArrayDesign();
        ad.setShortName( "GPL1" );
        ba.setArrayDesignUsed( ad );
        if ( hasOriginalPlatform ) {
            ba.setOriginalPlatform( ad );
        }
        return ba;
    }

    private void mockFreshFromGeo( ExpressionExperiment ee, String gsm, Characteristic freshChar ) {
        BioMaterial freshBm = new BioMaterial();
        freshBm.getCharacteristics().add( freshChar );
        ExpressionExperiment fresh = new ExpressionExperiment();
        fresh.getBioAssays().add( bioAssay( gsm, freshBm, false ) );
        GeoSeries series = new GeoSeries();
        series.setGeoAccession( ee.getAccession().getAccession() );
        doReturn( Collections.singletonList( series ) ).when( generator ).generate( "GSE1" );
        when( geoConverter.convert( series, true ) ).thenReturn( Collections.singletonList( fresh ) );
    }

    private static Characteristic characteristic( Long id, String value ) {
        Characteristic c = new Characteristic();
        c.setId( id );
        c.setCategory( "test" );
        c.setValue( value );
        return c;
    }
}
