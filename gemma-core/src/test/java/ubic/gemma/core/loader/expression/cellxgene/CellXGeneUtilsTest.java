package ubic.gemma.core.loader.expression.cellxgene;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.loader.expression.cellxgene.model.CollectionMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetVersion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CellXGeneUtilsTest {

    private static final DatasetVersion RNA = new DatasetVersion( "56a4bc14-9407-45fd-8786-f7f6fc87c9dd", "e6ef2a07-1b8e-49a8-a771-15b81971eac7", "Postnatal human brain development: RNA" );
    private static final DatasetVersion ATAC = new DatasetVersion( "11111111-1111-1111-1111-111111111111", "22222222-2222-2222-2222-222222222222", "Postnatal human brain development: ATAC" );
    private static final List<DatasetVersion> DATASETS = Arrays.asList( RNA, ATAC );

    @Test
    public void testResolveDatasetByPermanentId() {
        assertThat( CellXGeneUtils.resolveDataset( "c", DATASETS, "56a4bc14-9407-45fd-8786-f7f6fc87c9dd" ) ).isSameAs( RNA );
    }

    @Test
    public void testResolveDatasetByVersionId() {
        assertThat( CellXGeneUtils.resolveDataset( "c", DATASETS, "e6ef2a07-1b8e-49a8-a771-15b81971eac7" ) ).isSameAs( RNA );
    }

    @Test
    public void testResolveDatasetByTitle() {
        assertThat( CellXGeneUtils.resolveDataset( "c", DATASETS, "Postnatal human brain development: ATAC" ) ).isSameAs( ATAC );
        assertThat( CellXGeneUtils.resolveDataset( "c", DATASETS, "  postnatal HUMAN brain development: atac " ) ).isSameAs( ATAC );
    }

    @Test
    public void testResolveDatasetPrefersExactTitle() {
        DatasetVersion upper = new DatasetVersion( "a", "a1", "All Cells" );
        DatasetVersion lower = new DatasetVersion( "b", "b1", "All cells" );
        assertThat( CellXGeneUtils.resolveDataset( "c", Arrays.asList( upper, lower ), "All cells" ) ).isSameAs( lower );
    }

    @Test
    public void testResolveDatasetWhenTitleIsAmbiguous() {
        DatasetVersion upper = new DatasetVersion( "a", "a1", "All Cells" );
        DatasetVersion lower = new DatasetVersion( "b", "b1", "All cells" );
        assertThatThrownBy( () -> CellXGeneUtils.resolveDataset( "c", Arrays.asList( upper, lower ), "all cells" ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "More than one dataset" )
                .hasMessageContaining( "a: All Cells" )
                .hasMessageContaining( "b: All cells" );
    }

    @Test
    public void testResolveDatasetWhenNothingMatches() {
        assertThatThrownBy( () -> CellXGeneUtils.resolveDataset( "c", DATASETS, "Lung" ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "No dataset matching 'Lung' in CELLxGENE collection c" )
                .hasMessageContaining( "56a4bc14-9407-45fd-8786-f7f6fc87c9dd: Postnatal human brain development: RNA" );
    }

    @Test
    public void testGetDatasetMetadata() {
        DatasetMetadata dm = new DatasetMetadata();
        dm.setId( "e6ef2a07-1b8e-49a8-a771-15b81971eac7" );
        CollectionMetadata cm = new CollectionMetadata();
        cm.setId( "c" );
        cm.setDatasets( Collections.singletonList( dm ) );
        assertThat( CellXGeneUtils.getDatasetMetadata( cm, RNA ) ).isSameAs( dm );
        assertThatThrownBy( () -> CellXGeneUtils.getDatasetMetadata( cm, ATAC ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "22222222-2222-2222-2222-222222222222" );
    }
}
