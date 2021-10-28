package ubic.gemma.persistence.util;

import org.hibernate.Query;
import org.junit.Test;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ubic.gemma.persistence.util.AclQueryUtils.addAclJoinParameters;
import static ubic.gemma.persistence.util.AclQueryUtils.formAclJoinClause;

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
                .contains( "inner join aoi.entries ace" );
    }

    @Test
    public void testAddAclJoinParameters() {
        Query query = mock( Query.class );
        addAclJoinParameters( query, ExpressionExperiment.class );
        System.out.println( query );
        verify( query ).setParameter( "aoiType", "ubic.gemma.model.expression.experiment.ExpressionExperiment" );
    }

}