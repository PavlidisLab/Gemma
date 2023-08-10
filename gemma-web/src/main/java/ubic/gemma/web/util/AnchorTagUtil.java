package ubic.gemma.web.util;

import org.compass.core.util.Assert;
import org.hibernate.Hibernate;
import ubic.gemma.model.common.Describable;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.servlet.ServletContext;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * Used to generate hyperlinks to various things in Gemma.
 *
 * @author luke
 */
public class AnchorTagUtil {

    public static String getBioMaterialLink( BioMaterial bm, String link, ServletContext servletContext ) {
        Assert.notNull( bm.getId() );
        return getLink( String.format( servletContext.getContextPath() + "/bioMaterial/showBioMaterial.html?id=%d", bm.getId() ),
                linkForDescribable( bm, link, "Sample" ) );
    }

    public static String getExperimentalDesignLink( ExperimentalDesign ed, String link, ServletContext servletContext ) {
        Assert.notNull( ed.getId() );
        return getLink( String.format( servletContext.getContextPath() + "/experimentalDesign/showExperimentalDesign.html?edid=%d", ed.getId() ),
                linkForDescribable( ed, link, "Experimental Design" ) );
    }

    public static String getExpressionExperimentLink( ExpressionExperiment ee, String link, ServletContext servletContext ) {
        Assert.notNull( ee.getId() );
        return getLink( AnchorTagUtil.getExpressionExperimentUrl( ee, servletContext ),
                linkForDescribable( ee, defaultIfBlank( link, Hibernate.isInitialized( ee ) ? ee.getShortName() : null ), "Dataset" ) );
    }

    public static String getExpressionExperimentUrl( ExpressionExperiment ee, ServletContext servletContext ) {
        Assert.notNull( ee.getId() );
        return String.format( servletContext.getContextPath() + "/expressionExperiment/showExpressionExperiment.html?id=%d", ee.getId() );
    }

    private static String linkForDescribable( Describable d, String link, String entityName ) {
        return defaultIfBlank( link, defaultIfBlank( Hibernate.isInitialized( d ) ? d.getName() : null, entityName + " #" + d.getId() ) );
    }

    private static String getLink( String url, String link ) {
        return String.format( "<a href=\"%s\">%s</a>", url, escapeHtml4( link ) );
    }
}
