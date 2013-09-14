package ubic.gemma.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Used to generate hyperlinks to various things in Gemma.\
 * 
 * @author luke
 * @version $Id$
 */
public class AnchorTagUtil {

    public static String getArrayDesignLink( Long adId, String link ) {
        return getArrayDesignLink( adId, link, null );
    }

    public static String getArrayDesignLink( Long adId, String link, String hover ) {
        return getLink( String.format( "Gemma/arrays/showArrayDesign.html?id=%d", adId ), link, hover );
    }

    public static String getBioMaterialLink( Long bmId, String link ) {
        return getBioMaterialLink( bmId, link, null );
    }

    public static String getBioMaterialLink( Long bmId, String link, String hover ) {
        return getLink( String.format( "/Gemma/bioMaterial/showBioMaterial.html?id=%d", bmId ), link, hover );
    }

    /**
     * @param eeId Id of the experimental design
     * @param link
     * @return
     */
    public static String getExperimentalDesignLink( Long eeId, String link ) {
        return getExperimentalDesignLink( eeId, link, null );
    }

    /**
     * @param eeId Id of the experimental design
     * @param link
     * @param hover
     * @return
     */
    public static String getExperimentalDesignLink( Long edId, String link, String hover ) {
        return getLink( String.format( "/Gemma/experimentalDesign/showExperimentalDesign.html?edid=%d", edId ),
                ( StringUtils.isBlank( link ) ? "Experimental Design" : link ), hover );
    }

    public static String getExpressionExperimentLink( Long eeId, String link ) {
        return getExpressionExperimentLink( eeId, link, null );
    }

    public static String getExpressionExperimentLink( Long eeId, String link, String hover ) {
        return getLink( getExpressionExperimentUrl( eeId ), link, hover );
    }

    public static String getExpressionExperimentUrl( long eeId ) {
        return String.format( "/Gemma/expressionExperiment/showExpressionExperiment.html?id=%d", eeId );
    }

    public static String getLink( String url, String link, String hover ) {
        StringBuffer buf = new StringBuffer();
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

    public static String getProbeLink( Long probeId, String link ) {
        // FIXME this isn't the greatest page - it's kind of old-style.
        return getLink( String.format( "/Gemma/compositeSequence/show.html?id=%d", probeId ), link, "" );
    }
}
