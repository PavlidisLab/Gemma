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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.loader.util.parser.ExternalDatabaseUtils;
import ubic.gemma.loader.util.parser.RecordParser;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.SequenceType;

/**
 * FASTA sequence file parser. Results are in BioSequence objects. Parsing a single record
 * 
 * @author pavlidis
 * @version $Id$
 */
public class FastaParser extends RecordParser<BioSequence> {

    private static final String NIA_HEADER_REGEX = ">?H\\d{4}\\w\\d{2}-\\d.*";

    Pattern pattern;

    private Collection<BioSequence> results = new ArrayList<BioSequence>();

    public FastaParser() {
        String patternStr = "^(.*)$";
        pattern = Pattern.compile( patternStr, Pattern.MULTILINE );
    }

    @Override
    public Object parseOneRecord( String record ) {

        if ( StringUtils.isBlank( record ) ) return null;

        Matcher matcher = pattern.matcher( record );

        Collection<BioSequence> bioSequences = parseHeader( matcher );

        if ( bioSequences.size() == 0 ) {
            return null;
        }

        StringBuilder sequence = new StringBuilder();
        while ( matcher.find() ) {
            // skip comments.
            if ( matcher.group( 1 ).startsWith( ";" ) ) continue;

            sequence.append( matcher.group( 1 ) );
        }

        if ( sequence.length() == 0 ) {
            return null;
        }

        for ( BioSequence bioSequence : bioSequences ) {
            bioSequence.setLength( new Long( sequence.length() ) );
            bioSequence.setIsApproximateLength( false );
            bioSequence.setSequence( sequence.toString() );
        }
        return bioSequences;

    }

    /**
     * Recognizes Defline format as described at {@link http://en.wikipedia.org/wiki/Fasta_format#Sequence_identifiers}.
     * Our amendments: FIXME: recognize multi-line headers separated by ^A.(used for redundant sequences)
     * <p>
     * FIXME: parsing of more obscure (to us) headers might not be complete.
     * 
     * @param bioSequence
     */
    private Collection<BioSequence> parseHeader( Matcher matcher ) {
        Collection<BioSequence> bioSequences = new HashSet<BioSequence>();
        boolean gotSomething = matcher.find();

        if ( !gotSomething ) {
            throw new IllegalArgumentException( "Invalid FASTA record" );
        }

        String header = matcher.group( 1 );

        String[] recordHeaders = StringUtils.split( header, '>' );

        boolean keep = false;
        for ( String rheader : recordHeaders ) {

            BioSequence bioSequence = BioSequence.Factory.newInstance();
            bioSequence.setName( rheader );

            /*
             * Look for either a '|' or a ':'. Allow for the possibility of ':' and then '|' occuring; use whichever
             * comes first.
             */
            int firstPipe = rheader.indexOf( '|' );
            int firstColon = rheader.indexOf( ':' );

            if ( firstPipe > 0 && ( firstColon < 0 || firstPipe < firstColon ) ) {
                keep = parseDeflineHeader( bioSequence, rheader );
            } else if ( firstColon > 0 ) {
                keep = parseAffyHeader( bioSequence, rheader );
            } else if ( rheader.matches( NIA_HEADER_REGEX ) ) {
                keep = parseNIA( bioSequence, rheader );
            } else {
                // just treat the whole header as the sequence name.
                keep = parseDeflineHeader( bioSequence, rheader );
            }

            if ( keep ) bioSequences.add( bioSequence );
        }
        return bioSequences;
    }

    /**
     * <pre>
     *        Affymetrix targets or collapsed sequence     target:array:probeset;
     *        Affymetrix &quot;style&quot; file            target:probename
     *        Affymetrix probe                             probe:array:probeset:xcoord:ycoord; Interrogation_Position=XXXX; Antisense;
     *        Affymetrix consensus/exemplar                exemplar:array:probeset; gb|accession; gb:accession /DEF=Homo sapiens metalloprotease-like, disintegrin-like, cysteine-rich protein 2 delta (ADAM22) mRNA, alternative splice product, complete cds.  /FEA=mRNA /GEN=ADAM22 /PROD=metalloprotease-like,
     *        Affymetrix-like format                       array:probe or other string containing ':'.
     * </pre>
     * 
     * @param bioSequence
     * @param header
     * @return
     */
    private boolean parseAffyHeader( BioSequence bioSequence, String header ) {
        // affymetrix format
        String[] split = StringUtils.split( header, ":;" );

        String firstTag = StringUtils.removeStart( split[0], ">" );
        if ( firstTag.equals( "probe" ) ) {
            bioSequence.setName( split[1] + ":" + split[2] + ":" + split[3] + ":" + split[4] );
        } else if ( firstTag.equals( "target" ) ) {
            // split[1] = array name or probe name
            // split[2] = probe name
            if ( split.length > 2 ) {
                bioSequence.setName( split[2] );
            } else {
                bioSequence.setName( split[1] );
            }

        } else if ( firstTag.equals( "exemplar" ) ) {
            bioSequence.setName( split[1] + ":" + split[2] );
            bioSequence.setDescription( split[3] );
        } else {
            // This is the case if the xxxx:xxxx format is used on non-affy
            bioSequence.setName( StringUtils.removeStart( header, ">" ) );
            return true;
        }

        for ( String string : split ) {

            string = StringUtils.strip( string );

            // fill in the sequence database entry
            if ( string.startsWith( "gb|" ) || string.startsWith( "gb:" ) ) {
                String[] splits = StringUtils.split( string, ":|" );
                String genbankAcc = splits[1];
                DatabaseEntry genbank = ExternalDatabaseUtils.getGenbankAccession( genbankAcc );
                bioSequence.setName( genbank.getAccession() );
                bioSequence.setSequenceDatabaseEntry( genbank );
                if ( log.isDebugEnabled() )
                    log.debug( "Got genbank accession " + genbankAcc + " for " + bioSequence.getName() );
                break;
            }

        }
        return true;
    }

    /**
     * The following formats are supported
     * <ul>
     * <li>GenBank: gi|gi-number|gb|accession|locus
     * <li>EMBL Data Library : gi|gi-number|emb|accession|locus
     * <li>DDBJ, DNA Database of Japan : gi|gi-number|dbj|accession|locus
     * <li>NBRF PIR : pir||entry
     * <li>Protein Research Foundation : prf||name
     * <li>SWISS-PROT : sp|accession|name
     * <li>Brookhaven Protein Data Bank (1) : pdb|entry|chain
     * <li>Brookhaven Protein Data Bank (2) : entry:chain|PDBID|CHAIN|SEQUENCE
     * <li>Patents : pat|country|number
     * <li>GenInfo Backbone Id bbs|number
     * <li>General database identifier : gnl|database|identifier
     * <li>NCBI Reference Sequence : ref|accession|locus
     * <li>Local Sequence identifier : lcl|identifier
     * <li>NIA 15k and 7k sets : H[0-9A-Z]{1-9}-\d | alternate (example: &gt;H4002F12-5 )
     * <li>Generic: probeid
     * </ul>
     * 
     * @param bioSequence
     * @param header
     * @return
     */
    private boolean parseDeflineHeader( BioSequence bioSequence, String header ) {
        // one of the genbank formats.
        String[] split = StringUtils.splitPreserveAllTokens( header, "|;" );

        String firstTag = split[0];

        // assert firstTag.startsWith( ">" );
        // assert firstTag.length() > 1;
        firstTag = StringUtils.removeStart( firstTag, ">" );

        // FIXME check for array lengths, throw illegal argument exceptions.

        if ( firstTag.equals( "gi" ) ) {
            bioSequence.setDescription( split[4] );
            String genbankAcc = split[3]; // with version number, possibly
            DatabaseEntry genbank = ExternalDatabaseUtils.getGenbankAccession( genbankAcc );
            bioSequence.setName( genbank.getAccession() ); // without version number.
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
        } else if ( firstTag.matches( NIA_HEADER_REGEX ) ) {
            return parseNIA( bioSequence, header );
        } else {
            // generic.
            bioSequence.setName( split[0] );
            if ( split.length > 1 ) bioSequence.setDescription( split[1] );
            // log.warn( "Defline-style FASTA header in unrecognized format, started with " + firstTag );
            // return false;
        }
        return true;
    }

    /**
     * This is a special case, but these are used on microarrays.
     * 
     * @param bioSequence
     * @param header
     * @return
     */
    private boolean parseNIA( BioSequence bioSequence, String header ) {
        String firstTag = StringUtils.removeStart( header, ">" );
        if ( firstTag.contains( "alternate" ) ) {
            log.info( header + ": alternate sequence, skipping" );
            return false;
        }
        String[] subFields = firstTag.split( "-" );
        bioSequence.setName( subFields[0] );
        bioSequence.setDescription( "NIA sequence" );
        bioSequence.setType( SequenceType.EST );
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void addResult( Object obj ) {
        results.addAll( ( Collection<BioSequence> ) obj );

    }

    @Override
    public Collection<BioSequence> getResults() {
        return results;
    }
}
