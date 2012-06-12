/**
 * 
 */
package ubic.gemma.web.taglib.common.description;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import ubic.basecode.dataStructure.graph.DirectedGraph;

/**
 * Tag to display a hierarchy of GO terms.
 * 
 * @author Paul
 * @version $Id$
 */
@Deprecated
public class GeneOntologyTag extends TagSupport {

    DirectedGraph goGraph;

    /**
     * 
     */
    private static final long serialVersionUID = 591455901815264608L;

    @Override
    public int doEndTag() {

        return EVAL_PAGE;
    }

    /**
     * @param goGraph
     */
    public void setGoGraph( DirectedGraph goGraph ) {
        this.goGraph = goGraph;
    }

    @Override
    public int doStartTag() throws JspException {

        StringBuilder buf = new StringBuilder();

        if ( this.goGraph == null || this.goGraph.getItems().keySet().size() == 0 ) {
            buf.append( "No GO terms to display" );
        } else {
            buf.append( "<pre>" );
            buf.append( goGraph.toString() );
            buf.append( "<pre>" );
        }

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( this.getClass().getName() + ex.getMessage() );
        }
        return SKIP_BODY;

    }

}
