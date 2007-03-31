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
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;

/**
 * @jsp.tag name="expressionDataMatrix" body-content="empty"
 * @author keshav
 * @version $Id$
 */
public class ExpressionDataMatrixVisualizerTag extends TagSupport {

    private static final long serialVersionUID = 6403196597063627020L;

    private Log log = LogFactory.getLog( this.getClass() );

    private double EMSIZE = .825;
    // TODO To add EL support, set this and not the
    // expressionDataMatrixVisualization in the setter. A good refresher is
    // here:http://www.phptr.com/articles/article.asp?p=30946&seqNum=9&rl=1.
    // private String expressionDataMatrixVisualizationName = null;

    private ExpressionDataMatrix expressionDataMatrix = null;

    private Map<CompositeSequence, Collection<Gene>> genes;

    /**
     * @jsp.attribute description="The object to visualize." required="true" rtexprvalue="true"
     * @param expressionDataMatrix
     */
    public void setExpressionDataMatrix( ExpressionDataMatrix expressionDataMatrix ) {
        this.expressionDataMatrix = expressionDataMatrix;
    }

    /**
     * @jsp.attribute description="Gene data (map of composite sequences to genes) for the matrix." required="false"
     *                rtexprvalue="true"
     * @param genes
     */
    public void setGenes( Map<CompositeSequence, Collection<Gene>> genes ) {
        this.genes = genes;
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

            Double[][] m = ( Double[][] ) expressionDataMatrix.getMatrix();

            // Collection<DesignElement> compositeSequences = expressionDataMatrix.getRowElements();

            StringBuilder buf = new StringBuilder();

            // TODO read this in
            String type = "heatmap";

            /* random identifier for ExpressionDataMatrix */
            String id = "id_" + Math.abs( RandomUtils.nextInt() );
            this.pageContext.getSession().setAttribute( id, expressionDataMatrix );

            if ( expressionDataMatrix == null || m.length == 0 ) {
                buf.append( "No data to display" );
            } else {
                buf.append( "<table border=\"0\">" );
                // buf.append( "<tr>" );
                // buf.append( "<td>&nbsp;</td>" );
                // buf.append( "<td align=\"left\" valign=\"bottom\" >Probe
                // Set&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Gene<br/><br/></td>" );
                // buf.append( "</tr>" );

                buf.append( "<tr>" );
                buf.append( "<td border=\"0\" rowspan=\"5\" align='right'>" );

                Double emHeight = EMSIZE * expressionDataMatrix.rows() + 8.5;
                buf.append( "<img style='height : " + emHeight.toString() + "em;' src=\"visualizeDataMatrix.html?type="
                        + type + "&id=" + id + "\"border=1/>" );

                buf.append( "</td>" );

                buf.append( "<td colspan='2' valign='bottom'>" );

                buf
                        .append( "<table border='0' cellpadding='0' cellspacing='0'><tbody><tr><th nowrap='nowrap' width='125' ><span class='annotation'>Probe</span></th><th nowrap='nowrap' width='125'><span class='annotation'>Gene</span></th></tr></tbody></table>" );
                buf.append( "</td>" );
                buf.append( "</tr>" );
                buf.append( "<tr>" );

                // plug in design elements into a guaranteed order list (we will need to guarantee order to
                // build the table properly
                List<ExpressionDataMatrixRowElement> rowElements = expressionDataMatrix.getRowElements();

                // print out the composite sequence name
                buf.append( "<td style='font-size : .825em; line-height:1.0em;' valign='bottom' align=\"left\">" );
                for ( int i = 0; i < rowElements.size(); i++ ) {
                    buf.append( rowElements.get( i ) + "<br />\n" );
                }
                buf.append( "</td>" );
                // print out the gene associated with the cs
                buf.append( "<td style='font-size : .825em; line-height:1.0em;' valign='bottom' align=\"left\">" );
                for ( int i = 0; i < rowElements.size(); i++ ) {
                    CompositeSequence compositeSequence = ( CompositeSequence ) rowElements.get( i ).getDesignElement();

                    if ( genes != null && genes.containsKey( compositeSequence ) ) {
                        Collection associatedGenes = genes.get( compositeSequence );
                        Iterator iter = associatedGenes.iterator();
                        // TODO only adding the first gene ... add others as well? YES.
                        if ( iter.hasNext() ) {
                            Gene gene = ( Gene ) iter.next();
                            String symbol = gene.getOfficialSymbol();
                            if ( !StringUtils.isEmpty( symbol ) ) {
                                buf
                                        .append( "<a href=\"http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=search&term="
                                                + symbol + "\">" + symbol + "</a>" ); // FIXME add Gemma link.
                            }
                            String name = gene.getOfficialName();
                            if ( StringUtils.isNotBlank( name ) ) {
                                buf.append( "&nbsp;&nbsp;&nbsp;" + name );
                            }
                        }
                    }
                    buf.append( "<br />" );
                    // buf.append( designElements.get( i ).getName() + "<br />\n");
                }
                buf.append( "</td>" );

                buf.append( "</tr>" );
                buf.append( "</table>" );
            }

            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            log.error( ex, ex );
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
