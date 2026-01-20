package ubic.gemma.core.visualization.cellbrowser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static ubic.gemma.core.util.StringUtils.urlEncode;
import static ubic.gemma.core.visualization.cellbrowser.CellBrowserUtils.constructDatasetName;

@Service
public class CellBrowserServiceImpl implements CellBrowserService {

    @Value("${gemma.cellBrowser.baseUrl}")
    private String baseUrl;

    @Value("${gemma.cellBrowser.dir}")
    private Path cellBrowserDir;

    @Override
    public String getBrowserUrl( ExpressionExperiment ee ) {
        return baseUrl + "?ds=" + urlEncode( constructDatasetName( ee ) );
    }

    @Override
    public boolean hasBrowser( ExpressionExperiment ee ) {
        return Files.exists( cellBrowserDir.resolve( constructDatasetName( ee ) ) );
    }
}
