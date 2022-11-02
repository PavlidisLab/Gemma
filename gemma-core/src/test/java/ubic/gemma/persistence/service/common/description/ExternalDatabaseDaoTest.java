package ubic.gemma.persistence.service.common.description;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@TestExecutionListeners(TransactionalTestExecutionListener.class)
public class ExternalDatabaseDaoTest extends BaseSpringContextTest {

    @Autowired
    private ExternalDatabaseDao externalDatabaseDao;

    /* fixtures */
    private ExternalDatabase ed;

    @After
    public void tearDown() {
        if ( ed != null ) {
            externalDatabaseDao.remove( ed );
        }
    }

    @Test
    @Transactional
    public void testCreateEnsureThatAuditTrailIsCreatedByAop() {
        ed = ExternalDatabase.Factory.newInstance( "test", DatabaseType.OTHER );
        assertThat( ed.getAuditTrail() ).isNull();
        ed = externalDatabaseDao.create( ed );
        assertThat( ed.getAuditTrail() ).isNotNull();
        assertThat( ed.getAuditTrail().getEvents() ).hasSize( 1 );
        ed.setLastUpdated( new Date() );
        externalDatabaseDao.update( ed );
        assertThat( ed.getAuditTrail().getEvents() ).hasSize( 2 );
    }
}