package ubic.gemma.core.analysis.report;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest5;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The report used to be asserted through its on-disk cache — generate, then read the
 * Java-serialized {@code WhatsNew.new} / {@code WhatsNew.updated} files back and compare. That
 * cache is gone (its only reader went with gemma-web), so what is left to pin is the query
 * surface itself: the windows agree with each other, and the cheap count agrees with the full
 * report.
 */
public class WhatsNewServiceTest extends BaseSpringContextTest5 {

    @Autowired
    private WhatsNewService whatsNewService;

    /**
     * The daily window is contained in the weekly one. Holds regardless of what the test DB
     * happens to contain, including when both are empty.
     */
    @Test
    public void testDailyReportIsContainedInTheWeeklyReport() {
        WhatsNew daily = whatsNewService.getDailyReport();
        WhatsNew weekly = whatsNewService.getWeeklyReport();

        assertThat( weekly.getNewExpressionExperiments() )
                .containsAll( daily.getNewExpressionExperiments() );
        assertThat( weekly.getNewArrayDesigns() )
                .containsAll( daily.getNewArrayDesigns() );
    }

    /**
     * {@code countNewExpressionExperiments} exists so the daily {@link HomeStats} snapshot can
     * skip {@code getReport}'s platform / taxon / biomaterial passes. It must not skip anything
     * that changes the number.
     */
    @Test
    public void testCountAgreesWithTheFullReport() {
        Date since = new Date( System.currentTimeMillis() - TimeUnit.DAYS.toMillis( 7 ) );

        long counted = whatsNewService.countNewExpressionExperiments( since );

        assertThat( counted )
                .isEqualTo( whatsNewService.getReport( since ).getNewExpressionExperiments().size() );
    }

    /**
     * A window that ended before Gemma existed has nothing in it — guards against the count
     * silently ignoring its date argument and returning the whole corpus.
     */
    @Test
    public void testEmptyWindowCountsZero() {
        Date tomorrow = new Date( System.currentTimeMillis() + TimeUnit.DAYS.toMillis( 1 ) );

        assertThat( whatsNewService.countNewExpressionExperiments( tomorrow ) ).isZero();
    }
}
