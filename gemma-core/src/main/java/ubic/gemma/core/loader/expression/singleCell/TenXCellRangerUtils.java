package ubic.gemma.core.loader.expression.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import no.uib.cipr.matrix.io.MatrixVectorReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

/**
 * Utilities for detecting 10x Cell Ranger data in GEO series and sample metadata.
 *
 * @author poirigui
 */
@CommonsLog
public class TenXCellRangerUtils {

    /**
     * Detect if a description (i.e. a GEO data processing section) indicates that the data originates from a 10x Cell
     * Ranger platform.
     */
    public static boolean detect10x( String description ) {
        return Strings.CI.containsAny( description, "cellranger", "cell ranger" );
    }

    /**
     * Detect if a description (i.e. a GEO data processing section) indicates that the data is filtered.
     */
    public static boolean detect10xFiltered( String description ) {
        return Strings.CI.containsAny( description, "filtered_feature_bc_matrix" );
    }

    /**
     * Detect if a description (i.e. a GEO data processing section) indicates that the data is unfiltered.
     */
    public static boolean detect10xUnfiltered( String description ) {
        return Strings.CI.containsAny( description, "raw", "unfiltered", "raw_feature_bc_matrix" );
    }

    public static boolean detect10xFromMexFile( Path mexFile ) {
        String[] comments;
        try ( MatrixVectorReader reader = new MatrixVectorReader( new InputStreamReader( new GZIPInputStream( Files.newInputStream( mexFile ) ) ) ) ) {
            reader.readMatrixInfo();
            comments = reader.readComments();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        // The comment is in the following form:
        // %metadata_json: {"software_version": "Cell Ranger cellranger-7.1.0", "format_version": 2}
        // For older Cell Ranger releases
        // %%MatrixMarket matrix coordinate integer general
        // %metadata_json: {"format_version": 2, "software_version": "3.1.0"}
        for ( String comment : comments ) {
            if ( comment.startsWith( "metadata_json: " ) ) {
                JSONObject parsedMetadata;
                try {
                    parsedMetadata = new JSONObject( comment.substring( "metadata_json: ".length() ) );
                } catch ( JSONException e ) {
                    log.warn( "Failed to parse %metadata_json: comment from " + mexFile + ".", e );
                    continue;
                }
                if ( parsedMetadata.has( "software_version" ) ) {
                    String softwareVersion = parsedMetadata.getString( "software_version" );
                    return // Cell Ranger 7, 8, 9 (and hopefully 10 too)
                            softwareVersion.startsWith( "Cell Ranger cellranger-" )
                                    // Cell Ranger 4, 5, 6
                                    || softwareVersion.startsWith( "cellranger-" )
                                    // Cell Ranger 4 (this one is odd because cellranger-4.0.0 also occurs in the wild)
                                    || softwareVersion.equals( "Cell Ranger 4" )
                                    // Cell Ranger 3
                                    || softwareVersion.equals( "3.1.0" )
                                    || softwareVersion.equals( "3.0.0" ) || softwareVersion.equals( "3.0.1" ) || softwareVersion.equals( "3.0.2" );
                }
            }
        }
        return false;
    }

    /**
     * We essentially rely on the presence of specific production number of reagent kits in the extraction protocol
     * section.
     * <p>
     * TODO: this will need more work and tests
     */
    @Nullable
    public static String detect10xChemistry( String description ) {
        if ( ( containsNormalizeSpaceAndSingleQuoteCaseInsensitive( description, "10x" ) && containsNormalizeSpaceAndSingleQuoteCaseInsensitive( description, "3'" ) && containsNormalizeSpaceAndSingleQuoteCaseInsensitive( description, "v3" ) )
                || Strings.CS.containsAny( description,
                // https://www.10xgenomics.com/support/universal-three-prime-gene-expression/documentation/steps/library-prep/chromium-next-gem-automated-single-cell-3-reagent-kits-user-guide-v-3-1-chemistry
                "1000141", "1000147", "1000136", "1000146", "1000213", "1000215",
                // https://www.10xgenomics.com/support/universal-three-prime-gene-expression/documentation/steps/library-prep/chromium-next-gem-single-cell-3-ht-reagent-kits-v-3-1-dual-index
                "1000370", "1000371", "1000215",
                // https://www.10xgenomics.com/support/universal-three-prime-gene-expression/documentation/steps/library-prep/chromium-single-cell-3-reagent-kits-user-guide-v-3-1-chemistry-dual-index
                "1000268", "1000269", "1000120", "1000127", "1000215"

        ) ) {
            return "SC3Pv3" + ( isHighThroughput( description ) ? "HT" : "" ) + ( isCs1( description ) ? "-CS1" : "-polyA" ) + ( isOcm( description ) ? "-OCM" : "" );
        }
        if ( ( containsNormalizeSpaceAndSingleQuoteCaseInsensitive( description, "10x" ) && containsNormalizeSpaceAndSingleQuoteCaseInsensitive( description, "3'" ) && containsNormalizeSpaceAndSingleQuoteCaseInsensitive( description, "v4" ) )
                || Strings.CS.containsAny( description,
                // https://www.10xgenomics.com/support/universal-three-prime-gene-expression/documentation/steps/library-prep/gem-x-universal-3-prime-gene-expression-v-4-4-plex-reagent-kits
                "1000779", "1000747", "1000215",
                // https://www.10xgenomics.com/support/universal-three-prime-gene-expression/documentation/steps/library-prep/chromium-gem-x-single-cell-3-v4-gene-expression-user-guide
                "1000686", "1000690", "1000215" ) ) {
            return "SC3Pv4" + ( isCs1( description ) ? "-CS1" : "-polyA" ) + ( isOcm( description ) ? "-OCM" : "" );
        }
        return null;
    }

    private static boolean containsNormalizeSpaceAndSingleQuoteCaseInsensitive( String s, String t ) {
        return Strings.CI.contains( StringUtils.normalizeSpace( s.replaceAll( "[ʼ’′]", "'" ) ), t );
    }

    private static boolean isHighThroughput( String description ) {
        return Strings.CS.containsAny( description,
                // https://www.10xgenomics.com/support/universal-three-prime-gene-expression/documentation/steps/library-prep/chromium-next-gem-single-cell-3-ht-reagent-kits-v-3-1-dual-index
                "1000370", "1000371", "1000215"
        );
    }

    private static boolean isCs1( String description ) {
        // TODO: detect old CS1 chemistry
        return false;
    }

    private static boolean isOcm( String description ) {
        // TODO: detect on-chip multiplexing
        return Strings.CS.containsAny( description,
                // https://www.10xgenomics.com/support/universal-three-prime-gene-expression/documentation/steps/library-prep/gem-x-universal-3-prime-gene-expression-v-4-4-plex-reagent-kits
                "1000779", "1000747", "1000215"
        );
    }
}
