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
package ubic.gemma.util;

/**
 * Used to generate hyperlinks to various things in Gemma.\
 * 
 * @author luke
 * @version $Id$
 */
public class GemmaLinkUtils {

    public static String getLink( String url, String link, String hover ) {
        StringBuffer buf = new StringBuffer();
        buf.append( "<a href=\"" );
        buf.append( url );
        buf.append( "\"" );
        if ( hover != null ) {
            buf.append( " class=\"tooltip\"" );
        }
        buf.append( ">" );
        buf.append( link );
        if ( hover != null ) {
            buf.append( "<span class=\"tooltiptext\">" );
            buf.append( hover );
            buf.append( "</span>" );
        }
        buf.append( "</a>" );
        return buf.toString();
    }

    public static String getExpressionExperimentUrl( long eeId ) {
        return String.format( "/Gemma/expressionExperiment/showExpressionExperiment.html?id=%d", eeId );
    }

    public static String getExpressionExperimentLink( Long eeId, String link ) {
        return getExpressionExperimentLink( eeId, link, null );
    }

    public static String getExpressionExperimentLink( Long eeId, String link, String hover ) {
        return getLink( getExpressionExperimentUrl( eeId ), link, hover );
    }

    public static String getExperimentalDesignLink( Long edId, String link ) {
        return getExperimentalDesignLink( edId, link, null );
    }

    public static String getExperimentalDesignLink( Long edId, String link, String hover ) {
        return getLink( String.format( "/Gemma/expressionExperiment/showExperimnetalDesign.html?id=%d", edId ), link,
                hover );
    }

    public static String getProbeLink( Long probeId, String link ) {
        // FIXME this isn't the greatest page - it's kind of old-style.
        return getLink( String.format( "/Gemma/compositeSequence/show.html?id=%d", probeId ), link, "" );
    }

    public static String getArrayDesignLink( Long adId, String link ) {
        return getArrayDesignLink( adId, link, null );
    }

    public static String getArrayDesignLink( Long adId, String link, String hover ) {
        return getLink( String.format( "Gemma/arrays/showArrayDesign.html?id=%d", adId ), link, hover );
    }

    public static String getBioMaterialLink( Long bmId, String link ) {
        return getBioMaterialLink( bmId, link, null );
    }

    public static String getBioMaterialLink( Long bmId, String link, String hover ) {
        return getLink( String.format( "/Gemma/bioMaterial/showBioMaterial.html?id=%d", bmId ), link, hover );
    }
}
