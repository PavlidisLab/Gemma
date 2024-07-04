package ubic.gemma.core.loader.expression.singleCell;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.core.loader.util.hdf5.H5Attribute;
import ubic.gemma.core.loader.util.hdf5.H5File;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

public class SingleCellDataTransformationsTest {

    @BeforeClass
    public static void checkIfAnnDataAndScipyAreInstalled() throws IOException {
        SingleCellDataTranspose transpose = new SingleCellDataTranspose();
        assumeTrue( "scipy is required to run this test", transpose.isPackageInstalled( "scipy" ) );
        assumeTrue( "anndata is required to run this test", transpose.isPackageInstalled( "anndata" ) );
    }

    @Test
    public void testTranspose() throws IOException {
        SingleCellDataTranspose transpose = new SingleCellDataTranspose();
        transpose.setInputFile( new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath() );
        transpose.setInputDataType( SingleCellDataType.ANNDATA );
        transpose.setOutputFile( Files.createTempFile( "test", null ) );
        transpose.setOutputDataType( SingleCellDataType.ANNDATA );
        transpose.perform();
    }

    @Test
    public void testPack() throws IOException {
        SingleCellDataPack pack = new SingleCellDataPack();
        pack.setInputFile( new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath() );
        pack.setInputDataType( SingleCellDataType.ANNDATA );
        pack.setOutputFile( Files.createTempFile( "test", null ) );
        pack.setOutputDataType( SingleCellDataType.ANNDATA );
        pack.perform();
    }

    @Test
    public void testSortBySample() throws IOException {
        SingleCellDataSortBySample pack = new SingleCellDataSortBySample();
        pack.setInputFile( new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath() );
        pack.setInputDataType( SingleCellDataType.ANNDATA );
        pack.setOutputFile( Files.createTempFile( "test", null ) );
        pack.setOutputDataType( SingleCellDataType.ANNDATA );
        pack.setSampleColumnName( "ID" );
        pack.perform();
    }

    @Test
    public void testSample() throws IOException {
        Path outputFile = Files.createTempFile( "test", null );
        SingleCellDataSample pack = new SingleCellDataSample();
        pack.setInputFile( new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath() );
        pack.setInputDataType( SingleCellDataType.ANNDATA );
        pack.setOutputFile( outputFile );
        pack.setOutputDataType( SingleCellDataType.ANNDATA );
        pack.setNumberOfCells( 100 );
        pack.setNumberOfGenes( 50 );
        pack.perform();
        try ( H5File f = H5File.open( outputFile ) ) {
            assertEquals( 100, f.getDataset( "var/_index" ).size() );
            assertEquals( 50, f.getDataset( "obs/_index" ).size() );
            int[] shape = f.getAttribute( "X", "shape" ).map( H5Attribute::toIntegerVector ).orElse( null );
            assertNotNull( shape );
            assertEquals( 50, shape[0] );
            assertEquals( 100, shape[1] );
        }
    }
}