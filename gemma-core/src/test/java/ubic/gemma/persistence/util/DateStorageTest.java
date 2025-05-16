package ubic.gemma.persistence.util;

import com.mysql.cj.jdbc.Driver;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.config.SettingsConfig;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.category.IntegrationTest;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

@Category(IntegrationTest.class)
@ContextConfiguration
public class DateStorageTest extends BaseTest {

    @Import(SettingsConfig.class)
    @Configuration
    @TestComponent
    static class DateStorageTestContextConfiguration {

        @Bean
        public DataSource dataSource(
                @Value("${gemma.testdb.url}") String databaseUrl,
                @Value("${gemma.testdb.user}") String databaseUsername,
                @Value("${gemma.testdb.password}") String databasePassword
        ) throws SQLException {
            SimpleDriverDataSource ds = new SimpleDriverDataSource( new Driver(), databaseUrl, databaseUsername, databasePassword );
            Properties props = new Properties();
            props.put( "connectionTimeZone", "America/Vancouver" );
            ds.setConnectionProperties( props );
            return ds;
        }
    }

    @Autowired
    private DataSource dataSource;

    /**
     * Simulate a case where Gemma is used/deployed in a different timezone than America/Vancouver.
     */
    @Test
    public void testDeploymentInDifferentTimeZone() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate( dataSource );
        jdbcTemplate.execute( "drop table if exists datetime_test " );
        jdbcTemplate.execute( "create table datetime_test (ID BIGINT AUTO_INCREMENT PRIMARY KEY , LAST_UPDATED DATETIME(3))" );
        Calendar c = Calendar.getInstance();
        c.set( 2024, Calendar.JANUARY, 9, 12, 59, 2 );
        c.setTimeZone( TimeZone.getTimeZone( "America/Toronto" ) );
        Date d = c.getTime();
        // truncate milliseconds
        d.setTime( d.getTime() - ( d.getTime() % 1000 ) );
        jdbcTemplate.execute( "insert into datetime_test (LAST_UPDATED) values (?)", ( PreparedStatementCallback<Object> ) ps -> {
            ps.setObject( 1, d );
            ps.execute();
            return null;
        } );
        assertEquals( d, jdbcTemplate.queryForObject( "select LAST_UPDATED from datetime_test limit 1", Date.class ) );
        // literal value should be stored in the America/Vancouver time zone
        assertEquals( "2024-01-09 09:59:02", jdbcTemplate.queryForObject( "select LAST_UPDATED from datetime_test limit 1", String.class ) );
    }
}
