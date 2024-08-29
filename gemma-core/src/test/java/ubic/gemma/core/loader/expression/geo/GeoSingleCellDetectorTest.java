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
import org.springframework.context.annotation.Import;
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
import ubic.gemma.core.loader.util.ftp.FTPConfig;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.core.util.test.category.GeoTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
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

    @Import(FTPConfig.class)
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
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.MEX );
        GeoSample sample = getSample( series, "GSM6072067" );
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
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        GeoSample sample = getSample( series, "GSM5319989" );
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
        GeoSample sample = getSample( series, "GSM6022251" );
        assertThat( detector.getAdditionalSupplementaryFiles( sample ) )
                .containsExactlyInAnyOrder(
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM6022nnn/GSM6022251/suppl/GSM6022251_MBM01_sc_CD45pos_filtered.h5",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM6022nnn/GSM6022251/suppl/GSM6022251_MBM01_sc_counts.csv.gz" );
    }

    /**
     * This is a pathologic case with nested zip and gzipped files.
     */
    @Test
    public void testGSE162631() throws IOException {
        GeoSeries series = readSeriesFromGeo( "GSE162631" );
        assertThat( detector.hasSingleCellData( series ) ).isFalse();
    }

    @Test
    public void testGSE148611() throws IOException {
        GeoSeries series = readSeriesFromGeo( "GSE148611" );
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        // detector.downloadSingleCellData( series );
    }

    @Test
    @Ignore("This is an example of a single MEX dataset at the series-level for all the samples.")
    public void testGSE193884() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE193884" );
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
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.MEX );
        assertThatThrownBy( () -> detector.downloadSingleCellData( series ) )
                .isInstanceOf( UnsupportedOperationException.class )
                .hasMessage( "MEX files were found, but single-cell data is not supported at the series level." );
    }

    /**
     * This is a case of a MEX dataset that has matrix and barcodes at the sample-level (as we support it!), but genes
     * at the series level.
     */
    @Test
    @Category(SlowTest.class)
    public void testGSE242423() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE242423" );
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.MEX );
        // FIXME: files are detected as additional because the detection does not account for the files supplied by the
        //        series
        assertThat( detector.getAdditionalSupplementaryFiles( series ) )
                .containsExactly( "ftp://ftp.ncbi.nlm.nih.gov/geo/series/GSE242nnn/GSE242423/suppl/GSE242423_scRNA_genes.tsv.gz" );
        assertThat( series.getSamples() ).allSatisfy( s -> {
            // uncontextualized, the sample does not have single-cell data because it lacks features.tsv.gz
            assertThat( detector.getAdditionalSupplementaryFiles( s ) )
                    .hasSize( 2 )
                    .satisfiesExactlyInAnyOrder( s1 -> assertThat( s1 ).endsWith( "matrix.mtx.gz" ), s2 -> assertThat( s2 ).endsWith( "barcodes.tsv.gz" ) );
            // with the context of its series, it can properly detect the presence of the features.tsv.gz file from the
            // parent series
            assertThat( detector.getAdditionalSupplementaryFiles( series, s ) )
                    .isEmpty();
        } );
        detector.downloadSingleCellData( series, getSample( series, "GSM7763419" ) );
        assertThat( tmpDir )
                .isDirectoryRecursivelyContaining( "glob:**/GSM7763419/barcodes.tsv.gz" )
                .isDirectoryRecursivelyContaining( "glob:**/GSM7763419/features.tsv.gz" )
                .isDirectoryRecursivelyContaining( "glob:**/GSM7763419/matrix.mtx.gz" );
    }

    /**
     * This is an AnnData dataset that uses the {@code .h5ad.h5} extension.
     */
    @Test
    public void testGSE132188() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE132188" );
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.ANNDATA );
    }

    /**
     * This is a custom H5 attachment that is unsupported.
     */
    @Test
    public void testGSE162807() throws IOException {
        GeoSeries series = readSeriesFromGeo( "GSE162807" );
        assertThat( detector.hasSingleCellData( series ) ).isFalse();
    }

    /**
     * This is a curious example of a SOFT file that has NONE indicators in its supplementary materials.
     */
    @Test
    public void testGSE158184() throws IOException {
        GeoSeries series = readSeriesFromGeo( "GSE217464" );
        for ( GeoSample sample : series.getSamples() ) {
            assertThat( detector.getAdditionalSupplementaryFiles( sample ) )
                    .isEmpty();
        }
    }

    /**
     * This dataset as a mix of scRNA-Seq and ATAC-Seq
     */
    @Test
    public void testGSE185737() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE185737" );
        assertThat( detector.getSingleCellDataType(
                series
        ) ).isEqualTo( SingleCellDataType.MEX );
        // this is an ATAC-seq sample
        GeoSample sample = getSample( series, "GSM5623074" );
        assertThat( detector.hasSingleCellData( sample ) )
                .isFalse();
        assertThat( detector.getAdditionalSupplementaryFiles( sample ) )
                .isEmpty();
    }

    /**
     * This file has a large TAR attachment in its ATAC-seq samples that should be ignored.
     */
    @Test
    public void testGSE196516() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE196516" );
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.MEX );
    }

    /**
     * This series contains large TAR attachment that should be skipped when inspecting for MEX.
     */
    @Test
    public void testGSE235314() throws IOException {
        GeoSeries series = readSeriesFromGeo( "GSE235314" );
        assertThat( detector.hasSingleCellData( series ) ).isFalse();
        GeoSample sample = getSample( series, "GSM7498809" );
        assertThat( detector.getAdditionalSupplementaryFiles( sample ) )
                .containsExactly( "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM7498nnn/GSM7498809/suppl/GSM7498809_CEA.tar.gz" );
    }

    /**
     * Similar case here with large TAR attachments, but in this case it has a lot of samples to skip.
     */
    @Test
    @Category(SlowTest.class)
    public void testGSE201263() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE201262" );
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.MEX );
    }

    /**
     * This files produces "java.util.zip.ZipException: invalid block type" apparently...
     */
    @Test
    @Category(SlowTest.class)
    @Ignore("This files produces \"java.util.zip.ZipException: invalid block type\" apparently.")
    public void testGSE200202() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE200202" );
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.ANNDATA );
        detector.downloadSingleCellData( series );
    }

    /**
     * This example has some additional supplementary materials that are inside a TAR archive that contains MEX data.
     */
    @Test
    @Category(SlowTest.class)
    public void testGSE155499() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE155499" );
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.MEX );
        GeoSample sample = getSample( series, "GSM4705328" );
        assertThat( detector.getAdditionalSupplementaryFiles( series, sample ) )
                .containsExactlyInAnyOrder(
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/clustering/kmeans_6_clusters/clusters.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/clustering/kmeans_2_clusters/clusters.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/clustering/kmeans_8_clusters/clusters.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/clustering/kmeans_4_clusters/clusters.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/clustering/kmeans_7_clusters/clusters.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/clustering/kmeans_3_clusters/clusters.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/clustering/graphclust/clusters.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/clustering/kmeans_10_clusters/clusters.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/clustering/kmeans_9_clusters/clusters.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/clustering/kmeans_5_clusters/clusters.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/pca/10_components/components.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/pca/10_components/genes_selected.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/pca/10_components/dispersion.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/pca/10_components/variance.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/pca/10_components/projection.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/tsne/2_components/projection.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/diffexp/kmeans_6_clusters/differential_expression.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/diffexp/kmeans_2_clusters/differential_expression.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/diffexp/kmeans_8_clusters/differential_expression.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/diffexp/kmeans_4_clusters/differential_expression.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/diffexp/kmeans_7_clusters/differential_expression.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/diffexp/kmeans_3_clusters/differential_expression.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/diffexp/graphclust/differential_expression.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/diffexp/kmeans_10_clusters/differential_expression.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/diffexp/kmeans_9_clusters/differential_expression.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/analysis/diffexp/kmeans_5_clusters/differential_expression.csv",
                        "ftp://ftp.ncbi.nlm.nih.gov/geo/samples/GSM4705nnn/GSM4705328/suppl/GSM4705328_B6CD.cellranger.tar.gz!/cloupe.cloupe" );
    }

    @Test
    @Category(SlowTest.class)
    public void testGSE201032() throws IOException {
        GeoSeries series = readSeriesFromGeo( "GSE201032" );
        assertThat( detector.getAdditionalSupplementaryFiles( series ) )
                .containsExactly( "ftp://ftp.ncbi.nlm.nih.gov/geo/series/GSE201nnn/GSE201032/suppl/GSE201032_Metadata.csv.gz" );
        assertThat( series.getSamples() )
                .hasSize( 8 )
                .allSatisfy( sample -> assertThat( detector.getAdditionalSupplementaryFiles( series, sample ) ).isEmpty() );
    }

    /**
     * This is a MEX dataset with a barcode_metadata.tsv.gz file.
     */
    @Test
    public void testGSE218621() throws NoSingleCellDataFoundException, IOException {
        GeoSeries series = readSeriesFromGeo( "GSE218621" );
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.MEX );
        GeoSample sample = getSample( series, "GSM6753396" );
        assertThat( detector.hasSingleCellData( sample ) ).isTrue();
        assertThatThrownBy( () -> detector.downloadSingleCellData( sample ) )
                .isInstanceOf( UnsupportedOperationException.class );
    }

    /**
     * This dataset has a mixture of Loom and MEX, the latter should take precedence because Loom is not supported yet.
     */
    @Test
    public void testGSE237862() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE237862" );
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.MEX );
        assertThat( detector.getAllSingleCellDataTypes( series ) )
                .containsExactlyInAnyOrder( SingleCellDataType.LOOM, SingleCellDataType.MEX );
    }

    /**
     * This is a Loom dataset with files in individual samples
     */
    @Test
    public void testGSE179516() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE179516" );
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.LOOM );
        assertThatThrownBy( () -> detector.downloadSingleCellData( series ) )
                .isInstanceOf( NoSingleCellDataFoundException.class );
        assertThatThrownBy( () -> detector.downloadSingleCellData( series, getSample( series, "GSM5419628" ) ) )
                .isInstanceOf( UnsupportedOperationException.class );
    }

    /**
     * This is a Loom dataset with a single file in the series. We support detection and download, but not loading.
     */
    @Test
    @Category(SlowTest.class)
    public void testGSE159416() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE159416" );
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.LOOM );
        detector.downloadSingleCellData( series );
        assertThat( tmpDir )
                .isDirectoryRecursivelyContaining( "glob:**/GSE159416.loom" );
        assertThatThrownBy( () -> detector.getSingleCellDataLoader( series ) )
                .isInstanceOf( UnsupportedOperationException.class );
    }

    /**
     * MEX dataset stored in ZIP archives.
     */
    @Test
    @Category(SlowTest.class)
    public void testGSE178226() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE178226" );
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.MEX );
    }

    /**
     * This one uses non-standard file names.
     */
    @Test
    @Category(SlowTest.class)
    public void testGSE199762() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE199762" );
        try {
            detector.setMexFileSuffixes( "barcodes.tsv", "features.tsv", "counts.mtx" );
            assertThat( detector.hasSingleCellData( series ) ).isTrue();
            assertThat( detector.getSingleCellDataType( series ) ).isEqualTo( SingleCellDataType.MEX );
            detector.downloadSingleCellData( series, getSample( series, "GSM8002943" ) );
            assertThat( tmpDir )
                    .isDirectoryRecursivelyContaining( "glob:**/GSE199762/GSM8002943/barcodes.tsv.gz" )
                    .isDirectoryRecursivelyContaining( "glob:**/GSE199762/GSM8002943/features.tsv.gz" )
                    .isDirectoryRecursivelyContaining( "glob:**/GSE199762/GSM8002943/matrix.mtx.gz" );
        } finally {
            detector.resetMexFileSuffixes();
        }
    }

    /**
     * This dataset has technical replicates, this is not supported.
     */
    @Test
    @Category(SlowTest.class)
    public void testGSE155695() throws IOException, NoSingleCellDataFoundException {
        GeoSeries series = readSeriesFromGeo( "GSE155695" );
        assertThat( detector.hasSingleCellData( series ) ).isTrue();
        // this sample has technical replicates, but those can only be detected at download time because
        // hasSingleCellData() is designed to run as fast s possible
        GeoSample sample = getSample( series, "GSM4710634" );
        assertThatThrownBy( () -> detector.downloadSingleCellData( series, sample ) )
                .isInstanceOf( UnsupportedOperationException.class );
        assertThat( tmpDir )
                .isDirectoryNotContaining( "glob:**/GSM4710634" );
        // FIXME: this sample also has technical replicates, but it is not being detected due to its naming scheme
        GeoSample sample2 = getSample( series, "GSM4710635" );
        detector.downloadSingleCellData( series, sample2 );
        assertThat( tmpDir )
                .isDirectoryRecursivelyContaining( "glob:**/GSE155695/GSM4710635/barcodes.tsv.gz" )
                .isDirectoryRecursivelyContaining( "glob:**/GSE155695/GSM4710635/features.tsv.gz" )
                .isDirectoryRecursivelyContaining( "glob:**/GSE155695/GSM4710635/matrix.mtx.gz" );
    }

    /**
     * This sample has duplicated cell IDs.
     */
    @Test
    public void testGSM4282408() throws NoSingleCellDataFoundException, IOException {
        GeoSeries series = readSeriesFromGeo( "GSE144172" );
        GeoSample sample = getSample( series, "GSM4282408" );
        detector.downloadSingleCellData( series, sample );
        List<BioAssay> samples = Collections.singletonList( BioAssay.Factory.newInstance( "GSM4282408" ) );
        assertThatThrownBy( () -> detector.getSingleCellDataLoader( series ).getSingleCellDimension( samples ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessage( "Sample GSM4282408 has duplicate cell IDs." );
    }

    private GeoSeries readSeriesFromGeo( String accession ) throws IOException {
        try ( InputStream is = new GZIPInputStream( new URL( "https://ftp.ncbi.nlm.nih.gov/geo/series/" + accession.substring( 0, 6 ) + "nnn/" + accession + "/soft/" + accession + "_family.soft.gz" ).openStream() ) ) {
            GeoFamilyParser parser = new GeoFamilyParser();
            parser.parse( is );
            return requireNonNull( requireNonNull( parser.getUniqueResult() ).getSeriesMap().get( accession ) );
        }
    }

    private GeoSample getSample( GeoSeries series, String sampleAccession ) {
        return series.getSamples().stream()
                .filter( s -> sampleAccession.equals( s.getGeoAccession() ) )
                .findFirst()
                .orElseThrow( NullPointerException::new );
    }
}