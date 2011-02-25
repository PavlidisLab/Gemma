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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.Constants;
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

    private static final int MAX_GENE_NAME_CELL_LENGTH = 100;

    private static final int MAX_GENE_SYMBOL_CELL_LENGTH = 15;

    private static final double IMAGE_HEADER_EM_HEIGHT = 10.5;

    private static final double MAGIC_EM_SIZE = 0.917;

    private static final String PROBE_VIEW_URL = "/compositeSequence/show.html?id=";

    private static final long serialVersionUID = 6403196597063627020L;

    private Log log = LogFactory.getLog( this.getClass() );

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

            Double[][] m = ( Double[][] ) expressionDataMatrix.getRawMatrix();

            StringBuilder buf = new StringBuilder();

            // TODO read this in as an option.
            String type = "heatmap";

            /* random identifier for ExpressionDataMatrix, stored in session. */
            String matrixId = RandomStringUtils.randomAlphanumeric( 30 ).toUpperCase();

            this.pageContext.getSession().setAttribute( matrixId, expressionDataMatrix );

            if ( expressionDataMatrix == null || m.length == 0 ) {
                buf.append( "No data to display" );
            } else {
                buf.append( "<div class=\"datamatrix\"><table border=\"0\">" );

                buf.append( "<tr >" );

                /* image itself */
                buf.append( "<td cellpadding=\"0\" rowspan=\"2\" align='right' valign=\"bottom\" >" );
                // this is the tricky part: how to keep the image scaled appropriately.
                Double emHeight = expressionDataMatrix.rows() + IMAGE_HEADER_EM_HEIGHT;
                buf.append( "<img style='margin-right:0px;height : " + emHeight.toString()
                        + "em;' src=\"visualizeDataMatrix.html?type=" + type + "&id=" + matrixId + "\" border='0' />" );

                buf.append( "</td>" );

                /* annotation table headings */
                buf.append( "<td colspan='3' valign='bottom'>" );
                buf
                        .append( "<table border='0' cellpadding='0' cellspacing='0' >"
                                + "<tbody style=\"height:"
                                + IMAGE_HEADER_EM_HEIGHT
                                + "em;\" ><tr><th valign='bottom' nowrap='nowrap' width='125' ><span class='annotation'>Probe</span></th>"
                                + "<th valign='bottom' nowrap='nowrap' width='125'><span class='annotation'>Gene</span></th>"
                                + "<th valign='bottom' nowrap='nowrap' width='200'><span class='annotation'>Name</span></th>"
                                + "</tr></tbody></table>" );
                buf.append( "</td>" );
                buf.append( "</tr>" );

                /* annotations */
                buf.append( "<tr>" );

                // plug in design elements into a guaranteed order list (we will need to guarantee order to
                // build the table properly
                List<ExpressionDataMatrixRowElement> rowElements = expressionDataMatrix.getRowElements();

                addProbeColumn( buf, rowElements );
                addGeneSymbolColumn( buf, rowElements );
                addGeneNameColumn( buf, rowElements );

                buf.append( "</tr>" );
                buf.append( "</table>" );

                buf.append( "<a target=\"blank\" href=\"visualizeDataMatrix.html?type=text&id=" + matrixId
                        + "\">View as text</a>" );

                buf.append( "</div>" );
            }

            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            log.error( ex, ex );
            throw new JspException( "ExpressionDataMatrixVisualizerTag: " + ex.getMessage() );
        }
        return SKIP_BODY;
    }

    private void addProbeColumn( StringBuilder buf, List<ExpressionDataMatrixRowElement> rowElements ) {
        openColumnTableData( buf );
        for ( int i = 0; i < rowElements.size(); i++ ) {
            buf.append( "<span " + alternateRowStyle( i, 125 ) + ">" );
            CompositeSequence designElement = rowElements.get( i ).getDesignElement();
            buf.append( "<a href=\"/" + Constants.APP_NAME + PROBE_VIEW_URL + designElement.getId() + "\">"
                    + designElement.getName() + "</a></span><br />\n" );
        }
        buf.append( "</td>" );
    }

    private final static int MAX_GENES_TO_SHOW_PER_ROW = 2;

    /**
     * @param buf
     * @param rowElements
     */
    private void addGeneSymbolColumn( StringBuilder buf, List<ExpressionDataMatrixRowElement> rowElements ) {
        openColumnTableData( buf );
        for ( int i = 0; i < rowElements.size(); i++ ) {
            CompositeSequence compositeSequence = rowElements.get( i ).getDesignElement();
            buf.append( "<span " + alternateRowStyle( i, 125 ) + ">" );
            if ( genes == null || !genes.containsKey( compositeSequence ) ) {
                buf.append( "</span><br />" );
                continue;
            }
            int geneNum = 0;
            int geneStringLength = 0;
            Iterator<Gene> it = genes.get( compositeSequence ).iterator();
            for ( ; it.hasNext(); ) {
                Gene gene = it.next();
                String symbol = gene.getOfficialSymbol();
                if ( !StringUtils.isEmpty( symbol ) ) {
                    String symbolTrimmed = symbol;
                    geneStringLength += symbolTrimmed.length();
                    int abbreviationLength = symbol.length() - ( geneStringLength - MAX_GENE_SYMBOL_CELL_LENGTH );
                    if ( ( geneStringLength > MAX_GENE_SYMBOL_CELL_LENGTH ) && ( abbreviationLength > 3 ) ) {
                        symbolTrimmed = StringUtils.abbreviate( symbol, abbreviationLength );
                    }
                    // if (StringUtils.abbreviate( symbol, MAX_GENE_SYMBOL_LENGTH );

                    buf.append( "<a title=\"" + symbol + "\" href=\"/Gemma/gene/showGene.html?id=" + gene.getId()
                            + "\">" + symbolTrimmed + "</a>" );
                }
                geneNum++;
                if ( it.hasNext() && geneNum == MAX_GENES_TO_SHOW_PER_ROW ) {
                    buf.append( "&nbsp;<a title=\"More genes not shown\">...</a>" );
                    break;
                }
                if ( it.hasNext() && geneStringLength > MAX_GENE_SYMBOL_CELL_LENGTH ) {
                    break;
                }
                if ( it.hasNext() ) {
                    geneStringLength += 3;
                    buf.append( "&nbsp;|&nbsp;" );
                }

            }
            buf.append( "</span><br />" );
        }

        buf.append( "</td>" );
    }

    /**
     * @param buf
     */
    private void openColumnTableData( StringBuilder buf ) {
        buf.append( "<td nowrap='nowrap' style='font-size :" + MAGIC_EM_SIZE
                + "em; line-height:1.0em;' valign='bottom' align=\"left\">" );
    }

    /**
     * @param buf
     * @param rowElements
     */
    private void addGeneNameColumn( StringBuilder buf, List<ExpressionDataMatrixRowElement> rowElements ) {
        openColumnTableData( buf );
        for ( int i = 0; i < rowElements.size(); i++ ) {
            CompositeSequence compositeSequence = rowElements.get( i ).getDesignElement();
            buf.append( "<span " + alternateRowStyle( i, 200 ) + ">" );
            if ( genes == null || !genes.containsKey( compositeSequence ) ) {
                buf.append( "</span><br />" );
                continue;
            }
            List<Gene> geneList = new ArrayList<Gene>();
            int nameLength = 0;
            geneList.addAll( genes.get( compositeSequence ) );
            for ( int geneNum = 0; geneNum < geneList.size(); geneNum++ ) {
                Gene gene = geneList.get( geneNum );
                String name = gene.getOfficialName();
                if ( geneNum == MAX_GENES_TO_SHOW_PER_ROW ) {
                    if ( StringUtils.isNotBlank( name ) ) {
                        buf.append( "&nbsp<a title=\"More genes not shown including " + name + "\">...</a>" );
                    }
                    break;
                }

                if ( StringUtils.isNotBlank( name ) ) {
                    String nameTrimmed = name;
                    nameLength += name.length();
                    int abbreviationLength = name.length() - ( nameLength - MAX_GENE_NAME_CELL_LENGTH );
                    if ( ( nameLength > MAX_GENE_NAME_CELL_LENGTH ) && abbreviationLength > 3 ) {
                        nameTrimmed = StringUtils.abbreviate( name, abbreviationLength );
                    }
                    buf.append( "<a title=\"" + name + " [" + gene.getOfficialSymbol() + "]\">" + nameTrimmed + "</a>" );
                }

                if ( ( geneNum < geneList.size() - 1 ) && nameLength > MAX_GENE_SYMBOL_CELL_LENGTH ) {
                    break;
                }
                if ( StringUtils.isNotBlank( name ) && geneNum < geneList.size() - 1
                        && StringUtils.isNotBlank( geneList.get( geneNum + 1 ).getName() ) ) {
                    buf.append( "&nbsp;|&nbsp;" );
                }
            }
            buf.append( "</span><br />" );
        }
        buf.append( "</td>" );
    }

    private String alternateRowStyle( int i, int width ) {
        if ( i % 2 == 0 ) {
            // FIXME this doesn't work as desired.
            // return "style=\"width:" + width + "px;background-color:#eee;\"";
            return "";
        }
        return "";
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() {
        log.debug( "end tag" );
        return EVAL_PAGE;
    }

}
