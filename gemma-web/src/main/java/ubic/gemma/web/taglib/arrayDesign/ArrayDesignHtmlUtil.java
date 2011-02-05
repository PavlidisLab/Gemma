/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.web.taglib.arrayDesign;

import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignHtmlUtil {

    /**
     * Generate a pretty HTML table with the array design stats summary.
     * 
     * @param object
     * @return
     */
    public static String getSummaryHtml( ArrayDesignValueObject object ) {
        return "<table class='datasummary'>" + "<tr>" + "<td colspan=2 align=center>" + "</td></tr>"
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
                /*
                 * "<tr><td title=\"Number of probes mapping to predicted genes\">" + "&nbsp;&nbsp;Pred." +
                 * "</td><td align=\"right\" >" + object.getNumProbesToPredictedGenes() + "</td></tr>" +
                 * 
                 * "<tr><td  title=\"Number of probes mapping to non-gene locations in the genome\">" +
                 * "&nbsp;&nbsp;Unknown" + "</td><td align=\"right\" >" + object.getNumProbesToProbeAlignedRegions() +
                 * "</td></tr>" +
                 */

                "<tr><td title=\"Number of unique genes represented on the array\" >"
                + "Unique genes"
                + "</td><td align=\"right\" >"
                + object.getNumGenes()
                + "</td></tr>"
                + "<tr><td colspan=2 align='center' class='small'>"
                + "(as of "
                + object.getDateCached()
                + ")"
                + "</td></tr>" + "</table>";
    }
}
