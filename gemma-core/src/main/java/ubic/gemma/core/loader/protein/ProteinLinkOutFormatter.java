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
package ubic.gemma.core.loader.protein;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.model.common.description.DatabaseEntry;

/**
 * This class concentrates functionality for formatting url links to external websites which provide protein protein
 * interaction data. It also provides functionality to format information relating to string within gemma such as
 * evidence codes.
 * For example the string url can be appended with: &amp;limit=20 which increases the number of links displayed on the string
 * page or &amp;required_score=700 to increase the confidence before displaying the interaction.
 *
 * @author ldonnison
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class ProteinLinkOutFormatter {

    /**
     * Name of parameter to pass in link to STRING to limit the number of interactions shown on string page
     */
    private static final String LIMIT_PARAMETER = "&limit=";
    /**
     * Confidence of score before displaying on STRING page
     */
    private static final String REQUIRED_CONFIDENCE = "&required_score=";
    /**
     * Gemma default confidence score to use for displaying string links that is display all links with low confidence
     */
    private static final String DEFAULT_CONFIDENCE = "150";
    /**
     * Equals sign for url
     */
    private static final String EQUALS_IN_URL = "=";
    /**
     * Plural extension
     */
    private static final String PLURAL_EXTENSION = "s";
    /**
     * spacer to use to separate evidence codes for display
     */
    private static final String EVIDENCE_SPACER = ":";

    private static final Log log = LogFactory.getLog( ProteinLinkOutFormatter.class );

    /**
     * Method to format url for string protein protein interaction. Different parameters can be queried for, such as
     * increasing number of links displayed on string page. This method allows that number to be changed.
     *
     * @param interactions of links to display on page
     * @return String appended with extra value
     */
    public String addStringInteractionsShown( String interactions ) {
        return LIMIT_PARAMETER.concat( interactions );
    }

    /**
     * Method to format url for string protein protein interaction. Different parameters can be queried for, such as
     * increasing number of links displayed on string page. This method allows that number to be changed.
     *
     * @param reqScore required confidence of score
     * @return String appended with extra value
     */
    public String addStringRequiredConfidence( String reqScore ) {
        return REQUIRED_CONFIDENCE.concat( reqScore );
    }

    /**
     * Method that creates a string url. The url is stored in the db as two parts which need merging together that it
     * url and accession id. For a protein protein interaction there is no id so instead the id has been stored as two
     * ensembl protein ids merged together. The url has been stored in db as if only protein id is being passed as such
     * to actually pass the two ids a s has to be added to the url. identifier &gt; identifiers. Thus s is added to the url.
     *
     * @param entry db entry
     * @return Formatted url
     */
    public String getBaseUrl( DatabaseEntry entry ) {
        String uri = entry.getUri();
        String accession = entry.getAccession();
        String baseUri = "";

        if ( uri != null && !uri.isEmpty() && accession != null && !accession.isEmpty() ) {
            if ( uri.endsWith( EQUALS_IN_URL ) ) {
                baseUri = ( uri.replaceFirst( EQUALS_IN_URL, PLURAL_EXTENSION + EQUALS_IN_URL ) );
            } else {
                baseUri = uri.concat( PLURAL_EXTENSION + EQUALS_IN_URL );
            }
            baseUri = baseUri.concat( accession );
        }
        return baseUri;
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

    /**
     * Convert a byte representing the evidence as stored in db to a textural display of evidence. e.g {0,1,0,0,0,1,0} &gt;
     * GeneFusion:Database
     *
     * @param bytes byte array representing evidence
     * @return Formatted text of evidence
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
                        evidenceString.append( currentEvidence.getDisplayText() ).append( EVIDENCE_SPACER );
                    }
                }
                return evidenceString.substring( 0, ( evidenceString.lastIndexOf( EVIDENCE_SPACER ) ) );
            }
            log.warn( "The byte array provided was not the correct size for the protein protein interaction" );

        } catch ( Exception e ) {
            // should really be a more specific exception
            throw new RuntimeException( "Bit Vector representing evidence codes for proteins was at error " + e );
        }
        return evidenceString.toString();
    }

    /**
     * Method that creates a formatted STRING url with extra parameters appended
     *
     * @param entry        Database entry representing protein protein interaction
     * @param interactions number of interactions to show on string page
     * @param reqScore     required confidence of score
     * @return String formatted url.
     */
    public String getStringProteinProteinInteractionLinkFormatted( DatabaseEntry entry, String interactions,
            String reqScore ) {
        String finalUrl = getBaseUrl( entry );
        if ( interactions != null ) {
            finalUrl = finalUrl.concat( addStringInteractionsShown( interactions ) );
        }
        if ( reqScore != null ) {
            finalUrl = finalUrl.concat( addStringRequiredConfidence( reqScore ) );
        }

        return finalUrl;
    }

    /**
     * Get the default STRING url for for gemma, which sets the confidence level low.
     *
     * @param entry Database entry representing protein protein interaction
     * @return String formatted url.
     */
    public String getStringProteinProteinInteractionLinkGemmaDefault( DatabaseEntry entry ) {

        String finalUrl = getBaseUrl( entry );
        if ( finalUrl != null && !finalUrl.isEmpty() ) {
            finalUrl = finalUrl.concat( addStringRequiredConfidence( DEFAULT_CONFIDENCE ) );
        }
        return finalUrl;
    }

}
