package ubic.gemma.core.search.indexer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.lang.Nullable;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the two staleness signals in {@link ScheduledSearchReindexer}.
 * <p>
 * The audit-event signal cannot see deletions: {@code getUpdatedSinceDate} selects live rows, and
 * a deleted entity has neither a row nor an audit trail. Before the index-drift signal existed, a
 * night whose only change was a deletion left the index serving documents for entities that no
 * longer existed — which is how ExpressionExperiment 91719 outlived its row for ten weeks.
 * <p>
 * {@code describeIndexDrift} is overridden here rather than mocked: it needs a live SessionFactory
 * and a real Lucene directory, neither of which belongs in a unit test. The count comparison
 * itself is exercised by {@code MassIndexerSmokeIntegrationTest}'s environment; what matters here
 * is the decision logic built on top of it.
 */
public class ScheduledSearchReindexerTest {

    @TempDir
    Path appdataHome;

    private AuditEventService auditEventService;
    private RecordingReindexer reindexer;

    /**
     * Test double that records which classes were reindexed and serves canned drift descriptions.
     */
    private static class RecordingReindexer extends ScheduledSearchReindexer {

        final List<Class<?>> reindexed = new ArrayList<>();
        final List<Class<?>> driftChecked = new ArrayList<>();
        @Nullable
        String drift = null;
        /**
         * Drift description returned once the class has been reindexed — {@code null} models a
         * rebuild that reconciled the counts.
         */
        @Nullable
        String driftAfterRebuild = null;

        @Nullable
        @Override
        String describeIndexDrift( Class<? extends Auditable> clazz ) {
            driftChecked.add( clazz );
            return reindexed.contains( clazz ) ? driftAfterRebuild : drift;
        }
    }

    @BeforeEach
    public void setUp() {
        auditEventService = mock( AuditEventService.class );
        IndexerService indexerService = mock( IndexerService.class );
        reindexer = new RecordingReindexer();
        // Record reindex calls through the mock so the drift double can tell "before" from "after".
        org.mockito.Mockito.doAnswer( invocation -> {
            reindexer.reindexed.add( invocation.getArgument( 0 ) );
            return null;
        } ).when( indexerService ).index( any() );
        ReflectionTestUtils.setField( reindexer, "auditEventService", auditEventService );
        ReflectionTestUtils.setField( reindexer, "indexerService", indexerService );
        ReflectionTestUtils.setField( reindexer, "appdataHome", appdataHome.toString() );
    }

    @Test
    public void quietNightWithMatchingCountsDoesNotReindex() {
        noAuditedUpdates();
        reindexer.drift = null;

        reindexer.reindexStale();

        assertThat( reindexer.reindexed ).isEmpty();
        assertThat( reindexer.driftChecked ).containsExactly( ExpressionExperiment.class, ArrayDesign.class );
    }

    /**
     * The gap this signal exists to close: nothing was updated, but the index holds more documents
     * than the database holds rows, so entities were deleted since the last rebuild.
     */
    @Test
    public void surplusDocumentsTriggerAReindexWithNoAuditedUpdates() {
        noAuditedUpdates();
        reindexer.drift = "25695 Lucene documents against 25694 database rows (+1)";

        reindexer.reindexStale();

        assertThat( reindexer.reindexed ).contains( ExpressionExperiment.class );
    }

    @Test
    public void auditedUpdatesStillTriggerAReindexWhenCountsMatch() {
        when( auditEventService.getUpdatedSinceDate( eq( ExpressionExperiment.class ), any( Date.class ) ) )
                .thenReturn( Collections.singletonList( new ExpressionExperiment() ) );
        when( auditEventService.getUpdatedSinceDate( eq( ArrayDesign.class ), any( Date.class ) ) )
                .thenReturn( Collections.emptyList() );
        reindexer.drift = null;

        reindexer.reindexStale();

        assertThat( reindexer.reindexed ).containsExactly( ExpressionExperiment.class );
    }

    /**
     * An unreadable index reports drift, which must reach the rebuild rather than being swallowed.
     */
    @Test
    public void uncountableIndexTriggersAReindex() {
        noAuditedUpdates();
        reindexer.drift = "an uncountable Lucene index (likely missing or corrupt: boom)";

        reindexer.reindexStale();

        assertThat( reindexer.reindexed ).contains( ExpressionExperiment.class );
    }

    /**
     * A drift-triggered rebuild re-checks afterwards. When the rebuild reconciled the counts the
     * class is left alone; the residual-drift warning is the interesting case and is covered below.
     */
    @Test
    public void resolvedDriftIsRecheckedOnceAfterTheRebuild() {
        noAuditedUpdates();
        reindexer.drift = "25695 Lucene documents against 25694 database rows (+1)";
        reindexer.driftAfterRebuild = null;

        reindexer.reindexStale();

        // once before the rebuild, once after, per class that drifted
        assertThat( reindexer.driftChecked ).containsExactly(
                ExpressionExperiment.class, ExpressionExperiment.class,
                ArrayDesign.class, ArrayDesign.class );
    }

    @Test
    public void residualDriftAfterARebuildStillAdvancesTheMarker() {
        noAuditedUpdates();
        reindexer.drift = "25000 Lucene documents against 25694 database rows (-694)";
        reindexer.driftAfterRebuild = "25000 Lucene documents against 25694 database rows (-694)";

        reindexer.reindexStale();

        // The rebuild ran and succeeded; a marker left unadvanced would re-run the audit leg from
        // epoch zero forever. The residual drift is reported by a warning, not by a stuck marker.
        assertThat( reindexer.reindexed ).contains( ExpressionExperiment.class );
        assertThat( markerFor( ExpressionExperiment.class ) ).exists();
    }

    /**
     * Per-class isolation: the marker is what carries the audit cutoff forward, and a class that
     * was never rebuilt must not get one.
     */
    @Test
    public void skippedClassGetsNoMarker() {
        noAuditedUpdates();
        reindexer.drift = null;

        reindexer.reindexStale();

        assertThat( markerFor( ExpressionExperiment.class ) ).doesNotExist();
    }

    /**
     * The audit cutoff comes from the marker, so a rebuild has to leave a parseable timestamp
     * behind — an unparseable one silently degrades every later run to a full-history scan.
     */
    @Test
    public void markerHoldsAParseableInstant() throws Exception {
        noAuditedUpdates();
        reindexer.drift = "25695 Lucene documents against 25694 database rows (+1)";

        reindexer.reindexStale();

        Path marker = markerFor( ExpressionExperiment.class );
        assertThat( marker ).exists();
        Instant written = Instant.parse( new String( Files.readAllBytes( marker ) ).trim() );
        assertThat( written ).isBeforeOrEqualTo( Instant.now() );
    }

    // ---- helpers ---------------------------------------------------------

    private void noAuditedUpdates() {
        when( auditEventService.getUpdatedSinceDate( any(), any( Date.class ) ) )
                .thenReturn( Collections.emptyList() );
    }

    private Path markerFor( Class<?> clazz ) {
        return appdataHome.resolve( "search" ).resolve( "last_reindex." + clazz.getSimpleName() + ".timestamp" );
    }
}
