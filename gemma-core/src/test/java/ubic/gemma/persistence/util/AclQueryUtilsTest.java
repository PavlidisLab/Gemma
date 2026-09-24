package ubic.gemma.persistence.util;

import org.hibernate.query.ParameterMetadata;
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

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
        // addAclParameters introspects query.getParameterMetadata().getNamedParameterNames()
        // via hasNamedParameter() / setParameterIfPresent() to bind only the params actually
        // present in the HQL (post EXISTS-rewrite the HQL exposes :aclQueryUtils_aoiType, not
        // :aclQueryUtils_aoiClassId). Stub the metadata so the introspection survives the mock.
        ParameterMetadata metadata = mock( ParameterMetadata.class );
        when( metadata.getNamedParameterNames() ).thenReturn( Collections.singleton( "aclQueryUtils_aoiType" ) );
        when( query.getParameterMetadata() ).thenReturn( metadata );
        addAclParameters( query, ExpressionExperiment.class );
        verify( query ).setParameter( "aclQueryUtils_aoiType", "ubic.gemma.model.expression.experiment.ExpressionExperiment" );
    }

    @Test
    public void testFormNativeRestrictionClause() {
        // Admin bypass emits empty (no filter needed).
        assertThat( formNativeAclRestrictionClause( ( SessionFactoryImplementor ) sessionFactory, "EE.ID" ) ).isEmpty();
    }

    @Test
    public void testFormNativeRestrictionClauseAsAnonymous() {
        this.runAsAnonymous();
        assertThat( formNativeAclRestrictionClause( ( SessionFactoryImplementor ) sessionFactory, "EE.ID" ) )
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

    /**
     * The {@code isPublic} filter emits {@link AclQueryUtils#formIsPubliclyReadableExpression(String)},
     * which declares :aclQueryUtils_aoiClassId for every caller — including an administrator, whose
     * restriction clause is only the {@code (1=1)} placeholder. The binding therefore cannot sit
     * behind the admin bypass in {@link AclQueryUtils#addAclParameters(Query, Class)}.
     * <p>
     * Before the fix this threw {@code No argument for named parameter ':aclQueryUtils_aoiClassId'},
     * which is what /datasets?filter=isPublic returned to every authenticated curator.
     */
    @Test
    public void testIsPubliclyReadableExpressionIsBoundForAdmin() {
        super.runAsAdmin();
        Query q = session.createQuery(
                "select ee from ExpressionExperiment ee"
                        + formAclRestrictionClause( "ee.id" )
                        + " and " + formIsPubliclyReadableExpression( "ee.id" ) + " = false" );
        addAclParameters( q, ExpressionExperiment.class );
        q.setMaxResults( 1 );
        q.list();
    }

    @Test
    public void testIsPubliclyReadableExpressionIsBoundForAnonymous() {
        runAsAnonymous();
        Query q = session.createQuery(
                "select ee from ExpressionExperiment ee"
                        + formAclRestrictionClause( "ee.id" )
                        + " and " + formIsPubliclyReadableExpression( "ee.id" ) + " = true" );
        addAclParameters( q, ExpressionExperiment.class );
        q.setMaxResults( 1 );
        q.list();
    }

    @Test
    public void testNative() {
        Query q = session.createNativeQuery(
                        "select {I.*} from INVESTIGATION {I} "
                                + "where {I}.class = 'ExpressionExperiment'"
                                + formNativeAclRestrictionClause( ( SessionFactoryImplementor ) sessionFactory, "{I}.id" ) )
                .addEntity( "I", ExpressionExperiment.class );
        addAclParameters( q, ExpressionExperiment.class );
        q.setMaxResults( 1 );
        q.list();
    }

    @Test
    public void testNativeAsUser() {
        runAsUser( "bob" );
        Query q = session.createNativeQuery(
                        "select {I.*} from INVESTIGATION {I} "
                                + "where {I}.class = 'ExpressionExperiment'"
                                + formNativeAclRestrictionClause( ( SessionFactoryImplementor ) sessionFactory, "{I}.id" ) )
                .addEntity( "I", ExpressionExperiment.class );
        addAclParameters( q, ExpressionExperiment.class );
        q.setMaxResults( 1 );
        q.list();
    }

    @Test
    public void testNativeAsAnonymous() {
        runAsAnonymous();
        Query q = session.createNativeQuery(
                        "select {I.*} from INVESTIGATION {I} "
                                + "where {I}.class = 'ExpressionExperiment'"
                                + formNativeAclRestrictionClause( ( SessionFactoryImplementor ) sessionFactory, "{I}.id" ) )
                .addEntity( "I", ExpressionExperiment.class );
        addAclParameters( q, ExpressionExperiment.class );
        q.setMaxResults( 1 );
        q.list();
    }
}