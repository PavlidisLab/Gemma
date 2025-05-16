package ubic.gemma.core.loader.expression;

import org.junit.Test;
import org.springframework.util.ResourceUtils;
import ubic.gemma.core.loader.expression.sequencing.SequencingMetadata;
import ubic.gemma.core.loader.expression.sequencing.SequencingMetadataFileDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.core.loader.util.mapper.SimpleBioAssayMapper;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SequencingMetadataFileDataLoaderTest {

    @Test
    public void test() throws IOException {
        SequencingMetadataFileDataLoader loader = new SequencingMetadataFileDataLoader( mock(), ResourceUtils.getFile( "classpath:data/loader/expression/sequencing-metadata-complete.tsv" ).toPath(), null, new SimpleBioAssayMapper() );
        BioAssay ba = BioAssay.Factory.newInstance( "test" );
        assertThat( loader.getSequencingMetadata( Collections.singleton( ba ) ) )
                .containsEntry( ba, SequencingMetadata.builder().readLength( 100 ).readCount( 142928L ).isPaired( true ).build() );
    }

    @Test
    public void testWithDelegateThatProducesLibrarySize() throws IOException {
        BioAssay ba = BioAssay.Factory.newInstance( "test" );
        SingleCellDataLoader delegate = mock();
        when( delegate.getSequencingMetadata( anyCollection() ) )
                .thenReturn( Collections.singletonMap( ba, SequencingMetadata.builder().readCount( 42L ).build() ) );
        SequencingMetadataFileDataLoader loader = new SequencingMetadataFileDataLoader( delegate, ResourceUtils.getFile( "classpath:data/loader/expression/sequencing-metadata.tsv" ).toPath(), null, new SimpleBioAssayMapper() );
        assertThat( loader.getSequencingMetadata( Collections.singleton( ba ) ) )
                .containsEntry( ba, SequencingMetadata.builder().readLength( 100 ).isPaired( true ).readCount( 42L ).build() );
    }

    @Test
    public void testOverwriteValueFromDelegate() throws IOException {
        BioAssay ba = BioAssay.Factory.newInstance( "test" );
        SingleCellDataLoader delegate = mock();
        when( delegate.getSequencingMetadata( anyCollection() ) )
                .thenReturn( Collections.singletonMap( ba, SequencingMetadata.builder().readLength( 50 ).build() ) );
        SequencingMetadataFileDataLoader loader = new SequencingMetadataFileDataLoader( delegate, ResourceUtils.getFile( "classpath:data/loader/expression/sequencing-metadata.tsv" ).toPath(), null, new SimpleBioAssayMapper() );
        assertThat( loader.getSequencingMetadata( Collections.singleton( ba ) ) )
                .containsEntry( ba, SequencingMetadata.builder().readLength( 100 ).isPaired( true ).build() );
    }

    @Test
    public void testWithDefault() throws IOException {
        BioAssay ba = BioAssay.Factory.newInstance( "test" );
        SequencingMetadata defaultMetadata = SequencingMetadata.builder().readCount( 20000L ).build();
        SingleCellDataLoader delegate = mock();
        when( delegate.getSequencingMetadata( anyCollection() ) )
                .thenReturn( Collections.singletonMap( ba, SequencingMetadata.builder().readCount( 10000L ).build() ) );
        SequencingMetadataFileDataLoader loader = new SequencingMetadataFileDataLoader( delegate, ResourceUtils.getFile( "classpath:data/loader/expression/sequencing-metadata.tsv" ).toPath(), defaultMetadata, new SimpleBioAssayMapper() );
        assertThat( loader.getSequencingMetadata( Collections.singleton( ba ) ) )
                .containsEntry( ba, SequencingMetadata.builder().readLength( 100 ).readCount( 10000L ).isPaired( true ).build() );
    }
}