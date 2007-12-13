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
package ubic.gemma.web.taglib.displaytag.expression.experiment;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.displaytag.decorator.TableDecorator;

import ubic.gemma.loader.expression.geo.model.GeoRecord;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author pavlidis
 * @version $Id$
 */
public class GeoRecordWrapper extends TableDecorator {

    /**
     * Link for loading or viewing, depending on whether the data are already in the database.
     * 
     * @return
     */
    public String getInGemma() {
        GeoRecord record = ( GeoRecord ) getCurrentRowObject();

        if ( record.getCorrespondingExperiments().size() == 0 ) {
            String accession = record.getGeoAccession();
            return "<div id=\"upload-button.notyet\">Sorry</div>";
            // return "<strong><form method=\"POST\" action=\"/Gemma/loadExpressionExperiment.html\"><input
            // type=\"hidden\" name=\"accession\" value=\""
            // + accession + "\" /><input type=\"submit\" value=\"Load\" /></form></strong>";
        } else {
            StringBuilder buf = new StringBuilder();
            for ( ExpressionExperiment ee : record.getCorrespondingExperiments() ) {
                buf.append( "<a href=\"/Gemma/expressionExperiment/showExpressionExperiment.html?" );
                buf.append( "id=" + ee.getId() + "\">" + ee.getShortName() );
                buf.append( "</a> " );
            }
            return buf.toString();
        }
    }

    public String getDetails() {
        GeoRecord record = ( GeoRecord ) getCurrentRowObject();
        String accession = record.getGeoAccession();
        return "<a href=\"#\" onClick=\"showDetails(\'" + accession + "\')\">Details</a>";
    }

    /**
     * @return
     */
    public String getGeoAccessionLink() {
        GeoRecord record = ( GeoRecord ) getCurrentRowObject();
        return "<a href='http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=" + record.getGeoAccession() + "'>"
                + record.getGeoAccession() + "</a>";
    }

    public String getReleaseDateNoTime() {
        GeoRecord record = ( GeoRecord ) getCurrentRowObject();
        Date d = record.getReleaseDate();
        SimpleDateFormat df = new SimpleDateFormat( "dd/MM/yy" ); // F out.
        try {
            return df.format( d );
        } catch ( Exception e ) {
            return "[date unparseable]";
        }
    }

    /**
     * @return
     */
    public String getTaxa() {
        GeoRecord record = ( GeoRecord ) getCurrentRowObject();
        StringBuilder buf = new StringBuilder();
        for ( String org : record.getOrganisms() ) {
            buf.append( org + " " );
        }
        return buf.toString();
    }

}