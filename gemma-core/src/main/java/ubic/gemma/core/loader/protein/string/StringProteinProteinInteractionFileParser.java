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
package ubic.gemma.core.loader.protein.string;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.protein.StringProteinInteractionEvidenceCodeEnum;
import ubic.gemma.core.loader.protein.string.model.StringProteinProteinInteraction;
import ubic.gemma.core.loader.util.parser.BasicLineParser;
import ubic.gemma.core.loader.util.parser.FileFormatException;
import ubic.gemma.model.genome.Taxon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Class that is responsible for parsing the string protein protein interaction file (protein.links.detailed.txt).
 * String has the following line format:
 * protein1 protein2 neighborhood fusion co-occurrence coexpression experimental database text-mining combined_score.
 * The first two fields are strings and represent ensembl protein ids, the other 8 fields represent evidence for the
 * interactions. Currently no logic connected to the evidence and any evidence is taken as is. The file is large and
 * contains over 600 taxon as of version 8.2, the file is filtered based on ncbi id which is appended to the start of
 * the protein1 and protein 2. Thus filtering involves only parsing those lines that contain taxon of interest.
 *
 * @author ldonnison
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
@Deprecated
public class StringProteinProteinInteractionFileParser extends BasicLineParser<StringProteinProteinInteraction> {

    /**
     * String uses space as the delimiter
     */
    private static final char FIELD_DELIMITER = ' ';
    /**
     * String file format which has 10 columns
     */
    private static final int STRING_PROTEIN_PROTEIN_INTERACTION_FIELDS_PER_ROW = 10;
    /**
     * The Collection of StringProteinProteinInteraction of taxon that are of interest note
     * StringProteinProteinInteraction has hashCode method overridden as HashSet uses this method to test for equality
     */
    final Collection<StringProteinProteinInteraction> proteinProteinInteractions = new HashSet<>();
    /**
     * Taxon of interest in the string file
     */
    private Collection<Taxon> taxa = new ArrayList<>();

    /**
     * Typical line of string file is of the following format:
     * <pre>
     * 882.DVU0001 882.DVU0002 707 0 0 0 0 0 172 742
     * </pre>
     * 882.DVU0001 and 882.DVU0002 refer to protein 1 and protein2 Note the 882 is the ncbi taxon id, the other part is
     * an external id (ensembl). Method takes the array representing a line of string file and creates a
     * StringProteinProteinInteraction object.
     *
     * @param fields Line split on delimiter
     * @return StringProteinProteinInteraction value object.
     */
    public StringProteinProteinInteraction createStringProteinProteinInteraction( String[] fields ) {
        // validate
        if ( fields == null ) {
            return null;
        }
        if ( fields[0] == null || fields[1] == null || fields[0].isEmpty() || fields[1].isEmpty() ) {
            return null;
        }

        String[] protein1AndTaxa = StringUtils.split( fields[0], "." );
        int taxonIdProtein1 = Integer.parseInt( protein1AndTaxa[0] );

        String[] protein2AndTaxa = StringUtils.split( fields[1], "." );
        int taxonIdProtein2 = Integer.parseInt( protein2AndTaxa[0] );

        // Check that the two proteins taxa match that is the taxon appended to protein name match
        if ( taxonIdProtein1 != taxonIdProtein2 ) {
            throw new FileFormatException(
                    "Protein 1 " + fields[0] + " protein 2  " + fields[1] + " do not contain matching taxons" );
        }
        // taxon not supported skip it
        if ( !( this.getNcbiValidTaxon() ).contains( taxonIdProtein1 ) ) {
            return null;
        }

        // always ensure that protein 1 and protein 2 are set same alphabetical order makes matching much easier later
        // hashcode equality method relies on them being in consistent order.
        // use hashcode as mixed alphanumeric code
        Integer protein1Infile = fields[0].hashCode();
        Integer protein2InFile = fields[1].hashCode();
        StringProteinProteinInteraction stringProteinProteinInteraction;

        if ( protein1Infile.compareTo( protein2InFile ) < 0 ) {
            stringProteinProteinInteraction = new StringProteinProteinInteraction( fields[0], fields[1] );
        } else {
            stringProteinProteinInteraction = new StringProteinProteinInteraction( fields[1], fields[0] );
        }

        stringProteinProteinInteraction.setNcbiTaxonId( taxonIdProtein1 );

        // validate the line make sure these fields are numeric
        for ( int i = 2; i < fields.length; i++ ) {
            if ( !StringUtils.isNumeric( fields[i] ) ) {
                throw new FileFormatException( "This line does not contain valid number " );
            }
        }

        stringProteinProteinInteraction
                .addEvidenceCodeScoreToMap( StringProteinInteractionEvidenceCodeEnum.NEIGHBORHOOD,
                        Integer.valueOf( fields[2] ) );
        stringProteinProteinInteraction.addEvidenceCodeScoreToMap( StringProteinInteractionEvidenceCodeEnum.GENEFUSION,
                Integer.valueOf( fields[3] ) );
        stringProteinProteinInteraction.addEvidenceCodeScoreToMap( StringProteinInteractionEvidenceCodeEnum.COOCCURENCE,
                Integer.valueOf( fields[4] ) );
        stringProteinProteinInteraction
                .addEvidenceCodeScoreToMap( StringProteinInteractionEvidenceCodeEnum.COEXPRESSION,
                        Integer.valueOf( fields[5] ) );
        stringProteinProteinInteraction
                .addEvidenceCodeScoreToMap( StringProteinInteractionEvidenceCodeEnum.EXPERIMENTAL,
                        Integer.valueOf( fields[6] ) );
        stringProteinProteinInteraction.addEvidenceCodeScoreToMap( StringProteinInteractionEvidenceCodeEnum.DATABASE,
                Integer.valueOf( fields[7] ) );
        stringProteinProteinInteraction.addEvidenceCodeScoreToMap( StringProteinInteractionEvidenceCodeEnum.TEXTMINING,
                Integer.valueOf( fields[8] ) );

        stringProteinProteinInteraction.setCombined_score( Double.valueOf( fields[9] ) );
        return stringProteinProteinInteraction;
    }

    @Override
    public Collection<StringProteinProteinInteraction> getResults() {
        return proteinProteinInteractions;
    }

    /**
     * Method to add a StringProteinProteinInteraction the map objects.
     *
     * @param obj value object representing parsed line
     */
    @Override
    protected void addResult( StringProteinProteinInteraction obj ) {
        proteinProteinInteractions.add( obj );
    }

    /**
     * Getter for taxon that are to be selected for in the string file
     *
     * @return taxon to be parsed
     */
    @SuppressWarnings("unused") // Possible external use
    public Collection<Taxon> getTaxa() {
        return taxa;
    }

    /**
     * Setter for the taxon that are to be selected for in the string file
     *
     * @param taxa to be parsed
     */
    public void setTaxa( Collection<Taxon> taxa ) {
        this.taxa = taxa;
    }

    /**
     * Parse a string file line into an array representing the components, on successful validation create a
     * StringProteinProteinInteraction value object.
     *
     * @param line The line to parse
     * @return StringProteinProteinInteraction the value object.
     */
    @Override
    public StringProteinProteinInteraction parseOneLine( String line ) {

        // header line skip or empty line
        if ( line.startsWith( "protein" ) || line.isEmpty() ) {
            return null;
        }

        String[] fields = StringUtils
                .splitPreserveAllTokens( line, StringProteinProteinInteractionFileParser.FIELD_DELIMITER );

        if ( fields.length
                != StringProteinProteinInteractionFileParser.STRING_PROTEIN_PROTEIN_INTERACTION_FIELDS_PER_ROW ) {
            log.info( "check file format" );
            throw new FileFormatException(
                    "Line + " + line + " is not in the right format: has " + fields.length + " fields, expected "
                            + StringProteinProteinInteractionFileParser.STRING_PROTEIN_PROTEIN_INTERACTION_FIELDS_PER_ROW );
        }

        try {
            return this.createStringProteinProteinInteraction( fields );

        } catch ( NumberFormatException | FileFormatException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * Method to generate a collection of ncbi ids for the taxon which are to be used.
     *
     * @return Collection of integers representing the ncbis to use.
     */
    private Collection<Integer> getNcbiValidTaxon() {
        Collection<Integer> taxaNcbi = new HashSet<>();
        for ( Taxon taxon : this.taxa ) {
            taxaNcbi.add( taxon.getNcbiId() );

            if ( taxon.getSecondaryNcbiId() != null )
                taxaNcbi.add( taxon.getSecondaryNcbiId() );
        }
        return taxaNcbi;
    }

}
