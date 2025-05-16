package ubic.gemma.core.metrics;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration
public class GenericMeterRegistryConfigurerTest extends BaseTest {

    @Configuration
    @TestComponent
    @EnableAspectJAutoProxy
    static class GenericMeterRegistryConfigurerTestContextConfiguration {

        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        public TimedAspect timedAspect( MeterRegistry meterRegistry ) {
            return new TimedAspect( meterRegistry );
        }

        @Bean
        public SessionFactory sessionFactory() {
            SessionFactory sf = mock( SessionFactory.class );
            Statistics s = mock( Statistics.class );
            when( s.isStatisticsEnabled() ).thenReturn( Boolean.FALSE );
            when( sf.getStatistics() ).thenReturn( s );
            return sf;
        }

        @Bean
        public GenericMeterRegistryConfigurer meterRegistryConfigurer( MeterRegistry meterRegistry ) {
            return new GenericMeterRegistryConfigurer( meterRegistry, Collections.emptyList() );
        }

        @Bean
        public MyService myService() {
            return new MyService();
        }

        @Bean
        public MyService2 myService2() {
            return new MyService2Impl();
        }
    }

    @Service
    public static class MyService {

        @Timed
        public void test() {
        }
    }

    public interface MyService2 {
        @Timed
        void test();
    }

    public static class MyService2Impl implements MyService2 {

        @Override
        public void test() {

        }
    }

    @Autowired
    private MyService myService;

    @Autowired
    private MyService2 myService2;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    public void testTimedMethod() {
        assertThat( getTimer( MyService.class, "test" ) )
                .isNull();
        myService.test();
        assertThat( getTimer( MyService.class, "test" ) )
                .isNotNull();
    }

    @Test
    @Ignore("The @Timed annotation does not work at interface-level, see https://github.com/PavlidisLab/Gemma/issues/541.")
    public void testTimedMethodAtInterfaceLevel() {
        assertThat( getTimer( MyService2.class, "test" ) ).isNull();
        myService2.test();
        assertThat( getTimer( MyService2.class, "test" ) ).isNotNull();
    }

    private Timer getTimer( Class<?> clazz, String methodName ) {
        return meterRegistry.find( "method.timed" )
                .tag( "class", GenericMeterRegistryConfigurerTest.class.getName() + "$" + clazz.getSimpleName() )
                .tag( "method", methodName )
                .timer();
    }
}