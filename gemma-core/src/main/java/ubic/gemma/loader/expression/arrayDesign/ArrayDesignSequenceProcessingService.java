/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.loader.expression.arrayDesign;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.SequenceType;

/**
 * @author paul
 * @version $Id$
 */
public interface ArrayDesignSequenceProcessingService {

    /**
     * When checking a BLAST database for sequences, we stop after checking Genbank accessions versions up to this value
     * (e.g, AA22930.1)
     */
    static final int MAX_VERSION_NUMBER = 20;

    /**
     * Associate sequences with an array design.
     * 
     * @param designElements
     * @param sequences, for Affymetrix these should be the Collapsed probe sequences.
     * @throws IOException
     */
    public abstract void assignSequencesToDesignElements( Collection<CompositeSequence> designElements,
            Collection<BioSequence> sequences );

    /**
     * Associate sequences with an array design. It is assumed that the name of the sequences can be matched to the name
     * of a design element.
     * 
     * @param designElements
     * @param fastaFile
     * @throws IOException
     */
    public abstract void assignSequencesToDesignElements( Collection<CompositeSequence> designElements, File fastaFile )
            throws IOException;

    public abstract void assignSequencesToDesignElements( Collection<CompositeSequence> designElements, InputStream fastaFile ) throws IOException;

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
    public abstract Collection<BioSequence> processAffymetrixDesign( ArrayDesign arrayDesign,
            InputStream probeSequenceFile, Taxon taxon, boolean force ) throws IOException;

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
    public abstract ArrayDesign processAffymetrixDesign( String arrayDesignName, String arrayDesignFile,
            String probeSequenceFile, Taxon taxon ) throws IOException;

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
    public abstract Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, InputStream sequenceFile,
            SequenceType sequenceType ) throws IOException;

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
     * @see ArrayDesignProbeRenamingServiceImpl.DUPLICATE_PROBE_NAME_MUNGE_SEPARATOR for the specification of how
     *      duplicate probes are munged.
     */
    public abstract Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, InputStream sequenceFile,
            SequenceType sequenceType, Taxon taxon ) throws IOException;

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
    public abstract Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign,
            InputStream sequenceIdentifierFile, String[] databaseNames, String blastDbHome, Taxon taxon, boolean force )
            throws IOException;

    public abstract Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, String[] databaseNames,
            boolean force );

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
    public abstract Collection<BioSequence> processArrayDesign( ArrayDesign arrayDesign, String[] databaseNames,
            String blastDbHome, boolean force );

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
    public abstract BioSequence processSingleAccession( String sequenceId, String[] databaseNames, String blastDbHome,
            boolean force );

    abstract Taxon validateTaxon( Taxon taxon, ArrayDesign arrayDesign ) throws IllegalArgumentException;

}