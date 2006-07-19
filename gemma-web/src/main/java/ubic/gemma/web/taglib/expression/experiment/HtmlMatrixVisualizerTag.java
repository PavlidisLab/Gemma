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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.visualization.ExpressionDataMatrix;
import ubic.gemma.visualization.HtmlMatrixVisualizer;

/**
 * @jsp.tag name="expressionDataMatrix" body-content="empty"
 * @author keshav
 * @version $Id$
 */
public class HtmlMatrixVisualizerTag extends TagSupport {
    private Log log = LogFactory.getLog( this.getClass() );

    // TODO if you decide to add EL support, set this and not the
    // expressionDataMatrix in the setter. A good refresher is
    // here:http://www.phptr.com/articles/article.asp?p=30946&seqNum=9&rl=1. Remember, you were having problems
    // with adding EL support to this before
    // private String expressionDataMatrixName = null;

    private ExpressionDataMatrix expressionDataMatrix = null;

    /**
     * @jsp.attribute description="The expressionDataMatrix object" required="true" rtexprvalue="true"
     * @param expressionDataMatrix
     */
    public void setExpressionDataMatrix( ExpressionDataMatrix expressionDataMatrix ) {
        this.expressionDataMatrix = expressionDataMatrix;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() {

        log.debug( "start tag" );

        Map<String, DesignElementDataVector> m = expressionDataMatrix.getDataMap();

        List<String> designElementNames = new ArrayList( m.keySet() ); // convert set to list to set the labels

        // for ( String key : m.keySet() ) {
        // log.debug( key );
        // DesignElementDataVector vector = m.get(key);
        // }

        HtmlMatrixVisualizer visualizer = new HtmlMatrixVisualizer();
        visualizer.setRowLabels( designElementNames );
        log.debug("pageContext " + this.pageContext);
        visualizer.createVisualization( expressionDataMatrix );

        return SKIP_BODY;
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
