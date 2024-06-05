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
package ubic.gemma.core.loader.expression.arrayDesign;

import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.sequence.ProbeMapperConfig;
import ubic.gemma.core.goldenpath.GoldenPathSequenceAnalysis;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author Paul
 */
public interface ArrayDesignProbeMapperService {

    /**
     * Print results to STDOUT
     *
     * @param compositeSequence composite sequence
     * @param col               blat associations
     */
    void printResult( CompositeSequence compositeSequence, Collection<BlatAssociation> col );

    /**
     * Do probe mapping, writing the results to the database and using default settings.
     *
     * @param arrayDesign AD
     */
    void processArrayDesign( ArrayDesign arrayDesign );

    /**
     * @param arrayDesign AD
     * @param config      config
     * @param useDB       if false, the results will not be written to the database, but printed to stdout instead.
     */
    void processArrayDesign( ArrayDesign arrayDesign, ProbeMapperConfig config, boolean useDB );

    /**
     * Annotate an array design using a direct source file. This should only be used if we can't run sequence analysis
     * ourselves.
     * The expected file format is tab-delimited with the following columns:
     * <ul>
     * <li>Probe name which must match the probe names Gemma uses for the array design.
     * <li>Sequence name. If blank, it will be ignored but the probe will still be mapped if possible. The probe will be
     * skipped if it isn't already associated with a sequence. If not blank, it will be checked against the sequence for
     * the probe. If the probe has no sequence, it will be used to create one. If it does, it will be checked for a name
     * match.
     * <li>Gene symbol. More than one gene can be specified, delimited by '|'. Genes will only be found if Gemma has a
     * unambiguous match to the name. The gene must already exist in the system.
     * </ul>
     * Comment lines begin with '#';
     * Note that <em>all</em> the RNA gene products of the gene will be associated with the sequence. This is necessary
     * because 1) Gemma associates sequences with transcripts, not genes and 2) if all we get is a gene, we have to
     * assume all gene products are relevant.
     *
     * @param arrayDesign AD
     * @param taxon       We require this to ensure correct association of the sequences with the genes.
     * @param source      source
     * @param sourceDB    describes where the annotations came from. Can be null if you really don't know.
     * @param ncbiIds     true if the values provided are ncbi ids, not gene symbols (ncbi ids are more reliable)
     * @throws IllegalStateException if the input file doesn't match the array design.
     * @throws IOException           when IO problems occur.
     */
    void processArrayDesign( ArrayDesign arrayDesign, Taxon taxon, File source, ExternalDatabase sourceDB,
            boolean ncbiIds ) throws IOException;

    @Transactional
    Map<String, Collection<BlatAssociation>> processCompositeSequence( ProbeMapperConfig config, Taxon taxon,
            GoldenPathSequenceAnalysis goldenPathDb, CompositeSequence compositeSequence );

    /**
     * Delete outdated annotation and associated experiment files.
     *
     * @param arrayDesign platform
     */
    void deleteOldFiles( ArrayDesign arrayDesign );

}