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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.sequence.SequenceManipulation;
import ubic.gemma.loader.genome.FastaCmd;
import ubic.gemma.loader.genome.FastaParser;
import ubic.gemma.loader.genome.SimpleFastaCmd;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.designElement.Reporter;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.biosequence.PolymerType;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.util.progress.ProgressData;
import ubic.gemma.util.progress.ProgressManager;

/**
 * Handles collapsing the sequences, attaching sequences to DesignElements
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="arrayDesignSequenceProcessingService"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="bioSequenceService" ref="bioSequenceService"
 */
public class ArrayDesignSequenceProcessingService {

    private static final int BATCH_SIZE = 100;

    private static final int MAX_VERSION_NUMBER = 10;

    private static Log log = LogFactory.getLog( ArrayDesignSequenceProcessingService.class.getName() );

    private ArrayDesignService arrayDesignService;

    private PersisterHelper persisterHelper;

    private BioSequenceService bioSequenceService;

    // /**
    // * For the case where the reporter and compositeSequence are the same thing, make a new reporter and add it to the
    // * array design.
    // *
    // * @param arrayDesign
    // * @param compositeSequence
    // * @param bioSequence
    // */
    // private void addReporter( CompositeSequence compositeSequence ) {
    // BioSequence bioSequence = compositeSequence.getBiologicalCharacteristic();
    // Reporter reporter = Reporter.Factory.newInstance();
    // reporter.setCompositeSequence( compositeSequence );
    // reporter.setImmobilizedCharacteristic( bioSequence );
    // reporter.setName( compositeSequence.getName() );
    // reporter.setDescription( "Reporter same as composite sequence" );
    // compositeSequence.getComponentReporters().add( reporter );
    // }

    /**
     * Associate sequences with an array design.
     * 
     * @param designElements
     * @param sequences, for Affymetrix these should be the Collapsed probe sequences.
     * @throws IOException
     */
    public void assignSequencesToDesignElements( Collection<? extends DesignElement> designElements,
            Collection<BioSequence> sequences ) {

        Map<String, BioSequence> nameMap = new HashMap<String, BioSequence>();
        for ( BioSequence sequence : sequences ) {
            nameMap.put( this.deMangleProbeId( sequence.getName() ), sequence );
        }

        int numNotFound = 0;
        for ( DesignElement designElement : designElements ) {
            if ( !nameMap.containsKey( designElement.getName() ) ) {
                log.debug( "No sequence matches " + designElement.getName() );
                numNotFound++;
                continue;
            }

            if ( designElement instanceof CompositeSequence ) {
                ( ( CompositeSequence ) designElement ).setBiologicalCharacteristic( nameMap.get( designElement
                        .getName() ) );
            } else if ( designElement instanceof Reporter ) {
                // ( ( Reporter ) designElement ).setImmobilizedCharacteristic( nameMap.get( designElement.getName() )
                // );
            } else {
                throw new IllegalStateException( "DesignElement was not of a known class" );
            }
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
    public void assignSequencesToDesignElements( Collection<? extends DesignElement> designElements, File fastaFile )
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
    protected void assignSequencesToDesignElements( Collection<? extends DesignElement> designElements,
            InputStream fastaFile ) throws IOException {

        FastaParser fp = new FastaParser();
        fp.parse( fastaFile );
        Collection<BioSequence> sequences = fp.getResults();
        log.debug( "Parsed " + sequences.size() + " sequences" );

        assignSequencesToDesignElements( designElements, sequences );
    }

    /**
     * Collapse probe sequences down into biosequences.
     * 
     * @param arrayName
     * @param probeSequences
     * @return
     * @throws IOException
     */
    public Collection<BioSequence> collapse( Collection<CompositeSequence> probeSequences ) {
        Collection<BioSequence> results = new HashSet<BioSequence>();
        for ( CompositeSequence sequence : probeSequences ) {
            BioSequence m = SequenceManipulation.collapse( sequence );
            m.setDescription( "Collapsed from probes for " + sequence.getName() + " ["
                    + sequence.getArrayDesign().getName() + "]" );
            results.add( m );
        }
        return results;
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

    /**
     * Use this to add sequences to an existing Affymetrix design.
     * 
     * @param arrayDesign An existing ArrayDesign that already has compositeSequences filled in.
     * @param probeSequenceFile InputStream from a tab-delimited probe sequence file.
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public Collection<BioSequence> processAffymetrixDesign( ArrayDesign arrayDesign, InputStream probeSequenceFile,
            Taxon taxon ) throws IOException {

        log.info( "Processing Affymetrix design" );

        boolean wasOriginallyLackingCompositeSequences = arrayDesign.getCompositeSequences().size() == 0;

        Collection<BioSequence> bioSequences = new HashSet<BioSequence>();

        int done = 0;
        int percent = 0;

        AffyProbeReader apr = new AffyProbeReader();
        apr.parse( probeSequenceFile );
        Collection<CompositeSequence> compositeSequencesFromProbes = apr.getResults();

        int total = compositeSequencesFromProbes.size();

        Map<String, CompositeSequence> quickFindMap = new HashMap<String, CompositeSequence>();
        Collection<BioSequence> sequenceBuffer = new ArrayList<BioSequence>();
        Collection<CompositeSequence> csBuffer = new ArrayList<CompositeSequence>();
        for ( CompositeSequence compositeSequence : compositeSequencesFromProbes ) {

            compositeSequence.setArrayDesign( arrayDesign );
            BioSequence collapsed = SequenceManipulation.collapse( compositeSequence );
            collapsed.setName( compositeSequence.getName() + "_collapsed" );
            collapsed.setType( SequenceType.AFFY_COLLAPSED );
            collapsed.setPolymerType( PolymerType.DNA );
            collapsed.setTaxon( taxon );

            sequenceBuffer.add( collapsed );
            csBuffer.add( compositeSequence );
            if ( sequenceBuffer.size() == BATCH_SIZE ) {
                flushBuffer( bioSequences, sequenceBuffer, csBuffer );
            }

            if ( wasOriginallyLackingCompositeSequences ) {
                arrayDesign.getCompositeSequences().add( compositeSequence );
            } else {
                quickFindMap.put( compositeSequence.getName(), compositeSequence );
            }

            if ( ++done % 1000 == 0 ) {
                percent = updateProgress( total, done, percent );
            }
        }
        flushBuffer( bioSequences, sequenceBuffer, csBuffer );
        updateProgress( total, done, percent );

        if ( !wasOriginallyLackingCompositeSequences ) {
            for ( CompositeSequence originalCompositeSequence : arrayDesign.getCompositeSequences() ) {
                // go back and fill this information into the composite sequences, namely the database entry
                // information.
                CompositeSequence compositeSequenceFromParse = quickFindMap.get( originalCompositeSequence.getName() );
                if ( compositeSequenceFromParse == null ) {
                    throw new IllegalArgumentException( "Array Design file did not contain "
                            + originalCompositeSequence.getName() );
                }

                originalCompositeSequence.setBiologicalCharacteristic( compositeSequenceFromParse
                        .getBiologicalCharacteristic() );

                assert originalCompositeSequence.getBiologicalCharacteristic().getId() != null;

                originalCompositeSequence.setArrayDesign( compositeSequenceFromParse.getArrayDesign() );

            }
        }

        arrayDesign.setAdvertisedNumberOfDesignElements( compositeSequencesFromProbes.size() );
        arrayDesignService.update( arrayDesign );

        return bioSequences;
    }

    @SuppressWarnings("unchecked")
    private void flushBuffer( Collection<BioSequence> bioSequences, Collection<BioSequence> sequenceBuffer,
            Collection<CompositeSequence> csBuffer ) {
        Collection<BioSequence> newOnes = bioSequenceService.create( sequenceBuffer );
        bioSequences.addAll( newOnes );
        Iterator<CompositeSequence> csit = csBuffer.iterator();
        for ( BioSequence sequence : newOnes ) {
            CompositeSequence cs = csit.next();
            cs.setBiologicalCharacteristic( sequence );
        }
        csBuffer.clear();
        sequenceBuffer.clear();
    }

    /**
     * Create a new Affymetrix design from scratch, given the name.
     * 
     * @param arrayDesignName design name.
     * @param arrayDesignFile design file in our 'old fashioned' format.
     * @param probeSequenceFile probe file
     * @param taxon
     * @return ArrayDesign with CompositeSequences, Reporters, ImmobilizedCharacteristics and BiologicalCharacteristics
     *         filled in.
     */
    protected ArrayDesign processAffymetrixDesign( String arrayDesignName, InputStream arrayDesignFile,
            InputStream probeSequenceFile, Taxon taxon ) throws IOException {
        ArrayDesign result = ArrayDesign.Factory.newInstance();
        result.setName( arrayDesignName );

        Contact contact = Contact.Factory.newInstance();
        contact.setName( "Affymetrix" );
        result.setDesignProvider( contact );

        result = arrayDesignService.create( result );

        CompositeSequenceParser csp = new CompositeSequenceParser();
        csp.parse( arrayDesignFile );
        Collection<CompositeSequence> rawCompositeSequences = csp.getResults();
        result.setCompositeSequences( rawCompositeSequences );

        this.processAffymetrixDesign( result, probeSequenceFile, taxon );

        return result;
    }

    /**
     * @param Array design name.
     * @param Array design file in our 'old fashioned' format.
     * @param Affymetrix probe file
     * @param taxon
     * @return ArrayDesign with CompositeSequences, Reporters, ImmobilizedCharacteristics and BiologicalCharacteristics
     *         filled in.
     */
    public ArrayDesign processAffymetrixDesign( String arrayDesignName, String arrayDesignFile,
            String probeSequenceFile, Taxon taxon ) throws IOException {
        InputStream arrayDesignFileStream = new BufferedInputStream( new FileInputStream( arrayDesignFile ) );
        InputStream probeSequenceFileStream = new BufferedInputStream( new FileInputStream( probeSequenceFile ) );
        return this.processAffymetrixDesign( arrayDesignName, arrayDesignFileStream, probeSequenceFileStream, taxon );
    }

    public Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, String[] databaseNames ) {
        return this.processArrayDesign( arrayDesign, databaseNames, null );
    }

    /**
     * For the case where the sequences are retrieved simply by the Genbank accession. For this to work, the array
     * design must already have the biosequence objects, but they haven't been populated with the actual sequences (if
     * they have, the values will be replaced)
     * 
     * @param arrayDesign
     * @param databaseNames the names of the BLAST-formatted databases to search (e.g., nt, est_mouse)
     * @return
     */
    public Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, String[] databaseNames,
            String blastDbHome ) {
        Map<String, BioSequence> accessionsToFetch = new HashMap<String, BioSequence>();
        Collection<String> notFound = accessionsToFetch.keySet();

        int versionNumber = 1;
        initializeFetchList( arrayDesign, accessionsToFetch, versionNumber );

        Collection<BioSequence> finalResult = new HashSet<BioSequence>();

        while ( versionNumber < MAX_VERSION_NUMBER ) {
            Collection<BioSequence> retrievedSequences = searchBlastDbs( databaseNames, blastDbHome, notFound );

            Map<String, BioSequence> found = updateSequences( accessionsToFetch, retrievedSequences );

            finalResult.addAll( found.values() );

            notFound = getUnFound( notFound, found );

            if ( notFound.isEmpty() ) {
                break;
            }

            // logMissingSequences( arrayDesign, notFound );

            // bump up the version numbers.
            ++versionNumber;

            for ( String accession : notFound ) {
                // remove the version number and increase it
                BioSequence bs = accessionsToFetch.get( accession );
                accessionsToFetch.remove( accession );
                accession = accession.replaceFirst( "\\d$", Integer.toString( versionNumber ) );
                accessionsToFetch.put( accession, bs );
            }
            notFound = accessionsToFetch.keySet();

        }

        if ( !notFound.isEmpty() ) {
            logMissingSequences( arrayDesign, notFound );
        }
        return finalResult;

    }

    /**
     * @param arrayDesign
     * @param notFound
     * @return
     */
    private void logMissingSequences( ArrayDesign arrayDesign, Collection<String> notFound ) {
        log.warn( notFound.size() + " sequences were not found for " + arrayDesign );
        StringBuilder buf = new StringBuilder();
        buf.append( "Missing sequences for following accessions " + "at version numbers up to " + MAX_VERSION_NUMBER
                + " : " );
        for ( String string : notFound ) {
            string = string.replaceFirst( "\\.\\d$", "" );
            buf.append( string + " " );
        }
        log.info( buf.toString() );
    }

    /**
     * @param arrayDesign
     * @param accessionsToFetch
     * @param versionNumber
     */
    private void initializeFetchList( ArrayDesign arrayDesign, Map<String, BioSequence> accessionsToFetch,
            int versionNumber ) {
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            BioSequence bs = cs.getBiologicalCharacteristic();
            if ( bs == null ) {
                log.warn( cs + " has no biosequence" );
                continue;
            }
            String accession = bs.getSequenceDatabaseEntry().getAccession();
            accession = addVersionNumber( accession, versionNumber ); // wild guess - we don't know the version.
            accessionsToFetch.put( accession, bs );
        }
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
     * copy sequences into the original versions.
     * 
     * @param accessionsToFetch
     * @param retrievedSequences
     * @return
     */
    private Map<String, BioSequence> updateSequences( Map<String, BioSequence> accessionsToFetch,
            Collection<BioSequence> retrievedSequences ) {

        Map<String, BioSequence> found = new HashMap<String, BioSequence>();
        for ( BioSequence sequence : retrievedSequences ) {
            String sequenceString = sequence.getSequence();

            if ( StringUtils.isBlank( sequenceString ) ) {
                log.warn( "Blank sequence for " + sequence );
                continue;
            }

            String accession = sequence.getSequenceDatabaseEntry().getAccession();
            BioSequence old = accessionsToFetch.get( accession );

            old.setSequence( sequenceString );
            old.setLength( new Long( sequence.getSequence().length() ) );
            old.setIsApproximateLength( false );
            found.put( accession, old );
            accessionsToFetch.remove( accession );
        }
        bioSequenceService.update( found.values() );
        return found;
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
        return retrievedSequences;
    }

    /**
     * Add a version number if it is missing; this is needed for sucessful retrieval from blast databases.
     * 
     * @param accession
     * @return
     */
    private String addVersionNumber( String accession, int versionNumber ) {
        if ( !accession.matches( "\\.\\d$" ) ) {
            accession = accession + "." + versionNumber;
        }
        return accession;
    }

    /**
     * The sequence file <em>must</em> provide an unambiguous way to associate the sequences with design elements on
     * the array.
     * <p>
     * If the SequenceType is AFFY_PROBE, the sequences will be treated as probes in probe sets, in Affymetrix 'tabbed'
     * format. Otherwise the format of the file is assumed to be FASTA, with one CompositeSequence per FASTA element;
     * there is further assumed to be just one Reporter per CompositeSequence (that is, they are the same thing). The
     * FASTA file must use a standard defline format (as described at
     * {@link http://en.wikipedia.org/wiki/Fasta_format#Sequence_identifiers}.
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
     * @param taxon
     * @throws IOException
     * @see ubic.gemma.loader.genome.FastaParser
     */
    @SuppressWarnings("unchecked")
    public Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, InputStream sequenceFile,
            SequenceType sequenceType, Taxon taxon ) throws IOException {

        if ( sequenceType == SequenceType.AFFY_PROBE ) {
            return this.processAffymetrixDesign( arrayDesign, sequenceFile, taxon );
        }

        log.info( "Processing non-Affymetrix design" );

        boolean wasOriginallyLackingCompositeSequences = arrayDesign.getCompositeSequences().size() == 0;

        if ( wasOriginallyLackingCompositeSequences ) {
            throw new IllegalArgumentException(
                    "You need to pass in an array design that already has compositeSequences filled in." );
        }

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

            // find and update?
            sequence = ( BioSequence ) persisterHelper.persist( sequence );

            nameMap.put( this.deMangleProbeId( sequence.getName() ), sequence );

            if ( sequence.getSequenceDatabaseEntry() != null ) {
                gbIdMap.put( sequence.getSequenceDatabaseEntry().getAccession(), sequence );
            } else {
                if ( log.isWarnEnabled() ) log.warn( "No sequence database entry for " + sequence.getName() );
            }

            if ( ++done % 1000 == 0 ) {
                percent = updateProgress( total, done, percent );
            }

        }

        for ( CompositeSequence compositeSequence : arrayDesign.getCompositeSequences() ) {

            // go back and fill information into the composite sequences, namely the database entry information.
            BioSequence match = null;
            if ( nameMap.containsKey( compositeSequence.getName() ) ) {
                match = nameMap.get( compositeSequence.getName() );
            } else if ( compositeSequence.getBiologicalCharacteristic() != null
                    && compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry() != null
                    && gbIdMap.containsKey( compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry()
                            .getAccession() ) ) {
                match = gbIdMap.get( compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry()
                        .getAccession() );
            } else {
                throw new IllegalStateException( "No match for " + compositeSequence );
            }

            assert match.getId() != null;

            // overwrite the existing characteristic if necessary.
            compositeSequence.setBiologicalCharacteristic( match );
            compositeSequence.setArrayDesign( arrayDesign );

            // addReporter( compositeSequence );

            if ( ++done % 1000 == 0 ) {
                percent = updateProgress( total, done, percent );
            }
        }

        log.info( "Updating sequences on arrayDesign" );
        arrayDesignService.update( arrayDesign );

        return bioSequences;

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
            ProgressManager.updateCurrentThreadsProgressJob( new ProgressData( newPercent, howManyAreDone
                    + " items of " + totalThingsToDo + " processed." ) );
        }

        log.info( howManyAreDone + " items of " + totalThingsToDo + " processed." );

        return newPercent;
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

    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }
}
