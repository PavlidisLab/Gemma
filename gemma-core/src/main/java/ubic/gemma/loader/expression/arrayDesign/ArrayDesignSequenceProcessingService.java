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
package ubic.gemma.loader.expression.arrayDesign;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.report.ArrayDesignReportService;
import ubic.gemma.analysis.sequence.SequenceManipulation;
import ubic.gemma.loader.genome.FastaCmd;
import ubic.gemma.loader.genome.FastaParser;
import ubic.gemma.loader.genome.ProbeSequenceParser;
import ubic.gemma.loader.genome.SimpleFastaCmd;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.biosequence.PolymerType;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.persistence.Persister;

/**
 * Handles collapsing the sequences, attaching sequences to DesignElements, either from provided input or via a fetch.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Service
public class ArrayDesignSequenceProcessingService {

    /**
     * After seeing more than this number of compositeSequences lacking sequences we don't give a detailed warning.
     */
    private static final int MAX_NUM_WITH_NO_SEQUENCE_FOR_DETAILED_WARNINGS = 20;

    private static final int BATCH_SIZE = 100;

    /**
     * When checking a BLAST database for sequences, we stop after checking Genbank accessions versions up to this value
     * (e.g, AA22930.1)
     */
    private static final int MAX_VERSION_NUMBER = 20;

    private static Log log = LogFactory.getLog( ArrayDesignSequenceProcessingService.class.getName() );

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private Persister persisterHelper;

    @Autowired
    private BioSequenceService bioSequenceService;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    ArrayDesignReportService arrayDesignReportService;

    /**
     * @param nameMap
     * @param sequence
     */
    private void addToMaps( Map<String, BioSequence> gbIdMap, Map<String, BioSequence> nameMap, BioSequence sequence ) {
        nameMap.put( this.deMangleProbeId( sequence.getName() ), sequence );

        if ( sequence.getSequenceDatabaseEntry() != null ) {
            gbIdMap.put( sequence.getSequenceDatabaseEntry().getAccession(), sequence );
        } else {
            if ( log.isTraceEnabled() ) log.trace( "No sequence database entry for " + sequence.getName() );
        }
    }

    /**
     * Associate sequences with an array design.
     * 
     * @param designElements
     * @param sequences, for Affymetrix these should be the Collapsed probe sequences.
     * @throws IOException
     */
    public void assignSequencesToDesignElements( Collection<CompositeSequence> designElements,
            Collection<BioSequence> sequences ) {

        Map<String, BioSequence> nameMap = new HashMap<String, BioSequence>();
        for ( BioSequence sequence : sequences ) {
            nameMap.put( this.deMangleProbeId( sequence.getName() ), sequence );
        }

        int numNotFound = 0;
        for ( CompositeSequence designElement : designElements ) {
            if ( !nameMap.containsKey( designElement.getName() ) ) {
                log.debug( "No sequence matches " + designElement.getName() );
                numNotFound++;
                continue;
            }

            designElement.setBiologicalCharacteristic( nameMap.get( designElement.getName() ) );

        }

        log.info( sequences.size() + " sequences processed for " + designElements.size() + " design elements" );
        if ( numNotFound > 0 ) {
            log.warn( numNotFound + " probes had no matching sequence" );
        }
    }

    /**
     * Associate sequences with an array design. It is assumed that the name of the sequences can be matched to the name
     * of a design element.
     * 
     * @param designElements
     * @param fastaFile
     * @throws IOException
     */
    public void assignSequencesToDesignElements( Collection<CompositeSequence> designElements, File fastaFile )
            throws IOException {

        FastaParser fp = new FastaParser();
        fp.parse( fastaFile );
        Collection<BioSequence> sequences = fp.getResults();
        log.debug( "Parsed " + sequences.size() + " sequences" );

        assignSequencesToDesignElements( designElements, sequences );
    }

    /**
     * Associate sequences with an array design. It is assumed that the name of the sequences can be matched to the name
     * of a design element. Provided for testing purposes.
     * 
     * @param designElements
     * @param fastaFile
     * @throws IOException
     */
    protected void assignSequencesToDesignElements( Collection<CompositeSequence> designElements, InputStream fastaFile )
            throws IOException {

        FastaParser fp = new FastaParser();
        fp.parse( fastaFile );
        Collection<BioSequence> sequences = fp.getResults();
        log.debug( "Parsed " + sequences.size() + " sequences" );

        assignSequencesToDesignElements( designElements, sequences );
    }

    /**
     * When the probe id is in the format ArrayName:ProbeId, just return the ProbeId. For anything else return the
     * entire string.
     * 
     * @param probeId
     * @return
     */
    private String deMangleProbeId( String probeId ) {
        String[] toks = StringUtils.split( probeId, ":" );
        if ( toks.length > 1 ) {
            return toks[1];
        }
        return probeId;
    }

    private void flushBuffer( Collection<BioSequence> bioSequences, Collection<BioSequence> sequenceBuffer,
            Map<String, CompositeSequence> csBuffer ) {
        Collection<BioSequence> newOnes = bioSequenceService.findOrCreate( sequenceBuffer );
        bioSequences.addAll( newOnes );
        for ( BioSequence sequence : newOnes ) {
            CompositeSequence cs = csBuffer.get( sequence.getName() );
            assert cs != null;
            cs.setBiologicalCharacteristic( sequence );
        }
        csBuffer.clear();
        sequenceBuffer.clear();
    }

    /**
     * Used to check if an IMAGE clone exists to use for an accession. If the IMAGE clone is used instead, we update the
     * composite sequence.
     * 
     * @param cs
     * @return
     */
    private String getAccession( CompositeSequence cs ) {
        BioSequence bs = cs.getBiologicalCharacteristic();
        if ( bs.getSequenceDatabaseEntry() == null ) {
            return null;
        }
        bs = this.bioSequenceService.thaw( bs );
        return bs.getSequenceDatabaseEntry().getAccession();
    }

    /**
     * @param accessionsToFetch
     * @param found
     * @return
     */
    private Collection<String> getUnFound( Collection<String> accessionsToFetch, Map<String, BioSequence> found ) {
        Collection<String> notFound = new HashSet<String>();
        for ( String accession : accessionsToFetch ) {
            if ( !found.containsKey( accession ) ) {
                notFound.add( accession );
            }
        }
        return notFound;
    }

    /**
     * @param arrayDesign
     * @param accessionsToFetch
     * @param sequenceProvided
     * @param noSequence
     */
    private void informAboutFetchListResults( ArrayDesign arrayDesign, Map<String, BioSequence> accessionsToFetch,
            int sequenceProvided, int noSequence ) {
        log.info( "Array Design has " + accessionsToFetch.size() + " accessions to fetch for "
                + arrayDesign.getCompositeSequences().size() + " compositeSequences" );
        log.info( sequenceProvided + " had sequences already and will not be replaced" );
        log.info( noSequence + " have no BioSequence association at all and will not be processed further." );
    }

    /**
     * @param arrayDesign
     * @param accessionsToFetch
     * @param force if true, sequence will be replaced even if it is already there.
     * @return map of biosequence accessions to BioSequences (the existing ones)
     */
    private Map<String, BioSequence> initializeFetchList( ArrayDesign arrayDesign, boolean force ) {
        Map<String, BioSequence> accessionsToFetch = new HashMap<String, BioSequence>();
        int sequenceProvided = 0;
        int noSequence = 0;
        boolean warned = false;
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            BioSequence bs = cs.getBiologicalCharacteristic();
            if ( bs == null ) {
                warned = warnAboutMissingSequence( noSequence, warned, cs );
                noSequence++;
                continue;
            }

            if ( !force && StringUtils.isNotBlank( bs.getSequence() ) ) {
                sequenceProvided++;
                continue;
            }

            String accession = getAccession( cs );

            if ( accession == null ) {
                if ( log.isDebugEnabled() ) log.debug( "No accession for " + cs + ": " + bs );
                continue;
            }

            accessionsToFetch.put( accession, bs );
        }
        informAboutFetchListResults( arrayDesign, accessionsToFetch, sequenceProvided, noSequence );
        return accessionsToFetch;
    }

    /**
     * @param arrayDesign
     * @param notFound
     * @return
     */
    private void logMissingSequences( ArrayDesign arrayDesign, Collection<String> notFound ) {
        log.warn( notFound.size() + " sequences were not found (or were already filled in) for " + arrayDesign );
        StringBuilder buf = new StringBuilder();
        buf.append( "Missing (or already present) sequences for following accessions " + "at version numbers up to "
                + MAX_VERSION_NUMBER + " : " );
        int i = 0;
        for ( String string : notFound ) {
            string = string.replaceFirst( "\\.\\d$", "" );
            buf.append( string + " " );
            if ( ++i > 10 ) {
                buf.append( "... (skipping logging of rest)" );
                break;
            }
        }
        log.info( buf.toString() );
    }

    /**
     * @param numWithNoSequence
     * @param compositeSequence
     */
    private void notifyAboutMissingSequences( int numWithNoSequence, CompositeSequence compositeSequence ) {
        if ( numWithNoSequence == MAX_NUM_WITH_NO_SEQUENCE_FOR_DETAILED_WARNINGS ) {
            log.warn( "More than " + MAX_NUM_WITH_NO_SEQUENCE_FOR_DETAILED_WARNINGS + " compositeSequences do not have"
                    + " biologicalCharacteristics, skipping further details." );
        } else if ( numWithNoSequence < MAX_NUM_WITH_NO_SEQUENCE_FOR_DETAILED_WARNINGS ) {
            log.warn( "No sequence match for " + compositeSequence + " (Description="
                    + compositeSequence.getDescription() + "); it will not have a biologicalCharacteristic!" );
        }
    }

    /**
     * If the sequence already exists
     * 
     * @param sequence
     * @return
     */
    private BioSequence persistSequence( BioSequence sequence ) {
        return ( BioSequence ) persisterHelper.persistOrUpdate( sequence );
    }

    /**
     * Use this to add sequences to an existing Affymetrix design.
     * 
     * @param arrayDesign An existing ArrayDesign that already has compositeSequences filled in.
     * @param probeSequenceFile InputStream from a tab-delimited probe sequence file.
     * @param force if true, the sequences will be overwritten even if they already exist (That is, if the actual ATGCs
     *        need to be replaced, but the BioSequences are already filled in)
     * @param taxon validated taxon
     * @throws IOException
     */
    public Collection<BioSequence> processAffymetrixDesign( ArrayDesign arrayDesign, InputStream probeSequenceFile,
            Taxon taxon, boolean force ) throws IOException {

        log.info( "Processing Affymetrix design" );
        // arrayDesignService.thaw( arrayDesign );
        boolean wasOriginallyLackingCompositeSequences = arrayDesign.getCompositeSequences().size() == 0;
        taxon = validateTaxon( taxon, arrayDesign );
        Collection<BioSequence> bioSequences = new HashSet<BioSequence>();

        int done = 0;
        int percent = 0;

        AffyProbeReader apr = new AffyProbeReader();
        apr.parse( probeSequenceFile );
        Collection<CompositeSequence> compositeSequencesFromProbes = apr.getKeySet();

        int total = compositeSequencesFromProbes.size();

        Map<String, CompositeSequence> quickFindMap = new HashMap<String, CompositeSequence>();
        List<BioSequence> sequenceBuffer = new ArrayList<BioSequence>();
        Map<String, CompositeSequence> csBuffer = new HashMap<String, CompositeSequence>();
        for ( CompositeSequence newCompositeSequence : compositeSequencesFromProbes ) {

            // these composite sequences are just use
            newCompositeSequence.setArrayDesign( arrayDesign );
            BioSequence collapsed = SequenceManipulation.collapse( apr.get( newCompositeSequence ) );
            String sequenceName = newCompositeSequence.getName() + "_collapsed";
            collapsed.setName( sequenceName );
            collapsed.setType( SequenceType.AFFY_COLLAPSED );
            collapsed.setPolymerType( PolymerType.DNA );
            collapsed.setTaxon( taxon );

            sequenceBuffer.add( collapsed );
            if ( csBuffer.containsKey( sequenceName ) )
                throw new IllegalArgumentException( "All probes must have unique names" );
            csBuffer.put( sequenceName, newCompositeSequence );
            if ( sequenceBuffer.size() == BATCH_SIZE ) {
                flushBuffer( bioSequences, sequenceBuffer, csBuffer );
            }

            if ( wasOriginallyLackingCompositeSequences ) {
                arrayDesign.getCompositeSequences().add( newCompositeSequence );
            } else {
                if ( force ) {
                    collapsed = persistSequence( collapsed );
                    assert collapsed.getTaxon().equals( taxon );
                }
                quickFindMap.put( newCompositeSequence.getName(), newCompositeSequence );
            }

            if ( ++done % 1000 == 0 ) {
                percent = updateProgress( total, done, percent );
            }
        }
        flushBuffer( bioSequences, sequenceBuffer, csBuffer );
        updateProgress( total, done, percent );

        if ( !wasOriginallyLackingCompositeSequences ) {
            percent = 0;
            done = 0;
            int numWithNoSequence = 0;
            for ( CompositeSequence originalCompositeSequence : arrayDesign.getCompositeSequences() ) {
                // go back and fill this information into the composite sequences, namely the database entry
                // information.

                CompositeSequence compositeSequenceFromParse = quickFindMap.get( originalCompositeSequence.getName() );
                if ( compositeSequenceFromParse == null ) {
                    numWithNoSequence++;
                    notifyAboutMissingSequences( numWithNoSequence, originalCompositeSequence );
                    continue;
                }

                log.debug( originalCompositeSequence + " matches " + compositeSequenceFromParse + " seq is "
                        + compositeSequenceFromParse.getBiologicalCharacteristic() );

                originalCompositeSequence.setBiologicalCharacteristic( compositeSequenceFromParse
                        .getBiologicalCharacteristic() );

                assert originalCompositeSequence.getBiologicalCharacteristic().getId() != null;

                originalCompositeSequence.setArrayDesign( compositeSequenceFromParse.getArrayDesign() );

                if ( ++done % 1000 == 0 ) {
                    percent = updateProgress( total, done, percent );
                }
            }
            log.info( numWithNoSequence + "/" + arrayDesign.getCompositeSequences().size()
                    + " probes could not be matched to a sequence" );
        }

        arrayDesign.setAdvertisedNumberOfDesignElements( compositeSequencesFromProbes.size() );
        log.info( "Updating " + arrayDesign );

        arrayDesignService.update( arrayDesign );

        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );
        log.info( "Done adding sequence information!" );
        return bioSequences;
    }

    /**
     * If taxon is null then it has not been provided on the command line, then deduce the taxon from the arrayDesign.
     * If there are 0 or more than one taxon on the array design throw an error as this programme can only be run for 1
     * taxon at a time if processing from a file.
     * 
     * @param taxon Taxon as passed in on the command line
     * @param arrayDesign Array design to process
     * @return taxon Taxon to process
     * @throws IllegalArgumentException Thrown when there is not exactly 1 taxon.
     */
    protected Taxon validateTaxon( Taxon taxon, ArrayDesign arrayDesign ) throws IllegalArgumentException {

        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "Array design cannot be null" );
        }

        if ( taxon == null ) {
            Collection<Taxon> taxaOnArray = arrayDesignService.getTaxa( arrayDesign.getId() );

            if ( taxaOnArray.size() == 1 && taxaOnArray.iterator().next() != null ) {
                return taxaOnArray.iterator().next();
            }
            throw new IllegalArgumentException( taxaOnArray.size() + " taxa found for " + arrayDesign
                    + "please specify which taxon to run" );
        }
        return taxon;
    }

    /**
     * Create a new Affymetrix design from scratch, given the name.
     * 
     * @param arrayDesignName design name.
     * @param arrayDesignFile design file in our 'old fashioned' format, but sequence information is ignored.
     * @param probeSequenceFile probe file
     * @return ArrayDesign with CompositeSequences, Reporters, ImmobilizedCharacteristics and BiologicalCharacteristics
     *         filled in.
     * @deprecated This method creates the Affymetrix array design using an input file format we no longer really
     *             support, so it should be avoided (for Affymetrix).
     */
    @Deprecated
    protected ArrayDesign processAffymetrixDesign( String arrayDesignName, Taxon taxon, InputStream arrayDesignFile,
            InputStream probeSequenceFile ) throws IOException {
        ArrayDesign result = ArrayDesign.Factory.newInstance();
        result.setName( arrayDesignName );
        result.setPrimaryTaxon( taxon );

        Contact contact = Contact.Factory.newInstance();
        contact.setName( "Affymetrix" );

        result.setDesignProvider( contact );

        CompositeSequenceParser csp = new CompositeSequenceParser();
        csp.setTaxon( taxon );
        csp.parse( arrayDesignFile );
        Collection<CompositeSequence> rawCompositeSequences = csp.getResults();
        for ( CompositeSequence sequence : rawCompositeSequences ) {
            sequence.setArrayDesign( result );
            sequence.setBiologicalCharacteristic( null ); // we don't want the sequence information from these files.
        }
        result.setCompositeSequences( rawCompositeSequences );

        result = ( ArrayDesign ) persisterHelper.persist( result );
        taxon = validateTaxon( taxon, result );
        this.processAffymetrixDesign( result, probeSequenceFile, taxon, false );

        return result;
    }

    /**
     * @param Array design name.
     * @param Array design file in our 'old fashioned' format.
     * @param Affymetrix probe file
     * @param taxon
     * @return ArrayDesign with CompositeSequences, Reporters, ImmobilizedCharacteristics and BiologicalCharacteristics
     *         filled in.
     * @deprecated {@see processAffymetrixDesign}
     */
    @Deprecated
    public ArrayDesign processAffymetrixDesign( String arrayDesignName, String arrayDesignFile,
            String probeSequenceFile, Taxon taxon ) throws IOException {
        InputStream arrayDesignFileStream = new BufferedInputStream( new FileInputStream( arrayDesignFile ) );
        InputStream probeSequenceFileStream = new BufferedInputStream( new FileInputStream( probeSequenceFile ) );
        return this.processAffymetrixDesign( arrayDesignName, taxon, arrayDesignFileStream, probeSequenceFileStream );
    }

    /**
     * The sequence file <em>must</em> provide an unambiguous way to associate the sequences with design elements on the
     * array.
     * <p>
     * If the SequenceType is AFFY_PROBE, the sequences will be treated as probes in probe sets, in Affymetrix 'tabbed'
     * format. Otherwise the format of the file is assumed to be FASTA, with one CompositeSequence per FASTA element;
     * there is further assumed to be just one Reporter per CompositeSequence (that is, they are the same thing). The
     * FASTA file must use a standard defline format (as described at {@link http
     * ://en.wikipedia.org/wiki/Fasta_format#Sequence_identifiers}.
     * <p>
     * For FASTA files, the match-up of the sequence with the design element is done using the following tests, until
     * one passes:
     * <ol>
     * <li>The format line contains an explicit reference to the name of the CompositeSequence (probe id).</li>
     * <li>The BioSequence for the CompositeSequences are already filled in, and there is a matching external database
     * identifier (e.g., Genbank accession). This will only work if Genbank accessions do not re-occur in the FASTA
     * file.</li>
     * </ol>
     * 
     * @param arrayDesign
     * @param sequenceFile FASTA format
     * @param sequenceType - e.g., SequenceType.DNA (generic), SequenceType.AFFY_PROBE, or SequenceType.OLIGO.
     * @throws IOException
     * @see ubic.gemma.loader.genome.FastaParser
     */
    public Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, InputStream sequenceFile,
            SequenceType sequenceType ) throws IOException {
        return this.processArrayDesign( arrayDesign, sequenceFile, sequenceType, null );
    }

    /**
     * @param sequenceIdentifierFile with two columns: first is probe id, second is genbank accession.
     * @return
     * @throws IOException
     */
    private Map<String, String> parseAccessionFile( InputStream sequenceIdentifierFile ) throws IOException {
        BufferedReader br = new BufferedReader( new InputStreamReader( sequenceIdentifierFile ) );

        String line = null;

        StopWatch timer = new StopWatch();
        timer.start();

        Map<String, String> probe2acc = new HashMap<String, String>();
        int count = 0;
        int totalLines = 0;
        while ( ( line = br.readLine() ) != null ) {
            String[] fields = line.split( "\t" );
            ++totalLines;
            if ( fields.length < 2 ) {
                continue;
            }

            String probeName = fields[0];
            String seqAcc = fields[1];

            if ( StringUtils.isBlank( seqAcc ) ) {
                continue;
            }

            probe2acc.put( probeName, seqAcc );
            if ( ++count % 2000 == 0 && timer.getTime() > 10000 ) {
                log.info( count + " / " + totalLines + " probes read so far have identifiers" );
            }
        }
        br.close();
        log.info( count + " / " + totalLines + " probes have accessions" );
        return probe2acc;

    }

    /**
     * The sequence file <em>must</em> provide an unambiguous way to associate the sequences with design elements on the
     * array. If probe does not have a match to a sequence in the input file, the sequence for that probe will be
     * nulled.
     * <p>
     * If the SequenceType is AFFY_PROBE, the sequences will be treated as probes in probe sets, in Affymetrix 'tabbed'
     * format.
     * <p>
     * If the SequenceType is OLIGO, the input is treated as a table (see ProbeSequenceParser; to retain semi-backwards
     * compatibility, FASTA is detected but an exception will be thrown).
     * <p>
     * Otherwise the format of the file is assumed to be FASTA, with one CompositeSequence per FASTA element; there is
     * further assumed to be just one Reporter per CompositeSequence (that is, they are the same thing). The FASTA file
     * must use a standard defline format (as described at {@link http
     * ://en.wikipedia.org/wiki/Fasta_format#Sequence_identifiers}.
     * <p>
     * For FASTA files, the match-up of the sequence with the design element is done using the following tests, until
     * one passes:
     * <ol>
     * <li>The format line contains an explicit reference to the name of the CompositeSequence (probe id)</li>
     * <li>The format line sequence name matches the CompositeSequence name with a suffix added to disambiguate
     * duplicates. That is, sometimes the same sequence appears on the array more than once, and this is the identifier
     * used for the probe; we add something like "___[string]" to the end of probe name in this case. For example, a
     * sequence with name M100000439 will match probes named M100000439 as well as M100000439___Dup1.
     * <li>The BioSequence for the CompositeSequences are already filled in, and there is a matching external database
     * identifier (e.g., Genbank accession). This will only work if Genbank accessions do not re-occur in the FASTA
     * file.</li>
     * </ol>
     * 
     * @param arrayDesign
     * @param sequenceFile FASTA, Affymetrix or tabbed format (depending on the type)
     * @param sequenceType - e.g., SequenceType.DNA (generic), SequenceType.AFFY_PROBE, or SequenceType.OLIGO.
     * @param taxon - if null, attempt to determine it from the array design.
     * @throws IOException
     * @see ubic.gemma.loader.genome.FastaParser
     * @see ArrayDesignProbeRenamingService.DUPLICATE_PROBE_NAME_MUNGE_SEPARATOR for the specification of how duplicate
     *      probes are munged.
     */
    public Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, InputStream sequenceFile,
            SequenceType sequenceType, Taxon taxon ) throws IOException {

        // arrayDesign = arrayDesignService.thaw( arrayDesign );

        if ( sequenceType.equals( SequenceType.AFFY_PROBE ) ) {
            return this.processAffymetrixDesign( arrayDesign, sequenceFile, taxon, true );
        } else if ( sequenceType.equals( SequenceType.OLIGO ) ) {
            return this.processOligoDesign( arrayDesign, sequenceFile, taxon );
        }
        taxon = validateTaxon( taxon, arrayDesign );

        checkForCompositeSequences( arrayDesign );

        FastaParser fastaParser = new FastaParser();
        fastaParser.parse( sequenceFile );
        Collection<BioSequence> bioSequences = fastaParser.getResults();

        // make two maps: one for genbank ids, one for the sequence name.
        Map<String, BioSequence> gbIdMap = new HashMap<String, BioSequence>();
        Map<String, BioSequence> nameMap = new HashMap<String, BioSequence>();

        int total = bioSequences.size() + arrayDesign.getCompositeSequences().size();
        int done = 0;
        int percent = 0;

        for ( BioSequence sequence : bioSequences ) {

            sequence.setType( sequenceType );
            sequence.setPolymerType( PolymerType.DNA );
            sequence.setTaxon( taxon );
            sequence = persistSequence( sequence );

            addToMaps( gbIdMap, nameMap, sequence );

        }

        log.info( "Sequences done, updating composite sequences" );

        int numWithNoSequence = 0;
        int numMatchedByAccession = 0;
        int numMatchedByProbeName = 0;
        String mungeRegex = ArrayDesignProbeRenamingService.DUPLICATE_PROBE_NAME_MUNGE_SEPARATOR + ".+$";

        for ( CompositeSequence compositeSequence : arrayDesign.getCompositeSequences() ) {

            if ( log.isTraceEnabled() ) log.trace( "Looking for sequence for: " + compositeSequence.getName() );

            BioSequence match = null;
            if ( nameMap.containsKey( compositeSequence.getName() ) ) {
                match = nameMap.get( compositeSequence.getName() );
                numMatchedByProbeName++;
            } else if ( compositeSequence.getName().matches( mungeRegex ) ) {
                String unMungedName = compositeSequence.getName().replaceFirst( mungeRegex, "" );
                if ( nameMap.containsKey( unMungedName ) ) {
                    match = nameMap.get( unMungedName );
                    numMatchedByProbeName++;
                    continue;
                }
            } else {
                BioSequence biologicalCharacteristic = compositeSequence.getBiologicalCharacteristic();
                if ( biologicalCharacteristic != null ) {
                    biologicalCharacteristic = bioSequenceService.thaw( biologicalCharacteristic );
                    if ( biologicalCharacteristic.getSequenceDatabaseEntry() != null
                            && gbIdMap.containsKey( biologicalCharacteristic.getSequenceDatabaseEntry().getAccession() ) ) {
                        match = gbIdMap.get( biologicalCharacteristic.getSequenceDatabaseEntry().getAccession() );
                        numMatchedByAccession++;
                    } else {
                        compositeSequence.setBiologicalCharacteristic( null );
                        numWithNoSequence++;
                        notifyAboutMissingSequences( numWithNoSequence, compositeSequence );
                    }
                } else {
                    numWithNoSequence++;
                    notifyAboutMissingSequences( numWithNoSequence, compositeSequence );
                }
            }

            if ( match != null ) {
                // overwrite the existing characteristic if necessary.
                compositeSequence.setBiologicalCharacteristic( match );
                compositeSequence.setArrayDesign( arrayDesign );
            }

            if ( ++done % 1000 == 0 ) {
                percent = updateProgress( total, done, percent );
            }
        }

        log.info( numMatchedByAccession + "/" + arrayDesign.getCompositeSequences().size()
                + " composite sequences were matched to sequences by Genbank accession" );
        log.info( numMatchedByProbeName + "/" + arrayDesign.getCompositeSequences().size()
                + " composite sequences were matched to sequences by probe name" );

        if ( numWithNoSequence > 0 )
            log.info( "There were " + numWithNoSequence + "/" + arrayDesign.getCompositeSequences().size()
                    + " composite sequences with no associated biological characteristic" );

        log.info( "Updating sequences on arrayDesign" );
        arrayDesignService.update( arrayDesign );

        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );
        return bioSequences;

    }

    /**
     * @param arrayDesign
     * @param sequenceFile; the expected format is described in {@link ProbeSequenceParser}
     * @param taxon
     * @return
     * @see ProbeSequenceParser
     */
    private Collection<BioSequence> processOligoDesign( ArrayDesign arrayDesign, InputStream sequenceFile, Taxon taxon )
            throws IOException {
        checkForCompositeSequences( arrayDesign );

        ProbeSequenceParser parser = new ProbeSequenceParser();
        parser.parse( sequenceFile );

        int total = arrayDesign.getCompositeSequences().size();
        int done = 0;
        int percent = 0;
        taxon = validateTaxon( taxon, arrayDesign );

        log.info( "Sequences done, updating composite sequences" );

        int numWithNoSequence = 0;

        Collection<BioSequence> res = new HashSet<BioSequence>();
        for ( CompositeSequence compositeSequence : arrayDesign.getCompositeSequences() ) {

            if ( log.isTraceEnabled() ) log.trace( "Looking for sequence for: " + compositeSequence.getName() );
            BioSequence sequence = parser.get( compositeSequence.getName() );

            if ( sequence != null ) {
                // overwrite the existing characteristic if necessary.
                assert sequence.getSequence() != null;
                sequence.setType( SequenceType.OLIGO );
                sequence.setPolymerType( PolymerType.DNA );
                sequence.setTaxon( taxon );
                sequence = persistSequence( sequence );
                compositeSequence.setBiologicalCharacteristic( sequence );
                compositeSequence.setArrayDesign( arrayDesign );
                res.add( sequence );
            } else {
                numWithNoSequence++;
                notifyAboutMissingSequences( numWithNoSequence, compositeSequence );
            }

            if ( ++done % 1000 == 0 ) {
                percent = updateProgress( total, done, percent );
            }
        }

        if ( numWithNoSequence > 0 )
            log.info( "There were " + numWithNoSequence + "/" + arrayDesign.getCompositeSequences().size()
                    + " composite sequences with no associated biological characteristic" );

        log.info( "Updating sequences on arrayDesign" );
        arrayDesignService.update( arrayDesign );
        return res;
    }

    private void checkForCompositeSequences( ArrayDesign arrayDesign ) {
        boolean wasOriginallyLackingCompositeSequences = arrayDesign.getCompositeSequences().size() == 0;

        if ( wasOriginallyLackingCompositeSequences ) {
            throw new IllegalArgumentException(
                    "You need to pass in an array design that already has compositeSequences filled in." );
        }
    }

    public Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, String[] databaseNames, boolean force ) {
        return this.processArrayDesign( arrayDesign, databaseNames, null, force );
    }

    /**
     * For the case where the sequences are retrieved simply by the Genbank accession. For this to work, the array
     * design must already have the biosequence objects, but they haven't been populated with the actual sequences (if
     * they have, the values will be replaced if force=true)
     * <p>
     * Sequences that appear to be IMAGE clones are given another check and the Genbank accession used to retrieve the
     * sequence is based on that, not the one provided in the Biosequence; if it differs it will be replaced. This
     * happens when the Genbank accession is for a Refseq (for example) but the actual clone on the array is from IMAGE.
     * 
     * @param arrayDesign
     * @param databaseNames the names of the BLAST-formatted databases to search (e.g., nt, est_mouse)
     * @param blastDbHome where to find the blast databases for sequence retrieval
     * @param force If true, then when an existing BioSequence contains a non-empty sequence value, it will be
     *        overwritten with a new one.
     * @return
     */
    public Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, String[] databaseNames,
            String blastDbHome, boolean force ) {

        Map<String, BioSequence> accessionsToFetch = initializeFetchList( arrayDesign, force );

        if ( accessionsToFetch.size() == 0 ) {
            log.info( "No accessions to fetch, no processing will be done" );
            return null;
        }

        Collection<Taxon> taxaOnArray = arrayDesignService.getTaxa( arrayDesign.getId() );
        // not taxon found
        if ( taxaOnArray.size() == 0 ) {
            throw new IllegalArgumentException( taxaOnArray.size() + " taxon found for " + arrayDesign
                    + "please specifiy which taxon to run" );
        }

        Collection<String> notFound = accessionsToFetch.keySet();
        Collection<BioSequence> finalResult = new HashSet<BioSequence>();

        int versionNumber = 1;
        while ( versionNumber < MAX_VERSION_NUMBER ) {
            Collection<BioSequence> retrievedSequences = searchBlastDbs( databaseNames, blastDbHome, notFound );

            // we can loop through the taxons as we can ignore sequence when retrieved and arraydesign taxon not match.

            Map<String, BioSequence> found = findOrUpdateSequences( accessionsToFetch, retrievedSequences, taxaOnArray,
                    force );

            finalResult.addAll( found.values() );
            notFound = getUnFound( notFound, found );

            if ( notFound.isEmpty() ) {
                break;
            }

            // bump up the version numbers.

            for ( String accession : notFound ) {
                if ( log.isTraceEnabled() )
                    log.trace( accession + " not found, increasing version number to " + versionNumber );
                // remove the version number and increase it
                BioSequence bs = accessionsToFetch.get( accession );
                accessionsToFetch.remove( accession );

                // add or increase the version number.
                accession = accession.replaceFirst( "\\.\\d+$", "" );
                accession = accession + "." + Integer.toString( versionNumber );
                accessionsToFetch.put( accession, bs );
            }
            notFound = accessionsToFetch.keySet();
            ++versionNumber;
        }

        if ( !notFound.isEmpty() ) {
            logMissingSequences( arrayDesign, notFound );
        }
        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );

        return finalResult;

    }

    /**
     * Intended for use with array designs that use sequences that are in genbank, but the accessions need to be
     * assigned after the array is already in the system. This happens when only partial or incorrect information is in
     * GEO, for example, when Refseq ids are provided instead of the EST clone that was arrayed.
     * <p>
     * This method ALWAYS clobbers the BioSequence associations that are associated with the array design (at least, if
     * any of the probe identifiers in the file given match the array design).
     * 
     * @param arrayDesign
     * @param sequenceIdentifierFile Sequence file has two columns: column 1 is a probe id, column 2 is a genbank
     *        accession or sequence name, delimited by tab. Sequences will be fetched from BLAST databases if possible;
     *        ones missing will be sought directly in Gemma.
     * @param databaseNames
     * @param blastDbHome
     * @param taxon
     * @param force If true, if an existing BioSequence that matches is found in the system, any existing sequence
     *        information in the BioSequence will be overwritten.
     * @return
     * @throws IOException
     */
    public Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, InputStream sequenceIdentifierFile,
            String[] databaseNames, String blastDbHome, Taxon taxon, boolean force ) throws IOException {
        checkForCompositeSequences( arrayDesign );

        Map<String, String> probe2acc = parseAccessionFile( sequenceIdentifierFile );
        Collection<BioSequence> finalResult = new HashSet<BioSequence>();
        Collection<String> notFound = new HashSet<String>();

        // values that were enot found
        notFound.addAll( probe2acc.values() );

        // the actual thing values to search for (with version numbers)
        Collection<String> accessionsToFetch = new HashSet<String>();
        accessionsToFetch.addAll( probe2acc.values() );

        // only 1 taxon should be on array design if taxon not supplied on command line
        taxon = validateTaxon( taxon, arrayDesign );

        /*
         * Fill in sequences from BLAST databases.
         */
        int versionNumber = 1;
        int numSwitched = 0;
        while ( versionNumber < MAX_VERSION_NUMBER ) {
            Collection<BioSequence> retrievedSequences = searchBlastDbs( databaseNames, blastDbHome, notFound );

            // map of accessions to sequence.
            Map<String, BioSequence> found = findOrUpdateSequences( accessionsToFetch, retrievedSequences, taxon, force );

            finalResult.addAll( retrievedSequences );

            // replace the sequences.
            for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
                String probeName = cs.getName();
                String acc = probe2acc.get( probeName );
                if ( found.containsKey( acc ) ) {
                    numSwitched++;
                    log.debug( "Setting seq. for " + cs + " to " + found.get( acc ) );
                    cs.setBiologicalCharacteristic( found.get( acc ) );
                }
            }

            notFound = getUnFound( notFound, found );

            if ( notFound.isEmpty() ) {
                break; // we're done!
            }

            // bump up the version numbers for ones we haven't found yet.

            for ( String accession : notFound ) {
                if ( log.isTraceEnabled() )
                    log.trace( accession + " not found, increasing version number to " + versionNumber );
                accessionsToFetch.remove( accession );

                // add or increase the version number.
                accession = accession.replaceFirst( "\\.\\d+$", "" );
                accession = accession + "." + Integer.toString( versionNumber );
                accessionsToFetch.add( accession );
            }
            notFound = accessionsToFetch;

            ++versionNumber;

        }

        if ( !notFound.isEmpty() && taxon != null ) {

            Collection<String> stillLooking = new HashSet<String>();
            stillLooking.addAll( notFound );
            notFound.clear();

            /*
             * clear the version number.
             */
            for ( String accession : stillLooking ) {
                notFound.remove( accession );
                accession = accession.replaceFirst( "\\.\\d+$", "" );
                notFound.add( accession );
            }
            assert notFound.size() > 0;
            /*
             * See if they're already in Gemma. This is good for sequences that are not in genbank but have been loaded
             * previously.
             */
            Map<String, BioSequence> found = findLocalSequences( notFound, taxon );
            finalResult.addAll( found.values() );

            for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
                String probeName = cs.getName();
                String acc = probe2acc.get( probeName );
                if ( found.containsKey( acc ) ) {
                    numSwitched++;
                    log.debug( "Setting seq. for " + cs + " to " + found.get( acc ) );
                    cs.setBiologicalCharacteristic( found.get( acc ) );
                }
            }
            notFound = getUnFound( notFound, found );
        }

        if ( !notFound.isEmpty() ) {
            logMissingSequences( arrayDesign, notFound );
        }

        log.info( numSwitched + " composite sequences had their biologicalCharacteristics changed" );

        arrayDesignService.update( arrayDesign );

        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );
        return finalResult;

    }

    /**
     * @param databaseNames
     * @param blastDbHome
     * @param accessionsToFetch
     * @return
     */
    private Collection<BioSequence> searchBlastDbs( String[] databaseNames, String blastDbHome,
            Collection<String> accessionsToFetch ) {
        // search the databases.
        FastaCmd fc = new SimpleFastaCmd();
        Collection<BioSequence> retrievedSequences = new HashSet<BioSequence>();
        for ( String dbname : databaseNames ) {
            Collection<BioSequence> moreBioSequences;
            if ( blastDbHome != null ) {
                moreBioSequences = fc.getBatchAccessions( accessionsToFetch, dbname, blastDbHome );
            } else {
                moreBioSequences = fc.getBatchAccessions( accessionsToFetch, dbname );
            }

            if ( log.isDebugEnabled() )
                log.debug( moreBioSequences.size() + " sequences of " + accessionsToFetch.size() + " fetched "
                        + " from " + dbname );
            retrievedSequences.addAll( moreBioSequences );
        }

        fillInGenbank( retrievedSequences );

        return retrievedSequences;
    }

    /**
     * @param retrievedSequences
     */
    private void fillInGenbank( Collection<BioSequence> retrievedSequences ) {
        ExternalDatabase genbank = getGenbank();
        assert genbank.getId() != null;
        for ( BioSequence bioSequence : retrievedSequences ) {
            if ( bioSequence.getSequenceDatabaseEntry() == null ) {
                log.warn( "No database entry for " + bioSequence );
                continue;
            }
            if ( bioSequence.getSequenceDatabaseEntry().getExternalDatabase().getId() == null ) {
                bioSequence.getSequenceDatabaseEntry().setExternalDatabase( genbank );
            }
        }
    }

    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

    private ExternalDatabase getGenbank() {
        return this.externalDatabaseService.find( "Genbank" );
    }

    /**
     * Search for a single accession
     * 
     * @param databaseNames
     * @param blastDbHome
     * @param accessionToFetch
     * @return
     */
    private BioSequence searchBlastDbs( String[] databaseNames, String blastDbHome, String accessionToFetch ) {
        FastaCmd fc = new SimpleFastaCmd();
        for ( String dbname : databaseNames ) {
            BioSequence moreBioSequence;
            if ( blastDbHome != null ) {
                moreBioSequence = fc.getByAccession( accessionToFetch, dbname, blastDbHome );
            } else {
                moreBioSequence = fc.getByAccession( accessionToFetch, dbname, null );
            }
            if ( moreBioSequence != null ) return moreBioSequence;
        }
        return null;

    }

    /**
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param bioSequenceService the bioSequenceService to set
     */
    public void setBioSequenceService( BioSequenceService bioSequenceService ) {
        this.bioSequenceService = bioSequenceService;
    }

    public void setPersisterHelper( Persister persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * FIXME - factor this out, it might be useful elsewhere.
     * 
     * @param totalThingsToDo
     * @param howManyAreDone
     * @param percentDoneLastTimeWeChecked
     * @return
     */
    private int updateProgress( int totalThingsToDo, int howManyAreDone, int percentDoneLastTimeWeChecked ) {
        int newPercent = ( int ) Math.ceil( ( 100.00 * howManyAreDone / totalThingsToDo ) );
        if ( newPercent > percentDoneLastTimeWeChecked ) {
            log.info( howManyAreDone + " sequence+probes of " + totalThingsToDo + " processed." );
        }

        return newPercent;
    }

    /**
     * Copy sequences into the original versions, or create new sequences in the DB, as needed.
     * 
     * @param accessionsToFetch accessions that we need to fill in
     * @param retrievedSequences candidate sequence information for copying into the database.
     * @param force If true, if an existing BioSequence that matches if found in the system, any existing sequence
     *        information in the BioSequence will be overwritten.
     * @param taxa Representing taxa on array
     * @return Items that were found.
     */
    private Map<String, BioSequence> findOrUpdateSequences( Map<String, BioSequence> accessionsToFetch,
            Collection<BioSequence> retrievedSequences, Collection<Taxon> taxa, boolean force ) {

        Map<String, BioSequence> found = new HashMap<String, BioSequence>();
        for ( Taxon taxon : taxa ) {

            if ( taxon == null ) {
                log.warn( "Null taxon ..." ); // probably should be an exception
            }
            assert taxon != null;

            boolean warned = false;
            for ( BioSequence sequence : retrievedSequences ) {
                if ( sequence.getTaxon() == null ) {
                    if ( !warned ) {
                        log.warn( "Sequence taxon is null [" + sequence + "], copying array taxon " + taxon
                                + " ; further warnings for this array taxon are suppressed." );
                    }
                    warned = true;
                } else if ( !sequence.getTaxon().equals( taxon ) ) {
                    continue;
                }

                sequence.setTaxon( taxon );
                if ( sequence.getSequenceDatabaseEntry() == null ) {
                    log.warn( "Sequence from BLAST db lacks database entry: " + sequence + "; skipping" );
                    continue;
                }
                sequence = createOrUpdateGenbankSequence( sequence, force );
                String accession = sequence.getSequenceDatabaseEntry().getAccession();
                found.put( accession, sequence );
                accessionsToFetch.remove( accession );
            }
        }
        return found;
    }

    /**
     * Copy sequences into the original versions, or create new sequences in the DB, as needed.
     * 
     * @param accessionsToFetch
     * @param retrievedSequences
     * @param force If true, if an existing BioSequence that matches if found in the system, any existing sequence
     *        information in the BioSequence will be overwritten.
     * @return Items that were found.
     */
    private Map<String, BioSequence> findOrUpdateSequences( Collection<String> accessionsToFetch,
            Collection<BioSequence> retrievedSequences, Taxon taxon, boolean force ) {

        Map<String, BioSequence> found = new HashMap<String, BioSequence>();
        for ( BioSequence sequence : retrievedSequences ) {
            if ( log.isDebugEnabled() ) log.debug( "Processing retrieved sequence: " + sequence );
            sequence.setTaxon( taxon );
            sequence = createOrUpdateGenbankSequence( sequence, force );
            String accession = sequence.getSequenceDatabaseEntry().getAccession();
            found.put( accession, sequence );
            accessionsToFetch.remove( accession );
        }
        return found;
    }

    /**
     * @param identifiersToSearch
     * @param taxon
     * @return
     */
    private Map<String, BioSequence> findLocalSequences( Collection<String> identifiersToSearch, Taxon taxon ) {
        Map<String, BioSequence> found = new HashMap<String, BioSequence>();
        for ( String id : identifiersToSearch ) {
            BioSequence template = BioSequence.Factory.newInstance();
            template.setTaxon( taxon );
            template.setName( id );
            BioSequence seq = bioSequenceService.find( template );
            if ( seq != null ) {
                found.put( id, seq );
            }
        }
        return found;
    }

    /**
     * @param noSequence
     * @param warned
     * @param cs
     * @return
     */
    private boolean warnAboutMissingSequence( int noSequence, boolean warned, CompositeSequence cs ) {
        if ( !warned ) {
            if ( noSequence < 20 ) {
                log.warn( cs + " has no biosequence" );
            } else {
                log.warn( "...More than 20 are missing sequences, details omitted" );
                warned = true;
            }
        }
        return warned;
    }

    /**
     * Update a single sequence in the system.
     * 
     * @param sequenceId
     * @param databaseNames
     * @param blastDbHome
     * @param force If true, if an existing BioSequence that matches if found in the system, any existing sequence
     *        information in the BioSequence will be overwritten.
     * @return persistent BioSequence.
     */
    public BioSequence processSingleAccession( String sequenceId, String[] databaseNames, String blastDbHome,
            boolean force ) {
        BioSequence found = this.searchBlastDbs( databaseNames, blastDbHome, sequenceId );
        if ( found == null ) return null;
        return createOrUpdateGenbankSequence( found, force );

    }

    /**
     * @param found a new (nonpersistent) biosequence that can be used to create a new entry or update an existing one
     *        with the sequence. The sequence would have come from Genbank.
     * @param force If true, if an existing BioSequence that matches if found in the system, any existing sequence
     *        information in the BioSequence will be overwritten. Otherwise, the sequence will only be updated if the
     *        actual sequence information was missing in our DB and 'found' has a sequence.
     * @return persistent BioSequence.
     */
    private BioSequence createOrUpdateGenbankSequence( BioSequence found, boolean force ) {
        assert found != null;
        DatabaseEntry sequenceDatabaseEntry = found.getSequenceDatabaseEntry();

        assert sequenceDatabaseEntry != null; // this should always be the case because the sequences comes from
        // genbank (blastdb)
        assert sequenceDatabaseEntry.getExternalDatabase() != null;

        BioSequence existing = null;

        existing = bioSequenceService.findByAccession( sequenceDatabaseEntry );

        BioSequence result = null;
        if ( existing == null ) {
            if ( log.isDebugEnabled() ) log.debug( "Find (or creating) new sequence " + found );

            result = bioSequenceService.find( found ); // there still might be a match.

            if ( result == null ) {
                result = bioSequenceService.create( found );
            }
        } else {
            result = existing;
        }

        assert result != null;
        // note that no matter what we make sure the database entry is filled in.
        if ( force || ( StringUtils.isBlank( result.getSequence() ) && !StringUtils.isBlank( found.getSequence() ) ) ) {
            result = updateExistingWithSequenceData( found, result );
        } else {
            fillInDatabaseEntry( found, result );
        }

        // result = bioSequenceService.thaw( result );

        return result;
    }

    /**
     * Unfortunately we have to make sure the database entry is filled in, even if we aren't using 'force'. This seems
     * due in part to an old bug in the system that left these blank. This is our opporunity to fill them in.
     */
    private void fillInDatabaseEntry( BioSequence found, BioSequence existing ) {
        if ( existing.getSequenceDatabaseEntry() == null ) {
            existing.setSequenceDatabaseEntry( found.getSequenceDatabaseEntry() );
            bioSequenceService.update( existing );
        }
    }

    /**
     * Replace information in "existing" with data from "found".
     * 
     * @param found
     * @param existing
     * @return
     */
    private BioSequence updateExistingWithSequenceData( BioSequence found, BioSequence existing ) {
        assert found != null;
        assert existing != null;

        if ( existing.getType() == null ) existing.setType( found.getType() ); // generic...
        existing.setLength( found.getLength() );
        assert found.getSequence() != null;

        existing.setSequence( found.getSequence() );
        existing.setIsApproximateLength( found.getIsApproximateLength() );

        if ( existing.getSequenceDatabaseEntry() == null ) {
            log.debug( "Inserting database entry into existing sequence " + existing );
            existing.setSequenceDatabaseEntry( found.getSequenceDatabaseEntry() );
        }

        // This is just for debugging purposes -- some array designs give suspicious sequence information.
        if ( existing.getSequence().length() > 10e4 ) {
            log.warn( existing + " - new sequence is very long for an expression probe ("
                    + existing.getSequence().length() + " bases)" );
        }

        bioSequenceService.update( existing );
        if ( log.isDebugEnabled() )
            log.debug( "Updated " + existing + " with sequence " + StringUtils.abbreviate( existing.getSequence(), 20 ) );

        assert found.getSequenceDatabaseEntry().getExternalDatabase() != null;
        return existing;
    }

}
