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
        String clause = formAclRestrictionClause( "ee.id" );
        assertThat( clause )
                .startsWith( "," )
                .contains( "AclObjectIdentity as aoi" )
                .contains( "aoi.identifier = ee.id" )
                .contains( "join aoi.ownerSid sid" )
                .doesNotContain( "join aoi.entries ace" );
    }

    @Test
    public void testFormAclJoinClauseAsNonAdminIncludesAoiEntriesInnerJointure() {
        super.runAsAnonymous();
        String clause = formAclRestrictionClause( "ee.id" );
        assertThat( clause )
                .startsWith( "," )
                .contains( "AclObjectIdentity as aoi" )
                .contains( "aoi.identifier = ee.id" )
                .contains( "join aoi.ownerSid sid" )
                .contains( "join aoi.entries ace" );
    }

    @Test
    public void testAddAclJoinParameters() {
        Query query = mock( Query.class );
        addAclParameters( query, ExpressionExperiment.class );
        verify( query ).setParameter( "aclQueryUtils_aoiType", "ubic.gemma.model.expression.experiment.ExpressionExperiment" );
    }

    @Test
    public void testFormNativeAclJoinClause() {
        assertThat( formNativeAclJoinClause( "EE.ID" ) )
                .startsWith( " " )
                .contains( "join acl_object_identity aoi" )
                .contains( "join acl_class aoi_cls" )
                .contains( "aoi_cls.class = :" )
                .contains( "aoi.object_id_identity = EE.ID" )
                .doesNotContain( "left join acl_entry ace" );
    }

    @Test
    public void testFormNativeAclJoinClauseAsAnonymous() {
        this.runAsAnonymous();
        assertThat( formNativeAclJoinClause( "EE.ID" ) )
                .startsWith( " " )
                .contains( "join acl_object_identity aoi" )
                .contains( "join acl_class aoi_cls" )
                .contains( "aoi_cls.class = :" )
                .contains( "aoi.object_id_identity = EE.ID" )
                .contains( "left join acl_entry ace on (aoi.id = ace.acl_object_identity)" );
    }

    @Test
    public void testFormNativeRestrictionClause() {
        assertThat( formNativeAclRestrictionClause( ( SessionFactoryImplementor ) sessionFactory ) ).isEmpty();
    }

    @Test
    public void testFormNativeRestrictionClauseAsAnonymous() {
        this.runAsAnonymous();
        assertThat( formNativeAclRestrictionClause( ( SessionFactoryImplementor ) sessionFactory ) )
                .startsWith( " " )
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