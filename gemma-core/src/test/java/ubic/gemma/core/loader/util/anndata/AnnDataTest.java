package ubic.gemma.core.loader.util.anndata;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.core.loader.util.hdf5.H5FundamentalType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

public class AnnDataTest {

    @Test
    public void test() throws IOException {
        Path dataPath = new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath();
        try ( AnnData ad = AnnData.open( dataPath ) ) {
            try ( Dataframe<?> var = ad.getVar() ) {
                assertThat( var.getIndexColumn() )
                        .isEqualTo( "_index" );
                assertThat( var.getColumns() )
                        .containsExactlyInAnyOrder( "Age", "BMI", "Blood.Toxicology", "Case", "Cause.of.Death", "DSM.IV.AUD",
                                "DSM.IV.CUD", "DSM.IV.OUD", "DSM.IV.Psych", "DSM.IV.SUD",
                                "Dur.OUD", "Dx_Comorbid", "Dx_OUD", "Dx_Substances", "ID", "Index.27", "Index.28", "Infxn.Dx",
                                "Manner.of.Death", "Medications.ATODc", "PMI", "Pair", "RIN", "Race", "Region", "Sex",
                                "Tissue.Storage.Time.mo.b", "Tobacco.ATOD", "X", "_index", "celltype1", "celltype2", "celltype3",
                                "dropletQC.keep", "dropletQC.nucFrac", "i5.index.seq", "i7.index.seq", "integrated_snn_res.0.1",
                                "integrated_snn_res.0.5", "integrated_snn_res.1", "integrated_snn_res.2", "level1", "level2", "miQC.keep",
                                "miQC.probability", "nCount_RNA", "nCount_SCT", "nFeature_RNA", "nFeature_SCT", "n_genes",
                                "orig.ident", "pH", "percent.mt", "scds.hybrid_score", "scds.keep", "seurat_clusters" );
                for ( String c : var.getColumns() ) {
                    Dataframe.Column<?, ?> col = var.getColumn( c );
                    System.out.println( col );
                    assertThat( col.size() ).isEqualTo( 1000 );
                    assertThat( col.uniqueValues() ).isNotEmpty();
                }
            }

            assertThat( ad.getVar( String.class )
                    .getColumn( "seurat_clusters", Integer.class )
                    .get( "CCTCTAGCAAGTGATA_1" ) )
                    .isEqualTo( 3 );

            try ( Dataframe<?> obs = ad.getObs() ) {
                assertThat( obs.getIndexColumn() )
                        .isEqualTo( "_index" );
                assertThat( obs.getColumns() )
                        .containsExactlyInAnyOrder( "_index", "dispersions", "dispersions_norm", "features",
                                "highly_variable", "means", "n_cells" );

                for ( String c : obs.getColumns() ) {
                    Dataframe.Column<?, ?> col = obs.getColumn( c );
                    assertThat( col.size() ).isEqualTo( 1000 );
                    assertThat( col.uniqueValues() ).isNotEmpty();
                    System.out.println( col );
                }
            }
        }
    }

    @Test
    public void testIndexing() throws IOException {
        Path dataPath = new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath();
        try ( AnnData ad = AnnData.open( dataPath ) ) {
            assertThatThrownBy( () -> ad.getObs().getIndex() )
                    .isInstanceOf( IllegalArgumentException.class );
            Dataframe.Column<String, String> ix = ad.getObs( String.class ).getIndex();
            assertEquals( "LINC01763", ix.get( 0 ) );
            assertThatThrownBy( () -> ix.get( 1203901923 ) )
                    .isInstanceOf( ArrayIndexOutOfBoundsException.class );
            assertEquals( "LINC01763", ix.get( "LINC01763" ) );
            assertThatThrownBy( () -> ix.get( "LINC01764" ) )
                    .isInstanceOf( NoSuchElementException.class );
        }
    }

    @Test
    public void testEnum() throws IOException {
        Path dataPath = new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath();
        try ( AnnData ad = AnnData.open( dataPath ); Dataframe<?> obs = ad.getObs() ) {
            // an enum type
            assertThat( obs.getColumnEncodingType( "highly_variable" ) )
                    .isEqualTo( "array" );
            assertThat( obs.getArrayColumn( "highly_variable" ) )
                    .satisfies( col -> {
                        assertThat( col.getType() ).satisfies( t -> {
                            assertThat( t.getFundamentalType() ).isEqualTo( H5FundamentalType.ENUM );
                            assertThat( t.getMemberNames() ).containsExactly( "FALSE", "TRUE" );
                        } );
                    } );
            assertThat( obs.getColumn( "highly_variable" ).uniqueValues() )
                    .hasSize( 2 );
            assertThat( obs.getColumn( "highly_variable", Integer.class ).uniqueValues() )
                    .containsExactly( 0, 1 );
            // explicitly, as a string
            assertThat( obs.getColumn( "highly_variable", String.class ).uniqueValues() )
                    .containsExactlyInAnyOrder( "TRUE", "FALSE" );
        }
    }
}