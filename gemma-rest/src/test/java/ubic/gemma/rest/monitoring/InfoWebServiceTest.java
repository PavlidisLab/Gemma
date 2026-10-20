package ubic.gemma.rest.monitoring;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.BuildInfo;

import java.util.Date;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito tests for {@link InfoWebService}. {@link BuildInfo} is mocked; the JVM/OS/uptime
 * blocks pull from {@link java.lang.management.ManagementFactory} which is fine to read live.
 */
class InfoWebServiceTest {

    @Test
    void buildBlockCarriesBuildInfoValues() {
        BuildInfo bi = mock( BuildInfo.class );
        // pick a UTC instant where the SimpleDateFormat output is deterministic
        Date ts = new Date( 1700000000000L );
        when( bi.getVersion() ).thenReturn( "1.32.7-SNAPSHOT" );
        when( bi.getGitHash() ).thenReturn( "abc1234" );
        when( bi.getTimestamp() ).thenReturn( ts );

        InfoWebService.InfoValueObject info = new InfoWebService( bi ).getInfo();

        assertThat( info.getBuild().getVersion() ).isEqualTo( "1.32.7-SNAPSHOT" );
        assertThat( info.getBuild().getGitHash() ).isEqualTo( "abc1234" );
        // compare against the same UTC SimpleDateFormat the service uses
        java.text.DateFormat fmt = new java.text.SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.ENGLISH );
        fmt.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        assertThat( info.getBuild().getTimestamp() ).isEqualTo( fmt.format( ts ) );
    }

    @Test
    void nullBuildTimestamp_yieldsNullTimestampField() {
        BuildInfo bi = mock( BuildInfo.class );
        when( bi.getVersion() ).thenReturn( "dev" );
        when( bi.getGitHash() ).thenReturn( null );
        when( bi.getTimestamp() ).thenReturn( null );

        InfoWebService.InfoValueObject info = new InfoWebService( bi ).getInfo();

        assertThat( info.getBuild().getVersion() ).isEqualTo( "dev" );
        assertThat( info.getBuild().getGitHash() ).isNull();
        assertThat( info.getBuild().getTimestamp() ).isNull();
    }

    @Test
    void javaBlockPopulatedFromSystemProperties() {
        BuildInfo bi = mock( BuildInfo.class );

        InfoWebService.InfoValueObject info = new InfoWebService( bi ).getInfo();

        assertThat( info.getJava().getVersion() ).isEqualTo( System.getProperty( "java.version" ) );
        assertThat( info.getJava().getVendor() ).isEqualTo( System.getProperty( "java.vendor" ) );
        assertThat( info.getJava().getVm() ).isEqualTo( System.getProperty( "java.vm.name" ) );
    }

    @Test
    void osBlockPopulatedFromSystemProperties() {
        BuildInfo bi = mock( BuildInfo.class );

        InfoWebService.InfoValueObject info = new InfoWebService( bi ).getInfo();

        assertThat( info.getOs().getName() ).isEqualTo( System.getProperty( "os.name" ) );
        assertThat( info.getOs().getVersion() ).isEqualTo( System.getProperty( "os.version" ) );
        assertThat( info.getOs().getArch() ).isEqualTo( System.getProperty( "os.arch" ) );
    }

    @Test
    void uptimeBlockReturnsPositiveValues() {
        BuildInfo bi = mock( BuildInfo.class );

        InfoWebService.InfoValueObject info = new InfoWebService( bi ).getInfo();

        // the JVM has been running before the test, so both fields must be positive
        assertThat( info.getUptime().getStartTimeMillis() ).isPositive();
        assertThat( info.getUptime().getUptimeMillis() ).isPositive();
    }
}
