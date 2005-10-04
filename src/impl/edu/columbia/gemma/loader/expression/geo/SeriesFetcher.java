package edu.columbia.gemma.loader.expression.geo;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.net.ftp.FTP;

import baseCode.util.NetUtils;
import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.loader.expression.geo.util.GeoUtil;
import edu.columbia.gemma.loader.loaderutils.FtpFetcher;

public class SeriesFetcher extends FtpFetcher {

    /**
     * 
     */
    public SeriesFetcher() throws ConfigurationException {
        Configuration config = new PropertiesConfiguration( "Gemma.properties" );
        this.localBasePath = ( String ) config.getProperty( "geo.local.datafile.basepath" );
        this.baseDir = ( String ) config.getProperty( "geo.remote.seriesDir" );
    }

    /**
     * @param accession
     * @throws SocketException
     * @throws IOException
     */
    public Collection<LocalFile> fetch( String accession ) {
        log.info( "Seeking GSE  file for " + accession );

        try {
            if ( this.f == null || !this.f.isConnected() ) f = GeoUtil.connect( FTP.BINARY_FILE_TYPE );
            File newDir = mkdir( accession );

            String outputFileName = newDir + "/" + accession + "_family.soft.gz";

            String seekFile = baseDir + "/" + accession + "_family.soft.gz";
            success = NetUtils.ftpDownloadFile( f, seekFile, outputFileName, force );

            if ( success ) {
                LocalFile file = fetchedFile( seekFile, outputFileName );
                log.info( "Retrieved " + seekFile + " for experiment(set) " + accession + " .Output file is "
                        + outputFileName );
                Collection<LocalFile> result = new HashSet<LocalFile>();
                result.add( file );
                return result;
            }
        } catch ( IOException e ) {
            log.error( e, e );
        }
        log.error( "Failed" );
        return null;

    }
}
