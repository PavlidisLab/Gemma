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
package ubic.gemma.web.taglib.displaytag.expression.experiment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

/**
 * Used to generate hyperlinks in displaytag tables.
 * <p>
 * See http://displaytag.sourceforge.net/10/tut_decorators.html and http://displaytag.sourceforge.net/10/tut_links.html
 * for explanation of how this works.
 *
 * @author pavlidis
 * @version $Id$
 *  
 */
public class ExpressionExperimentWrapper extends TableDecorator {


    Log log = LogFactory.getLog( this.getClass() );

    /**
     * @return String
     */
    public String getDataSource() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        if ( object != null && object.getAccession() != null ) {
            return object.getExternalDatabase();
        }
        return "No Source";
    }

    /**
     * @return String
     */
    public String getExternalDetailsLink() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        if ( object != null && object.getAccession() != null ) {
            return "<a href=\"" + object.getExternalUri() + "\">" + object.getAccession()
            + "</a>";             
        }
        return "No accession";
    }

    /**
     * Return detail string for an expression experiment
     * 
     * @return String
     */
    public String getDetails() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        if ( object != null && object.getAccession() != null ) {
           
            return object.getExternalDatabase() + " - " + object.getAccession();
        }
        return "No accession";
    }

    /**
     * @return String
     */
    public String getAssaysLink() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        long count = object.getBioAssayCount();
        
        return  count + "<small><a href=\"showBioAssaysFromExpressionExperiment.html?id=" + object.getId() + "\">"
        +  "(list)</a>";        
    }

    /**
     * @return String
     */
    public String getFactorsLink() {

        log.debug( getCurrentRowObject() );

        ExperimentalDesign object = ( ExperimentalDesign ) getCurrentRowObject();

        if ( object != null && object.getExperimentalFactors() != null ) {
            return "<a href=\"/Gemma/experimentalDesign/showExperimentalDesign.html?id=" + object.getId() + "\">"
                    + object.getExperimentalFactors().size() + "</a>";
        }
        return "No experimental factors";
    }

    /**
     * @return String
     */
    public String getExperimentalDesignNameLink() {

        log.debug( getCurrentRowObject() );

        ExperimentalDesign object = ( ExperimentalDesign ) getCurrentRowObject();
        String name = object.getName();
        if ( ( name == null ) || ( name.length() == 0 ) ) {
            name = "No name";
        }
        return "<a href=\"/Gemma/experimentalDesign/showExperimentalDesign.html?id=" + object.getId() + "\">" + name
                + "</a>";
    }

    /**
     * @return String
     */
    public String getDesignsLink() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        if ( object != null  ) {
            return "<a href=\"showExpressionExperiment.html?id=" + object.getId() + "\"> </a>";
        }
        return "No design";
    }

    /**
     * link to the expression experiment view, with the name as the link view
     * 
     * @return String
     */
    public String getNameLink() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        if ( object != null ) {
            return "<a href=\"showExpressionExperiment.html?id=" + object.getId() + "\">" + object.getName() + "</a>";
        }
        return "No design";
    }

    /**
     * @return The creation date.
     */
    public String getCreateDate() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        if ( object != null && object.getCreateDate() != null ) {

            Date date = object.getCreateDate();

            SimpleDateFormat dateFormat = new SimpleDateFormat( "MM/dd/yyyy" );
            dateFormat.setLenient( false );

            return dateFormat.format( date );
        }
        return "Creation date unavailable";
    }

    /**
     * @return View for key of the quantitation type counts.
     */
    public String getQtName() {
        Map.Entry entry = ( Map.Entry ) getCurrentRowObject();
        return ( String ) entry.getKey();
    }

    /**
     * @return View for value of the quantitation type counts.
     */
    public Integer getQtValue() {
        Map.Entry entry = ( Map.Entry ) getCurrentRowObject();
        return ( Integer ) entry.getValue();
    }

    /**
     * @return
     */
    public String getTaxon() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        return object.getTaxon();
    }
    
    public String getDelete() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        
        if ( object == null ) {
            return "Expression Experiment unavailable";
        }
        
        return "<div align='left'> <input type='button' onclick=\"location.href='deleteExpressionExperiment.html?id=" + object.getId() + "'\" value='Delete'>  </div>";

    }
}
