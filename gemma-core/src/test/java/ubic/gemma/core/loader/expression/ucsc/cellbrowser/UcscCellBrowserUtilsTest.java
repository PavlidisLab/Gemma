package ubic.gemma.core.loader.expression.ucsc.cellbrowser;

import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.map.DefaultedMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.core.util.LoggingProgressReporter;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableRule;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class UcscCellBrowserUtilsTest {

    @Rule
    public final NetworkAvailableRule networkAvailableRule = new NetworkAvailableRule();

    @Test
    @NetworkAvailable
    public void testGetDatasets() throws IOException {
        assertThat( UcscCellBrowserUtils.getDatasets() )
                .isNotEmpty();
    }

    @Test
    public void testParseDatasets() throws IOException {
        UcscCellBrowserUtils.parseDatasets( getClass().getResource( "/data/loader/expression/ucsc/cellbrowser/dataset.json" ) );
    }

    @Test
    @NetworkAvailable
    public void testGetDataset() throws IOException {
        UcscCellBrowserUtils.getDataset( "aging-brain" );
    }

    @Test
    @NetworkAvailable
    public void testGetDatasetDescription() throws IOException {
        UcscCellBrowserUtils.getDatasetDescription( "aging-brain" );
    }

    @Test
    public void testParseDataset() throws IOException {
        UcscCellBrowserUtils.parseDataset( getClass().getResource( "/data/loader/expression/ucsc/cellbrowser/aging-brain.json" ) );
    }

    @Test
    @Category(SlowTest.class)
    @NetworkAvailable
    public void testGetDatasetDataMatrix() throws IOException {
        QuantitationType quantitationType = QuantitationType.Factory.newInstance();
        quantitationType.setGeneralType( GeneralType.QUANTITATIVE );
        quantitationType.setType( StandardQuantitationType.AMOUNT );
        quantitationType.setRepresentation( PrimitiveType.DOUBLE );
        Map<String, CompositeSequence> designElementsMap = DefaultedMap.defaultedMap( new HashMap<String, CompositeSequence>(),
                ( Transformer<String, CompositeSequence> ) CompositeSequence.Factory::newInstance );
        Map<String, BioAssay> assayByName = new HashMap<>();
        Map<String, BioAssay> cellIdToAssayMap = UcscCellBrowserUtils.getDatasetMetadata( "adultPancreas", "experiment_name" )
                .entrySet().stream()
                .collect( Collectors.toMap( Map.Entry::getKey, e -> assayByName.computeIfAbsent( ( String ) e.getValue(), BioAssay.Factory::newInstance ) ) );
        SingleCellExpressionDataMatrix<?> matrix = UcscCellBrowserUtils.getDatasetDataMatrix( "adultPancreas",
                quantitationType, designElementsMap, cellIdToAssayMap, new LoggingProgressReporter( "download data matrix from Adult Pancreas dataset",
                        UcscCellBrowserUtilsTest.class.getName() ) );
        assertThat( matrix.getSingleCellDimension().getBioAssays() ).hasSize( 10 );
        assertThat( matrix.getDesignElements() ).hasSize( 23460 );
        assertThat( matrix.getCellIds() ).hasSize( 4026 );
    }
}