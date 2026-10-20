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
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Every registered secured child. Extend this when registering a new one — the duplicate check
     * below is only as complete as this list.
     */
    private static final List<Class<? extends SecuredChild<?>>> REGISTERED = Arrays.asList(
            BioAssay.class, BioMaterial.class, ExpressionExperimentSubSet.class, MeanVarianceRelation.class,
            ExperimentalDesign.class, ExperimentalFactor.class, FactorValue.class,
            DifferentialExpressionAnalysis.class, ExpressionAnalysisResultSet.class,
            SampleCoexpressionAnalysis.class, PrincipalComponentAnalysis.class );

    /**
     * No two secured children may resolve their owner through the same query.
     * <p>
     * Each query names the child's own association, so an identical string across two classes means
     * one of them is asking about the other's entity. That is not hypothetical: {@code
     * ExpressionExperimentSubSet}, {@code MeanVarianceRelation} and {@code ExperimentalDesign} were
     * registered with {@code ExpressionAnalysisResultSet}'s query — one copy-paste down three
     * consecutive lines — and nothing failed, because these queries feed a consistency linter that
     * treats an empty result as agreement. Run against the wrong table they returned nothing for
     * every input, so the linter passed all three without checking them.
     * <p>
     * Asserting the exact strings would catch it too, but only as a change detector. This asserts
     * the property that was actually violated, so it holds for registrations nobody has written yet.
     */
    @Test
    public void testNoTwoSecuredChildrenShareAnOwnerQuery() {
        Map<String, Class<?>> seenQueryToClass = new HashMap<>();
        for ( Class<? extends SecuredChild<?>> clazz : REGISTERED ) {
            for ( String query : aclClassMetadata.getSecurityOwnerIdQueries( clazz, ":identifier" ) ) {
                Class<?> previous = seenQueryToClass.put( query, clazz );
                assertThat( previous )
                        .withFailMessage( "%s and %s resolve their security owner through the same query,"
                                        + " so at least one is asking about the other's entity:%n  %s",
                                previous != null ? previous.getSimpleName() : null, clazz.getSimpleName(), query )
                        .isNull();
            }
        }
    }

    /**
     * The three that carried the wrong query, pinned explicitly: a subset names its source
     * experiment, while the design and the mean-variance relation are named BY the experiment.
     */
    @Test
    public void testTheThreeFormerlyCopyPastedRegistrations() {
        assertThat( aclClassMetadata.getSecurityOwnerIdQueries( ExpressionExperimentSubSet.class, ":identifier" ) )
                .containsExactly( "select eess.sourceExperiment.id from ExpressionExperimentSubSet eess where eess.id = :identifier" );
        assertThat( aclClassMetadata.getSecurityOwnerIdQueries( MeanVarianceRelation.class, ":identifier" ) )
                .containsExactly( "select ee.id from ExpressionExperiment ee where ee.meanVarianceRelation.id = :identifier" );
        assertThat( aclClassMetadata.getSecurityOwnerIdQueries( ExperimentalDesign.class, ":identifier" ) )
                .containsExactly( "select ee.id from ExpressionExperiment ee where ee.experimentalDesign.id = :identifier" );
    }
}