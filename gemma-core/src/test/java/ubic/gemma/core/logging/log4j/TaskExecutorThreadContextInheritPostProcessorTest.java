package ubic.gemma.core.logging.log4j;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;

@ContextConfiguration
public class TaskExecutorThreadContextInheritPostProcessorTest extends BaseTest {

    @TestComponent
    @Configuration
    static class CC {

        @Bean
        public static TaskExecutorThreadContextInheritPostProcessor taskExecutorThreadContextInheritPostProcessor() {
            return new TaskExecutorThreadContextInheritPostProcessor();
        }

        @Bean
        public TaskExecutor taskExecutor() {
            return new SimpleAsyncTaskExecutor();
        }
    }

    @Autowired
    private TaskExecutor taskExecutor;

    @Test
    public void test() {
        Assertions.assertThat( this.taskExecutor )
                .asInstanceOf( InstanceOfAssertFactories.type( DelegatingThreadContextTaskExecutor.class ) )
                .extracting( DelegatingThreadContextTaskExecutor::getDelegate )
                .isInstanceOf( SimpleAsyncTaskExecutor.class );
    }
}