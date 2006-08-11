/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.web.taglib.expression.experiment;

import java.util.List;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.visualization.ExpressionDataMatrix;
import ubic.gemma.visualization.ExpressionDataMatrixVisualization;

/**
 * @jsp.tag name="expressionDataMatrixVisualization" body-content="empty"
 * @author keshav
 * @version $Id$
 */
public class HtmlMatrixVisualizerTag extends TagSupport {

    private static final long serialVersionUID = 6403196597063627020L;

    private Log log = LogFactory.getLog( this.getClass() );

    /* supported only by internet explorer */
    private final String IE_IMG_PATH_SEPARATOR = "\\";

    /* supported by all browsers */
    private final String ALL_IMG_PATH_SEPARATOR = "/";

    // TODO To add EL support, set this and not the
    // expressionDataMatrixVisualization in the setter. A good refresher is
    // here:http://www.phptr.com/articles/article.asp?p=30946&seqNum=9&rl=1.
    // private String expressionDataMatrixVisualizationName = null;

    private ExpressionDataMatrixVisualization expressionDataMatrixVisualization = null;

    /**
     * @jsp.attribute description="The expressionDataMatrixVisualization object" required="true" rtexprvalue="true"
     * @param expressionDataMatrixVisualization
     */
    public void setExpressionDataMatrixVisualization(
            ExpressionDataMatrixVisualization expressionDataMatrixVisualization ) {
        this.expressionDataMatrixVisualization = expressionDataMatrixVisualization;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {

        log.debug( "start tag" );

        /* get metadata from ExpressionDataMatrixVisualization */
        ExpressionDataMatrix expressionDataMatrix = expressionDataMatrixVisualization.getExpressionDataMatrix();
        String outfile = expressionDataMatrixVisualization.getOutfile();

        Map<String, DesignElementDataVector> m = expressionDataMatrix.getDataMap();

        expressionDataMatrixVisualization.createVisualization();

        List<String> designElementNames = expressionDataMatrixVisualization.getRowLabels();

        expressionDataMatrixVisualization.saveImage( outfile );/* remove me when using dynamic images */

        /* Cannot use \ in non internet explorer browsers. Using / instead. */
        outfile = StringUtils.replace( outfile, IE_IMG_PATH_SEPARATOR, ALL_IMG_PATH_SEPARATOR );

        outfile = StringUtils.replace( outfile, StringUtils.splitByWholeSeparator( outfile, "visualization" )[0],
                "http://localhost:8080/" );// FIXME get the protocol, server, and port from request

        log.debug( "setting compatibility for non-IE browsers " + outfile );

        StringBuilder buf = new StringBuilder();

        if ( expressionDataMatrixVisualization.isSuppressVisualizations() ) {
            buf.append( "Visualizations suppressed." );
        } else if ( expressionDataMatrix == null || m.size() == 0 ) {
            buf.append( "No data to display" );
        } else {

            log.debug( "wrapping with html" );

            buf.append( "<table border=\"0\">" );
            buf.append( "<tr>" );
            buf.append( "<td>&nbsp;</td>" );
            buf.append( "<td align=\"left\">Probe Set<br/><br/></td>" );
            buf.append( "</tr>" );

            buf.append( "<tr>" );
            buf.append( "<td border=\"0\" rowspan=\"5\">" );
            buf.append( "<img src=\"" + outfile + "\">" );
            buf.append( "</td>" );
            buf.append( "<td align=\"left\">" );
            for ( String name : designElementNames ) {
                buf.append( "<font size=\"-1\">" );
                buf.append( "<a href=\"http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=search&term=" + name
                        + "\">" + name + "</a>" );
                buf.append( "</font>" );
                buf.append( "<br/>" );
            }
            buf.append( "</td>" );

            buf.append( "</tr>" );
            buf.append( "</table>" );
        }

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "HtmlMatrixVisualizationTag: " + ex.getMessage() );
        }

        log.debug( "return SKIP_BODY" );
        return SKIP_BODY;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() throws JspException {

        log.debug( "end tag" );

        return EVAL_PAGE;
    }

}
