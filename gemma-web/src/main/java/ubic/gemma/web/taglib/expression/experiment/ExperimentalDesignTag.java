/**
 * 
 */
package ubic.gemma.web.taglib.expression.experiment;

import java.util.Collection;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

/**
 * Used to display the experimetnal design information for a EE.
 * 
 * @jsp.tag name="eeDesign" body-content="empty"
 * @author pavlidis
 * @version $Id$
 */
public class ExperimentalDesignTag extends TagSupport {

    private static Log log = LogFactory.getLog( ExperimentalDesignTag.class.getName() );

    /**
     * 
     */
    private static final long serialVersionUID = 1478714878857705718L;
    private ExperimentalDesign experimentalDesign;

    /**
     * @param design
     * @jsp.attribute required="true" rtexprvalue="true"
     */
    public void setExperimentalDesign( ExperimentalDesign experimentalDesign ) {
        this.experimentalDesign = experimentalDesign;
    }

    @Override
    public int doEndTag() {
        return EVAL_PAGE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public int doStartTag() throws JspException {
        StringBuilder buf = new StringBuilder();

        Collection<ExperimentalFactor> experimentalFactors = experimentalDesign.getExperimentalFactors();

        String name = experimentalDesign.getName();
        String description = experimentalDesign.getDescription();
        buf.append( "<table>" );
        if ( StringUtils.isNotBlank( name ) ) buf.append( "<tr><td>Name</td><td>" + name + "</td></tr>" );
        buf.append( "<tr><td>Description</td><td>" + description + "</td></tr>" );
        buf.append( "<tr><td>Factors</td><td>" + experimentalFactors.size() + "</td></tr>" );
        buf.append( "<tr><td>Descriptors</td><td>" + experimentalDesign.getTypes().size() + "</td></tr>" );
        buf.append( "</table>" );

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            log.error( ex, ex );
            throw new JspException( "experimental design view tag: " + ex.getMessage() );
        }
        return SKIP_BODY;
    }
}
