package ubic.gemma.persistence.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author poirigui
 */
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class TableMaintenanceUtilIntegrationTest extends BaseSpringContextTest {

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Value("${gemma.gene2cs.path}")
    private Path gene2CsPath;

    @Before
    @After
    public void removeGene2CsStatusFileAndDirectory() {
        File f = gene2CsPath.toFile();
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
        assertThat( gene2CsPath ).exists();
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
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( ExpressionExperiment.class );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( BioMaterial.class );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( ExperimentalDesign.class );
        assertThatThrownBy( () -> {
            tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( FactorValue.class );
        } ).isInstanceOf( IllegalArgumentException.class );
    }

    @Test(expected = AccessDeniedException.class)
    public void testUpdateEE2CAsUser() {
        this.runAsAnonymous();
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries();
    }

    @Test
    @WithMockUser(authorities = "GROUP_AGENT")
    public void testUpdateExpressionExperiment2ArrayDesignEntries() {
        tableMaintenanceUtil.updateExpressionExperiment2ArrayDesignEntries();
    }
}
