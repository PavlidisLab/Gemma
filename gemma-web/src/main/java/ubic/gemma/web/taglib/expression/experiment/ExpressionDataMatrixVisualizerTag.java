/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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

import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.designElement.CompositeSequenceServiceImpl;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.visualization.ExpressionDataMatrixVisualizer;

/**
 * @jsp.tag name="expressionDataMatrixVisualizer" body-content="empty"
 * @author keshav
 * @version $Id$
 */
public class ExpressionDataMatrixVisualizerTag extends TagSupport {

    private static final long serialVersionUID = 6403196597063627020L;

    private Log log = LogFactory.getLog( this.getClass() );

    private CompositeSequenceService compositeSequenceService = null;

    // TODO To add EL support, set this and not the
    // expressionDataMatrixVisualization in the setter. A good refresher is
    // here:http://www.phptr.com/articles/article.asp?p=30946&seqNum=9&rl=1.
    // private String expressionDataMatrixVisualizationName = null;

    private ExpressionDataMatrixVisualizer expressionDataMatrixVisualizer = null;

    /**
     * @jsp.attribute description="The visualizer object" required="true" rtexprvalue="true"
     * @param expressionDataMatrixVisualizer
     */
    public void setExpressionDataMatrixVisualizer( ExpressionDataMatrixVisualizer expressionDataMatrixVisualizer ) {
        this.expressionDataMatrixVisualizer = expressionDataMatrixVisualizer;
        this.compositeSequenceService = new CompositeSequenceServiceImpl();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    @SuppressWarnings("unchecked")
    public int doStartTag() throws JspException {

        log.debug( "start tag" );

        try {
            /* get metadata from ExpressionDataMatrixVisualizer */
            ExpressionDataMatrix expressionDataMatrix = expressionDataMatrixVisualizer.getExpressionDataMatrix();

            Double[][] m = ( Double[][] ) expressionDataMatrix.getMatrix();

            Collection<DesignElement> compositeSequences = expressionDataMatrix.getRowElements();

            StringBuilder buf = new StringBuilder();

            // TODO read these in
            String type = "matrix";
            HttpSession session = this.pageContext.getSession();
            session.setAttribute( "type", type );
            session.setAttribute( "expressionDataMatrixVisualizer", expressionDataMatrixVisualizer );

            if ( expressionDataMatrix == null || m.length == 0 ) {
                buf.append( "No data to display" );
            } else {

                log.debug( "wrapping with html" );

                buf.append( "<table border=\"0\">" );
                buf.append( "<tr>" );
                buf.append( "<td>&nbsp;</td>" );
                buf.append( "<td align=\"left\">Probe Set&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Gene<br/><br/></td>" );
                buf.append( "</tr>" );

                buf.append( "<tr>" );
                buf.append( "<td border=\"0\" rowspan=\"5\">" );
                // buf.append( "<img src=\"visualizeDataMatrix.html?type=" + type + "\"border=1 width=300
                // height=300/>");
                buf.append( "<img src=\"visualizeDataMatrix.html?type=" + type + "\"border=1/>" );
                buf.append( "</td>" );
                buf.append( "<td align=\"left\">" );
                for ( DesignElement cs : compositeSequences ) {
                    assert cs instanceof CompositeSequence;
                    buf.append( "<font size=\"-1\">" );
                    buf.append( cs.getName() );
                    buf.append( "</font>" );

                    Collection associatedGenes = compositeSequenceService.getAssociatedGenes( ( CompositeSequence ) cs );
                    Iterator iter = associatedGenes.iterator();
                    // FIXME only adding the first gene
                    if ( iter.hasNext() ) {
                        Gene gene = ( Gene ) iter.next();
                        String name = gene.getName();
                        if ( !StringUtils.isEmpty( name ) ) {
                            buf.append( "&nbsp;&nbsp;&nbsp;" );
                            buf
                                    .append( "<a href=\"http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=search&term="
                                            + name + "\">" + name + "</a>" );
                        }
                    }

                    buf.append( "<br>" );
                }
                buf.append( "</td>" );

                buf.append( "</tr>" );
                buf.append( "</table>" );
            }

            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "ExpressionDataMatrixVisualizerTag: " + ex.getMessage() );
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
