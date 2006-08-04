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
package ubic.gemma.loader.genome;

import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.loader.util.parser.ExternalDatabaseUtils;
import ubic.gemma.loader.util.parser.RecordParser;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * FASTA sequence file parser. Results are in BioSequence objects.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class FastaParser extends RecordParser {
    Pattern pattern;

    private Collection<BioSequence> results = new HashSet<BioSequence>();

    public FastaParser() {
        String patternStr = "^(.*)$";
        pattern = Pattern.compile( patternStr, Pattern.MULTILINE );
    }

    @Override
    public Object parseOneRecord( String record ) {

        if ( StringUtils.isBlank( record ) ) return null;

        Matcher matcher = pattern.matcher( record );

        BioSequence bioSequence = BioSequence.Factory.newInstance();

        parseHeader( matcher, bioSequence );

        StringBuilder sequence = new StringBuilder();
        while ( matcher.find() ) {
            // skip comments.
            if ( matcher.group( 1 ).startsWith( ";" ) ) continue;

            sequence.append( matcher.group( 1 ) );
        }

        if ( sequence.length() == 0 ) {
            return null;
        }

        bioSequence.setLength( new Long( sequence.length() ) );
        bioSequence.setIsApproximateLength( false );
        bioSequence.setSequence( sequence.toString() );
        return bioSequence;

    }

    /**
     * Recognizes Defline format as described at {@link http://en.wikipedia.org/wiki/Fasta_format#Sequence_identifiers}.
     * 
     * <pre>
     *                                                                             GenBank                           gi|gi-number|gb|accession|locus
     *                                                                             EMBL Data Library                 gi|gi-number|emb|accession|locus
     *                                                                             DDBJ, DNA Database of Japan       gi|gi-number|dbj|accession|locus
     *                                                                             NBRF PIR                          pir||entry
     *                                                                             Protein Research Foundation       prf||name
     *                                                                             SWISS-PROT                        sp|accession|name
     *                                                                             Brookhaven Protein Data Bank (1)  pdb|entry|chain
     *                                                                             Brookhaven Protein Data Bank (2)  entry:chain|PDBID|CHAIN|SEQUENCE
     *                                                                             Patents                           pat|country|number 
     *                                                                             GenInfo Backbone Id               bbs|number 
     *                                                                             General database identifier       gnl|database|identifier
     *                                                                             NCBI Reference Sequence           ref|accession|locus
     *                                                                             Local Sequence identifier         lcl|identifier
     * </pre>
     * 
     * Our amendments:
     * 
     * <pre>
     *                                                                             Affymetrix targets or collapsed sequence     target:array:probeset;
     *                                                                             Affymetrix probe                  probe:array:probeset:xcoord:ycoord; Interrogation_Position=XXXX; Antisense;
     *                                                                             Affymetrix consensus/exemplar     exemplar:array:probeset; gb|accession; gb:accession /DEF=Homo sapiens metalloprotease-like, disintegrin-like, cysteine-rich protein 2 delta (ADAM22) mRNA, alternative splice product, complete cds.  /FEA=mRNA /GEN=ADAM22 /PROD=metalloprotease-like,
     * </pre>
     * 
     * FIXME: recognize multi-line headers separated by ^A.
     * <p>
     * FIXME: parsing of more obscure (to us) headers might not be complete.
     * 
     * @param bioSequence
     */
    private void parseHeader( Matcher matcher, BioSequence bioSequence ) {
        boolean gotSomething = matcher.find();

        if ( !gotSomething ) {
            throw new IllegalArgumentException( "Invalid FASTA record" );
        }

        String header = matcher.group( 1 );

        bioSequence.setName( header );

        /*
         * Look for either a '|' or a ':'. Allow for the possibility of ':' and then '|' occuring; use whichever comes
         * first.
         */

        int firstPipe = header.indexOf( '|' );
        int firstColon = header.indexOf( ':' );

        if ( firstPipe > 0 && ( firstColon < 0 || firstPipe < firstColon ) ) {
            // one of the genbank formats.
            String[] split = StringUtils.splitPreserveAllTokens( header, "|;" );

            String firstTag = split[0];

            assert firstTag.startsWith( ">" );
            assert firstTag.length() > 1;
            firstTag = StringUtils.removeStart( firstTag, ">" );

            // FIXME check for array lengths, throw illegal argument exceptions.

            if ( firstTag.equals( "gi" ) ) {
                bioSequence.setName( split[3] );
                bioSequence.setDescription( split[4] );

                String genbankAcc = split[3];
                DatabaseEntry genbank = ExternalDatabaseUtils.getGenbankAccession( genbankAcc );
                bioSequence.setSequenceDatabaseEntry( genbank );

            } else if ( firstTag.equals( "pir" ) ) {
                bioSequence.setName( split[1] );
            } else if ( firstTag.equals( "sp" ) ) {
                bioSequence.setName( split[1] );
                bioSequence.setDescription( split[2] );
            } else if ( firstTag.equals( "ref" ) ) {
                bioSequence.setName( split[1] );
                bioSequence.setDescription( split[2] );
            } else if ( firstTag.equals( "lcl" ) ) {
                bioSequence.setName( split[1] );
            } else if ( firstTag.equals( "pdb" ) ) {
                bioSequence.setName( split[1] );
                bioSequence.setDescription( split[2] );
            } else if ( firstTag.equals( "gnl" ) ) {
                bioSequence.setName( split[2] );
            } else if ( firstTag.equals( "entry:chain" ) ) {
                bioSequence.setName( split[1] );
            } else {
                throw new IllegalArgumentException( "Defline-style FASTA header in unrecognized format, started with "
                        + firstTag );
            }
        } else if ( firstColon > 0 ) {
            // affymetrix format
            String[] split = StringUtils.split( header, ":;" );

            String firstTag = StringUtils.removeStart( split[0], ">" );
            if ( firstTag.equals( "probe" ) ) {
                bioSequence.setName( split[1] + ":" + split[2] + ":" + split[3] + ":" + split[4] );
            } else if ( firstTag.equals( "target" ) ) {
                // split[1] = array name
                // split[2] = probe name
                String probeName = split[2]; // '.replaceFirst( ";", "" ); // Some files have this.
                bioSequence.setName( split[1] + ":" + probeName );
            } else if ( firstTag.equals( "exemplar" ) ) {
                bioSequence.setName( split[1] + ":" + split[2] );
                bioSequence.setDescription( split[3] );
            } else {
                throw new IllegalArgumentException(
                        "Affymetrix-style FASTA header in unrecognized format, started with " + firstTag );
            }

            for ( String string : split ) {

                string = StringUtils.strip( string );

                // fill in the sequence database entry
                if ( string.startsWith( "gb|" ) || string.startsWith( "gb:" ) ) {
                    String[] splits = StringUtils.split( string, ":|" );
                    String genbankAcc = splits[1];
                    DatabaseEntry genbank = ExternalDatabaseUtils.getGenbankAccession( genbankAcc );
                    bioSequence.setSequenceDatabaseEntry( genbank );
                    if ( log.isDebugEnabled() )
                        log.debug( "Got genbank accession " + genbankAcc + " for " + bioSequence.getName() );
                    break;
                }

            }

        } else {
            throw new IllegalArgumentException( "FASTA header in unrecognized format." );
        }

    }

    @Override
    protected void addResult( Object obj ) {
        results.add( ( BioSequence ) obj );

    }

    @Override
    public Collection<BioSequence> getResults() {
        return results;
    }
}
