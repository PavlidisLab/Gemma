package ubic.gemma.loader.genome.llnl;

import java.io.InputStream;

import ubic.gemma.loader.genome.llnl.ImageCumulativePlatesLoader;
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
        int actualValue = loader.load( is );
        assertEquals( 100, actualValue );
    }

    @Override
    protected void onTearDownInTransaction() throws Exception {
        is.close();
    }

}
