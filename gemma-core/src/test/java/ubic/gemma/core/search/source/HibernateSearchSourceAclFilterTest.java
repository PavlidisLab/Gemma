package ubic.gemma.core.search.source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.core.security.acl.domain.AclObjectIdentity;
import ubic.gemma.core.security.acl.domain.AclService;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the ACL post-filter in {@link HibernateSearchSource} against unresolvable ACL identities.
 * <p>
 * Spring Security's {@code JdbcAclService.readAclsById} throws {@link NotFoundException} and
 * discards the whole batch when any one identity has no ACL row, which a Lucene document
 * outliving its entity reliably produces. The filter has to drop exactly those hits: 500ing the
 * request was the reported failure, and denying the whole page would be the naive over-correction.
 */
public class HibernateSearchSourceAclFilterTest {

    private static final List<Sid> SIDS = Collections.singletonList( new PrincipalSid( "curator" ) );

    private HibernateSearchSource source;
    private AclService aclService;

    @BeforeEach
    public void setUp() {
        aclService = mock( AclService.class );
        SidRetrievalStrategy sidRetrievalStrategy = mock( SidRetrievalStrategy.class );
        when( sidRetrievalStrategy.getSids( any() ) ).thenReturn( SIDS );
        source = new HibernateSearchSource();
        ReflectionTestUtils.setField( source, "aclService", aclService );
        ReflectionTestUtils.setField( source, "sidRetrievalStrategy", sidRetrievalStrategy );
    }

    @Test
    public void readableHitsSurviveAndUnreadableOnesAreDropped() {
        stubBatch( acl( true ), acl( false ), acl( true ) );

        assertThat( idsAfterFilter( 100L, 101L, 102L ) ).containsExactly( 100L, 102L );
    }

    /**
     * An ACL that exists but carries no ACE matching the caller's sids: Spring's
     * {@code DefaultPermissionGrantingStrategy} throws rather than returning false.
     */
    @Test
    public void aclWithNoMatchingAceIsTreatedAsDeny() {
        stubBatch( acl( true ), aclThatThrows(), acl( true ) );

        assertThat( idsAfterFilter( 100L, 101L, 102L ) ).containsExactly( 100L, 102L );
    }

    /**
     * The reported failure: EE 91719 was in the Lucene index but gone from the database, so its
     * ACL row was gone too and the batch read aborted the entire search with a 500.
     */
    @Test
    public void hitWithNoAclRowIsDroppedRatherThanFailingTheSearch() {
        stubBatchThrowsAndPerIdResolvesAllBut( 91719L, true );

        assertThat( idsAfterFilter( 100L, 91719L, 102L ) ).containsExactly( 100L, 102L );
    }

    /**
     * The over-correction guard: one unresolvable identity must not deny its readable neighbours,
     * which is what catching {@link NotFoundException} into an empty map would do.
     */
    @Test
    public void oneUnresolvableHitDoesNotDenyTheWholePage() {
        stubBatchThrowsAndPerIdResolvesAllBut( 91719L, true );

        assertThat( idsAfterFilter( 91719L, 100L, 101L, 102L, 103L ) )
                .containsExactly( 100L, 101L, 102L, 103L );
    }

    /**
     * Per-hit READ decisions still apply on the degraded path — it re-reads the ACLs, it doesn't
     * wave them through.
     */
    @Test
    public void degradedPathStillEnforcesRead() {
        stubBatchThrowsAndPerIdResolvesAllBut( 91719L, false );

        assertThat( idsAfterFilter( 100L, 91719L, 102L ) ).isEmpty();
    }

    @Test
    public void emptyResultsShortCircuitWithoutTouchingTheAclService() {
        assertThat( source.filterByAcls( Collections.<SearchResult<ExpressionExperiment>>emptyList(),
                ExpressionExperiment.class ) ).isEmpty();
    }

    // ---- helpers ---------------------------------------------------------

    /**
     * @return the ids of the results that survive the filter, in result order.
     */
    private List<Long> idsAfterFilter( Long... ids ) {
        List<SearchResult<ExpressionExperiment>> results = new ArrayList<>();
        for ( Long id : ids ) {
            results.add( SearchResult.from( ExpressionExperiment.class, id, 0.5, null, "hibernateSearch" ) );
        }
        Collection<SearchResult<ExpressionExperiment>> filtered =
                source.filterByAcls( results, ExpressionExperiment.class );
        List<Long> out = new ArrayList<>();
        for ( SearchResult<ExpressionExperiment> r : filtered ) {
            out.add( r.getResultId() );
        }
        return out;
    }

    /**
     * Stub a successful batch read that answers with {@code acls} positionally against whatever
     * identities the filter asks for.
     */
    private void stubBatch( Acl... acls ) {
        when( aclService.readAclsById( anyList() ) ).thenAnswer( invocation -> {
            List<ObjectIdentity> oids = invocation.getArgument( 0 );
            Map<ObjectIdentity, Acl> out = new HashMap<>();
            for ( int i = 0; i < oids.size(); i++ ) {
                out.put( oids.get( i ), acls[i] );
            }
            return out;
        } );
    }

    /**
     * Reproduce Spring's all-or-nothing batch contract: the bulk read throws because
     * {@code missingId} has no ACL row, and the per-identity retry resolves every other identity
     * to an ACL granting (or denying) READ.
     */
    private void stubBatchThrowsAndPerIdResolvesAllBut( long missingId, boolean granted ) {
        when( aclService.readAclsById( anyList() ) )
                .thenThrow( new NotFoundException( "Unable to find ACL information for object identity" ) );
        when( aclService.readAclById( any( ObjectIdentity.class ) ) ).thenAnswer( invocation -> {
            AclObjectIdentity oid = invocation.getArgument( 0 );
            if ( oid.getIdentifier() == missingId ) {
                throw new NotFoundException( "Unable to find ACL information for object identity" );
            }
            return acl( granted );
        } );
    }

    private static Acl acl( boolean granted ) {
        Acl acl = mock( Acl.class );
        when( acl.isGranted( anyList(), anyList(), org.mockito.ArgumentMatchers.anyBoolean() ) )
                .thenReturn( granted );
        return acl;
    }

    private static Acl aclThatThrows() {
        Acl acl = mock( Acl.class );
        when( acl.isGranted( anyList(), anyList(), org.mockito.ArgumentMatchers.anyBoolean() ) )
                .thenThrow( new NotFoundException( "no ACE matched" ) );
        return acl;
    }
}
