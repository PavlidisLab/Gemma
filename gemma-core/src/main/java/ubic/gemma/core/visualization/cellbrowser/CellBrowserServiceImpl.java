package ubic.gemma.core.visualization.cellbrowser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class CellBrowserServiceImpl implements CellBrowserService {

    private static final String UNALLOWED_CHARS = "[^A-Za-z0-9-_]";

    @Value("${gemma.cellBrowser.baseUrl}")
    private String baseUrl;

    @Value("${gemma.cellBrowser.dir}")
    private Path cellBrowserDir;

    @Override
    public String getBrowserUrl( ExpressionExperiment ee ) {
        return baseUrl + "?ds=" + urlEncode( getDatasetName( ee ) );
    }

    @Override
    public boolean hasBrowser( ExpressionExperiment ee ) {
        return Files.exists( cellBrowserDir.resolve( getDatasetName( ee ) ) );
    }

    private String getDatasetName( ExpressionExperiment ee ) {
        return ee.getShortName().replaceAll( UNALLOWED_CHARS, "_" );
    }

    private static String urlEncode( String s ) {
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }
}
