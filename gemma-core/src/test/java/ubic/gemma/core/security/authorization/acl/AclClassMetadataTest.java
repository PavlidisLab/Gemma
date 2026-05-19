package ubic.gemma.core.security.authorization.acl;

import org.hibernate.SessionFactory;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest5;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

@ContextConfiguration
public class AclClassMetadataTest extends BaseTest5 {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public AclClassMetadata aclClassMetadata() {
            // Phase 2: the constructor eagerly walks sessionFactory.getMetamodel().getEntities() to
            // validate that every @Entity implementing SecuredChild is registered. RETURNS_DEEP_STUBS
            // makes the chain return mock-but-non-null intermediates, so the validation loop iterates
            // an empty mock Set (Mockito's default for collection returns) — fine for this test which
            // only exercises the static registration map.
            return new AclClassMetadata( mock( SessionFactory.class, RETURNS_DEEP_STUBS ) );
        }
    }

    @Autowired
    private AclClassMetadata aclClassMetadata;

    private static class MadeUpClass implements SecuredChild<ExpressionExperiment> {

        @Nullable
        @Override
        public Long getId() {
            return 0L;
        }

        @Override
        @Nullable
        public ExpressionExperiment getSecurityOwner() {
            return null;
        }
    }

    @Test
    public void test() {
        assertThatThrownBy( () -> aclClassMetadata.getSecurityOwnerClass( MadeUpClass.class ) )
                .isInstanceOf( NullPointerException.class )
                .hasMessage( MadeUpClass.class.getName() + " is not registered." );
        assertThat( aclClassMetadata.getSecurityOwnerClass( BioAssay.class ) ).isEqualTo( ExpressionExperiment.class );
        assertThat( aclClassMetadata.getSecurityOwnerIdQueries( BioAssay.class, ":identifier" ) )
                .containsExactly(
                        "select ee.id from ExpressionExperiment ee join ee.bioAssays ba where ba.id = :identifier group by ee",
                        "select eess.sourceExperiment.id from ExpressionExperimentSubSet eess join eess.bioAssays ba where ba.id = :identifier group by eess.sourceExperiment" );
    }
}