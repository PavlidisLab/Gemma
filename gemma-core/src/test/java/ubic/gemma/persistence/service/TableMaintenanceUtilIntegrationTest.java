package ubic.gemma.persistence.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.persistence.util.Settings;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author poirigui
 */
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class TableMaintenanceUtilIntegrationTest extends BaseSpringContextTest {

    private static final Path GENE2CS_PATH = Paths.get( Settings.getString( "gemma.appdata.home" ), "DbReports", "gene2cs.info" );
    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Before
    @After
    public void removeGene2CsStatusFileAndDirectory() {
        File f = GENE2CS_PATH.toFile();
        if ( f.exists() ) {
            assertThat( f.delete() ).isTrue();
            // also remove the parent folder
            assertThat( f.getParentFile().delete() ).isTrue();
        }
    }

    @Test
    @WithMockUser(authorities = "GROUP_AGENT")
    public void testWhenUserIsAgent() {
        tableMaintenanceUtil.updateGene2CsEntries();
        assertThat( GENE2CS_PATH ).exists();
    }

    @Test(expected = AccessDeniedException.class)
    public void testWhenUserIsAnonymous() {
        this.runAsAnonymous();
        tableMaintenanceUtil.updateGene2CsEntries();
    }

    @Test
    @WithMockUser(authorities = "GROUP_AGENT")
    public void testUpdateExpressionExperiment2CharacteristicEntries() {
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries();
    }

    @Test(expected = AccessDeniedException.class)
    public void testUpdateEE2CAsUser() {
        this.runAsAnonymous();
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries();
    }
}
