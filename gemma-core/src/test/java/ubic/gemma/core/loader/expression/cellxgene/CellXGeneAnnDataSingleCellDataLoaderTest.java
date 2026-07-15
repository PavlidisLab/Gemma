package ubic.gemma.core.loader.expression.cellxgene;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class CellXGeneAnnDataSingleCellDataLoaderTest {

    /**
     * Trimmed fixture derived from the CELLxGENE "HypoMap" atlas (a unified
     * single-cell atlas of the murine hypothalamus), which contains a "pooled"
     * donor alongside the individual SRR donors. See
     * make_cellxgene_pooled_fixture.py next to the fixture for how it was
     * produced.
     * <p>
     * This test originally downloaded dataset d3be7423-d664-4913-89a9-a506cae4c28f
     * live from CELLxGENE, but that dataset id was removed from the index when
     * the atlas was re-published. As of this writing HypoMap lives under:
     * <ul>
     *     <li>collection id: d86517f0-fa7e-4266-b82e-a521350d6d36</li>
     *     <li>dataset id: 87b802cc-73ca-422a-8cc7-6d6d38449b3f</li>
     * </ul>
     */
    @Test
    public void testKeepPooledSample() throws IOException {
        Path dataPath = new ClassPathResource( "/data/loader/expression/singleCell/cellxgene-pooled-sample.h5ad" ).getFile().toPath();

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
