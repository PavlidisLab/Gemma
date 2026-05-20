package ubic.gemma.core.loader.expression.geo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.geo.service.*;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.loader.util.ftp.FTPClientFactoryImpl;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableExtension;
import ubic.gemma.model.common.description.Characteristic;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(NetworkAvailableExtension.class)
@NetworkAvailable
@ContextConfiguration
public class GeoConverterTest2 {

    private GeoConverter gc;
    private FTPClientFactory ftpClientFactory;

    @BeforeEach
    public void setUp() {
        gc = new GeoConverterImpl();
        gc.setElementLimitForStrictness( 100000 );
        ftpClientFactory = new FTPClientFactoryImpl();
    }

    /**
     * These datasets were pulled from Gemma database on January 14th, 2026.
     */
    public static Stream<String> facSortedDatasets() {
        return Stream.of(
                "GSE65411",
                "GSE65309",
                "GSE155871",
                "GSE244159",
                "GSE244791",
                "GSE266143",
                "GSE250633",
                "GSE254084",
                "GSE117981",
                "GSE211935",
                "GSE135195",
                // "GSE44140", no useful keywords
                "GSE32675",
                "GSE76606",
                "GSE130268",
                "GSE283252",
                "GSE66856",
                "GSE83356",
                "GSE19474",
                "GSE148320",
                "GSE232833",
                "GSE232833",
                "GSE227313",
                "GSE214966",
                // "GSE153824", this one has indication in the characteristics, but that's too inconsistent
                "GSE296073",
                "GSE161255",
                // "GSE127449", has the "sorted" keyword, but nothing else around, so it's too generic
                "GSE158962",
                "GSE307375" );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("facSortedDatasets")
    public void testFacSorted( String geoAccession ) throws IOException {
        GeoSeries geoSeries = readSeriesFromGeo( geoAccession );
        assertThat( gc.convert( geoSeries ) )
                .singleElement()
                .satisfies( ee -> {
                    assertThat( ee.getCharacteristics() )
                            .hasSize( 2 )
                            .extracting( Characteristic::getValue )
                            .containsAnyOf( "bulk RNA-seq assay", "transcription profiling by array assay", "single-nucleus RNA sequencing assay", "single-cell RNA sequencing assay" )
                            .contains( "fluorescence-activated cell sorting" );
                } );
    }

    private GeoSeries readSeriesFromGeo( String accession ) throws IOException {
        URL url = GeoUtils.getUrl( accession, GeoSource.FTP, GeoFormat.SOFT, GeoScope.FAMILY, GeoAmount.FULL );
        try ( InputStream is = new GZIPInputStream( ftpClientFactory.openStream( url ) ) ) {
            GeoFamilyParser parser = new GeoFamilyParser();
            parser.parse( is );
            GeoSeries geoSeries = requireNonNull( requireNonNull( parser.getUniqueResult() ).getSeriesMap().get( accession ) );
            DatasetCombiner datasetCombiner = new DatasetCombiner();
            GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( geoSeries );
            geoSeries.setSampleCorrespondence( correspondence );
            return geoSeries;
        }
    }
}
