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
package ubic.gemma.core.loader.expression.arrayDesign;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.analysis.sequence.SequenceManipulation;
import ubic.gemma.core.loader.genome.FastaCmd;
import ubic.gemma.core.loader.genome.FastaParser;
import ubic.gemma.core.loader.genome.ProbeSequenceParser;
import ubic.gemma.core.loader.genome.SimpleFastaCmd;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.PolymerType;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;

import java.io.*;
import java.util.*;

/**
 * Handles collapsing the sequences, attaching sequences to DesignElements, either from provided input or via a fetch.
 *
 * @author pavlidis
 */
@Component

public class ArrayDesignSequenceProcessingServiceImpl implements ArrayDesignSequenceProcessingService {

    /**
     * When we encounter two probes with the same name, we add this string along with a unique identifier to the end of
     * the name. This comes into play when the probe name is the sequence name, and the same sequence is used multiple
     * times on the array design.
     */
    public static final String DUPLICATE_PROBE_NAME_MUNGE_SEPARATOR = "___";
    private static final int BATCH_SIZE = 100;
    /**
     * After seeing more than this number of compositeSequences lacking sequences we don't give a detailed warning.
     */
    private static final int MAX_NUM_WITH_NO_SEQUENCE_FOR_DETAILED_WARNINGS = 20;
    private static final Log log = LogFactory.getLog( ArrayDesignSequenceProcessingServiceImpl.class.getName() );
    private final ArrayDesignReportService arrayDesignReportService;
    private final ArrayDesignService arrayDesignService;
    private final BioSequenceService bioSequenceService;
    private final ExternalDatabaseService externalDatabaseService;
    private final Persister persisterHelper;
    private final String fastaCmdExe;

    @Autowired
    public ArrayDesignSequenceProcessingServiceImpl( ArrayDesignReportService arrayDesignReportService,
            ArrayDesignService arrayDesignService, BioSequenceService bioSequenceService,
            ExternalDatabaseService externalDatabaseService, Persister persisterHelper,
            @Value("${fastaCmd.exe}") String fastaCmdExe ) {
        this.arrayDesignReportService = arrayDesignReportService;
        this.arrayDesignService = arrayDesignService;
        this.bioSequenceService = bioSequenceService;
        this.externalDatabaseService = externalDatabaseService;
        this.persisterHelper = persisterHelper;
        this.fastaCmdExe = fastaCmdExe;
    }

    @Override
    public void assignSequencesToDesignElements( Collection<CompositeSequence> designElements,
            Collection<BioSequence> sequences ) {

        Map<String, BioSequence> nameMap = new HashMap<>();
        for ( BioSequence sequence : sequences ) {
            nameMap.put( this.deMangleProbeId( sequence.getName() ), sequence );
        }

        int numNotFound = 0;
        for ( CompositeSequence designElement : designElements ) {
            if ( !nameMap.containsKey( designElement.getName() ) ) {
                ArrayDesignSequenceProcessingServiceImpl.log.debug( "No sequence matches " + designElement.getName() );
                numNotFound++;
                continue;
            }

            designElement.setBiologicalCharacteristic( nameMap.get( designElement.getName() ) );

        }

        ArrayDesignSequenceProcessingServiceImpl.log
                .info( sequences.size() + " sequences processed for " + designElements.size() + " design elements" );
        if ( numNotFound > 0 ) {
            ArrayDesignSequenceProcessingServiceImpl.log.warn( numNotFound + " probes had no matching sequence" );
        }
    }

    @Override
    public void assignSequencesToDesignElements( Collection<CompositeSequence> designElements, File fastaFile )
            throws IOException {
        try ( FileInputStream fis = new FileInputStream( fastaFile ) ) {
            this.assignSequencesToDesignElements( designElements, fis );
        }
    }

    /**
     * Associate sequences with an array design. It is assumed that the name of the sequences can be matched to the name
     * of a design element. Provided for testing purposes.
     */
    @Override
    public void assignSequencesToDesignElements( Collection<CompositeSequence> designElements, InputStream fastaFile )
            throws IOException {
        FastaParser fp = new FastaParser();
        fp.parse( fastaFile );
        Collection<BioSequence> sequences = fp.getResults();
        ArrayDesignSequenceProcessingServiceImpl.log.debug( "Parsed " + sequences.size() + " sequences" );

        this.assignSequencesToDesignElements( designElements, sequences );
    }

    @Override
    public Collection<BioSequence> processAffymetrixDesign( ArrayDesign arrayDesign, InputStream probeSequenceFile,
            Taxon taxon ) throws IOException {

        ArrayDesignSequenceProcessingServiceImpl.log.info( "Processing Affymetrix design" );
        boolean wasOriginallyLackingCompositeSequences = arrayDesign.getCompositeSequences().size() == 0; // this would be unusual
        log.info( "Platform has " + arrayDesign.getCompositeSequences().size() + " elements" );

        taxon = this.validateTaxon( taxon, arrayDesign );
        Collection<BioSequence> bioSequences = new HashSet<>();

        int done = 0;
        int percent = 0;

        AffyProbeReader apr = new AffyProbeReader();
        apr.parse( probeSequenceFile );
        Collection<CompositeSequence> compositeSequencesFromProbes = apr.getKeySet();

        int total = compositeSequencesFromProbes.size();

        Map<String, CompositeSequence> quickFindMap = new HashMap<>();
        List<BioSequence> sequenceBuffer = new ArrayList<>();
        Map<String, CompositeSequence> csBuffer = new HashMap<>();
        for ( CompositeSequence newCompositeSequence : compositeSequencesFromProbes ) {

            // these composite sequences are just use
            newCompositeSequence.setArrayDesign( arrayDesign );
            BioSequence collapsed = SequenceManipulation.collapse( apr.get( newCompositeSequence ) );
            String sequenceName = newCompositeSequence.getName() + "_collapsed";
            collapsed.setName( sequenceName );
            collapsed.setType( SequenceType.AFFY_COLLAPSED );
            collapsed.setPolymerType( PolymerType.DNA );
            collapsed.setTaxon( taxon );

            if ( log.isDebugEnabled() ) {
                System.err.println( newCompositeSequence.getName() + " " + collapsed.getSequence() + "\n" );
            }

            if ( wasOriginallyLackingCompositeSequences ) {
                arrayDesign.getCompositeSequences().add( newCompositeSequence );
            } else {
                /*
                 * usual case. If it already exists, we try to update the sequence itself by default. This is generally
                 * safe for affymetrix probes because affy doesn't reuse probe names. These updates actually only affect
                 * the sequence itself in situations where we have a misparse.
                 */
                collapsed = this.persistSequence( collapsed );
                quickFindMap.put( newCompositeSequence.getName(), newCompositeSequence );
            }

            sequenceBuffer.add( collapsed );
            if ( csBuffer.containsKey( sequenceName ) )
                throw new IllegalArgumentException( "All probes must have unique names" );
            csBuffer.put( sequenceName, newCompositeSequence );
            if ( sequenceBuffer.size() == ArrayDesignSequenceProcessingServiceImpl.BATCH_SIZE ) {
                this.flushBuffer( bioSequences, sequenceBuffer, csBuffer );
            }

            if ( ++done % 1000 == 0 ) {
                percent = this.updateProgress( total, done, percent, null );
            }
        }

        this.flushBuffer( bioSequences, sequenceBuffer, csBuffer );
        this.updateProgress( total, done, percent, null );

        /*
         *
         */
        if ( !wasOriginallyLackingCompositeSequences ) {
            // usual case.
            percent = 0;
            done = 0;
            int numWithNoSequence = 0;
            for ( CompositeSequence originalCompositeSequence : arrayDesign.getCompositeSequences() ) {

                CompositeSequence compositeSequenceFromParse = quickFindMap.get( originalCompositeSequence.getName() );
                if ( compositeSequenceFromParse == null ) {
                    numWithNoSequence++;
                    this.notifyAboutMissingSequences( numWithNoSequence, originalCompositeSequence );
                    continue;
                }

                if ( log.isDebugEnabled() ) log.debug( originalCompositeSequence + " matches "
                        + compositeSequenceFromParse + " seq is "
                        + compositeSequenceFromParse.getBiologicalCharacteristic() );

                originalCompositeSequence
                        .setBiologicalCharacteristic( compositeSequenceFromParse.getBiologicalCharacteristic() );

                assert originalCompositeSequence.getBiologicalCharacteristic().getId() != null;

                originalCompositeSequence.setArrayDesign( compositeSequenceFromParse.getArrayDesign() );

                if ( ++done % 1000 == 0 ) {
                    percent = this.updateProgress( total, done, percent, numWithNoSequence );
                }
            }
            ArrayDesignSequenceProcessingServiceImpl.log
                    .info( numWithNoSequence + "/" + arrayDesign.getCompositeSequences().size()
                            + " probes could not be matched to a sequence" );
        }

        arrayDesign.setAdvertisedNumberOfDesignElements( compositeSequencesFromProbes.size() );
        ArrayDesignSequenceProcessingServiceImpl.log.info( "Updating " + arrayDesign );

        arrayDesignService.update( arrayDesign );

        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );
        ArrayDesignSequenceProcessingServiceImpl.log.info( "Done adding sequence information!" );
        return bioSequences;
    }

    @Override
    public Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, InputStream sequenceFile,
            SequenceType sequenceType ) throws IOException {
        return this.processArrayDesign( arrayDesign, sequenceFile, sequenceType, null );
    }


    @Override
    public Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, InputStream sequenceFile,
            SequenceType sequenceType, Taxon taxon ) throws IOException {
        return this.processArrayDesign( arrayDesign, sequenceFile, null, sequenceType, taxon );
    }

    /*
     * When submitting a file of sequences
     */
    @Override
    public Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, InputStream sequenceFile,
            InputStream sequenceIdentifierFile, SequenceType sequenceType, Taxon taxon ) throws IOException {

        // arrayDesign = arrayDesignService.thawRawAndProcessed( arrayDesign );

        if ( sequenceType.equals( SequenceType.AFFY_PROBE ) ) {
            return this.processAffymetrixDesign( arrayDesign, sequenceFile, taxon );
        } else if ( sequenceType.equals( SequenceType.OLIGO ) ) {
            return this.processOligoDesign( arrayDesign, sequenceFile, taxon );
        }
        taxon = this.validateTaxon( taxon, arrayDesign );

        this.checkForCompositeSequences( arrayDesign );

        FastaParser fastaParser = new FastaParser();
        fastaParser.parse( sequenceFile );
        Collection<BioSequence> bioSequences = fastaParser.getResults();

        Map<String, String> probe2acc = null;
        if ( sequenceIdentifierFile != null ) {
            probe2acc = this.parseAccessionFile( sequenceIdentifierFile );
        }

        // make two maps: one for genbank ids, one for the sequence name.
        Map<String, BioSequence> gbIdMap = new HashMap<>();
        Map<String, BioSequence> nameMap = new HashMap<>();

        int total = bioSequences.size() + arrayDesign.getCompositeSequences().size();
        int done = 0;
        int percent = 0;

        int i = 0;
        for ( BioSequence sequence : bioSequences ) {

            sequence.setType( sequenceType );
            sequence.setPolymerType( PolymerType.DNA );
            sequence.setTaxon( taxon );
            sequence = this.persistSequence( sequence );

            this.addToMaps( gbIdMap, nameMap, sequence );

            if ( ++i % 1000 == 0 ) {
                log.info( "Processed " + i + " sequences, last was " + sequence );
            }
        }

        ArrayDesignSequenceProcessingServiceImpl.log.info( i + " sequences done, updating composite sequences" );

        int numWithNoSequence = 0;
        int numMatchedByAccession = 0;
        int numMatchedByProbeName = 0;
        String mungeRegex = ArrayDesignSequenceProcessingServiceImpl.DUPLICATE_PROBE_NAME_MUNGE_SEPARATOR + ".+$";

        /*
         * Match the sequences to the probes
         */
        for ( CompositeSequence compositeSequence : arrayDesign.getCompositeSequences() ) {

            if ( ArrayDesignSequenceProcessingServiceImpl.log.isTraceEnabled() )
                ArrayDesignSequenceProcessingServiceImpl.log
                        .trace( "Looking for sequence for: " + compositeSequence.getName() );

            BioSequence match = null;
            if ( probe2acc != null && probe2acc.containsKey( compositeSequence.getName() ) ) {

                String seqAcc = probe2acc.get( compositeSequence.getName() );
                if ( nameMap.containsKey( seqAcc ) ) {
                    match = nameMap.get( seqAcc );
                    numMatchedByAccession++;
                }
            } else if ( nameMap.containsKey( compositeSequence.getName() ) ) {
                match = nameMap.get( compositeSequence.getName() );
                numMatchedByProbeName++;
            } else if ( compositeSequence.getName().matches( mungeRegex ) ) {
                String unMungedName = compositeSequence.getName().replaceFirst( mungeRegex, "" );
                if ( nameMap.containsKey( unMungedName ) ) {
                    numMatchedByProbeName++;
                    continue;
                }
            } else {
                BioSequence biologicalCharacteristic = compositeSequence.getBiologicalCharacteristic();
                if ( biologicalCharacteristic != null ) {
                    biologicalCharacteristic = bioSequenceService.thaw( biologicalCharacteristic );
                    if ( biologicalCharacteristic.getSequenceDatabaseEntry() != null && gbIdMap
                            .containsKey( biologicalCharacteristic.getSequenceDatabaseEntry().getAccession() ) ) {
                        match = gbIdMap.get( biologicalCharacteristic.getSequenceDatabaseEntry().getAccession() );
                        numMatchedByAccession++;
                    } else {
                        compositeSequence.setBiologicalCharacteristic( null );
                        numWithNoSequence++;
                        this.notifyAboutMissingSequences( numWithNoSequence, compositeSequence );
                    }
                } else {
                    numWithNoSequence++;
                    this.notifyAboutMissingSequences( numWithNoSequence, compositeSequence );
                }
            }

            if ( match != null ) {
                // overwrite the existing characteristic if necessary.
                compositeSequence.setBiologicalCharacteristic( match );
                compositeSequence.setArrayDesign( arrayDesign );
            }

            if ( ++done % 1000 == 0 ) {
                percent = this.updateProgress( total, done, percent, numWithNoSequence );
            }
        }

        ArrayDesignSequenceProcessingServiceImpl.log
                .info( numMatchedByAccession + "/" + arrayDesign.getCompositeSequences().size()
                        + " composite sequences were matched to sequences by GenBank accession" );
        ArrayDesignSequenceProcessingServiceImpl.log
                .info( numMatchedByProbeName + "/" + arrayDesign.getCompositeSequences().size()
                        + " composite sequences were matched to sequences by probe name" );

        if ( numWithNoSequence > 0 )
            ArrayDesignSequenceProcessingServiceImpl.log
                    .info( "There were " + numWithNoSequence + "/" + arrayDesign.getCompositeSequences().size()
                            + " composite sequences with no associated biological characteristic" );

        ArrayDesignSequenceProcessingServiceImpl.log.info( "Updating sequences on arrayDesign" );

        if ( arrayDesign.getPrimaryTaxon() == null && taxon != null ) {
            arrayDesign.setPrimaryTaxon( taxon );
        }
        arrayDesignService.update( arrayDesign );

        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );
        return bioSequences;

    }


    @Override
    public Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, InputStream sequenceIdentifierFile,
            String[] databaseNames, Taxon taxon, boolean force ) throws IOException {
        return this.processArrayDesign( arrayDesign, sequenceIdentifierFile, databaseNames, taxon, force, new SimpleFastaCmd( fastaCmdExe ) );
    }

    /*
     * When processing from a file of sequence IDs, retrieving missing sequences from blastdbs
     */
    @Override
    public Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, InputStream sequenceIdentifierFile,
            String[] databaseNames, Taxon taxon, boolean force, FastaCmd fc ) throws IOException {
        this.checkForCompositeSequences( arrayDesign );

        Map<String, String> probe2acc = this.parseAccessionFile( sequenceIdentifierFile );

        // values that were not found
        Collection<String> notFound = new HashSet<>( probe2acc.values() );

        // the actual thing values to search for (with version numbers)
        Collection<String> accessionsToFetch = new HashSet<>( probe2acc.values() );

        // only 1 taxon should be on array design if taxon not supplied on command line
        taxon = this.validateTaxon( taxon, arrayDesign );

        /*
         * Fill in sequences from BLAST databases.
         */
        int numSwitched = 0;

        Collection<BioSequence> retrievedSequences = this
                .searchBlastDbs( databaseNames, notFound, fc );

        // map of accessions to sequence.
        Map<String, BioSequence> found = this
                .findOrUpdateSequences( accessionsToFetch, retrievedSequences, taxon, force );

        Collection<BioSequence> finalResult = new HashSet<>( retrievedSequences );

        // replace the sequences.
        numSwitched = this.replaceSequences( arrayDesign, probe2acc, numSwitched, found );

        notFound = this.getUnFound( notFound, found );

        if ( !notFound.isEmpty() && taxon != null ) {

            Collection<String> stillLooking = new HashSet<>( notFound );
            notFound.clear();

            /*
             * clear the version number.
             */
            for ( String accession : stillLooking ) {
                notFound.remove( accession );
                accession = accession.replaceFirst( "\\.\\d+$", "" );
                notFound.add( accession );
            }
            assert !notFound.isEmpty();
            /*
             * See if they're already in Gemma. This is good for sequences that are not in genbank but have been loaded
             * previously.
             */
            found = this.findLocalSequences( notFound, taxon );
            finalResult.addAll( found.values() );

            numSwitched = this.replaceSequences( arrayDesign, probe2acc, numSwitched, found );
            notFound = this.getUnFound( notFound, found );
        }

        if ( !notFound.isEmpty() ) {
            this.logMissingSequences( arrayDesign, notFound );
        }

        ArrayDesignSequenceProcessingServiceImpl.log
                .info( numSwitched + " composite sequences had their biologicalCharacteristics changed" );

        if ( taxon != null ) {
            arrayDesign.setPrimaryTaxon( taxon );
        }

        arrayDesignService.update( arrayDesign );

        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );
        return finalResult;

    }

    @Override
    public Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, String[] databaseNames,
            boolean force ) {
        return this.processArrayDesign( arrayDesign, databaseNames, force, new SimpleFastaCmd( fastaCmdExe ) );
    }

    @Override
    public Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, String[] databaseNames,
            boolean force, FastaCmd fc ) {

        Map<String, BioSequence> accessionsToFetch = this.initializeFetchList( arrayDesign, force );

        if ( accessionsToFetch.isEmpty() ) {
            ArrayDesignSequenceProcessingServiceImpl.log.info( "No accessions to fetch, no processing will be done" );
            return null;
        }

        Collection<Taxon> taxaOnArray = arrayDesignService.getTaxa( arrayDesign );
        // not taxon found
        if ( taxaOnArray.isEmpty() ) {
            throw new IllegalArgumentException( "No taxon found for " + arrayDesign + ", please specify which taxon to run." );
        }

        Collection<String> notFound = accessionsToFetch.keySet();

        Collection<BioSequence> retrievedSequences = this
                .searchBlastDbs( databaseNames, notFound, fc );

        Map<String, BioSequence> found = this
                .findOrUpdateSequences( accessionsToFetch, retrievedSequences, taxaOnArray, force );

        Collection<BioSequence> finalResult = new HashSet<>( found.values() );
        notFound = this.getUnFound( notFound, found );

        if ( !notFound.isEmpty() ) {
            this.logMissingSequences( arrayDesign, notFound );
        }

        ArrayDesignSequenceProcessingServiceImpl.log.info( finalResult.size() + " sequences found" );
        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );

        return finalResult;

    }

    /**
     * Update a single sequence in the system.
     *
     * @param force If true, if an existing BioSequence that matches if found in the system, any existing sequence
     *              information in the BioSequence will be overwritten.
     * @return persistent BioSequence.
     */
    @Override
    public BioSequence processSingleAccession( String sequenceId, String[] databaseNames,
            boolean force ) {
        BioSequence found = this.searchBlastDbs( databaseNames, sequenceId, new SimpleFastaCmd( fastaCmdExe ) );
        if ( found == null )
            return null;
        return this.createOrUpdateGenBankSequence( found, force );

    }

    /**
     * If taxon is null then it has not been provided on the command line, then deduce the taxon from the arrayDesign.
     * If there are 0 or more than one taxon on the array design throw an error as this programme can only be run for 1
     * taxon at a time if processing from a file.
     *
     * @param  taxon                    Taxon as passed in on the command line
     * @param  arrayDesign              Array design to process
     * @return taxon Taxon to process
     * @throws IllegalArgumentException Thrown when there is not exactly 1 taxon.
     */
    @Override
    public Taxon validateTaxon( Taxon taxon, ArrayDesign arrayDesign ) throws IllegalArgumentException {

        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "Array design cannot be null" );
        }

        if ( taxon == null ) {
            if ( arrayDesign.getPrimaryTaxon() != null ) {
                return arrayDesign.getPrimaryTaxon();
            }
            Collection<Taxon> taxaOnArray = arrayDesignService.getTaxa( arrayDesign );

            if ( taxaOnArray.size() == 1 && taxaOnArray.iterator().next() != null ) {
                return taxaOnArray.iterator().next();
            }
            throw new IllegalArgumentException(
                    taxaOnArray.size() + " taxa found for " + arrayDesign + "please specify which taxon to run" );
        }
        return taxon;
    }

    private int replaceSequences( ArrayDesign arrayDesign, Map<String, String> probe2acc, int numSwitched,
            Map<String, BioSequence> found ) {
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            String probeName = cs.getName();
            String acc = probe2acc.get( probeName );
            if ( found.containsKey( acc ) ) {
                numSwitched++;
                ArrayDesignSequenceProcessingServiceImpl.log
                        .debug( "Setting seq. for " + cs + " to " + found.get( acc ) );
                cs.setBiologicalCharacteristic( found.get( acc ) );
            } else {
                log.debug( "No sequence found for " + acc );
            }
        }
        return numSwitched;
    }

    private void addToMaps( Map<String, BioSequence> gbIdMap, Map<String, BioSequence> nameMap, BioSequence sequence ) {
        nameMap.put( this.deMangleProbeId( sequence.getName() ), sequence );

        if ( sequence.getSequenceDatabaseEntry() != null ) {
            gbIdMap.put( sequence.getSequenceDatabaseEntry().getAccession(), sequence );
        } else {
            if ( ArrayDesignSequenceProcessingServiceImpl.log.isTraceEnabled() )
                ArrayDesignSequenceProcessingServiceImpl.log
                        .trace( "No sequence database entry for " + sequence.getName() );
        }
    }

    private void checkForCompositeSequences( ArrayDesign arrayDesign ) {
        boolean wasOriginallyLackingCompositeSequences = arrayDesign.getCompositeSequences().isEmpty();

        if ( wasOriginallyLackingCompositeSequences ) {
            throw new IllegalArgumentException(
                    "You need to pass in an array design that already has compositeSequences filled in." );
        }
    }

    /**
     * @param  found a new (non-persistent) biosequence that can be used to create a new entry or update an existing one
     *               with the sequence. The sequence would have come from GenBank.
     * @param  force If true, if an existing BioSequence that matches if found in the system, any existing sequence
     *               information in the BioSequence will be overwritten. Otherwise, the sequence will only be updated if
     *               the
     *               actual sequence information was missing in our DB and 'found' has a sequence.
     * @return persistent BioSequence.
     */
    private BioSequence createOrUpdateGenBankSequence( BioSequence found, boolean force ) {
        assert found != null;
        DatabaseEntry sequenceDatabaseEntry = found.getSequenceDatabaseEntry();

        assert sequenceDatabaseEntry != null; // this should always be the case because the sequences comes from
        // genbank (blastDb)
        assert sequenceDatabaseEntry.getExternalDatabase() != null;

        BioSequence existing;

        existing = bioSequenceService.findByAccession( sequenceDatabaseEntry );

        BioSequence result;
        if ( existing == null ) {
            if ( ArrayDesignSequenceProcessingServiceImpl.log.isDebugEnabled() )
                ArrayDesignSequenceProcessingServiceImpl.log.debug( "Find (or creating) new sequence " + found );

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
            result = this.updateExistingWithSequenceData( found, result );
        } else {
            this.fillInDatabaseEntry( found, result );
        }

        // result = bioSequenceService.thawRawAndProcessed( result );

        return result;
    }

    /**
     * When the probe id is in the format ArrayName:ProbeId, just return the ProbeId. For anything else return the
     * entire string.
     */
    private String deMangleProbeId( String probeId ) {
        String[] split = StringUtils.split( probeId, ":" );
        if ( split.length > 1 ) {
            return split[1];
        }
        return probeId;
    }

    /**
     * Unfortunately we have to make sure the database entry is filled in, even if we aren't using 'force'. This seems
     * due in part to an old bug in the system that left these blank. This is our opportunity to fill them in.
     */
    private void fillInDatabaseEntry( BioSequence found, BioSequence existing ) {
        if ( existing.getSequenceDatabaseEntry() == null ) {
            existing.setSequenceDatabaseEntry( found.getSequenceDatabaseEntry() );
            bioSequenceService.update( existing );
        }
    }

    private void fillInGenBank( Collection<BioSequence> retrievedSequences ) {
        ExternalDatabase genbank = this.getGenBank();
        assert genbank.getId() != null;
        for ( BioSequence bioSequence : retrievedSequences ) {
            if ( bioSequence.getSequenceDatabaseEntry() == null ) {
                ArrayDesignSequenceProcessingServiceImpl.log.warn( "No database entry for " + bioSequence );
                continue;
            }
            if ( bioSequence.getSequenceDatabaseEntry().getExternalDatabase().getId() == null ) {
                bioSequence.getSequenceDatabaseEntry().setExternalDatabase( genbank );
            }
        }
    }

    private Map<String, BioSequence> findLocalSequences( Collection<String> identifiersToSearch, Taxon taxon ) {
        Map<String, BioSequence> found = new HashMap<>();
        for ( String id : identifiersToSearch ) {
            BioSequence template = BioSequence.Factory.newInstance();
            template.setTaxon( taxon );
            template.setName( id );
            BioSequence seq = bioSequenceService.find( template );
            if ( seq != null ) {
                seq = bioSequenceService.thaw( seq );
                found.put( id, seq );
            }
        }
        return found;
    }

    /**
     * Copy sequences into the original versions, or create new sequences in the DB, as needed.
     *
     * @param  force If true, if an existing BioSequence that matches if found in the system, any existing sequence
     *               information in the BioSequence will be overwritten.
     * @return Items that were found.
     */
    private Map<String, BioSequence> findOrUpdateSequences( Collection<String> accessionsToFetch,
            Collection<BioSequence> retrievedSequences, Taxon taxon, boolean force ) {

        retrievedSequences = bioSequenceService.thaw( retrievedSequences );

        Map<String, BioSequence> found = new HashMap<>();
        for ( BioSequence sequence : retrievedSequences ) {
            if ( ArrayDesignSequenceProcessingServiceImpl.log.isDebugEnabled() )
                ArrayDesignSequenceProcessingServiceImpl.log.debug( "Processing retrieved sequence: " + sequence );
            sequence.setTaxon( taxon );
            sequence = this.createOrUpdateGenBankSequence( sequence, force );
            String accession = sequence.getSequenceDatabaseEntry().getAccession();
            found.put( accession, sequence );
            accessionsToFetch.remove( accession );
        }
        return found;
    }

    /**
     * Copy sequences into the original versions, or create new sequences in the DB, as needed.
     *
     * @param  accessionsToFetch  accessions that we need to fill in
     * @param  retrievedSequences candidate sequence information for copying into the database.
     * @param  force              If true, if an existing BioSequence that matches if found in the system, any existing
     *                            sequence
     *                            information in the BioSequence will be overwritten.
     * @param  taxa               Representing taxa on array
     * @return Items that were found.
     */
    private Map<String, BioSequence> findOrUpdateSequences( Map<String, BioSequence> accessionsToFetch,
            Collection<BioSequence> retrievedSequences, Collection<Taxon> taxa, boolean force ) {

        Map<String, BioSequence> found = new HashMap<>();
        for ( Taxon taxon : taxa ) {

            if ( taxon == null ) {
                ArrayDesignSequenceProcessingServiceImpl.log
                        .warn( "Null taxon ..." ); // probably should be an exception
            }
            assert taxon != null;

            boolean warned = false;
            for ( BioSequence sequence : retrievedSequences ) {
                if ( sequence.getTaxon() == null ) {
                    if ( !warned ) {
                        ArrayDesignSequenceProcessingServiceImpl.log
                                .warn( "Sequence taxon is null [" + sequence + "], copying array taxon " + taxon
                                        + " ; further warnings for this array taxon are suppressed." );
                    }
                    warned = true;
                } else if ( !sequence.getTaxon().equals( taxon ) ) {
                    continue;
                }

                sequence.setTaxon( taxon );
                if ( sequence.getSequenceDatabaseEntry() == null ) {
                    ArrayDesignSequenceProcessingServiceImpl.log
                            .warn( "Sequence from BLAST db lacks database entry: " + sequence + "; skipping" );
                    continue;
                }
                sequence = this.createOrUpdateGenBankSequence( sequence, force );
                sequence = this.bioSequenceService.thaw( sequence );
                String accession = sequence.getSequenceDatabaseEntry().getAccession();
                found.put( accession, sequence );
                accessionsToFetch.remove( accession );
            }
        }
        return found;
    }

    /**
     * for affymetrix processing
     *
     * @param bioSequences   bio sequences
     * @param sequenceBuffer sequence buffer
     * @param csBuffer       cs buffer
     */
    private void flushBuffer( Collection<BioSequence> bioSequences, Collection<BioSequence> sequenceBuffer,
            Map<String, CompositeSequence> csBuffer ) {
        Collection<BioSequence> newOnes = bioSequenceService.findOrCreate( sequenceBuffer );
        bioSequences.addAll( newOnes );
        for ( BioSequence sequence : newOnes ) {
            CompositeSequence cs = csBuffer.get( sequence.getName() );
            assert cs != null;
            if ( log.isDebugEnabled() ) {
                log.debug( "Updating " + cs + " to sequence " + sequence + ": " + sequence.getSequence() );
            }
            cs.setBiologicalCharacteristic( sequence );
        }
        csBuffer.clear();
        sequenceBuffer.clear();
    }

    /**
     * Used to check if an IMAGE clone exists to use for an accession. If the IMAGE clone is used instead, we update the
     * composite sequence.
     */
    private String getAccession( CompositeSequence cs ) {
        BioSequence bs = cs.getBiologicalCharacteristic();
        if ( bs.getSequenceDatabaseEntry() == null ) {
            return null;
        }
        bs = this.bioSequenceService.thaw( bs );
        return bs.getSequenceDatabaseEntry().getAccession();
    }

    private ExternalDatabase getGenBank() {
        return this.externalDatabaseService.findByName( ExternalDatabases.GENBANK );
    }

    private Collection<String> getUnFound( Collection<String> accessionsToFetch, Map<String, BioSequence> found ) {
        Collection<String> notFound = new HashSet<>();
        for ( String accession : accessionsToFetch ) {
            if ( !found.containsKey( accession ) ) {
                notFound.add( accession );
            }
        }
        return notFound;
    }

    private void informAboutFetchListResults( ArrayDesign arrayDesign, Map<String, BioSequence> accessionsToFetch,
            int sequenceProvided, int noSequence ) {
        ArrayDesignSequenceProcessingServiceImpl.log
                .info( "Array Design has " + accessionsToFetch.size() + " accessions to fetch for " + arrayDesign
                        .getCompositeSequences().size() + " compositeSequences" );
        ArrayDesignSequenceProcessingServiceImpl.log
                .info( sequenceProvided + " had sequences already and will not be replaced" );
        ArrayDesignSequenceProcessingServiceImpl.log
                .info( noSequence + " have no BioSequence association at all and will not be processed further." );
    }

    /**
     * @param  force if true, sequence will be replaced even if it is already there.
     * @return map of biosequence accessions to BioSequences (the existing ones)
     */
    private Map<String, BioSequence> initializeFetchList( ArrayDesign arrayDesign, boolean force ) {
        Map<String, BioSequence> accessionsToFetch = new HashMap<>();
        int sequenceProvided = 0;
        int noSequence = 0;
        boolean warned = false;
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            BioSequence bs = cs.getBiologicalCharacteristic();
            if ( bs == null ) {
                warned = this.warnAboutMissingSequence( noSequence, warned, cs );
                noSequence++;
                continue;
            }

            if ( !force && StringUtils.isNotBlank( bs.getSequence() ) ) {
                sequenceProvided++;
                continue;
            }

            String accession = this.getAccession( cs );

            if ( accession == null ) {
                if ( ArrayDesignSequenceProcessingServiceImpl.log.isDebugEnabled() )
                    ArrayDesignSequenceProcessingServiceImpl.log.debug( "No accession for " + cs + ": " + bs );
                continue;
            }

            accessionsToFetch.put( accession, bs );
        }
        this.informAboutFetchListResults( arrayDesign, accessionsToFetch, sequenceProvided, noSequence );
        return accessionsToFetch;
    }

    private void logMissingSequences( ArrayDesign arrayDesign, Collection<String> notFound ) {
        ArrayDesignSequenceProcessingServiceImpl.log
                .warn( notFound.size() + " sequences were not found (or were already filled in) for " + arrayDesign );
        StringBuilder buf = new StringBuilder();
        buf.append( "Missing (or already present) sequences for following accessions : " );
        int i = 0;
        for ( String string : notFound ) {
            string = string.replaceFirst( "\\.\\d$", "" );
            buf.append( string ).append( " " );
            if ( ++i > 10 ) {
                buf.append( "... (skipping logging of rest)" );
                break;
            }
        }
        ArrayDesignSequenceProcessingServiceImpl.log.info( buf.toString() );
    }

    private void notifyAboutMissingSequences( int numWithNoSequence, CompositeSequence compositeSequence ) {
        if ( numWithNoSequence == ArrayDesignSequenceProcessingServiceImpl.MAX_NUM_WITH_NO_SEQUENCE_FOR_DETAILED_WARNINGS ) {
            ArrayDesignSequenceProcessingServiceImpl.log.warn( "More than "
                    + ArrayDesignSequenceProcessingServiceImpl.MAX_NUM_WITH_NO_SEQUENCE_FOR_DETAILED_WARNINGS
                    + " compositeSequences do not have" + " biologicalCharacteristics, skipping further details." );
        } else if ( numWithNoSequence < ArrayDesignSequenceProcessingServiceImpl.MAX_NUM_WITH_NO_SEQUENCE_FOR_DETAILED_WARNINGS ) {
            ArrayDesignSequenceProcessingServiceImpl.log
                    .warn( "No sequence match for " + compositeSequence + " (Description=" + compositeSequence
                            .getDescription() + "); it will not have a biologicalCharacteristic!" );
        }
    }

    /**
     * @param sequenceIdentifierFile with two columns: first is probe id, second is genbank accession.
     */
    private Map<String, String> parseAccessionFile( InputStream sequenceIdentifierFile ) throws IOException {
        try ( BufferedReader br = new BufferedReader( new InputStreamReader( sequenceIdentifierFile ) ) ) {

            String line;

            StopWatch timer = new StopWatch();
            timer.start();

            Map<String, String> probe2acc = new HashMap<>();
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
                    ArrayDesignSequenceProcessingServiceImpl.log
                            .info( count + " / " + totalLines + " probes read so far have identifiers" );
                }
            }
            ArrayDesignSequenceProcessingServiceImpl.log.info( count + " / " + totalLines + " probes have accessions" );
            return probe2acc;
        }

    }

    /**
     * If the sequence already exists
     */
    private BioSequence persistSequence( BioSequence sequence ) {
        return ( BioSequence ) persisterHelper.persistOrUpdate( sequence );
    }

    /**
     * @param sequenceFile; the expected format is described in {@link ProbeSequenceParser}
     * @see                 ProbeSequenceParser
     */
    private Collection<BioSequence> processOligoDesign( ArrayDesign arrayDesign, InputStream sequenceFile, Taxon taxon )
            throws IOException {
        this.checkForCompositeSequences( arrayDesign );

        ProbeSequenceParser parser = new ProbeSequenceParser();
        parser.parse( sequenceFile );

        int total = arrayDesign.getCompositeSequences().size();
        int done = 0;
        int percent = 0;
        taxon = this.validateTaxon( taxon, arrayDesign );

        ArrayDesignSequenceProcessingServiceImpl.log.info( "Sequences done, updating composite sequences" );

        int numWithNoSequence = 0;

        Collection<BioSequence> res = new HashSet<>();
        for ( CompositeSequence compositeSequence : arrayDesign.getCompositeSequences() ) {

            if ( ArrayDesignSequenceProcessingServiceImpl.log.isTraceEnabled() )
                ArrayDesignSequenceProcessingServiceImpl.log
                        .trace( "Looking for sequence for: " + compositeSequence.getName() );
            BioSequence sequence = parser.get( compositeSequence.getName() );

            if ( sequence != null ) {
                // overwrite the existing characteristic if necessary.
                assert sequence.getSequence() != null;
                sequence.setType( SequenceType.OLIGO );
                sequence.setPolymerType( PolymerType.DNA );
                sequence.setTaxon( taxon );
                sequence = this.persistSequence( sequence );
                compositeSequence.setBiologicalCharacteristic( sequence );
                compositeSequence.setArrayDesign( arrayDesign );
                res.add( sequence );
            } else {
                numWithNoSequence++;
                this.notifyAboutMissingSequences( numWithNoSequence, compositeSequence );
            }

            if ( ++done % 1000 == 0 ) {
                percent = this.updateProgress( total, done, percent, numWithNoSequence );
            }
        }

        if ( numWithNoSequence > 0 )
            ArrayDesignSequenceProcessingServiceImpl.log
                    .info( "There were " + numWithNoSequence + "/" + arrayDesign.getCompositeSequences().size()
                            + " composite sequences with no associated biological characteristic" );

        ArrayDesignSequenceProcessingServiceImpl.log.info( "Updating sequences on arrayDesign" );
        arrayDesignService.update( arrayDesign );
        return res;
    }

    private Collection<BioSequence> searchBlastDbs( String[] databaseNames,
            Collection<String> accessionsToFetch, FastaCmd fc ) {

        Collection<BioSequence> retrievedSequences = new HashSet<>();
        for ( String dbName : databaseNames ) {
            Collection<BioSequence> moreBioSequences = fc.getBatchAccessions( accessionsToFetch, dbName );

            if ( ArrayDesignSequenceProcessingServiceImpl.log.isDebugEnabled() )
                ArrayDesignSequenceProcessingServiceImpl.log
                        .debug( moreBioSequences.size() + " sequences of " + accessionsToFetch.size() + " fetched "
                                + " from " + dbName );
            retrievedSequences.addAll( moreBioSequences );
        }

        this.fillInGenBank( retrievedSequences );

        return retrievedSequences;
    }

    /**
     * Search for a single accession
     */
    private BioSequence searchBlastDbs( String[] databaseNames, String accessionToFetch,
            FastaCmd fc ) {

        for ( String dbName : databaseNames ) {
            BioSequence moreBioSequence = fc.getByAccession( accessionToFetch, dbName );
            if ( moreBioSequence != null )
                return moreBioSequence;
        }
        return null;

    }

    /**
     * Replace information in "existing" with data from "found".
     */
    private BioSequence updateExistingWithSequenceData( BioSequence found, BioSequence existing ) {
        assert found != null;
        assert existing != null;

        if ( existing.getType() == null )
            existing.setType( found.getType() ); // generic...
        existing.setLength( found.getLength() );
        assert found.getSequence() != null;

        existing.setSequence( found.getSequence() );
        existing.setIsApproximateLength( found.getIsApproximateLength() );

        if ( existing.getSequenceDatabaseEntry() == null ) {
            ArrayDesignSequenceProcessingServiceImpl.log
                    .debug( "Inserting database entry into existing sequence " + existing );
            existing.setSequenceDatabaseEntry( found.getSequenceDatabaseEntry() );
        }

        // This is just for debugging purposes -- some array designs give suspicious sequence information.
        if ( existing.getSequence().length() > 10e4 ) {
            ArrayDesignSequenceProcessingServiceImpl.log
                    .warn( existing + " - new sequence is very long for an expression probe (" + existing.getSequence()
                            .length() + " bases)" );
        }

        bioSequenceService.update( existing );
        if ( ArrayDesignSequenceProcessingServiceImpl.log.isDebugEnabled() )
            ArrayDesignSequenceProcessingServiceImpl.log.debug( "Updated " + existing + " with sequence " + StringUtils
                    .abbreviate( existing.getSequence(), 20 ) );

        assert found.getSequenceDatabaseEntry().getExternalDatabase() != null;
        return existing;
    }

    private int updateProgress( int totalThingsToDo, int howManyAreDone, int percentDoneLastTimeWeChecked, Integer numWithNoSequence ) {
        int newPercent = ( int ) Math.ceil( ( 100.00 * howManyAreDone / totalThingsToDo ) );
        if ( newPercent > percentDoneLastTimeWeChecked ) {
            ArrayDesignSequenceProcessingServiceImpl.log
                    .info( howManyAreDone + " sequence+probes of " + totalThingsToDo + " processed; "
                            + ( numWithNoSequence == null ? "" : numWithNoSequence + " had no matching sequence." ) );
        }

        return newPercent;
    }

    private boolean warnAboutMissingSequence( int noSequence, boolean warned, CompositeSequence cs ) {
        if ( !warned ) {
            if ( noSequence < 20 ) {
                ArrayDesignSequenceProcessingServiceImpl.log.warn( cs + " has no biosequence" );
            } else {
                ArrayDesignSequenceProcessingServiceImpl.log
                        .warn( "...More than 20 are missing sequences, details omitted" );
                warned = true;
            }
        }
        return warned;
    }

}
