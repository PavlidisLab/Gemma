package ubic.gemma.apps;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.EntityLocatorImpl;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.util.Collection;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class ArrayDesignSequenceManipulatingCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public TestExplicitADsOnlyCli testExplicitADsOnlyCli() {
            return new TestExplicitADsOnlyCli();
        }

        @Bean
        public TestOrdinaryCli testOrdinaryCli() {
            return new TestOrdinaryCli();
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock();
        }

        @Bean
        public ArrayDesignReportService arrayDesignReportService() {
            return mock();
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock();
        }

        @Bean
        public AuditEventService auditEventService() {
            return mock();
        }

        @Bean
        public EntityLocator entityLocator() {
            return mock();
        }
    }

    /**
     * Stand-in for a destructive platform CLI ({@code detachSequences}, {@code deletePlatformElements}): must never
     * accept {@code -all}, only {@code -a}/{@code -f}.
     */
    static class TestExplicitADsOnlyCli extends ArrayDesignSequenceManipulatingCli {

        public TestExplicitADsOnlyCli() {
            setExplicitADsOnly();
        }

        @Override
        protected void processArrayDesign( ArrayDesign arrayDesign ) {
            addSuccessObject( arrayDesign, "Processed." );
        }
    }

    static class TestOrdinaryCli extends ArrayDesignSequenceManipulatingCli {

        @Override
        protected void processArrayDesign( ArrayDesign arrayDesign ) {
            addSuccessObject( arrayDesign, "Processed." );
        }
    }

    @Autowired
    private TestExplicitADsOnlyCli testExplicitADsOnlyCli;

    @Autowired
    private TestOrdinaryCli testOrdinaryCli;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private EntityLocator entityLocator;

    /**
     * {@code arrayDesignService} and {@code entityLocator} are shared, manually-created mocks (not
     * {@code @MockBean}), so nothing resets their recorded interactions between test methods; without this,
     * one test's real {@code loadAll()} call would leak into another test's {@code verify(..., never())}.
     */
    @BeforeEach
    public void resetMocks() {
        reset( arrayDesignService, entityLocator );
    }

    /**
     * {@code -all} is not a registered option here, so Commons CLI's short-option matching absorbs it as
     * {@code -a ll} (this CLI's own {@code -a} takes an argument) rather than rejecting it outright — but either
     * way it must never reach {@link ArrayDesignService#loadAll()}: "ll" isn't a real platform, so location fails
     * exactly as it would for any other typo, matching {@link EntityLocatorImpl#locateArrayDesign}'s real behavior.
     */
    @Test
    @WithMockUser
    public void testExplicitADsOnlyAllOptionNeverLoadsEveryPlatform() {
        when( entityLocator.locateArrayDesign( "ll" ) ).thenThrow( new NullPointerException( "No platform with short name ll" ) );
        assertThat( testExplicitADsOnlyCli )
                .withArguments( "-all" )
                .fails();
        verify( arrayDesignService, never() ).loadAll();
    }

    @Test
    @WithMockUser
    public void testExplicitADsOnlyRequiresAOrF() {
        assertThat( testExplicitADsOnlyCli )
                .withArguments()
                .fails()
                .exitCause()
                .hasMessageStartingWith( "No platforms matched the given options; name them with -a or -f "
                        + "(this command never operates on all platforms)." );
    }

    @Test
    @WithMockUser
    public void testExplicitADsOnlySucceedsWithA() {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setId( 1L );
        ad.setShortName( "GPL1" );
        when( entityLocator.locateArrayDesign( eq( "GPL1" ) ) ).thenReturn( ad );
        assertThat( testExplicitADsOnlyCli )
                .withArguments( "-a", "GPL1" )
                .succeeds();
    }

    /**
     * Regression check: a CLI on this base class that does NOT opt into {@link ArrayDesignSequenceManipulatingCli#setExplicitADsOnly()} keeps
     * {@code -all}, since that's shared by CLIs that legitimately process every platform (e.g. report regeneration).
     */
    @Test
    @WithMockUser
    public void testOrdinaryCliStillAcceptsAllOption() {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setId( 1L );
        ad.setShortName( "GPL1" );
        Collection<ArrayDesign> all = Collections.singletonList( ad );
        when( arrayDesignService.loadAll() ).thenReturn( all );
        assertThat( testOrdinaryCli )
                .withArguments( "-all" )
                .succeeds();
        verify( arrayDesignService ).loadAll();
    }
}
