package ubic.gemma.core.apps;

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
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignMergeService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.test.BaseCliTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.util.TestComponent;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
    }

    @Autowired
    private ArrayDesignMergeCli arrayDesignMergeCli;

    @Autowired
    private ArrayDesignMergeService arrayDesignMergeService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @After
    public void tearDown() {
        reset( arrayDesignService );
    }

    @Test
    @WithMockUser
    public void test() {
        ArrayDesign a = ArrayDesign.Factory.newInstance();
        ArrayDesign b = ArrayDesign.Factory.newInstance();
        ArrayDesign c = ArrayDesign.Factory.newInstance();
        when( arrayDesignService.findByShortName( "1" ) ).thenReturn( a );
        when( arrayDesignService.findByShortName( "2" ) ).thenReturn( b );
        when( arrayDesignService.findByShortName( "3" ) ).thenReturn( c );
        when( arrayDesignService.thaw( any( ArrayDesign.class ) ) ).thenAnswer( args -> args.getArgument( 0 ) );
        when( arrayDesignService.thaw( anyCollection() ) ).thenAnswer( args -> args.getArgument( 0 ) );
        Collection<ArrayDesign> otherPlatforms = new HashSet<>( Arrays.asList( b, c ) );
        assertThat( arrayDesignMergeCli.executeCommand( new String[] { "-a", "1", "-o", "2,3", "-s", "4", "-n", "four is better than one" } ) )
                .isEqualTo( AbstractCLI.SUCCESS );
        verify( arrayDesignMergeService ).merge( a, otherPlatforms, "four is better than one", "4", false );
    }
}
