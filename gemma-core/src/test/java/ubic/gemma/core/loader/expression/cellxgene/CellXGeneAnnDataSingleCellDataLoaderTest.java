package ubic.gemma.core.loader.expression.cellxgene;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.config.SettingsConfig;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetAsset;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetMetadata;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
public class CellXGeneAnnDataSingleCellDataLoaderTest extends BaseTest {

    @Configuration
    @TestComponent
    @Import(SettingsConfig.class)
    static class CC {

    }

    @Value("${cellxgene.local.singleCellData.basepath}")
    private Path cellXGeneDownloadPath;

    @Test
    @NetworkAvailable
    public void testKeepPooledSample() throws IOException {
        CellXGeneFetcher fetcher = new CellXGeneFetcher( new SimpleRetryPolicy( 3, 1000, 1.5 ), cellXGeneDownloadPath );

        DatasetMetadata dm = fetcher.fetchDatasetMetadata( "d3be7423-d664-4913-89a9-a506cae4c28f" );
        String assetId = dm.getDatasetAssets().stream()
                .filter( CellXGeneUtils::isAnnData )
                .map( DatasetAsset::getId )
                .findFirst()
                .orElseThrow( () -> new RuntimeException( "Could not find a H5AD asset for CELLxGENE dataset d3be7423-d664-4913-89a9-a506cae4c28f." ) );

        Path dataPath = fetcher.downloadDatasetAsset( dm.getId(), assetId, FileType.H5AD );

        try ( CellXGeneAnnDataSingleCellDataLoader loader = new CellXGeneAnnDataSingleCellDataLoader( dataPath, true, false ) ) {
            assertThat( loader.getSampleNames() ).containsExactly(
                    "SRR6854065", "SRR6854066", "SRR6854077", "SRR6854080",
                    "SRR6854090", "SRR6854135", "SRR6854136", "SRR6854141",
                    "SRR6854142", "SRR6854157", "SRR6854160", "SRR9000480",
                    "SRR9000481", "SRR9000482", "SRR9000483", "SRR9000484",
                    "SRR9000485", "SRR9000486", "SRR9000487", "SRR9000488",
                    "SRR9000489", "SRR9000491", "SRR9000492", "pooled"
            );

            List<BioAssay> samples = loader.getSampleNames().stream().map( sn -> BioAssay.Factory.newInstance( sn, null, BioMaterial.Factory.newInstance( sn ) ) )
                    .collect( Collectors.toList() );
            loader.getSamplesCharacteristics( samples );
        }

        try ( CellXGeneAnnDataSingleCellDataLoader loader = new CellXGeneAnnDataSingleCellDataLoader( dataPath, false, false ) ) {
            assertThat( loader.getSampleNames() ).containsExactly(
                    "SRR6854065", "SRR6854066", "SRR6854077", "SRR6854080",
                    "SRR6854090", "SRR6854135", "SRR6854136", "SRR6854141",
                    "SRR6854142", "SRR6854157", "SRR6854160", "SRR9000480",
                    "SRR9000481", "SRR9000482", "SRR9000483", "SRR9000484",
                    "SRR9000485", "SRR9000486", "SRR9000487", "SRR9000488",
                    "SRR9000489", "SRR9000491", "SRR9000492"
            );

            List<BioAssay> samples = loader.getSampleNames().stream().map( sn -> BioAssay.Factory.newInstance( sn, null, BioMaterial.Factory.newInstance( sn ) ) )
                    .collect( Collectors.toList() );
            loader.getSamplesCharacteristics( samples );
        }
    }
}