/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

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
    private ExpressionExperiment expressionExperiment;

    /**
     * @param design
     * @jsp.attribute required="true" rtexprvalue="true"
     */
    public void setExperimentalDesign( ExperimentalDesign experimentalDesign ) {
        this.experimentalDesign = experimentalDesign;
    }

    /**
     * @param design
     * @jsp.attribute required="false" rtexprvalue="true"
     */
    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
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
        if ( StringUtils.isNotBlank( name ) ) buf.append( "<tr><td>" + name + "</td></tr>" );
        if ( StringUtils.isNotBlank( description ) )
            buf.append( "<tr><td>Description:</td><td>" + description + "</td></tr>" );
        buf.append( "<tr><td>Factors:</td><td>" + experimentalFactors.size() + "</td></tr>" );

        if ( experimentalFactors.size() > 0 ) {
            /*
             * See eeDataFetch.js for this call.
             */
            buf
                    .append( "<tr><td>Design File:</td><td><a href=\"#\" onClick=\"fetchData(false,"
                            + +expressionExperiment.getId()
                            + ",\'text\', null,"
                            + experimentalDesign.getId()
                            + ")"
                            + "\"> Download </a>"
                            + "<a class=\"helpLink\" href=\"?\" onclick=\"showHelpTip(event, \'Tab-delimited design file for this experiment, if available.\'); return false\"><img src=\"/Gemma/images/help.png\"/> </a></td>"
                            + "</tr>" );
        }

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
