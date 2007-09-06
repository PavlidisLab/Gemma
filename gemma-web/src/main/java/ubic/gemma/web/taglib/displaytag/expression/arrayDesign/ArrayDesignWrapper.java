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
package ubic.gemma.web.taglib.displaytag.expression.arrayDesign;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;

/**
 * Used to generate hyperlinks in displaytag tables.
 * <p>
 * See http://displaytag.sourceforge.net/10/tut_decorators.html and http://displaytag.sourceforge.net/10/tut_links.html
 * for explanation of how this works.
 * 
 * @author joseph
 * @version $Id$
 */
public class ArrayDesignWrapper extends TableDecorator {

    Log log = LogFactory.getLog( this.getClass() );

    public String getLastSequenceUpdateDate() {
        ArrayDesignValueObject object = ( ArrayDesignValueObject ) getCurrentRowObject();

        // if it has been merged, put NA for 'not applicable'
        if ( getIsSubsumed().length() > 0 || getIsMergee().length() > 0 ) {
            return "NA";
        }

        Date dateObject = object.getLastSequenceUpdate();

        if ( dateObject != null ) {
            boolean mostRecent = determineIfMostRecent( dateObject, object );
            String fullDate = dateObject.toString();
            String shortDate = StringUtils.left( fullDate, 10 );
            shortDate = formatIfRecent( mostRecent, shortDate );
            return "<span title='" + fullDate + "'>" + shortDate + "</span>";
        } else {
            return "[None]";
        }
    }

    public String getLastRepeatMaskDate() {
        ArrayDesignValueObject object = ( ArrayDesignValueObject ) getCurrentRowObject();

        // if it has been merged, put NA for 'not applicable'
        if ( getIsSubsumed().length() > 0 || getIsMergee().length() > 0 ) {
            return "NA";
        }
        Date dateObject = object.getLastRepeatMask();

        if ( dateObject != null ) {
            boolean mostRecent = determineIfMostRecent( dateObject, object );
            String fullDate = dateObject.toString();
            String shortDate = StringUtils.left( fullDate, 10 );
            shortDate = formatIfRecent( mostRecent, shortDate );
            return "<span title='" + fullDate + "'>" + shortDate + "</span>";
        } else {
            return "[None]";
        }
    }

    public String getShortName() {
        ArrayDesignValueObject object = ( ArrayDesignValueObject ) getCurrentRowObject();
        String result = object.getShortName();

        if ( result == null ) result = "--";

        if ( ( object.getIsMerged() != null ) && ( object.getIsMerged() ) ) {
            result = result
                    + "&nbsp;<img title='merged' src=\"/Gemma/images/icons/chart_pie.png\" height=\"16\" width=\"16\" alt=\"Created by merge\" />";
        }
        if ( ( object.getIsMergee() != null ) && ( object.getIsMergee() ) ) {
            result = result
                    + "&nbsp;<img  title='mergee' src=\"/Gemma/images/icons/arrow_join.png\" height=\"16\" width=\"16\" alt=\"Part of a merge\"  />";
        }
        if ( ( object.getIsSubsumer() != null ) && ( object.getIsSubsumer() ) ) {
            result = result
                    + "&nbsp;<img title='subsumer' src=\"/Gemma/images/icons/sitemap.png\" height=\"16\" width=\"16\" alt=\"Subsumer\"  />";
        }
        if ( ( object.getIsSubsumed() != null ) && ( object.getIsSubsumed() ) ) {
            result = result
                    + "&nbsp;<img title='subsumed' src=\"/Gemma/images/icons/contrast_high.png\" height=\"16\" width=\"16\" alt=\"Sequences are subsumed by another\"  />";
        }
        return result;
    }

    public String getIsSubsumed() {
        ArrayDesignValueObject object = ( ArrayDesignValueObject ) getCurrentRowObject();
        if ( ( object.getIsSubsumed() != null ) && ( object.getIsSubsumed() ) ) {
            return "<<";
        } else {
            return "";
        }
    }

    public String getIsSubsumer() {
        ArrayDesignValueObject object = ( ArrayDesignValueObject ) getCurrentRowObject();
        if ( ( object.getIsSubsumer() != null ) && ( object.getIsSubsumer() ) ) {
            return ">>";
        } else {
            return "";
        }
    }

    public String getIsMerged() {
        ArrayDesignValueObject object = ( ArrayDesignValueObject ) getCurrentRowObject();
        if ( ( object.getIsMerged() != null ) && ( object.getIsMerged() ) ) {
            return "[";
        } else {
            return "";
        }
    }

    public String getIsMergee() {
        ArrayDesignValueObject object = ( ArrayDesignValueObject ) getCurrentRowObject();
        if ( ( object.getIsMergee() != null ) && ( object.getIsMergee() ) ) {
            return "]";
        } else {
            return "";
        }
    }

    private String formatIfRecent( boolean mostRecent, String shortDate ) {
        shortDate = mostRecent ? "<strong>" + shortDate + "</strong>" : shortDate;
        return shortDate;
    }

    public String getLastSequenceAnalysisDate() {
        ArrayDesignValueObject object = ( ArrayDesignValueObject ) getCurrentRowObject();
        Date dateObject = object.getLastSequenceAnalysis();
        // if it has been merged, put NA for 'not applicable'
        if ( getIsSubsumed().length() > 0 || getIsMergee().length() > 0 ) {
            return "NA";
        }
        if ( dateObject != null ) {
            boolean mostRecent = determineIfMostRecent( dateObject, object );
            String fullDate = dateObject.toString();
            String shortDate = StringUtils.left( fullDate, 10 );
            shortDate = formatIfRecent( mostRecent, shortDate );
            return "<span title='" + fullDate + "'>" + shortDate + "</span>";
        } else {
            return "[None]";
        }
    }

    public String getLastGeneMappingDate() {
        ArrayDesignValueObject object = ( ArrayDesignValueObject ) getCurrentRowObject();
        Date dateObject = object.getLastGeneMapping();
        // if it has been merged, put NA for 'not applicable'
        if ( getIsSubsumed().length() > 0 || getIsMergee().length() > 0 ) {
            return "NA";
        }
        if ( dateObject != null ) {
            boolean mostRecent = determineIfMostRecent( dateObject, object );
            String fullDate = dateObject.toString();
            String shortDate = StringUtils.left( fullDate, 10 );
            shortDate = formatIfRecent( mostRecent, shortDate );
            return "<span title='" + fullDate + "'>" + shortDate + "</span>";
        } else {
            return "[None]";
        }
    }

    public String getSummaryTable() {
        StringBuilder buf = new StringBuilder();
        ArrayDesignValueObject object = ( ArrayDesignValueObject ) getCurrentRowObject();

        // check if the summary numbers exist
        // if they donn't, just return not available
        if ( object.getNumProbeAlignments() == null ) {
            return "[Not avail.]";
        }

        String arraySummary = "arraySummary_" + object.getId();

        buf
                .append( "<span class=\"" + arraySummary + "\" onclick=\"return toggleVisibility('" + arraySummary
                        + "')\">" );
        buf.append( "<img src=\"/Gemma/images/chart_organisation_add.png\" /></span>" );

        buf.append( "<span class=\"" + arraySummary + "\" style=\"display:none\" onclick=\"return toggleVisibility('"
                + arraySummary + "')\">" );
        buf.append( "<img src=\"/Gemma/images/plus.png\" /></span>" );

        buf.append( "<a href=\"#\" onclick=\"return toggleVisibility('" + arraySummary + "')\" >Summary</a>" );

        buf.append( "<div class=\"" + arraySummary + "\" style=\"display:none\">" );

        buf.append( "<table class='datasummary'>" + "<tr>" + "<td colspan=2 align=center>" + "</td></tr>"
                + "<tr><td>Probes</td><td align=\"right\" >"
                + object.getDesignElementCount()
                + "</td></tr>"
                + "<tr><td title=\"Number of probes with sequences\">"
                + "With seq"
                + "</td><td align=\"right\" >"
                + object.getNumProbeSequences()
                + "</td></tr>"
                + "<tr><td title=\"Number of probes with at least one genome alignment\">"
                + "With align"
                + "</td>"
                + "<td align=\"right\" >"
                + object.getNumProbeAlignments()
                + "</td></tr>"
                + "<tr><td title=\"Number of probes mapped to genes (including predicted and anonymous locations)\">"
                + "Mapped"
                + "</td><td align=\"right\" >"
                + object.getNumProbesToGenes()
                + "</td></tr>"
                +

                "<tr><td  title=\"Number of probes mapping to known genes\">"
                + "&nbsp;&nbsp;Known"
                + "</td><td align=\"right\" >"
                + object.getNumProbesToKnownGenes()
                + "</td></tr>"
                +

                "<tr><td title=\"Number of probes mapping to predicted genes\">"
                + "&nbsp;&nbsp;Pred."
                + "</td><td align=\"right\" >"
                + object.getNumProbesToPredictedGenes()
                + "</td></tr>"
                +

                "<tr><td  title=\"Number of probes mapping to non-gene locations in the genome\">"
                + "&nbsp;&nbsp;Unknown"
                + "</td><td align=\"right\" >"
                + object.getNumProbesToProbeAlignedRegions()
                + "</td></tr>"
                +

                "<tr><td title=\"Number of unique genes represented on the array\" >"
                + "Unique genes"
                + "</td><td align=\"right\" >"
                + object.getNumGenes()
                + "</td></tr>"
                + "<tr><td colspan=2 align='center' class='small'>"
                + "(as of "
                + object.getDateCached()
                + ")"
                + "</td></tr>" + "</table>" );

        buf.append( "</div>" );
        return buf.toString();
    }

    public String getExpressionExperimentCountLink() {
        ArrayDesignValueObject object = ( ArrayDesignValueObject ) getCurrentRowObject();
        if ( object.getExpressionExperimentCount() != null && object.getExpressionExperimentCount() > 0 ) {
            long id = object.getId();

            return object.getExpressionExperimentCount()
                    + " <a href=\"showExpressionExperimentsFromArrayDesign.html?id=" + id + "\">"
                    + "<img src=\"/Gemma/images/magnifier.png\" height=10 width=10/></a>";
        }

        return "0";
    }

    public String getDelete() {
        ArrayDesignValueObject object = ( ArrayDesignValueObject ) getCurrentRowObject();

        if ( object == null ) {
            return "Array Design unavailable";
        }

        return "<form action=\"deleteArrayDesign.html?id=" + object.getId()
                + "\" onSubmit=\"return confirmDelete('Array Design " + object.getName()
                + "')\" method=\"post\"><input type=\"submit\"  value=\"Delete\" /></form>";

    }

    public String getRefreshReport() {
        ArrayDesignValueObject object = ( ArrayDesignValueObject ) getCurrentRowObject();
        if ( object == null ) {
            return "Array Design unavailable";
        }
        return "<input type=\"button\" value=\"Refresh\" " + "\" onClick=\"updateReport(" + object.getId() + ")\" >";
    }

    public String getColor() {
        ArrayDesignValueObject object = ( ArrayDesignValueObject ) getCurrentRowObject();

        if ( object == null ) {
            return "?";
        }

        String colorString = "";
        if ( object.getColor() == null ) {
            colorString = "?";
        } else if ( object.getColor().equalsIgnoreCase( "ONECOLOR" ) ) {
            colorString = "one";
        } else if ( object.getColor().equalsIgnoreCase( "TWOCOLOR" ) ) {
            colorString = "two";
        } else if ( object.getColor().equalsIgnoreCase( "DUALMODE" ) ) {
            colorString = "dual";
        } else {
            colorString = "No color";
        }
        return colorString;
    }

    /**
     * @param dateObject
     * @param object
     * @return
     */
    private boolean determineIfMostRecent( Date dateObject, ArrayDesignValueObject object ) {
        if ( dateObject == null ) return false;
        Date seqDate = object.getLastSequenceUpdate();
        Date analDate = object.getLastSequenceAnalysis();
        Date mapDate = object.getLastGeneMapping();
        Date repDate = object.getLastRepeatMask();

        if ( seqDate != null && dateObject.before( seqDate ) ) return false;
        if ( analDate != null && dateObject.before( analDate ) ) return false;
        if ( mapDate != null && dateObject.before( mapDate ) ) return false;
        if ( repDate != null && dateObject.before( repDate ) ) return false;

        return true;
    }

}
