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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.sequence.SequenceManipulation;
import ubic.gemma.loader.genome.FastaParser;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.designElement.Reporter;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.PolymerType;
import ubic.gemma.model.genome.biosequence.SequenceType;

/**
 * Handles collapsing the sequences, attaching sequences to DesignElements
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignSequenceProcessor {

    private static Log log = LogFactory.getLog( ArrayDesignSequenceProcessor.class.getName() );

    /**
     * Collapse probe sequences down into biosequences.
     * 
     * @param arrayName
     * @param probeSequences
     * @return
     * @throws IOException
     */
    public Collection<BioSequence> collapse( String arrayName, Collection<CompositeSequence> probeSequences )
            throws IOException {
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
            String seqName = sequence.getName();
            String[] toks = StringUtils.split( seqName, ':' );
            assert toks.length == 2 : "Incorrect number of tokens in sequence name " + seqName
                    + ", expected split on ':' to two token,.";
            nameMap.put( toks[1], sequence );
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
                ( ( Reporter ) designElement ).setImmobilizedCharacteristic( nameMap.get( designElement.getName() ) );
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
     * @param Array design name.
     * @param Array design file in our 'old fashioned' format.
     * @param Affymetrix probe file
     * @return ArrayDesign with CompositeSequences, Reporters, ImmobilizedCharacteristics and BiologicalCharacteristics
     *         filled in.
     */
    public ArrayDesign processAffymetrixDesign( String arrayDesignName, String arrayDesignFile, String probeSequenceFile )
            throws IOException {
        InputStream arrayDesignFileStream = new BufferedInputStream( new FileInputStream( arrayDesignFile ) );
        InputStream probeSequenceFileStream = new BufferedInputStream( new FileInputStream( probeSequenceFile ) );
        return this.processAffymetrixDesign( arrayDesignName, arrayDesignFileStream, probeSequenceFileStream );
    }

    /**
     * @param Array design name.
     * @param Array design file in our 'old fashioned' format.
     * @param Affymetrix probe file
     * @return ArrayDesign with CompositeSequences, Reporters, ImmobilizedCharacteristics and BiologicalCharacteristics
     *         filled in.
     */
    protected ArrayDesign processAffymetrixDesign( String arrayDesignName, InputStream arrayDesignFile,
            InputStream probeSequenceFile ) throws IOException {
        ArrayDesign result = ArrayDesign.Factory.newInstance();
        result.setName( arrayDesignName );

        AffyProbeReader apr = new AffyProbeReader();
        apr.parse( probeSequenceFile );
        Collection<CompositeSequence> compositeSequencesFromProbes = apr.getResults();
        Map<String, CompositeSequence> quickFindMap = new HashMap<String, CompositeSequence>();
        for ( CompositeSequence compositeSequence : compositeSequencesFromProbes ) {

            compositeSequence.setArrayDesign( result );
            BioSequence collapsed = SequenceManipulation.collapse( compositeSequence );
            collapsed.setName( compositeSequence.getName() + "_collapsed" );
            collapsed.setType( SequenceType.AFFY_COLLAPSED );
            collapsed.setPolymerType( PolymerType.DNA );
            compositeSequence.setBiologicalCharacteristic( collapsed );

            result.getReporters().addAll( compositeSequence.getComponentReporters() );

            for ( Reporter reporter : compositeSequence.getComponentReporters() ) {
                reporter.setArrayDesign( result );
            }

            quickFindMap.put( compositeSequence.getName(), compositeSequence );
        }

        CompositeSequenceParser csp = new CompositeSequenceParser();
        csp.parse( arrayDesignFile );
        Collection<CompositeSequence> rawCompositeSequences = csp.getResults();

        for ( CompositeSequence compositeSequence : rawCompositeSequences ) {
            // go back and fill this information into the composite sequences, namely the database entry information.
            CompositeSequence keeper = quickFindMap.get( compositeSequence.getName() );
            if ( keeper == null ) {
                throw new IllegalArgumentException( "Array Design file did not contain " + compositeSequence.getName() );
            }

            keeper.getBiologicalCharacteristic().setSequenceDatabaseEntry(
                    compositeSequence.getBiologicalCharacteristic().getSequenceDatabaseEntry() );

        }

        result.setAdvertisedNumberOfDesignElements( compositeSequencesFromProbes.size() );
        result.setCompositeSequences( compositeSequencesFromProbes );
        return result;
    }
}
