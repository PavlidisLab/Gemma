package edu.columbia.gemma.loader.expression.geo;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.net.ftp.FTP;

import baseCode.util.NetUtils;

import edu.columbia.gemma.loader.expression.geo.model.GeoFile;
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
    public GeoFile retrieveByFTP( String accession ) throws SocketException, IOException {
        log.info( "Seeking GSE  file for " + accession );

        if ( this.f == null || !this.f.isConnected() ) f = GeoUtil.connect( FTP.BINARY_FILE_TYPE );
        // create a place to store the files.
        File newDir = mkdir( accession );

        String outputFileName = newDir + "/" + accession + "_family.soft.gz";

        String seekFile = baseDir + "/" + accession + "_family.soft.gz";
        success = NetUtils.ftpDownloadFile( f, seekFile, outputFileName, force );

        if ( success ) {
            // get meta-data about the file.
            GeoFile file = new GeoFile();
            file.setDownloadDate( new SimpleDateFormat().format( new Date() ) );
            file.setDownloadURL( seekFile );
            file.setLocalPath( outputFileName );
            // file.setSize( outputFile.length() );
            log.info( "Retrieved " + seekFile + " for experiment(set) " + accession + " .Output file is "
                    + outputFileName );
            return file;
        }
        log.error( "Failed" );
        return null;

    }
}
