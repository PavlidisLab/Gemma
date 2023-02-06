package ubic.gemma.persistence.util;

import org.hibernate.Query;
import org.junit.Test;
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
        String clause = formAclJoinClause( "ee" );
        assertThat( clause )
                .contains( "ee.id" )
                .doesNotContain( "inner join aoi.entries ace" );
    }

    @Test
    public void testFormAclJoinClauseAsNonAdminIncludesAoiEntriesInnerJointure() {
        super.runAsAnonymous();
        String clause = formAclJoinClause( "ee" );
        assertThat( clause )
                .contains( "ee.id" )
                .contains( "join aoi.entries ace" );
    }

    @Test
    public void testAddAclJoinParameters() {
        Query query = mock( Query.class );
        when( query.getNamedParameters() ).thenReturn( new String[0] );
        addAclParameters( query, ExpressionExperiment.class );
        verify( query ).setParameter( "aclQueryUtils_aoiType", "ubic.gemma.model.expression.experiment.ExpressionExperiment" );
    }

    @Test
    public void testFormNativeAclJoinClause() {
        assertThat( formNativeAclJoinClause( "EE.ID" ) )
                .isEqualTo( " join ACLOBJECTIDENTITY aoi on (aoi.OBJECT_CLASS = :aclQueryUtils_aoiType and aoi.OBJECT_ID = EE.ID)" );
    }

    @Test
    public void testFormNativeAclJoinClauseAsAnonymous() {
        this.runAsAnonymous();
        assertThat( formNativeAclJoinClause( "EE.ID" ) )
                .isEqualTo( " join ACLOBJECTIDENTITY aoi on (aoi.OBJECT_CLASS = :aclQueryUtils_aoiType and aoi.OBJECT_ID = EE.ID) join ACLENTRY ace on (aoi.ID = ace.OBJECTIDENTITY_FK)" );
    }

    @Test
    public void testFormNativeRestrictionClause() {
        assertThat( formNativeAclRestrictionClause() ).isEmpty();
    }

    @Test
    public void testFormNativeRestrictionClauseAsAnonymous() {
        this.runAsAnonymous();
        assertThat( formNativeAclRestrictionClause() ).isEqualTo( " and (ace.MASK = :aclQueryUtils_readMask and ace.SID_FK = :aclQueryUtils_nonymousAuthSid)" );
    }
}