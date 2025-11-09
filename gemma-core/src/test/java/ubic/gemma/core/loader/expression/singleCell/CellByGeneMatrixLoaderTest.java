package ubic.gemma.core.loader.expression.singleCell;

import org.junit.Test;
import ubic.gemma.core.loader.util.mapper.SimpleDesignElementMapper;
import ubic.gemma.core.loader.util.mapper.TabularDataBioAssayMapper;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CellByGeneMatrixLoaderTest {

    @Test
    public void testGSE142535() throws IOException {
        List<BioAssay> assays = Arrays.asList(
                createAssay( "GSM4231441", "Mouse APP PBS J321" ),
                createAssay( "GSM4231442", "Mouse APP PBS J322" ),
                createAssay( "GSM4231443", "Mouse APP PBS J323" ),
                createAssay( "GSM4231444", "Mouse APP ASO J351" ),
                createAssay( "GSM4231445", "Mouse APP ASO J352" ),
                createAssay( "GSM4231446", "Mouse APP ASO J361" )
        );
        List<CompositeSequence> designElements = Arrays.asList(
                CompositeSequence.Factory.newInstance( "Adora2b" )
        );
        try ( SingleFileCellByGeneMatrixLoader loader = new SingleFileCellByGeneMatrixLoader( Paths.get( "/home/guillaume/Projets/Gemma/GSE142535_DropSeqAD.Mylip.digital_expression.txt.gz" ) ) ) {
            loader.setBioAssayToSampleNameMapper( new TabularDataBioAssayMapper() );
            loader.setIgnoreUnmatchedSamples( false );
            loader.setDesignElementToGeneMapper( new SimpleDesignElementMapper( designElements ) );
            assertThatThrownBy( loader::getSampleNames )
                    .isInstanceOf( UnsupportedOperationException.class );
            // assertThat( loader.getGenes() )
            //         .contains( "Adora2b" )
            //         .doesNotContain( "APP-PBS-J321_CCCCCCTTCGAT_Neuron_Dentate" ) // that's the header for the first column
            //         .hasSize( 28324 );
            // assertThat( loader.streamGenes() )
            //         .contains( "Adora2b" )
            //         .doesNotContain( "APP-PBS-J321_CCCCCCTTCGAT_Neuron_Dentate" ) // that's the header for the first column
            //         .hasSize( 28324 );
            SingleCellDimension scd = loader.getSingleCellDimension( assays );
            assertThat( scd.getCellIds() ).hasSize( 12822 );
            assertThat( scd.getBioAssays() ).hasSize( 6 );
            assertThat( scd.getBioAssaysOffset() ).containsExactly( 0, 1929, 3927, 6025, 8969, 11043 );
            QuantitationType qt = new QuantitationType();
            qt.setGeneralType( GeneralType.QUANTITATIVE );
            qt.setType( StandardQuantitationType.COUNT );
            qt.setScale( ScaleType.COUNT );
            qt.setRepresentation( PrimitiveType.INT );
            assertThat( loader.loadVectors( designElements, scd, qt ) )
                    .hasSize( 1 );
        }
    }

    private BioAssay createAssay( String geoAccession, String name ) {
        return BioAssay.Factory.newInstance( name, null, BioMaterial.Factory.newInstance( name ) );
    }
}