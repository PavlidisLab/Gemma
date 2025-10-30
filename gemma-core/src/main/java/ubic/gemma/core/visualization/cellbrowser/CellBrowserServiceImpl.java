package ubic.gemma.core.visualization.cellbrowser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellLevelMeasurements;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static ubic.gemma.core.visualization.cellbrowser.CellBrowserUtils.constructDatasetName;

@Service
public class CellBrowserServiceImpl implements CellBrowserService {

    @Value("${gemma.cellBrowser.baseUrl}")
    private String baseUrl;

    @Value("${gemma.cellBrowser.dir}")
    private Path cellBrowserDir;

    @Override
    public String getBrowserUrl( ExpressionExperiment ee, @Nullable String meta ) {
        return baseUrl + "?ds=" + urlEncode( constructDatasetName( ee ) ) + ( meta != null ? "&meta=" + urlEncode( meta ) : "" );
    }

    @Override
    public boolean hasBrowser( ExpressionExperiment ee ) {
        return Files.exists( cellBrowserDir.resolve( constructDatasetName( ee ) ) );
    }

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Override
    @Transactional(readOnly = true)
    public List<CellBrowserMapping> getCellBrowserMapping( ExpressionExperiment ee, boolean useRawColumnNames ) {
        Assert.isTrue( hasBrowser( ee ), ee + " does not have a Cell Browser." );
        List<ExperimentalFactor> factors = CellBrowserUtils.getFactors( ee );
        SingleCellExpressionExperimentService.SingleCellDimensionInitializationConfig config = SingleCellExpressionExperimentService.SingleCellDimensionInitializationConfig.builder()
                .includeCtas( true )
                .includeClcs( true )
                .build();
        SingleCellDimension scd = singleCellExpressionExperimentService.getPreferredSingleCellDimensionWithoutCellIds( ee, config )
                .orElseThrow( () -> new IllegalArgumentException( ee + " does not have a preferred single-cell dimension." ) );
        if ( scd == null ) {
            throw new IllegalArgumentException( "No SingleCellDimension found for " + ee + "." );
        }
        List<CellLevelCharacteristics> clcs = CellBrowserUtils.getCellLevelCharacteristics( scd );
        List<CellLevelMeasurements> clms = CellBrowserUtils.getCellLevelMeasurements( scd );
        return CellBrowserUtils.createMetadataMapping( factors, clcs, clms, useRawColumnNames ).stream()
                .filter( m -> hasMetaField( ee, m.getMetaColumnId() ) )
                .collect( Collectors.toList() );
    }

    private boolean hasMetaField( ExpressionExperiment ee, String fieldName ) {
        return Files.exists( cellBrowserDir.resolve( constructDatasetName( ee ) ).resolve( "metaFields" ).resolve( fieldName + ".bin.gz" ) );
    }

    private String urlEncode( String s ) {
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }
}
