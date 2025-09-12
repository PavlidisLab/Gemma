package ubic.gemma.core.loader.expression.singleCell;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.config.SettingsConfig;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.expression.singleCell.transform.*;
import ubic.gemma.core.loader.util.hdf5.H5Attribute;
import ubic.gemma.core.loader.util.hdf5.H5File;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

@Category(SlowTest.class)
@ContextConfiguration
public class SingleCellDataTransformationsTest extends BaseTest {

    @Configuration
    @TestComponent
    @Import({ SettingsConfig.class, SingleCellTransformationConfig.class })
    public static class Config {

    }

    @Autowired
    public ApplicationContext ctx;

    @Test
    public void testTranspose() throws IOException {
        checkIfPackageIsInstalled( "scipy" );
        checkIfPackageIsInstalled( "anndata" );
        SingleCellDataTranspose transpose = ctx.getBean( SingleCellDataTranspose.class );
        transpose.setInputFile( new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath(), SingleCellDataType.ANNDATA );
        transpose.setOutputFile( Files.createTempFile( "test", null ), SingleCellDataType.ANNDATA );
        transpose.perform();
    }

    @Test
    public void testPack() throws IOException {
        checkIfPackageIsInstalled( "scipy" );
        checkIfPackageIsInstalled( "anndata" );
        SingleCellDataPack pack = ctx.getBean( SingleCellDataPack.class );
        pack.setInputFile( new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath(), SingleCellDataType.ANNDATA );
        pack.setOutputFile( Files.createTempFile( "test", null ), SingleCellDataType.ANNDATA );
        pack.perform();
    }

    @Test
    public void testSortBySample() throws IOException {
        checkIfPackageIsInstalled( "scipy" );
        checkIfPackageIsInstalled( "anndata" );
        SingleCellDataSortBySample pack = ctx.getBean( SingleCellDataSortBySample.class );
        pack.setInputFile( new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath(), SingleCellDataType.ANNDATA );
        pack.setOutputFile( Files.createTempFile( "test", null ), SingleCellDataType.ANNDATA );
        pack.setSampleColumnName( "ID" );
        pack.perform();
    }

    @Test
    public void testSample() throws IOException {
        checkIfPackageIsInstalled( "anndata" );
        checkIfPackageIsInstalled( "numpy" );
        Path outputFile = Files.createTempFile( "test", null );
        SingleCellDataSample pack = ctx.getBean( SingleCellDataSample.class );
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
        checkIfPackageIsInstalled( "anndata" );
        SingleCellDataRewrite pack = ctx.getBean( SingleCellDataRewrite.class );
        pack.setInputFile( new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath(), SingleCellDataType.ANNDATA );
        pack.setOutputFile( Files.createTempFile( "test", null ), SingleCellDataType.ANNDATA );
        pack.perform();
    }

    @Test
    public void testUnraw() throws IOException {
        checkIfPackageIsInstalled( "anndata" );
        SingleCellDataUnraw pack = ctx.getBean( SingleCellDataUnraw.class );
        pack.setInputFile( new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath(), SingleCellDataType.ANNDATA );
        pack.setOutputFile( Files.createTempFile( "test", null ), SingleCellDataType.ANNDATA );
        pack.perform();
    }

    @Test
    public void testPipeline() throws IOException {
        checkIfPackageIsInstalled( "scipy" );
        checkIfPackageIsInstalled( "anndata" );
        SingleCellDataTransformationPipeline pipeline = new SingleCellDataTransformationPipeline( Arrays.asList(
                ctx.getBean( SingleCellDataRewrite.class ),
                ctx.getBean( SingleCellDataTranspose.class ),
                ctx.getBean( SingleCellDataPack.class )
        ) );
        pipeline.setInputFile( new ClassPathResource( "/data/loader/expression/singleCell/GSE225158_BU_OUD_Striatum_refined_all_SeuratObj_N22.h5ad" ).getFile().toPath(), SingleCellDataType.ANNDATA );
        pipeline.setOutputFile( Files.createTempFile( "test", null ), SingleCellDataType.ANNDATA );
        pipeline.perform();
    }

    private static final ConcurrentHashMap<String, Boolean> isPackageInstalled = new ConcurrentHashMap<>();

    private void checkIfPackageIsInstalled( String packageName ) {
        assumeTrue( packageName + " is required to run this test", isPackageInstalled.computeIfAbsent( packageName, ignored -> {
            SingleCellDataTranspose transpose = ctx.getBean( SingleCellDataTranspose.class );
            try {
                return transpose.isPackageInstalled( packageName );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        } ) );
    }
}