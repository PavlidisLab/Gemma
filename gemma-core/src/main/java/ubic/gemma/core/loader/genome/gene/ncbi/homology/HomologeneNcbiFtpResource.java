package ubic.gemma.core.loader.genome.gene.ncbi.homology;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.io.AbstractResource;
import ubic.basecode.util.FileTools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * Resource designed to retrieve Homologene data from NCBI FTP server.
 * @see HomologeneFetcher
 */
@CommonsLog
class HomologeneNcbiFtpResource extends AbstractResource {

    private final String filename;

    public HomologeneNcbiFtpResource( String filename ) {
        this.filename = filename;
    }

    @Override
    public String getDescription() {
        return String.format( "%s on NCBI FTP server", filename );
    }

    @Override
    public InputStream getInputStream() throws IOException {
        HomologeneFetcher hf = new HomologeneFetcher();
        Collection<File> downloadedFiles = hf.fetch( filename );

        if ( downloadedFiles == null || downloadedFiles.isEmpty() ) {
            throw new IOException( "Unable to download Homologene File. Aborting" );
        }

        if ( downloadedFiles.size() > 1 ) {
            log.info( "Downloaded more than 1 file for homologene.  Using 1st.  " );
        }

        File f = downloadedFiles.iterator().next();
        if ( !f.canRead() ) {
            throw new IOException( "Downloaded Homologene File. But unable to read Aborting" );
        }

        return FileTools.getInputStreamFromPlainOrCompressedFile( f.getAbsolutePath() );
    }
}
