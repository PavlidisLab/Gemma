package ubic.gemma.core.util.test;

import org.hibernate.cfg.Settings;
import org.hibernate.internal.SessionFactoryImpl;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.persistence.util.TestComponent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(SlowTest.class)
@ContextConfiguration
public class HibernateConfigTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class HibernateConfigTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

    }

    @Test
    public void test() {
        Settings settings = ( ( SessionFactoryImpl ) sessionFactory ).getSettings();
        assertEquals( 3, settings.getMaximumFetchDepth().intValue() );
        assertEquals( 500, settings.getDefaultBatchFetchSize() );
        assertTrue( settings.isJdbcBatchVersionedData() );
        assertTrue( settings.isOrderInsertsEnabled() );
        assertTrue( settings.isOrderUpdatesEnabled() );
    }
}
