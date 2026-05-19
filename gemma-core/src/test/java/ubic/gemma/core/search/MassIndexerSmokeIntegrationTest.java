/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
package ubic.gemma.core.search;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.search.indexer.IndexerService;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end smoke test for the Hibernate Search 7 mass-indexer + search service round-trip
 * (SEARCH_RECCE.md Step 6).
 *
 * <p>What this test does, in order:
 * <ol>
 *   <li>Boots the full Gemma Spring context (DB = {@code gemdtest}, full HS 7 wiring).</li>
 *   <li>Persists two fixtures with known, unique tokens — one {@link ExpressionExperiment}
 *       and one {@link Gene}. The tokens are random alphanumerics seeded per-test so the
 *       assertions can be precise even when the test DB carries leftover state from prior
 *       runs.</li>
 *   <li>Triggers the {@link IndexerService} mass-indexer for each fixture's entity root.
 *       {@code purgeAllOnStart(true)} wipes the on-disk Lucene index before rebuilding —
 *       which is destructive against the shared {@code gemma.search.dir}. See the
 *       gemdtest-serialization caveat in this class's Javadoc.</li>
 *   <li>Issues a {@link SearchService#search} with a {@link SearchSettings} carrying the
 *       known token, asserts non-empty results, and asserts the fixture is in the hit list.</li>
 *   <li>Cleans up the persisted fixtures so subsequent test runs start clean.</li>
 * </ol>
 *
 * <p><b>Why {@link Ignore} by default.</b> This IT is destructive against the shared
 * Lucene index directory ({@code gemma.search.dir}) and the gemdtest database (writes +
 * deletes EE/Gene rows). Parallel Gemma sub-agents serialize against gemdtest per the
 * project's parallel-agent gotchas, and the mass-indexer holds the index directory for
 * the duration of the rebuild. Running this opportunistically in CI alongside other
 * integration tests would either corrupt search results for those tests or fight them
 * for the index. Operators should invoke this IT manually as part of the Step-6 cutover
 * validation:
 *
 * <pre>{@code
 * mvn -pl gemma-core failsafe:integration-test \
 *     -Dit.test=MassIndexerSmokeIntegrationTest \
 *     -DfailIfNoTests=false
 * }</pre>
 *
 * <p>The {@code @Tag("integration")} inherited from {@link BaseSpringContextTest} →
 * {@link ubic.gemma.core.util.test.BaseIntegrationTest} ensures this class is routed to
 * Failsafe (i.e. {@code mvn verify}), not Surefire ({@code mvn test}), independent of
 * the {@code @Ignore}.
 *
 * <p>See {@code SEARCH_INDEX_OPERATIONS.md} for the production reindex procedure that
 * this IT smoke-tests.
 *
 * @author Phase 3 search restoration, Step 6
 */
@Ignore("Step 6: destructive against gemma.search.dir + gemdtest; invoke manually for cutover validation. "
        + "See class Javadoc and SEARCH_INDEX_OPERATIONS.md.")
public class MassIndexerSmokeIntegrationTest extends BaseSpringContextTest {

    /**
     * Random alphanumeric token embedded in fixture names. Unique per test run so the
     * assertions don't false-positive against pre-existing rows in {@code gemdtest}.
     */
    private static final String UNIQUE_TOKEN = "hs7smoke" + RandomStringUtils.insecure().nextAlphanumeric( 12 );

    private static final String EE_SHORT_NAME = "GSE_SMOKE_" + UNIQUE_TOKEN;
    private static final String EE_NAME = "Hibernate Search 7 mass-indexer smoke fixture " + UNIQUE_TOKEN;
    private static final String GENE_SYMBOL = "SMOKE" + UNIQUE_TOKEN;
    private static final String GENE_OFFICIAL_NAME = "smoke-test gene " + UNIQUE_TOKEN;

    @Autowired
    private SearchService searchService;

    @Autowired
    private IndexerService indexerService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private Persister persisterHelper;

    private ExpressionExperiment persistedEe;
    private Gene persistedGene;

    @After
    public void tearDown() {
        // Best-effort cleanup so re-runs don't accumulate fixtures. Failures here must not
        // mask the actual test result.
        if ( persistedEe != null ) {
            try {
                expressionExperimentService.remove( persistedEe );
            } catch ( Exception e ) {
                log.warn( "Failed to remove EE fixture " + persistedEe.getShortName() + ": " + e.getMessage() );
            }
        }
        if ( persistedGene != null ) {
            try {
                geneService.remove( persistedGene );
            } catch ( Exception e ) {
                log.warn( "Failed to remove Gene fixture " + persistedGene.getOfficialSymbol() + ": " + e.getMessage() );
            }
        }
    }

    @Test
    public void massIndexerRebuildMakesFixturesSearchable() throws Exception {
        // ---- 1. Persist fixtures with known tokens.
        Taxon human = taxonService.findByCommonName( "human" );
        assertThat( human )
                .as( "gemdtest must have the 'human' Taxon seeded" )
                .isNotNull();

        Gene gene = Gene.Factory.newInstance();
        gene.setName( GENE_OFFICIAL_NAME );
        gene.setOfficialName( GENE_OFFICIAL_NAME );
        gene.setOfficialSymbol( GENE_SYMBOL );
        gene.setNcbiGeneId( Integer.parseInt( RandomStringUtils.insecure().nextNumeric( 6 ) ) );
        gene.setTaxon( human );
        // Physical location is required by the gene persister path; build a throwaway one.
        Chromosome chromosome = Chromosome.Factory.newInstance(
                "X", null, this.getTestPersistentBioSequence(), human );
        chromosome = persisterHelper.persist( chromosome );
        PhysicalLocation pl = PhysicalLocation.Factory.newInstance();
        pl.setChromosome( chromosome );
        pl.setNucleotide( 100_000L );
        pl.setNucleotideLength( 1000 );
        pl.setStrand( "+" );
        gene.setPhysicalLocation( pl );
        persistedGene = geneService.create( gene );

        ExpressionExperiment ee = this.getTestPersistentBasicExpressionExperiment();
        ee.setShortName( EE_SHORT_NAME );
        ee.setName( EE_NAME );
        expressionExperimentService.update( ee );
        persistedEe = ee;

        // ---- 2. Trigger the mass-indexer for each root we care about.
        // This calls searchSession.massIndexer(EntityClass.class).startAndWait() under the
        // hood, with purgeAllOnStart(true). After it returns, the on-disk Lucene index
        // contains exactly the current DB state for that entity (modulo any rows committed
        // mid-rebuild, which is fine for a smoke test).
        indexerService.index( ExpressionExperiment.class );
        indexerService.index( Gene.class );

        // ---- 3. Round-trip search through SearchServiceImpl + the SearchSource chain.
        // We use the unique token in the fixture's short name / official symbol so the
        // hit set is precisely identifiable even when gemdtest carries unrelated rows.
        SearchService.SearchResultMap eeResults = searchService.search(
                SearchSettings.expressionExperimentSearch( EE_SHORT_NAME ),
                new SearchContext( null, null ) );

        assertThat( eeResults.toList() )
                .as( "mass-indexer + searchService.search should surface the EE fixture by its unique short name" )
                .isNotEmpty();
        assertThat( eeResults.getByResultType( ExpressionExperiment.class ) )
                .extracting( r -> r.getResultId() )
                .contains( persistedEe.getId() );

        SearchService.SearchResultMap geneResults = searchService.search(
                SearchSettings.geneSearch( GENE_SYMBOL, human ),
                new SearchContext( null, null ) );

        assertThat( geneResults.toList() )
                .as( "mass-indexer + searchService.search should surface the Gene fixture by its unique official symbol" )
                .isNotEmpty();
        assertThat( geneResults.getByResultType( Gene.class ) )
                .extracting( r -> r.getResultId() )
                .contains( persistedGene.getId() );
    }
}
