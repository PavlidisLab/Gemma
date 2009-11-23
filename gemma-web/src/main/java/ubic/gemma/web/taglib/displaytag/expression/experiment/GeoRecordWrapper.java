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
            return "<input type=\"button\" value=\"Load\" " + "\" onClick=\"load('" + accession + "')\" >";
        }
        StringBuilder buf = new StringBuilder();
        for ( Long ee : record.getCorrespondingExperiments() ) {
            buf.append( "<a href=\"/Gemma/expressionExperiment/showExpressionExperiment.html?" );
            buf.append( "id=" + ee + "\">" + record.getGeoAccession() );
            buf.append( "</a> " );
        }
        return buf.toString();
    }

    public String getDetails() {
        GeoRecord record = ( GeoRecord ) getCurrentRowObject();
        String accession = record.getGeoAccession();
        return "<a href=\"#\" onClick=\"showDetails(\'" + accession + "\')\">Details</a>";
    }

    /**
     * @return
     */
    public String getUsable() {
        GeoRecord record = ( GeoRecord ) getCurrentRowObject();
        boolean usable = record.isUsable();
        String accession = record.getGeoAccession();
        if ( record.getCorrespondingExperiments().size() > 0 ) {
            return "<img src=\"/Gemma/images/icons/gray-thumb.png\" width=\"16\" height=\"16\" alt=\"Already loaded\"/>"; // greyd
            // out
            // thumb
        } else if ( !usable ) {
            return "<span id=\""
                    + accession
                    + "-rating\"  onClick=\"toggleUsability('"
                    + accession
                    + "')\"  ><img src=\"/Gemma/images/icons/thumbsdown-red.png\"  alt=\"Judged unusable, click to toggle\"  width=\"16\" height=\"16\"  /></span>"; // thumbs
            // down
        }
        return "<span id=\""
                + accession
                + "-rating\"  onClick=\"toggleUsability('"
                + accession
                + "')\"><img src=\"/Gemma/images/icons/thumbsup.png\"  width=\"16\" height=\"16\"   alt=\"Usable, click to toggle\" /></span>"; // thumbs
        // up
    }

    public String getClicks() {
        GeoRecord record = ( GeoRecord ) getCurrentRowObject();
        StringBuilder buf = new StringBuilder();
        for ( int i = 0; i < Math.min( record.getPreviousClicks(), 5 ); i++ ) {
            buf.append( "&bull;" );
        }
        return buf.toString();

    }

    public String getTitle() {
        GeoRecord record = ( GeoRecord ) getCurrentRowObject();
        if ( record.getCorrespondingExperiments().size() == 0 ) {
            String title = record.getTitle();

            String replacementColor = "#774477";

            if ( title.toLowerCase().matches( ".*?array[\\s-]cgh.*?" ) ) {
                return "<span style=\"color:" + replacementColor + ";\">" + record.getTitle() + "</span>";
            } else if ( title.toLowerCase().matches( ".*?copy[\\s-]number.*" ) ) {
                return "<span style=\"color:" + replacementColor + ";\">" + record.getTitle() + "</span>";
            } else if ( title.toLowerCase().contains( "methylome" ) ) {
                return "<span style=\"color:" + replacementColor + ";\">" + record.getTitle() + "</span>";
            } else if ( title.toLowerCase().contains( "snp array" ) ) {
                return "<span style=\"color:" + replacementColor + ";\">" + record.getTitle() + "</span>";
            } else if ( title.toLowerCase().contains( "chip-on-chip" ) ) {
                return "<span style=\"color:" + replacementColor + ";\">" + record.getTitle() + "</span>";
            }

            title = title.replaceAll( "(microrna|miRNA)", "<span style=\"color:#BB1111\">$1</span>" );

            return title;

        }
        return "<span style=\"color:#BBBBBB;\">" + record.getTitle() + "</span>";

    }

    /**
     * @return
     */
    public String getGeoAccessionLink() {
        GeoRecord record = ( GeoRecord ) getCurrentRowObject();
        return "<a target='_blank' href='http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=" + record.getGeoAccession()
                + "'>" + record.getGeoAccession() + "</a>";
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