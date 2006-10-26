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

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.gui.ColorMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDesignElementDataVectorMatrix;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.visualization.HttpExpressionDataMatrixVisualizer;

/**
 * @jsp.tag name="httpExpressionDataMatrixVisualizer" body-content="empty"
 * @author keshav
 * @version $Id$
 */
public class ExpressionDataMatrixVisualizerTag extends TagSupport {

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

    private HttpExpressionDataMatrixVisualizer httpExpressionDataMatrixVisualizer = null;

    /**
     * @jsp.attribute description="The visualizer object" required="true" rtexprvalue="true"
     * @param httpExpressionDataMatrixVisualizer
     */
    public void setHttpExpressionDataMatrixVisualizer(
            HttpExpressionDataMatrixVisualizer httpExpressionDataMatrixVisualizer ) {
        this.httpExpressionDataMatrixVisualizer = httpExpressionDataMatrixVisualizer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {

        log.debug( "start tag" );

        try {
            // TODO use ExpressionDataDoubleMatrix
            /* get metadata from MatrixVisualizer */
            ExpressionDataDesignElementDataVectorMatrix expressionDataMatrix = httpExpressionDataMatrixVisualizer
                    .getExpressionDataMatrix();
            String imageFile = httpExpressionDataMatrixVisualizer.getImageFile();

            Map<String, DesignElementDataVector> m = expressionDataMatrix.getDataMap();

            ColorMatrix colorMatrix = httpExpressionDataMatrixVisualizer.createColorMatrix( expressionDataMatrix );

            List<String> designElementNames = httpExpressionDataMatrixVisualizer.getRowLabels();

            StringBuilder buf = new StringBuilder();

            // TODO read these in
            String type = "matrix";
            HttpSession session = this.pageContext.getSession();
            session.setAttribute( "type", type );
            session.setAttribute( "httpExpressionDataMatrixVisualizer", httpExpressionDataMatrixVisualizer );

            // TODO remove this don't suppress visualizations
            // if ( httpExpressionDataMatrixVisualizer.isSuppressVisualizations() ) {
            // buf.append( "Visualizations suppressed." );
            // }
            if ( expressionDataMatrix == null || m.size() == 0 ) {
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
                buf.append( "<img src=\"visualizeDataMatrix.html?type=" + type + "\"border=1 width=100 height=100/>" );
                buf.append( "</td>" );
                buf.append( "<td align=\"left\">" );
                for ( String name : designElementNames ) {
                    buf.append( "<font size=\"-1\">" );
                    buf.append( "<a href=\"http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=search&term="
                            + name + "\">" + name + "</a>" );
                    buf.append( "</font>" );
                    buf.append( "<br/>" );
                }
                buf.append( "</td>" );

                buf.append( "</tr>" );
                buf.append( "</table>" );
            }

            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "ExpressionDataMatrixVisualizationTag: " + ex.getMessage() );
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
