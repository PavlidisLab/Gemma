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

import java.io.File;
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
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.designElement.Reporter;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * Handles collapsing the sequences, loading the sequences into the database.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AffyProbeProcessor {

    private static Log log = LogFactory.getLog( AffyProbeProcessor.class.getName() );

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
     * @param sequences
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
}
