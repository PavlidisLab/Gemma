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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.taglibs.standard.lang.support.ExpressionEvaluatorManager;

import ubic.gemma.visualization.ExpressionDataMatrix;
import ubic.gemma.visualization.HtmlMatrixVisualizer;

/**
 * @jsp.tag name="expressionDataMatrix" body-content="empty"
 * @author keshav
 * @version $Id$
 */
public class HtmlMatrixVisualizerTag extends TagSupport {
    private Log log = LogFactory.getLog( this.getClass() );

    private String expressionDataMatrixName = null;
    private ExpressionDataMatrix expressionDataMatrix = null;

    /**
     * @jsp.attribute description="The expressionDataMatrix object" required="true" rtexprvalue="true"
     * @param expressionDataMatrix
     */
    public void setExpressionDataMatrix( String expressionDataMatrixName ) {
        this.expressionDataMatrixName = expressionDataMatrixName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {

        System.err.println( "start tag" );

        expressionDataMatrix = ( ExpressionDataMatrix ) ExpressionEvaluatorManager.evaluate( "expressionDataMatrix",
                expressionDataMatrixName, ExpressionDataMatrix.class, pageContext );

        // StringBuilder buf = new StringBuilder();
        // if ( this.expressionDataMatrix == null ) {
        // buf.append( "No data" );
        // } else {
        // buf.append( "<ol>" );
        // buf.append( "<render PNG>" );
        // HtmlMatrixVisualizer visualizer = new HtmlMatrixVisualizer();
        // visualizer.createVisualization( expressionDataMatrix );
        // visualizer.saveImage( null );
        // buf.append( "</ol>" );
        // }
        //
        // try {
        // pageContext.getOut().print( buf.toString() );
        // } catch ( Exception ex ) {
        // throw new JspException( this.getClass().getName() + ex.getMessage() );
        // }
        return SKIP_BODY;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() {

        System.err.println( "end tag" );

        return EVAL_PAGE;
    }

}
