package ubic.gemma.persistence.util;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import ubic.gemma.core.util.test.BaseSpringContextTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * This test cover the case where the {@link SpringProfiles#NODB} is active. In this scenario, the database cannot be
 * accessed either during initialization or later at runtime. This is achieved by a misconfigured data source declared
 * in applicationContext-dataSource.xml.
 * @author poirigui
 */
@ActiveProfiles({ "nodb" })
public class NodbProfileTest extends BaseSpringContextTest {

    @BeforeClass
    public static void disableHibernateUsageOfJdbcMetadataDefaults() {
        // FIXME: this is merely there to mimic the behaviour of SpringContextUtil.prepareContext()
        System.setProperty( "gemma.hibernate.use_jdbc_metadata_defaults", "false" );
    }

    @AfterClass
    public static void restoreSystemProperties() {
        System.clearProperty( "gemma.hibernate.use_jdbc_metadata_defaults" );
    }

    @Autowired
    private HikariDataSource dataSource;

    @Test
    public void test() {
        // TODO: check that dataSource.getConnection() has never been used
        assertThatThrownBy( dataSource::getConnection )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "dataSource or dataSourceClassName or jdbcUrl is required" );
    }
}