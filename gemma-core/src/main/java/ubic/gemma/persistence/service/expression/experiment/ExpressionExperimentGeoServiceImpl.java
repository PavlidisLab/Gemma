package ubic.gemma.persistence.service.expression.experiment;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.loader.expression.geo.GeoFamilyParser;
import ubic.gemma.core.loader.expression.geo.fetcher2.GeoFetcher;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.util.ftp.FTPClientFactory;
import ubic.gemma.core.util.FileUtils;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.core.util.locking.FileLockManager;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@CommonsLog
public class ExpressionExperimentGeoServiceImpl implements ExpressionExperimentGeoService {

    private static final SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy( 5, 500, 1.5 );

    @Autowired
    private FTPClientFactory ftpClientFactory;

    @Autowired
    private FileLockManager fileLockManager;

    @Value("${geo.local.datafile.basepath}")
    private Path geoSeriesDownloadPath;

    @Override
    public GeoSeries getGeoSeries( ExpressionExperiment ee ) {
        if ( ee.getAccession() == null || !ee.getAccession().getExternalDatabase().getName().equals( ExternalDatabases.GEO ) ) {
            log.warn( ee + " does not originate from GEO, no GEO series metadata will be retrieved." );
            return null;
        }
        GeoFetcher geoFetcher = new GeoFetcher( retryPolicy, geoSeriesDownloadPath );
        geoFetcher.setFtpClientFactory( ftpClientFactory );
        geoFetcher.setFileLockManager( fileLockManager );
        try {
            Path geoSeriesFile = geoFetcher.fetchSeriesFamilySoftFile( ee.getAccession().getAccession() );
            try ( InputStream is = FileUtils.openCompressedFile( geoSeriesFile ) ) {
                GeoFamilyParser gfp = new GeoFamilyParser();
                gfp.parse( is );
                return requireNonNull( gfp.getUniqueResult() ).getSeries().get( ee.getAccession().getAccession() );
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }
}
