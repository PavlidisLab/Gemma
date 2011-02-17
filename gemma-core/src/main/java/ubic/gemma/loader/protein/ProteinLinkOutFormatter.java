/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.loader.protein;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.description.DatabaseEntry;

/**
 * This class concentrates functionality for formating url links to external websites which provide protein protein
 * interaction data. It also provides functionality to format information relating to string within gemma such as
 * evidence codes.
 * <p>
 * For example the string url can be appended with: &limit=20 which increases the number of links dispayed on the string
 * page or &required_score=700 to increase the confidence before displaying the interacton.
 * 
 * @author ldonnison
 * @version $Id$
 */
public class ProteinLinkOutFormatter {

    /** Name of parameter to pass in link to STRING to limit the number of interactions shown on string page */
    private static final String LIMITPARAMETER = "&limit=";

    /** Confidence of score before displaying on STRING page */
    private static final String REQUIREDCONFIDENCE = "&required_score=";

    /** Gemma default confidence score to use for displaying string links that is display all links with low confidence */
    private static final String DEFAULTCONFIDENCE = "150";

    /** Equals sign for url */
    private static final String EQUALSINURL = "=";

    /** Plural extension */
    private static final String PLURALEXTENSION = "s";

    /** spacer to use to seperate evidence codes for display */
    private static final String EVIDENCESPACER = ":";

    private static Log log = LogFactory.getLog( ProteinLinkOutFormatter.class );

    /**
     * Method that creates a string url. The url is stored in the db as two parts which need merging together that it
     * url and accession id. For a protein protein interaction there is no id so instead the id has been strored as two
     * ensembl protein ids merged together. The url has been stored in db as if only protein id is being passed as such
     * to actually pass the two ids a s has to be added to the url. identifier >identifiers. Thus s is added to the url.
     * 
     * @param baseUrl
     * @return Formated url
     */
    public String getBaseUrl( DatabaseEntry entry ) {
        String uri = entry.getUri();
        String accession = entry.getAccession();
        String baseUri = "";

        if ( uri != null && !uri.isEmpty() && accession != null && !accession.isEmpty() ) {
            if ( uri.endsWith( EQUALSINURL ) ) {
                baseUri = ( uri.replaceFirst( EQUALSINURL, PLURALEXTENSION + EQUALSINURL ) );
            } else {
                baseUri = uri.concat( PLURALEXTENSION + EQUALSINURL );
            }
            baseUri = baseUri.concat( accession );
        }
        return baseUri;
    }

    /**
     * Get the default STRING url for for gemma, which sets the confidence level low.
     * 
     * @param entry Database entry representing protein protein interaction
     * @return String formated url.
     */
    public String getStringProteinProteinInteractionLinkGemmaDefault( DatabaseEntry entry ) {

        String finalUrl = getBaseUrl( entry );
        if ( finalUrl != null && !finalUrl.isEmpty() ) {
            finalUrl = finalUrl.concat( addStringRequiredConfidence( DEFAULTCONFIDENCE ) );
        }
        return finalUrl;
    }

    /**
     * Method that creates a formatted STRING url with extra parameters appended
     * 
     * @param entry Database entry representing protein protein interaction
     * @return String formated url.
     */
    public String getStringProteinProteinInteractionLinkFormatted( DatabaseEntry entry,
            String numberOfInteractionsToShowOnStringPage, String requiredConfidenceOfScore ) {
        String finalUrl = getBaseUrl( entry );
        if ( numberOfInteractionsToShowOnStringPage != null ) {
            finalUrl = finalUrl.concat( addStringInteractionsShown( numberOfInteractionsToShowOnStringPage ) );
        }
        if ( requiredConfidenceOfScore != null ) {
            finalUrl = finalUrl.concat( addStringRequiredConfidence( requiredConfidenceOfScore ) );
        }

        return finalUrl;
    }

    /**
     * Method to format url for string protein protein interaction. Different parameters can be queried for, such as
     * increasing number of links displayed on string page. This method allows that number to be changed.
     * 
     * @param baseUrl reprsesenting base string url
     * @param Number of links to display on page
     * @return String appended with extra value
     */
    public String addStringInteractionsShown( String numberOfInteractionsToShowOnStringPage ) {
        return LIMITPARAMETER.concat( numberOfInteractionsToShowOnStringPage );
    }

    /**
     * Method to format url for string protein protein interaction. Different parameters can be queried for, such as
     * increasing number of links displayed on string page. This method allows that number to be changed.
     * 
     * @param baseUrl reprsesenting base string url
     * @param Number of links to display on page
     * @return String appended with extra value
     */
    public String addStringRequiredConfidence( String requiredConfidenceOfScore ) {
        return REQUIREDCONFIDENCE.concat( requiredConfidenceOfScore );
    }

    /**
     * Convert a byte representing the evidence as stored in db to a textural display of evidence. e.g {0,1,0,0,0,1,0} >
     * GeneFusion:Database
     * 
     * @param bytes byte array representing evidence
     * @return Formated text of evidence
     */
    public String getEvidenceDisplayText( byte[] bytes ) {

        StringBuffer evidenceString;
        try {
            evidenceString = new StringBuffer();
            if ( bytes != null && bytes.length == StringProteinInteractionEvidenceCodeEnum.values().length ) {
                for ( StringProteinInteractionEvidenceCodeEnum currentEvidence : StringProteinInteractionEvidenceCodeEnum
                        .values() ) {
                    // if the byte at that particular position is 1 then that means that there is evidence
                    if ( ( bytes[currentEvidence.getPositionInArray()] ) == 1 ) {
                        evidenceString.append( currentEvidence.getDisplayText() ).append( EVIDENCESPACER );
                    }
                }
                return evidenceString.substring( 0, ( evidenceString.lastIndexOf( EVIDENCESPACER ) ) );
            } else {
                log.warn( "The byte array provided was not the correct size for the protein protein interaction" );
            }
        } catch ( Exception e ) {
            // should really be a more specific exception
            throw new RuntimeException( "Bit Vector representing evidence codes for proteins was at error " + e );
        }
        return evidenceString.toString();
    }

    /**
     * Confidence score as parsed from string file is not a percentage instead a number e.g. 150 in percentage that is
     * 0.150
     * 
     * @param confidenceScore As parsed by string file
     * @return Formatted confidence percentage as displayed by string
     */
    public String getConfidenceScoreAsPercentage( Double confidenceScore ) {
        String confidenceScoreAsDisplayedInString = null;
        if ( confidenceScore != null && confidenceScore != 0 ) {
            confidenceScoreAsDisplayedInString = Double.toString( confidenceScore / 1000 );
        }
        return confidenceScoreAsDisplayedInString;
    }

}
