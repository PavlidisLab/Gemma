package ubic.gemma.core.loader.expression.geo.singleCell;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.expression.geo.model.GeoChannel;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;

import javax.annotation.Nullable;

/**
 * Utilities for detecting 10x Cell Ranger data in GEO series and sample metadata.
 * @author poirigui
 */
public class TenXCellRangerUtils {

    public static boolean detect10x( GeoSample sample ) {
        return StringUtils.containsAnyIgnoreCase( sample.getDataProcessing(), "cellranger", "cell ranger" );
    }

    public static boolean detect10xFiltered( GeoSample sample ) {
        return StringUtils.containsAnyIgnoreCase( sample.getDataProcessing(), "sample_filtered_feature_bc_matrix" );
    }

    public static boolean detect10xUnfiltered( GeoSample sample ) {
        return StringUtils.containsAnyIgnoreCase( sample.getDataProcessing(), "raw", "unfiltered", "raw_feature_bc_matrix" );
    }

    /**
     * We essentially rely on the presence of specific production number of reagent kits in the extraction protocol
     * section.
     * <p>
     * TODO: this will need more work and tests
     */
    @Nullable
    public static String detect10xChemistry( GeoSample sample ) {
        for ( GeoChannel channel : sample.getChannels() ) {
            if ( StringUtils.containsIgnoreCase( channel.getExtractProtocol(), "Single Cell 3′ Gene Expression kit v3" )
                    || StringUtils.containsAny( channel.getExtractProtocol(),
                    // https://www.10xgenomics.com/support/universal-three-prime-gene-expression/documentation/steps/library-prep/chromium-next-gem-automated-single-cell-3-reagent-kits-user-guide-v-3-1-chemistry
                    "1000141", "1000147", "1000136", "1000146", "1000213", "1000215",
                    // https://www.10xgenomics.com/support/universal-three-prime-gene-expression/documentation/steps/library-prep/chromium-next-gem-single-cell-3-ht-reagent-kits-v-3-1-dual-index
                    "1000370", "1000371", "1000215",
                    // https://www.10xgenomics.com/support/universal-three-prime-gene-expression/documentation/steps/library-prep/chromium-single-cell-3-reagent-kits-user-guide-v-3-1-chemistry-dual-index
                    "1000268", "1000269", "1000120", "1000127", "1000215"

            ) ) {
                return "SC3Pv3" + ( isHighThroughput( channel ) ? "HT" : "" ) + ( isCs1( channel ) ? "-CS1" : "-polyA" ) + ( isOcm( channel ) ? "-OCM" : "" );
            }
            if ( StringUtils.containsIgnoreCase( channel.getExtractProtocol(), "Single Cell 3′ Gene Expression kit v4" )
                    || StringUtils.containsAny( channel.getExtractProtocol(),
                    // https://www.10xgenomics.com/support/universal-three-prime-gene-expression/documentation/steps/library-prep/gem-x-universal-3-prime-gene-expression-v-4-4-plex-reagent-kits
                    "1000779", "1000747", "1000215",
                    // https://www.10xgenomics.com/support/universal-three-prime-gene-expression/documentation/steps/library-prep/chromium-gem-x-single-cell-3-v4-gene-expression-user-guide
                    "1000686", "1000690", "1000215" ) ) {
                return "SC3Pv4" + ( isCs1( channel ) ? "-CS1" : "-polyA" ) + ( isOcm( channel ) ? "-OCM" : "" );
            }
        }
        return null;
    }

    private static boolean isHighThroughput( GeoChannel channel ) {
        return StringUtils.containsAny( channel.getExtractProtocol(),
                // https://www.10xgenomics.com/support/universal-three-prime-gene-expression/documentation/steps/library-prep/chromium-next-gem-single-cell-3-ht-reagent-kits-v-3-1-dual-index
                "1000370", "1000371", "1000215"
        );
    }

    private static boolean isCs1( GeoChannel channel ) {
        // TODO: detect old CS1 chemistry
        return false;
    }

    private static boolean isOcm( GeoChannel channel ) {
        // TODO: detect on-chip multiplexing
        return StringUtils.containsAny( channel.getExtractProtocol(),
                // https://www.10xgenomics.com/support/universal-three-prime-gene-expression/documentation/steps/library-prep/gem-x-universal-3-prime-gene-expression-v-4-4-plex-reagent-kits
                "1000779", "1000747", "1000215"
        );
    }
}
