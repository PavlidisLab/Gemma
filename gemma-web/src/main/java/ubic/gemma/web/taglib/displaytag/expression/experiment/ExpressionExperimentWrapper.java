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

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedLinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedMissingValueAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TooSmallDatasetLinkAnalysisEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

/**
 * Used to generate hyperlinks in displaytag tables.
 * <p>
 * See http://displaytag.sourceforge.net/10/tut_decorators.html and http://displaytag.sourceforge.net/10/tut_links.html
 * for explanation of how this works.
 * 
 * @author pavlidis
 * @author jrsantos
 * @version $Id$
 */
public class ExpressionExperimentWrapper extends TableDecorator {

    Log log = LogFactory.getLog( this.getClass() );

    /**
     * @return String
     */
    public String getDateCachedNoTime() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        if ( object.getDateCached() == null ) {
            return "";
        } else {
            if ( object.getDateCached().length() > 10 ) {
                return object.getDateCached().substring( 0, 10 );
            } else {
                return object.getDateCached();
            }
        }
    }

    /**
     * @return String
     */
    public String getDateCreatedNoTime() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        if ( object.getDateCreated() == null ) {
            return "";
        } else {
            if ( object.getDateCreated().length() > 10 ) {
                return object.getDateCreated().substring( 0, 10 );
            } else {
                return object.getDateCreated();
            }
        }
    }

    /**
     * @return String
     */
    public String getDateLastUpdatedNoTime() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        Date dateObject = object.getDateLastUpdated();
        if ( dateObject != null ) {
            String fullDate = dateObject.toString();
            String shortDate = StringUtils.left( fullDate, 10 );
            return "<span title='" + fullDate + "'>" + shortDate + "</span>";
        } else {
            return "[None]";
        }
    }

    /**
     * @return String
     */
    public String getDateMissingValueAnalysisNoTime() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        Date dateObject = object.getDateMissingValueAnalysis();
        if ( dateObject != null ) {
            AuditEventType type = object.getMissingValueAnalysisEventType();
            String fullDate = dateObject.toString();
            String shortDate = StringUtils.left( fullDate, 10 );
            String style = "";

            if ( type instanceof FailedMissingValueAnalysisEvent ) {
                style = "style=\"color=#F33;\"";
                // } else if ( type instanceof NoIntensityMissingValueAnalysisEvent ) {
                // style = "style=\"font-style=italic;\"";
            }

            return "<span " + style + " title='" + fullDate + "'>" + shortDate + "</span>";
        } else {
            return "[None]";
        }
    }

    /**
     * @return String
     */
    public String getDateRankComputationNoTime() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        Date dateObject = object.getDateRankComputation();
        if ( dateObject != null ) {
            // AuditEventType type = object.getRankComputationEventType();
            // if ( type instanceof FailedRankAnalysisEvent ) {
            // style = "style=\"color=#F33;\"";
            // }
            String fullDate = dateObject.toString();
            String shortDate = StringUtils.left( fullDate, 10 );
            return "<span title='" + fullDate + "'>" + shortDate + "</span>";
        } else {
            return "[None]";
        }
    }

    /**
     * @return String
     */
    public String getDateLinkAnalysisNoTime() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        Date dateObject = object.getDateLinkAnalysis();
        if ( dateObject != null ) {
            AuditEventType type = object.getLinkAnalysisEventType();

            String style = "";
            if ( type instanceof FailedLinkAnalysisEvent ) {
                style = "style=\"color:#F33;\"";
            } else if ( type instanceof TooSmallDatasetLinkAnalysisEvent ) {
                style = "style=\"font-style:italic;\"";
            }

            String fullDate = dateObject.toString();
            String shortDate = StringUtils.left( fullDate, 10 );
            return "<span " + style + " title='" + fullDate + "'>" + shortDate + "</span>";
        } else {
            return "[None]";
        }
    }

    /**
     * @return String
     */
    public String getDataSource() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        if ( object != null && object.getAccession() != null ) {
            // custom code
            // put in GEO link if it is GEO
            // TODO we should create external database links dynamically
            if ( object.getExternalDatabase().equalsIgnoreCase( "geo" ) ) {
                return "<a href='http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=" + object.getAccession()
                        + "'>GEO</a>";
            }
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
            return "<a href=\"" + object.getExternalUri() + "\">" + object.getAccession() + "</a>";
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

        return count + " <a href=\"/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id="
                + object.getId() + "\">" + "<img src=\"/Gemma/images/magnifier.png\" height=10 width=10/></a>";
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
        if ( object != null ) {
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
            return "<a title=\"" + object.getName()
                    + "\" href=\"/Gemma/expressionExperiment/showExpressionExperiment.html?id=" + object.getId()
                    + "\">" + StringUtils.abbreviate( object.getName(), 75 ) + "</a>";
        }
        return "No design";
    }

    /**
     * Returns the count of the array designs for the specific expression experiment
     * 
     * @return String
     */
    public String getArrayDesignLink() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        if ( object != null ) {
            return Long.toString( object.getArrayDesignCount() );
        }
        return "No design";
    }

    /**
     * @return View for key of the quantitation type counts.
     */
    public String getQtName() {
        Map.Entry entry = ( Map.Entry ) getCurrentRowObject();
        return ( ( QuantitationType ) entry.getKey() ).getName();
    }

    /**
     * @return View for status of the QTs
     */
    public String getQtPreferredStatus() {
        Map.Entry entry = ( Map.Entry ) getCurrentRowObject();
        if ( ( ( QuantitationType ) entry.getKey() ).getIsPreferred() ) {
            return "<input type=checkbox checked disabled></input>";
        } else {
            return "<input type=checkbox disabled></input>";
        }
    }

    /**
     * @return View for background status of the QTs
     */
    public String getQtBackground() {
        Map.Entry entry = ( Map.Entry ) getCurrentRowObject();
        if ( ( ( QuantitationType ) entry.getKey() ).getIsBackground() ) {
            return "<input type=checkbox checked disabled></input>";
        } else {
            return "<input type=checkbox disabled></input>";
        }
    }

    /**
     * @return View for background subtracted status of the QTs
     */
    public String getQtBackgroundSubtracted() {
        Map.Entry entry = ( Map.Entry ) getCurrentRowObject();
        if ( ( ( QuantitationType ) entry.getKey() ).getIsBackgroundSubtracted() ) {
            return "<input type=checkbox checked disabled></input>";
        } else {
            return "<input type=checkbox disabled></input>";
        }
    }

    /**
     * @return View for normalized status of the QTs
     */
    public String getQtNormalized() {
        Map.Entry entry = ( Map.Entry ) getCurrentRowObject();
        if ( ( ( QuantitationType ) entry.getKey() ).getIsNormalized() ) {
            return "<input type=checkbox checked disabled></input>";
        } else {
            return "<input type=checkbox disabled></input>";
        }
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

        // TODO: replace with more generic javascript method call instead of form.,
        return "<form action=\"deleteExpressionExperiment.html?id=" + object.getId()
                + "\" onSubmit=\"return confirmDelete('Expression experiment " + object.getName()
                + "')\" method=\"post\"><input type=\"submit\"  value=\"Delete\" /></form>";

    }

    public String getRefreshReport() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();

        if ( object == null ) {
            return "Expression Experiment unavailable";
        }
        // TODO: replace with more generic javascript method call instead of form.,
        return "<form action=\"generateExpressionExperimentLinkSummary.html?id=" + object.getId()
                + "\" onSubmit=\"return confirm('Refresh summary for experiment " + object.getName()
                + "?')\" method=\"post\"><input type=\"submit\"  value=\"Refresh\" title='Refresh Report'/></form>";

    }

    public String getEdit() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();

        if ( object == null ) {
            return "Expression Experiment unavailable";
        }

        return "<input type='button' " + " onclick=\"location.href='editExpressionExperiment.html?id=" + object.getId()
                + "'\"" + " value='Edit'>";

    }

    public String getSpecific() {

        ExpressionExperimentValueObject eeVo = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        if ( eeVo == null ) return "Expression Experiment unavailable";

        if ( eeVo.isSpecific() ) return "&bull;";

        return "";
    }

}
