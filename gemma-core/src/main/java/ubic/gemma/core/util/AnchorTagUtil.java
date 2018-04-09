package ubic.gemma.core.util;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.persistence.util.Settings;

/**
 * Used to generate hyperlinks to various things in Gemma.
 *
 * @author luke
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Possibly used in front end
public class AnchorTagUtil {

    public static String getArrayDesignLink( Long adId, String link ) {
        return AnchorTagUtil.getArrayDesignLink( adId, link, null );
    }

    public static String getArrayDesignLink( Long adId, String link, String hover ) {
        return AnchorTagUtil
                .getLink( String.format( Settings.getRootContext() + "/arrays/showArrayDesign.html?id=%d", adId ), link,
                        hover );
    }

    public static String getBioMaterialLink( Long bmId, String link ) {
        return AnchorTagUtil.getBioMaterialLink( bmId, link, null );
    }

    public static String getBioMaterialLink( Long bmId, String link, String hover ) {
        return AnchorTagUtil
                .getLink( String.format( Settings.getRootContext() + "/bioMaterial/showBioMaterial.html?id=%d", bmId ),
                        link, hover );
    }

    /**
     * @param eeId Id of the experimental design
     * @param link the link
     * @return experimental design link html
     */
    public static String getExperimentalDesignLink( Long eeId, String link ) {
        return AnchorTagUtil.getExperimentalDesignLink( eeId, link, null );
    }

    /**
     * @param edId  Id of the experimental design
     * @param link  the link
     * @param hover hover tooltip text
     * @return experimental design link html
     */
    public static String getExperimentalDesignLink( Long edId, String link, String hover ) {
        return AnchorTagUtil.getLink(
                String.format( Settings.getRootContext() + "/experimentalDesign/showExperimentalDesign.html?edid=%d",
                        edId ), ( StringUtils.isBlank( link ) ? "Experimental Design" : link ), hover );
    }

    public static String getExpressionExperimentLink( Long eeId, String link ) {
        return AnchorTagUtil.getExpressionExperimentLink( eeId, link, null );
    }

    public static String getExpressionExperimentLink( Long eeId, String link, String hover ) {
        return AnchorTagUtil.getLink( AnchorTagUtil.getExpressionExperimentUrl( eeId ), link, hover );
    }

    public static String getExpressionExperimentUrl( long eeId ) {
        return String.format( Settings.getRootContext() + "/expressionExperiment/showExpressionExperiment.html?id=%d",
                eeId );
    }

    public static String getLink( String url, String link, String hover ) {
        StringBuilder buf = new StringBuilder();
        buf.append( "<a href=\"" );
        buf.append( url );
        buf.append( "\"" );
        if ( hover != null ) {
            buf.append( " class=\"tooltip\"" );
        }
        buf.append( ">" );
        buf.append( link );
        if ( hover != null ) {
            buf.append( "<span class=\"tooltiptext\">" );
            buf.append( hover );
            buf.append( "</span>" );
        }
        buf.append( "</a>" );
        return buf.toString();
    }

}
