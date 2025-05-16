package ubic.gemma.core.analysis.report;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.core.util.test.BaseSpringContextTest;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class WhatsNewServiceTest extends BaseSpringContextTest {

    @Autowired
    private WhatsNewService whatsNewService;

    @Value("${gemma.appdata.home}")
    private String appDataHome;

    @Test
    @Ignore("Fails randomly on the CI")
    public void testGeneratePublicWeeklyReport() {
        // FIXME: generate some test data because we currently rely on other tests left-overs
        WhatsNew initialReport = whatsNewService.generateWeeklyReport();
        assertThat( Paths.get( appDataHome, "WhatsNew", "WhatsNew.new" ) ).exists();
        assertThat( Paths.get( appDataHome, "WhatsNew", "WhatsNew.updated" ) ).exists();
        WhatsNew report = whatsNewService.getLatestWeeklyReport();
        assertThat( report ).isNotNull();
        assertThat( report.getDate() ).isEqualTo( initialReport.getDate() );
        assertThat( report.getNewArrayDesigns() )
                .containsExactlyElementsOf( initialReport.getNewArrayDesigns() );
        assertThat( report.getUpdatedArrayDesigns() )
                .containsExactlyElementsOf( initialReport.getUpdatedArrayDesigns() );
        assertThat( report.getNewExpressionExperiments() )
                .containsExactlyElementsOf( initialReport.getNewExpressionExperiments() );
        assertThat( report.getUpdatedExpressionExperiments() )
                .containsExactlyElementsOf( initialReport.getUpdatedExpressionExperiments() );
        assertThat( report.getNewEEIdsPerTaxon() ).isEqualTo( initialReport.getNewEEIdsPerTaxon() );
        assertThat( report.getUpdatedEEIdsPerTaxon() ).isEqualTo( initialReport.getUpdatedEEIdsPerTaxon() );
        assertThat( report.getEeCountPerTaxon() ).isEqualTo( initialReport.getEeCountPerTaxon() );
    }
}