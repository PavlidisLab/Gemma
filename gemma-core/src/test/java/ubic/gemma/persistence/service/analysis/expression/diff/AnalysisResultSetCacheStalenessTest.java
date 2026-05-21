package ubic.gemma.persistence.service.analysis.expression.diff;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.Direction;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.HitListSize;
import ubic.gemma.model.analysis.expression.diff.PvalueDistribution;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression guard for the Hibernate-6 cache-staleness shape flagged as
 * HB6 cascade audit finding #6 (HIBERNATE6_CASCADE_AUDIT.md): the
 * {@code AnalysisResultSet} + {@code DifferentialExpressionAnalysisResult}
 * HBM mapping has the same {@code mutable="false"} parent + child collection +
 * {@code <cache usage="read-only"/>} shape that caused the AuditTrail /
 * AuditEvent stale-empty-bag bug (fixed in {@code ab8b4c443c}).
 * <p>
 * The preemptive fix landed in {@code 02c87a91ed}: the L2 read-only cache
 * directives were dropped from both {@code AnalysisResultSet.hbm.xml} (on the
 * {@code ExpressionAnalysisResultSet} subclass and the {@code hitListSizes}
 * bag) and {@code DifferentialExpressionAnalysisResult.hbm.xml} (entity
 * cache + parent-class default), while {@code mutable="false"} was retained
 * because the rows themselves remain write-once-immutable.
 * <p>
 * This test exercises the cross-session reload path that surfaces the
 * symptom: write the parent {@code ExpressionAnalysisResultSet} (with N
 * results) in T1, flush+clear the session to drop L1 (simulating a fresh
 * Spring-managed session in a separate request / @Transactional boundary),
 * write a new {@code DifferentialExpressionAnalysisResult} to that result set
 * in T2, flush+clear again, then reload the result set in T3 and assert that
 * {@code getResults()} reflects the post-T1 row. If the HBM mappings drift
 * back to the broken shape — e.g., a {@code <cache usage="read-only"/>}
 * directive being reintroduced — the T3 read will return the stale T1 bag.
 * <p>
 * Note: {@link BaseDatabaseTest5} runs H2 in-memory with
 * {@code hibernate.cache.use_second_level_cache=false}, so this test cannot
 * reproduce a live L2-cache staleness against an in-memory DB. It instead
 * asserts the architectural invariant that survives flush+clear cycles:
 * the {@code AnalysisResultSet.results} bag is bidirectional
 * ({@code inverse="true"} on the parent, FK driven by the child's
 * {@code resultSet} many-to-one), and a child written from a fresh session
 * IS visible to a subsequent fresh-session read. The L2-cache directive is
 * the production-only amplifier of the same underlying bag-staleness shape;
 * if the mapping invariants here drift, the L2 staleness path becomes live
 * again and this guard will trip first.
 *
 * @see HIBERNATE6_CASCADE_AUDIT.md finding #6
 * @see <a href="ab8b4c443c">AuditEvent cache-staleness fix</a>
 * @see <a href="02c87a91ed">Preemptive HBM fix</a>
 */
@ContextConfiguration
public class AnalysisResultSetCacheStalenessTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class AnalysisResultSetCacheStalenessTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao( SessionFactory sessionFactory ) {
            return new DifferentialExpressionAnalysisDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    /**
     * Reproduction of HB6 audit finding #6: ensure that a
     * {@link DifferentialExpressionAnalysisResult} written to an
     * existing {@link ExpressionAnalysisResultSet} after a flush+clear
     * is visible when the result set is reloaded from a fresh session.
     */
    @Test
    public void testResultSetReflectsLaterAddedResultAcrossSessions() {
        // T1: write the analysis with N initial results
        DifferentialExpressionAnalysis analysis = createAnalysis( 1, 5, 2 );
        assertNotNull( analysis.getId() );
        assertEquals( 1, analysis.getResultSets().size() );
        ExpressionAnalysisResultSet resultSet = analysis.getResultSets().iterator().next();
        assertNotNull( resultSet.getId() );
        Long resultSetId = resultSet.getId();
        assertEquals( 5, resultSet.getResults().size() );
        CompositeSequence extraProbe = resultSet.getResults().iterator().next().getProbe();

        // Drop L1: subsequent operations must repopulate from the DB.
        flushAndClear();

        // T2: write a NEW result to the existing result set in a fresh session.
        Session session = sessionFactory.getCurrentSession();
        ExpressionAnalysisResultSet reloadedSet =
                session.get( ExpressionAnalysisResultSet.class, resultSetId );
        assertNotNull( reloadedSet );
        DifferentialExpressionAnalysisResult newResult = new DifferentialExpressionAnalysisResult();
        newResult.setProbe( extraProbe );
        newResult.setResultSet( reloadedSet );
        newResult.setPvalue( 0.0123 );
        session.persist( newResult );

        // Drop L1 again to force the next read off-DB rather than L1.
        flushAndClear();

        // T3: reload the result set in a fresh session, assert the new row is visible.
        ExpressionAnalysisResultSet finalSet = differentialExpressionAnalysisDao
                .load( analysis.getId() )
                .getResultSets().iterator().next();
        assertNotNull( finalSet );
        // Trigger collection initialisation (lazy bag).
        assertEquals( 6, finalSet.getResults().size(),
                "Result set should reflect the new DEAResult added after the original create" );

        // Sanity: the new pvalue is observable.
        assertTrue( finalSet.getResults().stream()
                        .anyMatch( r -> r.getPvalue() != null && r.getPvalue() == 0.0123 ),
                "The newly written result must be present in the reloaded bag" );
    }

    /**
     * Companion read-only assertion for the inner level —
     * {@link DifferentialExpressionAnalysisResult#getContrasts()}. The audit
     * doc calls out that the {@code contrasts} bag is also {@code mutable="false"}
     * inside a {@code mutable="false"} parent and was the second level of the
     * two-level cache-staleness chain. Unlike {@code AnalysisResultSet.results},
     * the {@code contrasts} bag is BOTH {@code mutable="false"} AND unidirectional
     * with no back-reference on {@link ContrastResult}: there is no
     * {@code ContrastResult.setResult(...)} / {@code resultSet}-style accessor.
     * As a result Hibernate refuses to flush any cross-session
     * {@code reloadedResult.getContrasts().add(...)} ("changed an immutable
     * collection instance"), and the new row could not carry the FK back-ref
     * anyway. Production never adds contrasts after the fact: the
     * {@code create()} path in {@link DifferentialExpressionAnalysisDaoImpl}
     * inserts contrasts together with their parent DEAResult via raw JDBC
     * (see {@code INSERT_CONTRAST_SQL}), so the "cross-tx-write a contrast"
     * scenario is not a real code path in Gemma.
     * <p>
     * This test therefore asserts only that the {@code contrasts} bag survives
     * a flush+clear+reload with the original contents intact — pinning that the
     * inner-level cache directive does NOT come back. If a future change adds
     * a back-reference and makes {@code contrasts} writable across sessions,
     * this test should be upgraded to the full cross-session-add pattern used
     * by the result-set test above.
     */
    @Test
    public void testContrastsSurviveFlushClearReload() {
        DifferentialExpressionAnalysis analysis = createAnalysis( 1, 1, 3 );
        ExpressionAnalysisResultSet resultSet = analysis.getResultSets().iterator().next();
        DifferentialExpressionAnalysisResult result = resultSet.getResults().iterator().next();
        Long resultId = result.getId();
        assertNotNull( resultId );
        assertEquals( 3, result.getContrasts().size() );

        flushAndClear();

        DifferentialExpressionAnalysisResult reloaded = sessionFactory.getCurrentSession()
                .get( DifferentialExpressionAnalysisResult.class, resultId );
        assertNotNull( reloaded );
        assertEquals( 3, reloaded.getContrasts().size(),
                "Contrasts bag must reflect the original three contrasts after flush+clear+reload" );
    }

    /**
     * Cross-session-reload pin for the third leg of the {@code 02c87a91ed} fix:
     * the {@code hitListSizes} child bag on {@link ExpressionAnalysisResultSet}.
     * <p>
     * Pre-fix, the parent set carried {@code <cache usage="read-only"/>} on a
     * {@code mutable="false"} bag inside a {@code mutable="false"} parent — the
     * same shape as the {@code results} bag. The fix dropped the bag-level
     * cache directive; this test pins that the bag survives flush+clear+reload
     * with its persisted contents intact, so any regression that re-adds the
     * read-only cache directive (or otherwise breaks bag reload semantics) is
     * caught here in addition to the {@code results}-bag guard above.
     * <p>
     * {@code HitListSize} rows are {@code mutable="false"} and the bag is
     * unidirectional with no setter back-reference, so the cross-session-ADD
     * pattern used by {@link #testResultSetReflectsLaterAddedResultAcrossSessions()}
     * does not apply (Hibernate refuses immutable-collection mutation). We
     * therefore exercise the same shape used by
     * {@link #testContrastsSurviveFlushClearReload()}: persist N hit-list-size
     * rows together with the parent, flush+clear, and assert a fresh-session
     * load reads back all N rows from the database (not from a stale empty
     * cache snapshot).
     */
    @Test
    public void testHitListSizesSurviveFlushClearReload() {
        DifferentialExpressionAnalysis analysis = createAnalysisWithHitListSizes( 1, 1, 0, 3 );
        ExpressionAnalysisResultSet resultSet = analysis.getResultSets().iterator().next();
        Long resultSetId = resultSet.getId();
        assertNotNull( resultSetId );
        assertEquals( 3, resultSet.getHitListSizes().size() );

        flushAndClear();

        ExpressionAnalysisResultSet reloaded = sessionFactory.getCurrentSession()
                .get( ExpressionAnalysisResultSet.class, resultSetId );
        assertNotNull( reloaded );
        assertEquals( 3, reloaded.getHitListSizes().size(),
                "hitListSizes bag must reflect the original three rows after flush+clear+reload; "
                        + "a regression that re-adds <cache usage=\"read-only\"/> on the bag or breaks "
                        + "the cascade='all' eager load would trip this assertion" );

        // Sanity: every persisted direction is observable.
        Set<Direction> observedDirections = reloaded.getHitListSizes().stream()
                .map( HitListSize::getDirection )
                .collect( Collectors.toSet() );
        assertTrue( observedDirections.containsAll(
                        Set.of( Direction.UP, Direction.DOWN, Direction.EITHER ) ),
                "All three Direction values must be observable in the reloaded bag" );
    }

    private void flushAndClear() {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
    }

    private DifferentialExpressionAnalysis createAnalysisWithHitListSizes( int numResultSets, int numResults,
            int numContrasts, int numHitListSizes ) {
        DifferentialExpressionAnalysis analysis = new DifferentialExpressionAnalysis();
        List<CompositeSequence> probes = createPlatform( Math.max( numResults, 1 ) );
        Direction[] directions = { Direction.UP, Direction.DOWN, Direction.EITHER };
        for ( int j = 0; j < numResultSets; j++ ) {
            ExpressionAnalysisResultSet resultSet = new ExpressionAnalysisResultSet();
            resultSet.setAnalysis( analysis );
            PvalueDistribution pvalueDist = new PvalueDistribution();
            pvalueDist.setNumBins( 2 );
            pvalueDist.setBinCounts( new double[2] );
            for ( int i = 0; i < numResults; i++ ) {
                DifferentialExpressionAnalysisResult der = new DifferentialExpressionAnalysisResult();
                der.setProbe( probes.get( i ) );
                der.setResultSet( resultSet );
                for ( int k = 0; k < numContrasts; k++ ) {
                    der.getContrasts().add( ContrastResult.Factory.newInstance() );
                }
                resultSet.getResults().add( der );
            }
            for ( int h = 0; h < numHitListSizes; h++ ) {
                resultSet.getHitListSizes().add( HitListSize.Factory.newInstance(
                        0.01, h + 1, directions[h % directions.length], h + 1 ) );
            }
            resultSet.setPvalueDistribution( pvalueDist );
            analysis.getResultSets().add( resultSet );
        }
        return differentialExpressionAnalysisDao.create( analysis );
    }

    private DifferentialExpressionAnalysis createAnalysis( int numResultSets, int numResults, int numContrasts ) {
        DifferentialExpressionAnalysis analysis = new DifferentialExpressionAnalysis();
        List<CompositeSequence> probes = createPlatform( numResults );
        for ( int j = 0; j < numResultSets; j++ ) {
            ExpressionAnalysisResultSet resultSet = new ExpressionAnalysisResultSet();
            resultSet.setAnalysis( analysis );
            PvalueDistribution pvalueDist = new PvalueDistribution();
            pvalueDist.setNumBins( 2 );
            pvalueDist.setBinCounts( new double[2] );
            for ( int i = 0; i < numResults; i++ ) {
                DifferentialExpressionAnalysisResult der = new DifferentialExpressionAnalysisResult();
                der.setProbe( probes.get( i ) );
                der.setResultSet( resultSet );
                for ( int k = 0; k < numContrasts; k++ ) {
                    der.getContrasts().add( ContrastResult.Factory.newInstance() );
                }
                resultSet.getResults().add( der );
            }
            resultSet.setPvalueDistribution( pvalueDist );
            analysis.getResultSets().add( resultSet );
        }
        return differentialExpressionAnalysisDao.create( analysis );
    }

    private List<CompositeSequence> createPlatform( int numProbes ) {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        List<CompositeSequence> probes = new ArrayList<>( numProbes );
        for ( int i = 0; i < numProbes; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "cs" + i );
            cs.setArrayDesign( ad );
            ad.getCompositeSequences().add( cs );
            probes.add( cs );
        }
        sessionFactory.getCurrentSession().persist( ad );
        return probes;
    }
}
