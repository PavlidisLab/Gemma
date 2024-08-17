package ubic.gemma.core.loader.expression.geo;

import org.apache.commons.io.file.PathUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.geo.singleCell.GeoSingleCellDetector;
import ubic.gemma.core.loader.expression.geo.singleCell.NoSingleCellDataFoundException;
import ubic.gemma.core.loader.expression.singleCell.MexSingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.loader.util.ftp.FTPClientFactoryImpl;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.core.util.test.category.GeoTest;
import ubic.gemma.core.util.test.category.SlowTest;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TODO: move SOFT files in test resources and mock FTP downloads
 */
@Category(GeoTest.class)
@ContextConfiguration
public class GeoSingleCellDetectorTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class Config {
        @Bean
        public static PropertyPlaceholderConfigurer placeholderConfigurer() {
            TestPropertyPlaceholderConfigurer configurer = new TestPropertyPlaceholderConfigurer( "ncbi.host=ftp.ncbi.nlm.nih.gov", "ncbi.user=anonymous", "ncbi.password=" );
            // we can safely ignore GEO and ArrayExpress configurations
            configurer.setIgnoreUnresolvablePlaceholders( true );
            return configurer;
        }

        @Bean
        public FTPClientFactory ftpClientFactory() {
            return new FTPClientFactoryImpl();
        }
    }

    @Autowired
    private FTPClientFactory ftpClientFactory;

    private final GeoSingleCellDetector detector = new GeoSingleCellDetector();
    private Path tmpDir;

    @Before
    public void setUp() throws IOException {
        tmpDir = Files.createTempDirectory( "test" );
        detector.setFTPClientFactory( ftpClientFactory );
        detector.setDownloadDirectory( tmpDir );
    }

    @After
    public void cleanUp() throws IOException {
        PathUtils.deleteDirectory( tmpDir );
    }

    /**
     * AnnData (and also Seurat Disk, but the former is preferred)
     */
    @Test
    public void testGSE225158() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE225158" );
        assertThat( series ).isNotNull();
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.ANNDATA );
        assertThat( detector.getAllSingleCellDataTypes( series ) )
                .containsExactlyInAnyOrder( SingleCellDataType.ANNDATA, SingleCellDataType.SEURAT_DISK );
        GeoSample sample = series.getSamples().iterator().next();
        assertThat( detector.hasSingleCellData( sample ) ).isFalse();
    }

    /**
     * Typical MEX format: 3 supplementary files per sample
     */
    @Test
    public void testGSE224438() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE224438" );
        assertThat( series ).isNotNull();
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.MEX );
        assertThat( detector.getAdditionalSupplementaryFiles( series ) )
                .isEmpty();
        GeoSample sample = series.getSamples().iterator().next();
        assertThat( detector.hasSingleCellData( sample ) ).isTrue();
        assertThat( detector.getAdditionalSupplementaryFiles( sample ) )
                .isEmpty();
    }

    /**
     * This is a case of a MEX dataset where files are bundled in a per-sample TAR archive.
     */
    @Test
    @Category(SlowTest.class)
    public void testGSE201814() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE201814" );
        assertThat( series ).isNotNull();
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.MEX );
        GeoSample sample = series.getSamples().stream().filter( s -> s.getGeoAccession().equals( "GSM6072067" ) )
                .findFirst()
                .orElse( null );
        assertThat( sample ).isNotNull();
        assertThat( detector.hasSingleCellData( sample ) ).isTrue();
        detector.downloadSingleCellData( sample );
        assertThat( tmpDir )
                .isDirectoryRecursivelyContaining( "glob:**/GSM6072067/barcodes.tsv.gz" )
                .isDirectoryRecursivelyContaining( "glob:**/GSM6072067/features.tsv.gz" )
                .isDirectoryRecursivelyContaining( "glob:**/GSM6072067/matrix.mtx.gz" );
        // downloaded files will be reused, only the target will be linked
        detector.downloadSingleCellData( series, sample );
        assertThat( tmpDir )
                .isDirectoryRecursivelyContaining( "glob:**/GSE201814/GSM6072067/barcodes.tsv.gz" )
                .isDirectoryRecursivelyContaining( "glob:**/GSE201814/GSM6072067/features.tsv.gz" )
                .isDirectoryRecursivelyContaining( "glob:**/GSE201814/GSM6072067/matrix.mtx.gz" );
        // that should be a no-op
        detector.downloadSingleCellData( series, sample );
        SingleCellDataLoader loader = detector.getSingleCellDataLoader( series );
        assertThat( loader )
                .isInstanceOf( MexSingleCellDataLoader.class );
        assertThat( loader.getSampleNames() ).containsExactly( "GSM6072067" );
        assertThat( loader.getGenes() ).hasSize( 32285 );
    }

    @Test
    @Ignore("This is simply too slow to be practical.")
    @Category(SlowTest.class)
    public void testGSE201814DownloadAll() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE201814" );
        assertThat( series ).isNotNull();
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.MEX );
        detector.downloadSingleCellData( series );
        assertThat( tmpDir )
                .isDirectoryRecursivelyContaining( "glob:**/GSE201814/*/barcodes.tsv.gz" )
                .isDirectoryRecursivelyContaining( "glob:**/GSE201814/*/features.tsv.gz" )
                .isDirectoryRecursivelyContaining( "glob:**/GSE201814/*/matrix.mtx.gz" );
        assertThat( detector.getSingleCellDataLoader( series ) )
                .isInstanceOf( MexSingleCellDataLoader.class );
    }

    @Test
    public void testGSE228370() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE228370" );
        assertThat( series ).isNotNull();
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.MEX );
    }

    /**
     * Interesting case because it uses genes.tsv instead of features.tsv.
     * <p>
     * We need to download it to make sure that the file is properly stored.
     */
    @Test
    @Category(SlowTest.class)
    public void testGSE174574() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE174574" );
        assertThat( series ).isNotNull();
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        GeoSample sample = series.getSamples().stream()
                .filter( n -> n.getGeoAccession().equals( "GSM5319989" ) )
                .findFirst()
                .orElseThrow( NullPointerException::new );
        detector.downloadSingleCellData( sample );
        assertThat( tmpDir )
                .isDirectoryRecursivelyContaining( "glob:**/GSM5319989/barcodes.tsv.gz" )
                .isDirectoryRecursivelyContaining( "glob:**/GSM5319989/features.tsv.gz" )
                .isDirectoryRecursivelyContaining( "glob:**/GSM5319989/matrix.mtx.gz" );
    }

    /**
     * This dataset has multiple {@code .h5ad} supplementary files. The solution is to manually pick one of them.
     */
    @Test
    @Category(SlowTest.class)
    public void testGSE202051() throws IOException {
        GeoSeries series = readSeriesFromGeo( "GSE202051" );
        assertThat( series ).isNotNull();
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThatThrownBy( () -> detector.downloadSingleCellData( series ) )
                .isInstanceOf( IllegalArgumentException.class );
        // the real thing is GSE202051_totaldata-final-toshare.h5ad.gz, but it's just too big for a test
        detector.downloadSingleCellData( series, SingleCellDataType.ANNDATA, "ftp://ftp.ncbi.nlm.nih.gov/geo/series/GSE202nnn/GSE202051/suppl/GSE202051_adata_010orgCRT_10x.h5ad.gz" );
        assertThat( tmpDir ).isDirectoryRecursivelyContaining( "glob:**/GSE202051.h5ad" );
    }

    /**
     * This dataset does not use 'single-cell transcriptomic' as a library source, so we have to rely on keywords.
     * <p>
     * It als has a bunch of additional supplementary files.
     */
    @Test
    public void testGSE200218() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE200218" );
        assertThat( series ).isNotNull();
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.MEX );
        assertThat( detector.getAdditionalSupplementaryFiles( series ) )
                .containsExactlyInAnyOrder(
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/series/GSE200nnn/GSE200218/suppl/GSE200218_sc_sn_gene_names.csv.gz",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/series/GSE200nnn/GSE200218/suppl/GSE200218_sc_sn_counts.mtx.gz",
                        // "ftp://ftp.ncbi.nlm.nih.gov/geo/series/GSE200nnn/GSE200218/suppl/GSE200218_RAW.tar",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/series/GSE200nnn/GSE200218/suppl/GSE200218_sc_sn_integrated_data.csv.gz",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/series/GSE200nnn/GSE200218/suppl/GSE200218_VIPER_matrix_tumor_cells_sn.csv.gz",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/series/GSE200nnn/GSE200218/suppl/GSE200218_sc_sn_metadata.csv.gz",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/series/GSE200nnn/GSE200218/suppl/GSE200218_SCENIC_matrix_tumor_cells_sn.csv.gz" );
        GeoSample sample = series.getSamples().stream().filter( s -> s.getGeoAccession().equals( "GSM6022251" ) ).findFirst().get();
        assertThat( detector.getAdditionalSupplementaryFiles( sample ) )
                .containsExactlyInAnyOrder(
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM6022nnn/GSM6022251/suppl/GSM6022251_MBM01_sc_CD45pos_filtered.h5",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM6022nnn/GSM6022251/suppl/GSM6022251_MBM01_sc_counts.csv.gz" );
    }

    @Test
    @Ignore("This is a pathologic case with nested zip and gzipped files.")
    public void testGSE162631() throws IOException {
        GeoSeries series = readSeriesFromGeo( "GSE162631" );
        assertThat( series ).isNotNull();
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
    }

    @Test
    public void testGSE148611() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE148611" );
        assertThat( series ).isNotNull();
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        // detector.downloadSingleCellData( series );
    }

    @Test
    @Ignore("This is an example of a single MEX dataset at the series-level for all the samples.")
    public void testGSE193884() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE193884" );
        assertThat( series ).isNotNull();
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        detector.downloadSingleCellData( series );
    }

    /**
     * This is a case of a MEX dataset where files are located in the series. We don't support downloading those, but we
     * want to make sure we're producing a useful error message.
     */
    @Test
    public void testGSE147495() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE147495" );
        assertThat( series ).isNotNull();
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.MEX );
        assertThatThrownBy( () -> detector.downloadSingleCellData( series ) )
                .isInstanceOf( NoSingleCellDataFoundException.class )
                .hasMessage( "MEX files were found, but single-cell data is not supported at the series level." );
    }

    /**
     * This is a case of a MEX dataset that has matrix and barcodes at the sample-level (as we support it!), but genes
     * at the series level.
     */
    @Test
    public void testGSE242423() throws IOException {
        GeoSeries series = readSeriesFromGeo( "GSE242423" );
        assertThat( series ).isNotNull();
        assertThat( detector.hasSingleCellData( series ) ).isFalse();
    }

    /**
     * This is an AnnData dataset that uses the {@code .h5ad.h5} extension.
     */
    @Test
    public void testGSE132188() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE132188" );
        assertThat( series ).isNotNull();
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.ANNDATA );
    }

    /**
     * This is a custom H5 attachment that is unsupported.
     */
    @Test
    public void testGSE162807() throws IOException {
        GeoSeries series = readSeriesFromGeo( "GSE162807" );
        assertThat( series ).isNotNull();
        assertThat( detector.hasSingleCellData( series ) ).isFalse();
    }

    @Nullable
    private GeoSeries readSeriesFromGeo( String accession ) throws IOException {
        try ( InputStream is = new GZIPInputStream( new URL( "https://ftp.ncbi.nlm.nih.gov/geo/series/" + accession.substring( 0, 6 ) + "nnn/" + accession + "/soft/" + accession + "_family.soft.gz" ).openStream() ) ) {
            GeoFamilyParser parser = new GeoFamilyParser();
            parser.parse( is );
            return requireNonNull( parser.getUniqueResult() ).getSeriesMap().get( accession );
        }
    }
}