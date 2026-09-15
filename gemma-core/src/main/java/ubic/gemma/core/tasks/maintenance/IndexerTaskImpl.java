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
package ubic.gemma.core.tasks.maintenance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.search.indexer.IndexerService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Asynchronous mass-indexer task. Dispatches each requested entity class to
 * {@link IndexerService#index(Class)} in turn.
 *
 * <p>Mirrors the pre-strip {@code IndexerTaskImpl}. The HS 5 → HS 7 port lives in
 * {@link IndexerService}; this task is a thin scheduler.
 */
@Component
@Scope("prototype")
public class IndexerTaskImpl extends AbstractTask<IndexerTaskCommand> implements IndexerTask {

    @Autowired
    private IndexerService indexerService;

    @Override
    public TaskResult call() {
        IndexerTaskCommand cmd = getTaskCommand();
        // LinkedHashSet preserves the declared order, so logs stay predictable.
        Set<Class<? extends Identifiable>> classesToIndex = new LinkedHashSet<>();
        if ( cmd.isIndexGenes() ) {
            classesToIndex.add( Gene.class );
        }
        if ( cmd.isIndexDatasets() ) {
            classesToIndex.add( ExpressionExperiment.class );
        }
        if ( cmd.isIndexPlatforms() ) {
            classesToIndex.add( ArrayDesign.class );
        }
        if ( cmd.isIndexPublications() ) {
            classesToIndex.add( BibliographicReference.class );
        }
        if ( cmd.isIndexDesignElements() ) {
            classesToIndex.add( CompositeSequence.class );
        }
        if ( cmd.isIndexBioSequences() ) {
            classesToIndex.add( BioSequence.class );
        }
        if ( cmd.isIndexDatasetGroups() ) {
            classesToIndex.add( ExpressionExperimentSet.class );
        }
        if ( cmd.isIndexGeneGroups() ) {
            classesToIndex.add( GeneSet.class );
        }
        for ( Class<? extends Identifiable> clazz : classesToIndex ) {
            indexerService.index( clazz );
        }
        return newTaskResult( null );
    }
}
