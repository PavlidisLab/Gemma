package ubic.gemma.persistence.retry;

import org.junit.After;
import org.junit.Test;
import org.mockito.internal.verification.VerificationModeFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.test.BaseSpringContextTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class RetryTest extends BaseSpringContextTest implements InitializingBean {

    public interface TestRetryDao {

        int work();
    }

    @Service
    public static class TestRetryService {

        private TestRetryDao dao;

        @Retryable
        public int pessimisticOperation() {
            return dao.work();
        }

        @Transactional
        public int pessimisticOperationWithTransactionalAnnotation() {
            return dao.work();
        }

        public int pessimisticOperationWithoutRetry() {
            return dao.work();
        }

        public void setTestRetryDao( TestRetryDao testRetryDao ) {
            this.dao = testRetryDao;
        }
    }

    @Autowired
    private TestRetryService testRetryService;

    @Autowired
    public SimpleRetryPolicy retryPolicy;

    private TestRetryDao testRetryDao;

    @Override
    public void afterPropertiesSet() {
        testRetryDao = mock( TestRetryDao.class );
        testRetryService.setTestRetryDao( testRetryDao );
        // 10 is too slow for testing
        retryPolicy.setMaxAttempts( 3 );
    }

    @After
    public void tearDown() {
        reset( testRetryDao );
    }

    @Test
    public void testRetry() {
        when( testRetryDao.work() ).thenThrow( PessimisticLockingFailureException.class );
        assertThatThrownBy( testRetryService::pessimisticOperation )
                .isInstanceOf( PessimisticLockingFailureException.class );
        verify( testRetryDao, VerificationModeFactory.times( 3 ) ).work();
    }

    @Test
    public void testRetryWithRetryableExceptionInCause() {
        when( testRetryDao.work() ).thenThrow( new RuntimeException( new PessimisticLockingFailureException( "test" ) ) );
        assertThatThrownBy( testRetryService::pessimisticOperation )
                .isInstanceOf( RuntimeException.class )
                .cause()
                .isInstanceOf( PessimisticLockingFailureException.class );
        verify( testRetryDao, VerificationModeFactory.times( 3 ) ).work();
    }

    @Test
    public void testRetryWhenNoExceptionIsRaised() {
        testRetryService.pessimisticOperation();
        verify( testRetryDao ).work();
    }

    @Test
    public void testRetryWithNonRetryableException() {
        when( testRetryDao.work() ).thenThrow( RuntimeException.class );
        assertThatThrownBy( testRetryService::pessimisticOperation )
                .isInstanceOf( RuntimeException.class );
        verify( testRetryDao ).work();
        verifyNoMoreInteractions( testRetryDao );
    }

    @Test
    public void testRetryTransactionalOperation() {
        when( testRetryDao.work() ).thenThrow( PessimisticLockingFailureException.class );
        assertThatThrownBy( testRetryService::pessimisticOperationWithTransactionalAnnotation )
                .isInstanceOf( PessimisticLockingFailureException.class );
        verify( testRetryDao, VerificationModeFactory.times( 3 ) ).work();
    }

    @Test
    public void testRetryNonTransactionalOperation() {
        when( testRetryDao.work() ).thenThrow( PessimisticLockingFailureException.class );
        assertThatThrownBy( testRetryService::pessimisticOperationWithoutRetry )
                .isInstanceOf( PessimisticLockingFailureException.class );
        verify( testRetryDao ).work();
        verifyNoMoreInteractions( testRetryDao );
    }
}