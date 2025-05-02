package ubic.gemma.core.loader.expression.geo.fetcher2;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.loader.expression.geo.service.GeoFormat;
import ubic.gemma.core.loader.expression.geo.service.GeoSource;
import ubic.gemma.core.loader.expression.geo.service.GeoUtils;
import ubic.gemma.core.loader.util.fetcher2.AbstractFetcher;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.util.SimpleRetry;
import ubic.gemma.core.util.SimpleRetryPolicy;

import java.io.IOException;
import java.nio.file.Path;

@CommonsLog
public class GeoFetcher extends AbstractFetcher {

    private final SimpleRetry<IOException> retryTemplate;
    private final Path geoSeriesDownloadPath;

    public GeoFetcher( FTPClientFactory ftpClientFactory, SimpleRetryPolicy retryPolicy, Path geoSeriesDownloadPath ) {
        super( ftpClientFactory );
        this.retryTemplate = new SimpleRetry<>( retryPolicy, IOException.class, GeoFetcher.class.getName() );
        this.geoSeriesDownloadPath = geoSeriesDownloadPath;
    }

    public Path fetchSeriesFamilySoftFile( String accession ) throws IOException {
        return retryTemplate.execute( ( ctx ) -> {
            Path dest = geoSeriesDownloadPath.resolve( accession ).resolve( accession + ".soft.gz" );
            try {
                downloadViaFtp( accession, GeoUtils.getUrlForSeriesFamily( accession, GeoSource.FTP, GeoFormat.SOFT ), dest );
            } catch ( IOException e ) {
                log.warn( "Retrieving SOFT file for " + accession + " via FTP failed, trying HTTPS.", e );
                try {
                    downloadViaHttps( accession, GeoUtils.getUrlForSeriesFamily( accession, GeoSource.FTP_VIA_HTTPS, GeoFormat.SOFT ), dest );
                } catch ( IOException e1 ) {
                    log.warn( "Retrieving SOFT file for " + accession + " via HTTPS failed, trying via the GEO Browser directly.", e );
                    // last resort, ask GEO to generate the file
                    downloadViaHttps( accession, GeoUtils.getUrlForSeriesFamily( accession, GeoSource.QUERY, GeoFormat.SOFT ), dest );
                }
            }
            return dest;
        }, "download " + accession + ".soft.gz" );
    }
}
