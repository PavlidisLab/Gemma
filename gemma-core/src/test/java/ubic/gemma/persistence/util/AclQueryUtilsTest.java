package ubic.gemma.persistence.util;

import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ubic.gemma.persistence.util.AclQueryUtils.*;

public class AclQueryUtilsTest extends BaseSpringContextTest5 {

    @Autowired
    private SessionFactory sessionFactory;

    private Session session;

    @BeforeEach
    public void setUp() {
        this.session = sessionFactory.openSession();
    }

    @AfterEach
    public void tearDown() {
        this.session.close();
    }

    /**
     * Since ACL relies on the class name, and these class names are stored in the database, we must ensure that they
     * are never changed.
     */
    @Test
    public void testSecurableEntitiesNamesAreUnchanged() {
        assertThat( ArrayDesign.class.getCanonicalName() ).isEqualTo( "ubic.gemma.model.expression.arrayDesign.ArrayDesign" );
        assertThat( ExpressionExperiment.class.getCanonicalName() ).isEqualTo( "ubic.gemma.model.expression.experiment.ExpressionExperiment" );
    }

    @Test
    public void testFormAclJoinClauseAsAdmin() {
        super.runAsAdmin();
        // EXISTS rewrite: admin bypass emits " where (1=1)" as a placeholder so the caller's
        // " and X" concatenation idiom stays well-formed.
        String clause = formAclRestrictionClause( "ee.id" );
        assertThat( clause )
                .isEqualTo( " where (1=1)" );
    }

    @Test
    public void testFormAclJoinClauseAsNonAdminIncludesAoiEntriesInnerJointure() {
        super.runAsAnonymous();
        // EXISTS rewrite: emission is " where (exists (select 1 from AclObjectIdentity aoi ...))"
        // instead of the old Cartesian ", AclObjectIdentity aoi ... where ..." join.
        String clause = formAclRestrictionClause( "ee.id" );
        assertThat( clause )
                .startsWith( " where (exists (" )
                .contains( "from AclObjectIdentity aoi" )
                .contains( "aoi.identifier = ee.id" )
                .contains( "join aoi.ownerSid sid" )
                .contains( "join aoi.entries ace" )
                .endsWith( "))" );
    }

    @Test
    public void testAddAclJoinParameters() {
        // EXISTS rewrite: admin bypass binds no ACL parameters at all (no filter is applied).
        // Switch to anonymous so the aoiType parameter is bound.
        runAsAnonymous();
        Query query = mock( Query.class );
        addAclParameters( query, ExpressionExperiment.class );
        verify( query ).setParameter( "aclQueryUtils_aoiType", "ubic.gemma.model.expression.experiment.ExpressionExperiment" );
    }

    @Test
    public void testFormNativeAclJoinClause() {
        // EXISTS rewrite: the native join clause now stashes the id-column on a thread-local
        // and emits the empty string. The actual EXISTS sub-query is produced by
        // formNativeAclRestrictionClause. Drain the thread-local so we don't leak into the
        // next test (it's cleared on next read by formNativeAclRestrictionClause anyway, but
        // for an isolated test we don't want the side-effect to persist).
        assertThat( formNativeAclJoinClause( "EE.ID" ) ).isEmpty();
        // drain thread-local
        formNativeAclRestrictionClause( ( SessionFactoryImplementor ) sessionFactory );
    }

    @Test
    public void testFormNativeAclJoinClauseAsAnonymous() {
        this.runAsAnonymous();
        // Same as above: native join clause is now an empty string post-EXISTS rewrite.
        assertThat( formNativeAclJoinClause( "EE.ID" ) ).isEmpty();
        // drain thread-local
        formNativeAclRestrictionClause( ( SessionFactoryImplementor ) sessionFactory );
    }

    @Test
    public void testFormNativeRestrictionClause() {
        // Admin bypass emits empty (no filter needed). Must still call the join clause first
        // to satisfy the contract that the id-column is stashed.
        formNativeAclJoinClause( "EE.ID" );
        assertThat( formNativeAclRestrictionClause( ( SessionFactoryImplementor ) sessionFactory ) ).isEmpty();
    }

    @Test
    public void testFormNativeRestrictionClauseAsAnonymous() {
        this.runAsAnonymous();
        // Must call the join clause first so the EXISTS body knows which outer column to
        // correlate against.
        formNativeAclJoinClause( "EE.ID" );
        assertThat( formNativeAclRestrictionClause( ( SessionFactoryImplementor ) sessionFactory ) )
                .startsWith( " and exists (" )
                .contains( "from acl_object_identity aoi" )
                .contains( "aoi.object_id_identity = EE.ID" )
                .contains( "(ace.mask & 1) <> 0" )
                .contains( "ace.sid in" )
                .contains( "select sid.id from acl_sid sid where sid.principal = 0 and sid.sid = 'IS_AUTHENTICATED_ANONYMOUSLY'" );
    }

    @Test
    public void testAsAdmin() {
        Query q = session.createQuery(
                "select ee from ExpressionExperiment ee"
                        + formAclRestrictionClause( "ee.id" ) );
        addAclParameters( q, ExpressionExperiment.class );
        q.setMaxResults( 1 );
        q.list();
    }

    @Test
    public void testAsUser() {
        runAsUser( "bob", true );
        Query q = session.createQuery(
                "select ee from ExpressionExperiment ee"
                        + formAclRestrictionClause( "ee.id" ) );
        addAclParameters( q, ExpressionExperiment.class );
        q.setMaxResults( 1 );
        q.list();
    }

    @Test
    public void testAsAnonymous() {
        runAsAnonymous();
        Query q = session.createQuery(
                "select ee from ExpressionExperiment ee"
                        + formAclRestrictionClause( "ee.id" ) );
        addAclParameters( q, ExpressionExperiment.class );
        q.setMaxResults( 1 );
        q.list();
    }

    @Test
    public void testNative() {
        Query q = session.createNativeQuery(
                        "select {I.*} from INVESTIGATION {I}"
                                + formNativeAclJoinClause( "{I}.id" ) + " "
                                + "where {I}.class = 'ExpressionExperiment'"
                                + formNativeAclRestrictionClause( ( SessionFactoryImplementor ) sessionFactory ) )
                .addEntity( "I", ExpressionExperiment.class );
        addAclParameters( q, ExpressionExperiment.class );
        q.setMaxResults( 1 );
        q.list();
    }

    @Test
    public void testNativeAsUser() {
        runAsUser( "bob" );
        Query q = session.createNativeQuery(
                        "select {I.*} from INVESTIGATION {I}"
                                + formNativeAclJoinClause( "{I}.id" ) + " "
                                + "where {I}.class = 'ExpressionExperiment'"
                                + formNativeAclRestrictionClause( ( SessionFactoryImplementor ) sessionFactory ) )
                .addEntity( "I", ExpressionExperiment.class );
        addAclParameters( q, ExpressionExperiment.class );
        q.setMaxResults( 1 );
        q.list();
    }

    @Test
    public void testNativeAsAnonymous() {
        runAsAnonymous();
        Query q = session.createNativeQuery(
                        "select {I.*} from INVESTIGATION {I}"
                                + formNativeAclJoinClause( "{I}.id" ) + " "
                                + "where {I}.class = 'ExpressionExperiment'"
                                + formNativeAclRestrictionClause( ( SessionFactoryImplementor ) sessionFactory ) )
                .addEntity( "I", ExpressionExperiment.class );
        addAclParameters( q, ExpressionExperiment.class );
        q.setMaxResults( 1 );
        q.list();
    }
}