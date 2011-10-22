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

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.displaytag.decorator.TableDecorator;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.util.AnchorTagUtil;

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

    private static final int MIN_FACTORS = 1;
    private static final int MAX_FACTORS = 2;

    /**
     * @return String
     */
    public String getDateCachedNoTime() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        if ( object.getDateCached() == null ) {
            return "";
        }
        if ( object.getDateCached().toString().length() > 10 ) {
            return object.getDateCached().toString().substring( 0, 10 );
        }
        return object.getDateCached().toString();
    }

    /**
     * @return String
     */
    public String getDateCreatedNoTime() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        if ( object.getDateCreated() == null ) {
            return "";
        }
        if ( object.getDateCreated().toString().length() > 10 ) {
            return object.getDateCreated().toString().substring( 0, 10 );
        }
        return object.getDateCreated().toString();
    }

    /**
     * @return String
     */
    public String getDateLastUpdatedNoTime() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        Date dateObject = object.getDateLastUpdated();
        if ( dateObject != null ) {
            boolean mostRecent = determineIfMostRecent( dateObject, object );
            String fullDate = dateObject.toString();
            String shortDate = StringUtils.left( fullDate, 10 );
            shortDate = formatIfRecent( mostRecent, shortDate );
            return "<span title='" + fullDate + "'>" + shortDate + "</span>";
        }
        return "[None]";
    }

    /**
     * @return String
     */
    public String getDateLastArrayDesignUpdatedNoTime() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        Date dateObject = object.getDateArrayDesignLastUpdated();
        if ( dateObject != null ) {
            boolean mostRecent = determineIfMostRecent( dateObject, object );
            String fullDate = dateObject.toString();
            String shortDate = StringUtils.left( fullDate, 10 );
            shortDate = formatIfRecent( mostRecent, shortDate );
            return "<span title='" + fullDate + "'>" + shortDate + "</span>";
        }
        // This indicates that the probe analysis was never done.
        String style = "style=\"color:#3A3;\" title='Needs to be done'";
        return "<span " + style + "'>Needed</span>";
    }

    private String formatIfRecent( boolean mostRecent, String shortDate ) {
        shortDate = mostRecent ? "<strong>" + shortDate + "</strong>" : shortDate;
        return shortDate;
    }

    /**
     * @return String
     */
    public String getDateProcessedDataVectorUpdateNoTime() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        Date dateObject = object.getDateProcessedDataVectorComputation();

        String style = "";
        if ( object.getTechnologyType() != null
                && !object.getTechnologyType().equalsIgnoreCase( TechnologyType.ONECOLOR.toString() )
                && !object.isHasBothIntensities() ) {
            style = "style=\"color=#F33;\"";
            return "<span title=\"Lacks qts\" " + style + " >NA</span>";
        }

        if ( dateObject != null ) {
            boolean mostRecent = determineIfMostRecent( dateObject, object );
            String fullDate = dateObject.toString();
            String shortDate = StringUtils.left( fullDate, 10 );
            shortDate = formatIfRecent( mostRecent, shortDate );
            return "<span title='" + fullDate + "'>" + shortDate + "</span>";
        }
        style = "style=\"color:#3A3;\" title='Needs to be done'";
        return "<span " + style + "'>Needed</span>";
    }

    public String getTechnologyType() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        String tt = object.getTechnologyType();
        if ( tt == null ) {
            return "?";
        } else if ( tt.equals( "TWOCOLOR" ) ) {
            return "TwoC";
        } else if ( tt.equals( "ONECOLOR" ) ) {
            return "OneC";
        } else if ( tt.equals( "DUALMODE" ) ) {
            return "Dual";
        } else {
            return "<span color=\"red\">Mixed</span>";
        }
    }

    /**
     * @return String
     */
    public String getDateLinkAnalysisNoTime() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        Date dateObject = object.getDateLinkAnalysis();
        String style = "";
        if ( dateObject != null ) {
            boolean mostRecent = determineIfMostRecent( dateObject, object );
            String type = object.getLinkAnalysisEventType();

            if ( type.equals( "FailedLinkAnalysisEventImpl" ) ) {
                style = "style=\"color:#F33;\" title='There was an error during analysis'";
            } else if ( type.equals( "TooSmallDatasetLinkAnalysisEventImpl" ) ) {
                style = "style=\"font-style:italic;\" title='This dataset may be too small to analyze'";
                return "<span " + style + "'>small</span>";
            }

            String fullDate = dateObject.toString();
            String shortDate = StringUtils.left( fullDate, 10 );
            shortDate = formatIfRecent( mostRecent, shortDate );
            return "<span " + style + " title='" + fullDate + "'>" + shortDate + "</span>";
        } else if ( object.getBioAssayCount() <= 4 ) {
            style = "style=\"font-style:italic;\" title='This dataset may be too small to analyze'";
            return "<span " + style + "'>small</span>";
        }
        style = "style=\"color:#3A3;\" title='Needs to be done'";
        return "<span " + style + "'>Needed</span>";
    }

    /**
     * @return String
     */
    public String getDateDifferentialAnalysisNoTime() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        Date dateObject = object.getDateDifferentialAnalysis();
        String style = "";
        if ( dateObject != null ) {
            boolean mostRecent = determineIfMostRecent( dateObject, object );
            String fullDate = dateObject.toString();
            String shortDate = StringUtils.left( fullDate, 10 );
            shortDate = formatIfRecent( mostRecent, shortDate );
            return "<span " + style + " title='" + fullDate + "'>" + shortDate + "</span>";
        }

        if ( isDifferentialPossible() ) {
            style = "style=\"color:#3A3;\" title='Needs to be done'";
        } else {
            style = "style=\"color:#808080;\" title='Needs to be done'";
        }

        return "<span " + style + "'>Needed</span>";
    }

    /**
     * @return Returns true if the experiment has either 1 or 2 experimental factors.
     */
    public boolean isDifferentialPossible() {

        int numFactors = getNumFactors();

        if ( numFactors < MIN_FACTORS || numFactors > MAX_FACTORS ) {
            return false;
        }
        return true;
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

        return count
                + " <a title=\"Bioassay details\" href=\"/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id="
                + object.getId() + "\">" + "<img src=\"/Gemma/images/magnifier.png\" height='10' width='10'/></a>";
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
     * @return
     */
    public Integer getNumAnnotations() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        if ( object != null ) {
            return object.getNumAnnotations();
        }
        return 0;
    }

    /**
     * @return
     */
    public Integer getNumFactors() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        if ( object != null ) {
            return object.getNumPopulatedFactors();
        }
        return 0;
    }

    /**
     * link to the expression experiment view, with the name as the link view
     * 
     * @return String
     */
    public String getNameLink() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        if ( object != null ) {
            return AnchorTagUtil.getExpressionExperimentLink( object.getId(),
                    StringUtils.abbreviate( object.getName(), 75 ), object.getName() );
        }
        return "No design";
    }

    public String getStatus() {
        return getTroubleFlag().concat( getValidatedFlag() ).concat( getSampleRemovalFlag() ).concat( getPrivacyFlag() );
    }

    public String getTroubleFlag() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        StringBuilder buf = new StringBuilder();
        if ( object.getTroubled() ) {
            buf.append( "&nbsp;<img src='/Gemma/images/icons/warning.png' height='16' width='16' alt='trouble' title='" );
            buf.append( StringEscapeUtils.escapeHtml( object.getTroubleDetails() ) );
            buf.append( "' />" );
        }
        return buf.toString();
    }

    public String getSampleRemovalFlag() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        StringBuilder buf = new StringBuilder();
        if ( object.getSampleRemovedFlags() != null && object.getSampleRemovedFlags().size() > 0 ) {
            buf.append( "&nbsp;<img src='/Gemma/images/icons/exclamation.png' height='16' width='16' alt='validated' title='" );
            buf.append( StringEscapeUtils.escapeHtml( object.getSampleRemovedFlags().iterator().next().toString() ) ); // todo:
            // support
            // multiple.
            buf.append( "' />" );
        }
        return buf.toString();
    }

    public String getPrivacyFlag() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        StringBuilder buf = new StringBuilder();
        if ( !object.isIsPublic() ) {
            buf.append( "&nbsp;<img src='/Gemma/images/icons/lock.png' height='16' width='16' alt='private' />" );
        }
        return buf.toString();
    }

    public String getValidatedFlag() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        StringBuilder buf = new StringBuilder();
        if ( object.getValidated() ) {
            buf.append( "&nbsp;<img src='/Gemma/images/icons/ok.png' height='16' width='16' alt='validated' title='Has been validated by a curator' />" );
        }
        return buf.toString();
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
        Map.Entry<?, ?> entry = ( Map.Entry<?, ?> ) getCurrentRowObject();
        return ( ( QuantitationType ) entry.getKey() ).getName();
    }

    /**
     * @return View for status of the QTs
     */
    public String getQtPreferredStatus() {
        Map.Entry<?, ?> entry = ( Map.Entry<?, ?> ) getCurrentRowObject();
        if ( ( ( QuantitationType ) entry.getKey() ).getIsPreferred() ) {
            return "<input type=checkbox checked disabled></input>";
        }
        return "<input type=checkbox disabled></input>";
    }

    /**
     * @return View for background status of the QTs
     */
    public String getQtBackground() {
        Map.Entry<?, ?> entry = ( Map.Entry<?, ?> ) getCurrentRowObject();
        if ( ( ( QuantitationType ) entry.getKey() ).getIsBackground() ) {
            return "<input type=checkbox checked disabled></input>";
        }
        return "<input type=checkbox disabled></input>";
    }

    /**
     * @return View for background subtracted status of the QTs
     */
    public String getQtBackgroundSubtracted() {
        Map.Entry<?, ?> entry = ( Map.Entry<?, ?> ) getCurrentRowObject();
        if ( ( ( QuantitationType ) entry.getKey() ).getIsBackgroundSubtracted() ) {
            return "<input type=checkbox checked disabled></input>";
        }
        return "<input type=checkbox disabled></input>";
    }

    /**
     * @return View for normalized status of the QTs
     */
    public String getQtNormalized() {
        Map.Entry<?, ?> entry = ( Map.Entry<?, ?> ) getCurrentRowObject();
        if ( ( ( QuantitationType ) entry.getKey() ).getIsNormalized() ) {
            return "<input type=checkbox checked disabled></input>";
        }
        return "<input type=checkbox disabled></input>";
    }

    /**
     * @return View for value of the quantitation type counts.
     */
    public Integer getQtValue() {
        Map.Entry<?, ?> entry = ( Map.Entry<?, ?> ) getCurrentRowObject();
        return ( Integer ) entry.getValue();
    }

    /**
     * @return
     */
    public String getTaxon() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        return object.getTaxon();
    }

    public String getRefreshReport() {
        ExpressionExperimentValueObject object = ( ExpressionExperimentValueObject ) getCurrentRowObject();

        if ( object == null ) {
            return "Expression Experiment unavailable";
        }
        return " <input type=\"button\" value=\"Refresh\" onClick=\"return updateEEReport(" + object.getId() + ")\"/>";
    }

    public String getSpecific() {

        ExpressionExperimentValueObject eeVo = ( ExpressionExperimentValueObject ) getCurrentRowObject();
        if ( eeVo == null ) return "Expression Experiment unavailable";

        if ( eeVo.getHasProbeSpecificForQueryGene() ) return "&bull;";

        return "";
    }

    /**
     * @param dateObject
     * @param object
     * @return
     */
    private boolean determineIfMostRecent( Date dateObject, ExpressionExperimentValueObject object ) {
        if ( dateObject == null ) return false;
        Date linksdate = object.getDateLinkAnalysis();
        Date mvdate = object.getDateMissingValueAnalysis();
        Date rankDate = object.getDateProcessedDataVectorComputation();
        Date adDate = object.getDateArrayDesignLastUpdated();
        Date differentialDate = object.getDateDifferentialAnalysis();

        if ( adDate != null && dateObject.before( adDate ) ) return false;
        if ( rankDate != null && dateObject.before( rankDate ) ) return false;
        if ( mvdate != null && dateObject.before( mvdate ) ) return false;
        if ( linksdate != null && dateObject.before( linksdate ) ) return false;
        if ( differentialDate != null && dateObject.before( differentialDate ) ) return false;

        return true;
    }

}
