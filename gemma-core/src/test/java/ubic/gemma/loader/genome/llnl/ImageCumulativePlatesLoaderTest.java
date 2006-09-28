package ubic.gemma.loader.genome.llnl;

import java.io.InputStream;

import ubic.gemma.loader.genome.llnl.ImageCumulativePlatesLoader;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

public class ImageCumulativePlatesLoaderTest extends BaseTransactionalSpringContextTest {

    InputStream is;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        is = this.getClass().getResourceAsStream( "/data/loader/genome/cumulative.plates.test.txt" );
    }

    public void testLoadInputStream() throws Exception {
        ImageCumulativePlatesLoader loader = new ImageCumulativePlatesLoader();
        loader.setPersisterHelper( persisterHelper );
        loader.setBioSequenceService( ( BioSequenceService ) this.getBean( "bioSequenceService" ) );
        loader.setExternalDatabaseService( ( ExternalDatabaseService ) this.getBean( "externalDatabaseService" ) );
        int actualValue = loader.load( is );
        assertEquals( 425, actualValue );
    }

    @Override
    protected void onTearDownInTransaction() throws Exception {
        is.close();
    }

}
