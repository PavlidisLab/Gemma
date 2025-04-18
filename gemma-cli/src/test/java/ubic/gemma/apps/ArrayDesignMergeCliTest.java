package ubic.gemma.apps;

import gemma.gsec.authentication.ManualAuthenticationService;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignMergeService;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.cli.util.test.BaseCliTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class ArrayDesignMergeCliTest extends BaseCliTest {

    @Configuration
    @TestComponent
    public static class ArrayDesignMergeCliTestContextConfiguration {

        @Bean
        public ArrayDesignMergeCli arrayDesignMergeCli() {
            return new ArrayDesignMergeCli();
        }

        @Bean
        public ManualAuthenticationService manualAuthenticationService() {
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
        public ArrayDesignMergeService arrayDesignMergeService() {
            return mock( ArrayDesignMergeService.class );
        }

        @Bean
        public ArrayDesignReportService arrayDesignReportService() {
            return mock( ArrayDesignReportService.class );
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock( ArrayDesignService.class );
        }

        @Bean
        public GemmaRestApiClient gemmaRestApiClient() {
            return mock();
        }

        @Bean
        public EntityLocator entityLocator() {
            return mock();
        }
    }

    @Autowired
    private ArrayDesignMergeCli arrayDesignMergeCli;

    @Autowired
    private ArrayDesignMergeService arrayDesignMergeService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private EntityLocator entityLocator;

    @After
    public void tearDown() {
        reset( entityLocator, arrayDesignService );
    }

    @Test
    @WithMockUser
    public void test() {
        ArrayDesign a = ArrayDesign.Factory.newInstance();
        ArrayDesign b = ArrayDesign.Factory.newInstance();
        ArrayDesign c = ArrayDesign.Factory.newInstance();
        when( entityLocator.locateArrayDesign( "1" ) ).thenReturn( a );
        when( entityLocator.locateArrayDesign( "2" ) ).thenReturn( b );
        when( entityLocator.locateArrayDesign( "3" ) ).thenReturn( c );
        when( arrayDesignService.thaw( any( ArrayDesign.class ) ) ).thenAnswer( args -> args.getArgument( 0 ) );
        when( arrayDesignService.thaw( anyCollection() ) ).thenAnswer( args -> args.getArgument( 0 ) );
        Collection<ArrayDesign> otherPlatforms = new HashSet<>( Arrays.asList( b, c ) );
        assertThat( arrayDesignMergeCli )
                .withArguments( "-a", "1", "-o", "2,3", "-s", "4", "-n", "four is better than one" )
                .succeeds();
        verify( arrayDesignMergeService ).merge( a, otherPlatforms, "four is better than one", "4", false );
    }
}
