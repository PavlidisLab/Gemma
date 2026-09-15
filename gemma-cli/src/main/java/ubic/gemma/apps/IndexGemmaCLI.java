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
package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.cli.util.AbstractCLI;
import ubic.gemma.cli.util.CLI;
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

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Build or rebuild the Hibernate Search 7 mass index for one or more entity classes.
 *
 * <p>Each {@code -<opt>} flag toggles indexing for the corresponding entity root. With
 * no flags, all indexable roots are rebuilt.
 *
 * <p><b>Destructive.</b> Every selected entity's on-disk Lucene index is purged before
 * the rebuild ({@link IndexerService#index(Class)} sets {@code purgeAllOnStart(true)}).
 * The CLI is intended for full reindex runs (initial bring-up or after a schema change);
 * incremental write-through indexing is a separate (deferred) Phase-3 concern.
 *
 * <p>The {@code gemma.search.dir} system property must point to a writable directory
 * before this CLI runs — the HS 7 Lucene backend writes one sub-directory per indexed
 * entity under that root. The CLI validates the property at startup and aborts if it is
 * missing.
 *
 * <p>HS 5 → HS 7 port: the {@code IndexerService} call shape is unchanged; the
 * underlying Lucene segment format is incompatible, so any pre-existing search-dir
 * content from the HS-5 era must be discarded before the first HS-7 run.
 */
public class IndexGemmaCLI extends AbstractCLI {

    /**
     * The set of indexable entity roots this CLI knows about. The {@code option}
     * letters are kept stable with the pre-strip HS-5 CLI so existing operator habits
     * still work.
     */
    private static final IndexableEntity[] INDEXABLE_ENTITIES = {
            new IndexableEntity( "g", "genes", Gene.class ),
            new IndexableEntity( "e", "datasets", ExpressionExperiment.class ),
            new IndexableEntity( "a", "platforms", ArrayDesign.class ),
            new IndexableEntity( "b", "bibliographic references", BibliographicReference.class ),
            new IndexableEntity( "s", "probes", CompositeSequence.class ),
            new IndexableEntity( "q", "sequences", BioSequence.class ),
            new IndexableEntity( "x", "dataset groups", ExpressionExperimentSet.class ),
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

    private final Set<Class<? extends Identifiable>> classesToIndex = new LinkedHashSet<>();

    @Override
    public String getCommandName() {
        return "searchIndex";
    }

    @Override
    public String getShortDesc() {
        return "Create or update the Hibernate-Search Lucene indexes (DESTRUCTIVE: purges existing index per entity).";
    }

    @Override
    public CLI.CommandGroup getCommandGroup() {
        return CLI.CommandGroup.SYSTEM;
    }

    @Override
    protected void buildOptions( Options options ) {
        for ( IndexableEntity ie : INDEXABLE_ENTITIES ) {
            options.addOption( ie.option, null, false, "Index " + ie.description );
        }
        addThreadsOption( options );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        for ( IndexableEntity ie : INDEXABLE_ENTITIES ) {
            if ( commandLine.hasOption( ie.option ) ) {
                classesToIndex.add( ie.clazz );
            }
        }
        if ( classesToIndex.isEmpty() ) {
            // No flags == index everything (preserves pre-strip default).
            classesToIndex.addAll( Arrays.stream( INDEXABLE_ENTITIES )
                    .map( IndexableEntity::getClazz )
                    .collect( Collectors.toCollection( LinkedHashSet::new ) ) );
        }
    }

    @Override
    protected void doWork() {
        if ( searchDir == null ) {
            throw new IllegalStateException(
                    "gemma.search.dir is not configured. Set it in Settings.properties (or override via -D) "
                            + "before running the indexer; HS 7 writes one Lucene sub-directory per entity under that root." );
        }
        if ( classesToIndex.size() < INDEXABLE_ENTITIES.length ) {
            log.info( String.format( "The following entities will be indexed under %s:%n\t%s",
                    searchDir.getAbsolutePath(),
                    classesToIndex.stream().map( Class::getName ).collect( Collectors.joining( "\n\t" ) ) ) );
        } else {
            log.info( String.format( "All indexable entities will be reindexed under %s.", searchDir.getAbsolutePath() ) );
        }
        for ( Class<? extends Identifiable> classToIndex : classesToIndex ) {
            log.info( "Indexing " + classToIndex.getName() + "..." );
            indexerService.index( classToIndex, getNumThreads() );
        }
    }
}
