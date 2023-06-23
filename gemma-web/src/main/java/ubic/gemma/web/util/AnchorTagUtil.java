package ubic.gemma.web.util;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletContext;

/**
 * Used to generate hyperlinks to various things in Gemma.
 *
 * @author luke
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Possibly used in front end
public class AnchorTagUtil {

    public static String getArrayDesignLink( Long adId, String link, ServletContext servletContext ) {
        return AnchorTagUtil.getArrayDesignLink( adId, link, servletContext );
    }

    public static String getArrayDesignLink( Long adId, String link, String hover, ServletContext servletContext ) {
        return AnchorTagUtil
                .getLink( String.format( servletContext.getContextPath() + "/arrays/showArrayDesign.html?id=%d", adId ), link,
                        hover );
    }

    public static String getBioMaterialLink( Long bmId, String link, ServletContext servletContext ) {
        return AnchorTagUtil.getBioMaterialLink( bmId, link, null, servletContext );
    }

    public static String getBioMaterialLink( Long bmId, String link, String hover, ServletContext servletContext ) {
        return AnchorTagUtil
                .getLink( String.format( servletContext.getContextPath() + "/bioMaterial/showBioMaterial.html?id=%d", bmId ),
                        link, hover );
    }

    /**
     * @param eeId Id of the experimental design
     * @param link the link
     * @return experimental design link html
     */
    public static String getExperimentalDesignLink( Long eeId, String link, ServletContext servletContext ) {
        return AnchorTagUtil.getExperimentalDesignLink( eeId, link, servletContext );
    }

    /**
     * @param edId  Id of the experimental design
     * @param link  the link
     * @param hover hover tooltip text
     * @return experimental design link html
     */
    public static String getExperimentalDesignLink( Long edId, String link, String hover, ServletContext servletContext ) {
        return AnchorTagUtil.getLink(
                String.format( servletContext.getContextPath() + "/experimentalDesign/showExperimentalDesign.html?edid=%d",
                        edId ), ( StringUtils.isBlank( link ) ? "Experimental Design" : link ), hover );
    }

    public static String getExpressionExperimentLink( Long eeId, String link, ServletContext servletContext ) {
        return AnchorTagUtil.getExpressionExperimentLink( eeId, link, servletContext );
    }

    public static String getExpressionExperimentLink( Long eeId, String link, String hover, ServletContext servletContext ) {
        return AnchorTagUtil.getLink( AnchorTagUtil.getExpressionExperimentUrl( eeId, servletContext ), link, hover );
    }

    public static String getExpressionExperimentUrl( long eeId, ServletContext servletContext ) {
        return String.format( servletContext.getContextPath() + "/expressionExperiment/showExpressionExperiment.html?id=%d",
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
