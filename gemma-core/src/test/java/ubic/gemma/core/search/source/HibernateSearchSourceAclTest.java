package ubic.gemma.core.search.source;

import org.junit.jupiter.api.Test;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.core.security.acl.domain.AclObjectIdentity;
import ubic.gemma.core.security.acl.domain.AclService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins {@link HibernateSearchSource#readAclsLeniently} against the "one missing ACL 500s the whole
 * search page" regression.
 * <p>
 * Spring's {@link org.springframework.security.acls.jdbc.JdbcAclService#readAclsById(java.util.List)}
 * is all-or-nothing: it throws {@link NotFoundException} if ANY requested identity lacks an ACL row.
 * The search ACL post-filter batches every hit id into that one call, so a single hit whose ACL is
 * absent used to abort the entire request. A missing ACL must instead mean "deny that one hit" and
 * the rest of the page must still resolve.
 * <p>
 * Observed 2026-07-24: EE 92540 ({@code GSE307917.1}) was present in the Lucene index and the legacy
 * uppercase ACL tables but absent from the Spring-standard lowercase {@code acl_object_identity} the
 * migrated {@link AclService} reads, so every {@code datasets?query=...} that surfaced it 500ed.
 */
public class HibernateSearchSourceAclTest {

    private static final ObjectIdentity PRESENT = new AclObjectIdentity( ExpressionExperiment.class, 1L );
    private static final ObjectIdentity MISSING = new AclObjectIdentity( ExpressionExperiment.class, 92540L );

    private static HibernateSearchSource sourceWith( AclService aclService ) {
        HibernateSearchSource source = new HibernateSearchSource();
        ReflectionTestUtils.setField( source, "aclService", aclService );
        return source;
    }

    @Test
    public void whenEveryIdentityHasAnAcl_thenTheFastBatchPathIsUsed() {
        AclService aclService = mock( AclService.class );
        Acl a1 = mock( Acl.class );
        Acl a2 = mock( Acl.class );
        Map<ObjectIdentity, Acl> batch = new LinkedHashMap<>();
        batch.put( PRESENT, a1 );
        batch.put( new AclObjectIdentity( ExpressionExperiment.class, 2L ), a2 );
        when( aclService.readAclsById( anyList() ) ).thenReturn( batch );

        Collection<Acl> acls = sourceWith( aclService )
                .readAclsLeniently( Arrays.asList( PRESENT, new AclObjectIdentity( ExpressionExperiment.class, 2L ) ) );

        assertThat( acls ).containsExactlyInAnyOrder( a1, a2 );
        // hot path stays a single batched query; never degrades to per-identity reads.
        verify( aclService, never() ).readAclById( org.mockito.ArgumentMatchers.any() );
    }

    @Test
    public void whenOneIdentityHasNoAcl_thenTheMissingHitIsDroppedAndTheRestSurvive() {
        AclService aclService = mock( AclService.class );
        Acl present = mock( Acl.class );
        // Batch throws because MISSING has no ACL row — the pre-fix hard-500.
        when( aclService.readAclsById( anyList() ) )
                .thenThrow( new NotFoundException( "no ACL for object identity 92540" ) );
        // Per-identity fallback: PRESENT resolves, MISSING still throws and must be swallowed.
        when( aclService.readAclById( PRESENT ) ).thenReturn( present );
        when( aclService.readAclById( MISSING ) )
                .thenThrow( new NotFoundException( "no ACL for object identity 92540" ) );

        Collection<Acl> acls = sourceWith( aclService )
                .readAclsLeniently( Arrays.asList( PRESENT, MISSING ) );

        assertThat( acls ).containsExactly( present );
    }
}
