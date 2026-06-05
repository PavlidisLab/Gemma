package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.cli.util.AbstractCLI;
import ubic.gemma.cli.util.CLI;
import ubic.gemma.core.search.IndexerService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class IndexGemmaCLI extends AbstractCLI {

    /**
     * A list of all searchable entities this CLI supports.
     */
    private static final IndexableEntity[] indexableEntities = {
            new IndexableEntity( "g", "genes", Gene.class ),
            new IndexableEntity( "e", "datasets", ExpressionExperiment.class ),
            new IndexableEntity( "a", "platforms", ArrayDesign.class ),
            new IndexableEntity( "b", "bibliographic references", BibliographicReference.class ),
            new IndexableEntity( "s", "probes", CompositeSequence.class ),
            new IndexableEntity( "q", "sequences", BioSequence.class ),
            new IndexableEntity( "x", "datasets groups", ExpressionExperimentSet.class ),
            new IndexableEntity( "y", "gene sets", GeneSet.class )
    };

    @lombok.Value
    private static class IndexableEntity {
        String option;
        String description;
        Class<? extends Identifiable> clazz;
    }

    @Autowired
    private IndexerService indexerService;

    @Value("${gemma.search.dir}")
    private File searchDir;

    private final Set<Class<? extends Identifiable>> classesToIndex = new HashSet<>();

    @Override
    public String getCommandName() {
        return "searchIndex";
    }

    @Override
    public String getShortDesc() {
        return "Create or update the searchable indexes for a Gemma production system";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CLI.CommandGroup.SYSTEM;
    }

    @Override
    protected void buildOptions( Options options ) {
        for ( IndexableEntity ie : indexableEntities ) {
            options.addOption( ie.option, null, false, "Index " + ie.description );
        }
        addThreadsOption( options );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        for ( IndexableEntity ie : indexableEntities ) {
            if ( commandLine.hasOption( ie.option ) ) {
                classesToIndex.add( ie.clazz );
            }
        }
        if ( classesToIndex.isEmpty() ) {
            classesToIndex.addAll( Arrays.stream( indexableEntities ).map( IndexableEntity::getClazz ).collect( Collectors.toSet() ) );
        }
    }

    @Override
    protected void doWork() throws Exception {
        if ( classesToIndex.size() < indexableEntities.length ) {
            log.info( String.format( "The following entities will be indexed under %s:\n\t%s",
                    searchDir.getAbsolutePath(),
                    classesToIndex.stream().map( Class::getName ).collect( Collectors.joining( "\n\t" ) ) ) );
        } else {
            log.info( String.format( "All entities will be indexed under %s.", searchDir.getAbsolutePath() ) );
        }
        for ( Class<? extends Identifiable> classToIndex : classesToIndex ) {
            log.info( "Indexing " + classToIndex.getName() + "..." );
            indexerService.index( classToIndex, getNumThreads() );
        }
    }
}
