package ubic.gemma.persistence.util;

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static ubic.gemma.persistence.util.AclQueryUtils.*;

public class AclQueryUtilsTest extends BaseSpringContextTest {

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
        String clause = formAclJoinClause( "ee.id" );
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
        String clause = formAclJoinClause( "ee.id" );
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
                .contains( "join ACLOBJECTIDENTITY aoi" )
                .contains( "aoi.OBJECT_CLASS" )
                .contains( "aoi.OBJECT_ID = EE.ID" )
                .doesNotContain( "join ACLENTRY ace on (aoi.ID = ace.OBJECTIDENTITY_FK)" );
    }

    @Test
    public void testFormNativeAclJoinClauseAsAnonymous() {
        this.runAsAnonymous();
        assertThat( formNativeAclJoinClause( "EE.ID" ) )
                .startsWith( " " )
                .contains( "join ACLOBJECTIDENTITY aoi" )
                .contains( "aoi.OBJECT_CLASS" )
                .contains( "aoi.OBJECT_ID = EE.ID" )
                .contains( "join ACLENTRY ace on (aoi.ID = ace.OBJECTIDENTITY_FK)" );
    }

    @Test
    public void testFormNativeRestrictionClause() {
        assertThat( formNativeAclRestrictionClause() ).isEmpty();
    }

    @Test
    public void testFormNativeRestrictionClauseAsAnonymous() {
        this.runAsAnonymous();
        assertThat( formNativeAclRestrictionClause() )
                .startsWith( " " )
                .contains( "ace.MASK" )
                .contains( "ace.SID_FK" )
                .contains( "select sid.ID from ACLSID sid where sid.GRANTED_AUTHORITY = 'IS_AUTHENTICATED_ANONYMOUSLY'" );
    }

    @Autowired
    private SessionFactory sessionFactory;

    @Test
    public void testAsAdmin() {
        Query q = sessionFactory.openSession().createQuery(
                "select ee from ExpressionExperiment ee"
                        + formAclJoinClause( "ee.id" )
                        + formAclRestrictionClause() );
        addAclParameters( q, ExpressionExperiment.class );
        q.setMaxResults( 1 );
        q.list();
    }

    @Test
    public void testAsUser() {
        runAsUser( "bob", true );
        Query q = sessionFactory.openSession().createQuery(
                "select ee from ExpressionExperiment ee"
                        + formAclJoinClause( "ee.id" )
                        + formAclRestrictionClause() );
        addAclParameters( q, ExpressionExperiment.class );
        q.setMaxResults( 1 );
        q.list();
    }

    @Test
    public void testAsAnonymous() {
        runAsAnonymous();
        Query q = sessionFactory.openSession().createQuery(
                "select ee from ExpressionExperiment ee"
                        + formAclJoinClause( "ee.id" )
                        + formAclRestrictionClause() );
        addAclParameters( q, ExpressionExperiment.class );
        q.setMaxResults( 1 );
        q.list();
    }

    @Test
    public void testNative() {
        Query q = sessionFactory.openSession().createSQLQuery(
                        "select {I.*} from INVESTIGATION {I}"
                                + formNativeAclJoinClause( "{I}.id" ) + " "
                                + "where {I}.class = 'ExpressionExperiment'"
                                + formNativeAclRestrictionClause() )
                .addEntity( "I", ExpressionExperiment.class );
        addAclParameters( q, ExpressionExperiment.class );
        q.setMaxResults( 1 );
        q.list();
    }

    @Test
    public void testNativeAsUser() {
        runAsUser( "bob" );
        Query q = sessionFactory.openSession().createSQLQuery(
                        "select {I.*} from INVESTIGATION {I}"
                                + formNativeAclJoinClause( "{I}.id" ) + " "
                                + "where {I}.class = 'ExpressionExperiment'"
                                + formNativeAclRestrictionClause() )
                .addEntity( "I", ExpressionExperiment.class );
        addAclParameters( q, ExpressionExperiment.class );
        q.setMaxResults( 1 );
        q.list();
    }

    @Test
    public void testNativeAsAnonymous() {
        runAsAnonymous();
        Query q = sessionFactory.openSession().createSQLQuery(
                        "select {I.*} from INVESTIGATION {I}"
                                + formNativeAclJoinClause( "{I}.id" ) + " "
                                + "where {I}.class = 'ExpressionExperiment'"
                                + formNativeAclRestrictionClause() )
                .addEntity( "I", ExpressionExperiment.class );
        addAclParameters( q, ExpressionExperiment.class );
        q.setMaxResults( 1 );
        q.list();
    }
}