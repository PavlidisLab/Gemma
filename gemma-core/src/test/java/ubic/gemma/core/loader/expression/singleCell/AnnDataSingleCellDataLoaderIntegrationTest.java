package ubic.gemma.core.loader.expression.singleCell;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.geo.singleCell.AnnDataDetector;
import ubic.gemma.core.loader.expression.geo.singleCell.NoSingleCellDataFoundException;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCellDataTranspose;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.loader.util.mapper.MapBasedDesignElementMapper;
import ubic.gemma.core.loader.util.mapper.SimpleBioAssayMapper;
import ubic.gemma.core.util.test.BaseIntegrationTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.core.util.test.Assumptions.assumeThatFreeMemoryIsGreaterOrEqualTo;

public class AnnDataSingleCellDataLoaderIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FTPClientFactory ftpClientFactory;

    @Value("${gemma.download.path}")
    private String downloadPath;

    @Value("${python.exe}")
    private String pythonExecutable;

    /**
     * This test requires a fairly large input file which must be located in the download directory.
     */
    @Test
    @Category(SlowTest.class)
    public void testGSE225158() throws IOException {
        Path p = downloadAndTransposeGSE225158();
        AnnDataSingleCellDataLoader loader = new AnnDataSingleCellDataLoader( p );
        loader.setSampleFactorName( "ID" );
        loader.setBioAssayToSampleNameMapper( new SimpleBioAssayMapper() );
        List<BioAssay> samples = new ArrayList<>();
        for ( String sampleName : loader.getSampleNames() ) {
            samples.add( BioAssay.Factory.newInstance( sampleName ) );
        }
        SingleCellDimension dimension = loader.getSingleCellDimension( samples );
        QuantitationType qt = loader.getQuantitationTypes().iterator().next();
        Map<String, CompositeSequence> elementMapping = loader.getGenes().stream()
                .collect( Collectors.toMap( g -> g, CompositeSequence.Factory::newInstance ) );

        loader.setUnknownCellTypeIndicator( "UNK_ALL" );

        loader.setCellTypeFactorName( "celltype1" );
        assertThat( loader.getCellTypeAssignments( dimension ) )
                .singleElement()
                .satisfies( cta -> {
                    assertThat( cta.getCellTypes() )
                            .extracting( Characteristic::getValue )
                            .containsExactlyInAnyOrder( "Astrocytes", "Endothelial", "Interneurons", "MSNs", "Microglia", "Mural/Fibroblast", "Oligos", "Oligos_Pre" );
                } );

        loader.setCellTypeFactorName( "celltype2" );
        assertThat( loader.getCellTypeAssignments( dimension ) )
                .singleElement()
                .satisfies( cta -> {
                    assertThat( cta.getCellTypes() )
                            .extracting( Characteristic::getValue )
                            .containsExactlyInAnyOrder( "Astrocytes", "D1-Matrix", "D1-Striosome", "D1/D2-Hybrid", "D2-Matrix", "D2-Striosome", "Endothelial", "Interneurons", "Microglia", "Mural/Fibroblast", "Oligos", "Oligos_Pre", "UNK_MSN" );
                } );

        loader.setCellTypeFactorName( "celltype3" );
        loader.setUnknownCellTypeIndicator( null );
        assertThat( loader.getCellTypeAssignments( dimension ) )
                .singleElement()
                .satisfies( cta -> {
                    assertThat( cta.getCellTypes() )
                            .extracting( Characteristic::getValue )
                            .containsExactlyInAnyOrder( "Astrocytes", "D1-Matrix", "D1-Striosome", "D1/D2-Hybrid", "D2-Matrix", "D2-Striosome", "Endothelial", "Int-CCK", "Int-PTHLH", "Int-SST", "Int-TH", "Microglia", "Mural", "Oligos", "Oligos_Pre" );
                } );

        loader.setDesignElementMapper( new MapBasedDesignElementMapper( "test", elementMapping ) );
        assertThat( loader.loadVectors( elementMapping.values(), dimension, qt ).count() )
                .isEqualTo( 31393 );
    }

    private Path downloadAndTransposeGSE225158() throws IOException {
        assumeThatFreeMemoryIsGreaterOrEqualTo( 64 * 1024 * 1024 * 1024L, false );
        Path p = Paths.get( downloadPath, "singleCellData/GEO/GSE225158_transposed.h5ad" );
        if ( Files.exists( p ) ) {
            return p;
        }
        GeoSeries series = new GeoSeries();
        series.setGeoAccession( "GSE225158" );
        series.addToSupplementaryFiles( "ftp://ftp.ncbi.nlm.nih.gov/geo/series/GSE225nnn/GSE225158/suppl/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad.gz" );
        Path d;
        try {
            AnnDataDetector detector = new AnnDataDetector();
            detector.setDownloadDirectory( Paths.get( downloadPath, "singleCellData/GEO" ) );
            detector.setFTPClientFactory( ftpClientFactory );
            d = detector.downloadSingleCellData( series );
        } catch ( NoSingleCellDataFoundException e ) {
            throw new RuntimeException( e );
        }
        // transpose...
        // this step requires a *lot* of memory
        SingleCellDataTranspose transpose = new SingleCellDataTranspose();
        transpose.setPythonExecutable( pythonExecutable );
        transpose.setInputDataType( SingleCellDataType.ANNDATA );
        transpose.setInputFile( d );
        transpose.setOutputDataType( SingleCellDataType.ANNDATA );
        transpose.setOutputFile( p );
        transpose.perform();
        return p;
    }
}
