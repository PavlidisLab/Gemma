package ubic.gemma.core.tasks.maintenance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.search.IndexerService;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;

import java.util.HashSet;
import java.util.Set;

@Component
@Scope("prototype")
public class IndexerTaskImpl extends AbstractTask<IndexerTaskCommand> implements IndexerTask {

    @Autowired
    private IndexerService indexerService;

    @Override
    public TaskResult call() throws Exception {
        Set<Class<? extends Identifiable>> classesToIndex = new HashSet<>();
        if ( taskCommand.isIndexGene() ) {
            classesToIndex.add( Gene.class );
        }
        if ( taskCommand.isIndexEE() ) {
            classesToIndex.add( ExpressionExperiment.class );
        }
        if ( taskCommand.isIndexAD() ) {
            classesToIndex.add( ArrayDesign.class );
        }
        if ( taskCommand.isIndexBibRef() ) {
            classesToIndex.add( BibliographicReference.class );
        }
        if ( taskCommand.isIndexProbe() ) {
            classesToIndex.add( CompositeSequence.class );
        }
        if ( taskCommand.isIndexBioSequence() ) {
            classesToIndex.add( BioSequence.class );
        }
        if ( taskCommand.isIndexExperimentSet() ) {
            classesToIndex.add( ExpressionExperimentSet.class );
        }
        if ( taskCommand.isIndexGeneSet() ) {
            classesToIndex.add( GeneSet.class );
        }
        for ( Class<? extends Identifiable> clazz : classesToIndex ) {
            indexerService.index( clazz );
        }
        return new TaskResult( taskCommand, null );
    }
}
