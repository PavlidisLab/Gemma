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
import java.util.Collection;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * Used to generate hyperlinks in displaytag tables.
 * <p>
 * See http://displaytag.sourceforge.net/10/tut_decorators.html and http://displaytag.sourceforge.net/10/tut_links.html
 * for explanation of how this works.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionExperimentWrapper extends TableDecorator {

    Log log = LogFactory.getLog( this.getClass() );

    /**
     * @return String
     */
    public String getSourceLink() {
        ExpressionExperiment object = ( ExpressionExperiment ) getCurrentRowObject();
        if ( object.getSource() != null ) {
            return "<a href=\"" + object.getAccession().getExternalDatabase().getWebUri() + "\">" + object.getSource()
                    + "</a>";
        }
        return "No Source";
    }

    /**
     * @return String
     */
    public String getDetailsLink() {
        ExpressionExperiment object = ( ExpressionExperiment ) getCurrentRowObject();
        if ( object.getAccession() != null ) {
//            return "<a href=\"showExpressionExperiment.html?name=" + object.getName() + "\">"
//                    + object.getAccession().getExternalDatabase().getName() + " - "
//                    + object.getAccession().getAccession() + "</a>";
            return "<a href=\"showExpressionExperiment.html?id=" + object.getId() + "\">"
            + object.getAccession().getExternalDatabase().getName() + " - "
            + object.getAccession().getAccession() + "</a>";
        }
        return "No accession";
    }

    /**
     * @return String
     */
    public String getAssaysLink() {
        ExpressionExperiment object = ( ExpressionExperiment ) getCurrentRowObject();
        if ( object.getBioAssays() != null ) {
//            return "<a href=\"showExpressionExperiment.html?name=" + object.getName() + "\">"
//                    + object.getBioAssays().size() + "</a>";
            return "<a href=\"showExpressionExperiment.html?id=" + object.getId() + "\">"
            + object.getBioAssays().size() + "</a>";
        }
        return "No bioassays";
    }

    /**
     * @return String
     */
    public String getFactorsLink() {

        log.debug( getCurrentRowObject() );

        ExperimentalDesign object = ( ExperimentalDesign ) getCurrentRowObject();

        if ( object.getExperimentalFactors() != null ) {
            return "<a href=\"/Gemma/experimentalDesign/showExperimentalDesign.html?name=" + object.getName() + "\">"
                    + object.getExperimentalFactors().size() + "</a>";
        }
        return "No experimental factors";
    }

    /**
     * @return String
     */
    public String getDesignsLink() {
        ExpressionExperiment object = ( ExpressionExperiment ) getCurrentRowObject();
        if ( object.getExperimentalDesigns() != null ) {
            return "<a href=\"showExpressionExperiment.html?name=" + object.getName() + "\">"
                    + object.getExperimentalDesigns().size() + "</a>";
        }
        return "No design";
    }

    /**
     * @return
     */
    public String getIdLink() {
        ExpressionExperiment object = ( ExpressionExperiment ) getCurrentRowObject();
        if ( object.getExperimentalDesigns() != null ) {
            return "<a href=\"showExpressionExperiment.html?id=" + object.getId() + "\">" + object.getId() + "</a>";
        }
        return "No design";
    }

    /**
     * @return The creation date.
     */
    public String getCreateDate() {
        ExpressionExperiment object = ( ExpressionExperiment ) getCurrentRowObject();
        if ( object.getAuditTrail() != null ) {

            Date date = object.getAuditTrail().getCreationEvent().getDate();

            SimpleDateFormat dateFormat = new SimpleDateFormat( "MM/dd/yyyy" );
            dateFormat.setLenient( false );

            return dateFormat.format( date );
        }
        return "Creation date unavailable";
    }

    /**
     * @return
     */
    public String getTaxon() {
        ExpressionExperiment object = ( ExpressionExperiment ) getCurrentRowObject();
        Collection bioAssayCol = object.getBioAssays();
        BioAssay bioAssay = null;

        if ( bioAssayCol != null )
            bioAssay = ( BioAssay ) bioAssayCol.iterator().next();

        else
            return "Taxon unavailable";

        Collection bioMaterialCol = bioAssay.getSamplesUsed();
        Taxon taxon = null;

        if ( bioMaterialCol != null && bioMaterialCol.size() != 0 ) {
            BioMaterial bioMaterial = ( BioMaterial ) bioMaterialCol.iterator().next();
            taxon = bioMaterial.getSourceTaxon();
        } else {
            return "Taxon unavailable";
        }

        if ( taxon != null ) return taxon.getScientificName();

        return "Taxon unavailable";
    }
}
