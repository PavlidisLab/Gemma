package ubic.gemma.core.loader.expression.singleCell;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.loader.expression.singleCell.transform.*;
import ubic.gemma.core.loader.util.hdf5.H5Attribute;
import ubic.gemma.core.loader.util.hdf5.H5File;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

@Category(SlowTest.class)
public class SingleCellDataTransformationsTest {

    private static final Path pythonExecutable = Paths.get( Settings.getString( "python.exe" ) );

    @BeforeClass
    public static void checkIfAnnDataAndScipyAreInstalled() throws IOException {
        SingleCellDataTranspose transpose = new SingleCellDataTranspose();
        transpose.setPythonExecutable( pythonExecutable );
        assumeTrue( "scipy is required to run this test", transpose.isPackageInstalled( "scipy" ) );
        assumeTrue( "anndata is required to run this test", transpose.isPackageInstalled( "anndata" ) );
    }

    @Test
    public void testTranspose() throws IOException {
        SingleCellDataTranspose transpose = new SingleCellDataTranspose();
        transpose.setPythonExecutable( pythonExecutable );
        transpose.setInputFile( new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath(), SingleCellDataType.ANNDATA );
        transpose.setOutputFile( Files.createTempFile( "test", null ), SingleCellDataType.ANNDATA );
        transpose.perform();
    }

    @Test
    public void testPack() throws IOException {
        SingleCellDataPack pack = new SingleCellDataPack();
        pack.setPythonExecutable( pythonExecutable );
        pack.setInputFile( new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath(), SingleCellDataType.ANNDATA );
        pack.setOutputFile( Files.createTempFile( "test", null ), SingleCellDataType.ANNDATA );
        pack.perform();
    }

    @Test
    public void testSortBySample() throws IOException {
        SingleCellDataSortBySample pack = new SingleCellDataSortBySample();
        pack.setPythonExecutable( pythonExecutable );
        pack.setInputFile( new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath(), SingleCellDataType.ANNDATA );
        pack.setOutputFile( Files.createTempFile( "test", null ), SingleCellDataType.ANNDATA );
        pack.setSampleColumnName( "ID" );
        pack.perform();
    }

    @Test
    public void testSample() throws IOException {
        Path outputFile = Files.createTempFile( "test", null );
        SingleCellDataSample pack = new SingleCellDataSample();
        pack.setPythonExecutable( pythonExecutable );
        pack.setInputFile( new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath(), SingleCellDataType.ANNDATA );
        pack.setOutputFile( outputFile, SingleCellDataType.ANNDATA );
        pack.setNumberOfCellIds( 100 );
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

    @Test
    public void testRewrite() throws IOException {
        SingleCellDataRewrite pack = new SingleCellDataRewrite();
        pack.setPythonExecutable( pythonExecutable );
        pack.setInputFile( new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath(), SingleCellDataType.ANNDATA );
        pack.setOutputFile( Files.createTempFile( "test", null ), SingleCellDataType.ANNDATA );
        pack.perform();
    }

    @Test
    public void testUnraw() throws IOException {
        SingleCellDataUnraw pack = new SingleCellDataUnraw();
        pack.setPythonExecutable( pythonExecutable );
        pack.setInputFile( new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath(), SingleCellDataType.ANNDATA );
        pack.setOutputFile( Files.createTempFile( "test", null ), SingleCellDataType.ANNDATA );
        pack.perform();
    }

    @Test
    public void testPipeline() throws IOException {
        SingleCellDataTransformationPipeline pipeline = new SingleCellDataTransformationPipeline( Arrays.asList(
                new SingleCellDataRewrite(),
                new SingleCellDataTranspose(),
                new SingleCellDataPack()
        ) );
        pipeline.setPythonExecutable( pythonExecutable );
        pipeline.setInputFile( new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath(), SingleCellDataType.ANNDATA );
        pipeline.setOutputFile( Files.createTempFile( "test", null ), SingleCellDataType.ANNDATA );
        pipeline.perform();
    }
}