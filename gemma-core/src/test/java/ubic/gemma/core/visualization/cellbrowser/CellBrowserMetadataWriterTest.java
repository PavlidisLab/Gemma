package ubic.gemma.core.visualization.cellbrowser;

import org.junit.Before;
import org.junit.Test;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.persistence.service.expression.experiment.RandomExperimentalDesignUtils.randomCategoricalFactor;

public class CellBrowserMetadataWriterTest {

    private final Random random = new Random();

    private ExpressionExperiment ee;
    private SingleCellDimension scd;

    @Before
    public void setUp() {
        random.setSeed( 123L );

        List<SingleCellExpressionDataVector> vectors = RandomSingleCellDataUtils.randomSingleCellVectors();
        ee = vectors.iterator().next().getExpressionExperiment();
        scd = vectors.iterator().next().getSingleCellDimension();

        ExperimentalDesign design = new ExperimentalDesign();
        ee.setExperimentalDesign( design );
        randomCategoricalFactor( ee, "genotype", 2 );
        randomCategoricalFactor( ee, "treatment", 2 );

        // add some random bioassay metadata
        ee.getBioAssays().iterator().next().setIsOutlier( true );

        ee.getBioAssays().iterator().next().setProcessingDate( new Date() );

        // this is identical for all assays, so it must be ignored
        for ( BioAssay ba : ee.getBioAssays() ) {
            ba.setSequencePairedReads( true );
        }

        Category[] categories = new Category[] { Categories.CELL_TYPE, Categories.MASK };
        String[][] categoryValues = new String[][] {
                new String[] { "cellType1", "cellType2", "cellType3" },
                new String[] { "true", "false" }
        };

        // add some sample characteristics
        for ( BioAssay ba : ee.getBioAssays() ) {
            for ( int i = 0; i < categories.length; i++ ) {
                Category cat = categories[i];
                String[] catValues = categoryValues[i];
                ba.getSampleUsed().getCharacteristics().add( Characteristic.Factory.newInstance( cat, catValues[random.nextInt( catValues.length )], null ) );
            }
        }
    }

    @Test
    public void test() throws IOException {
        CellBrowserMetadataWriter writer = new CellBrowserMetadataWriter();
        StringWriter dest = new StringWriter();
        writer.write( ee, scd, dest );
        assertThat( dest.toString() )
                .contains( "cellId\tBioassay\tgenotype\ttreatment\tis.outlier\tprocessing.date\tcell.type\tmask\n" )
                .hasLineCount( 4000 + 1 );
    }

    @Test
    public void testUseRawColumnNames() throws IOException {
        CellBrowserMetadataWriter writer = new CellBrowserMetadataWriter();
        StringWriter dest = new StringWriter();
        writer.setUseRawColumnNames( true );
        writer.write( ee, scd, dest );
        assertThat( dest.toString() )
                .contains( "cellId\tBioassay\tgenotype\ttreatment\tis outlier\tprocessing date\tcell type\tmask\n" )
                .hasLineCount( 4000 + 1 );
    }

    @Test
    public void testSeparateSampleFromAssayIdentifiers() throws IOException {
        CellBrowserMetadataWriter writer = new CellBrowserMetadataWriter();
        StringWriter dest = new StringWriter();
        writer.setSeparateSampleFromAssayIdentifiers( true );
        writer.write( ee, scd, dest );
        assertThat( dest.toString() )
                .contains( "cellId\tSample\tAssay\tgenotype\ttreatment\tis.outlier\tprocessing.date\tcell.type\tmask\n" )
                .hasLineCount( 4000 + 1 );
    }
}