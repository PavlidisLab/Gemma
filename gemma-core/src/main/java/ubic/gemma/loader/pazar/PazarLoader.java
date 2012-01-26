package ubic.gemma.loader.pazar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface PazarLoader {

    /**
     * @param is
     * @return
     * @throws IOException
     */
    public abstract int load( InputStream is ) throws IOException;

    /**
     * @param file
     * @return
     * @throws IOException
     */
    public abstract int load( File file ) throws IOException;

}