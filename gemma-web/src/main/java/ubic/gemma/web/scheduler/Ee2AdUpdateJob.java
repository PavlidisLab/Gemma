package ubic.gemma.web.scheduler;

import lombok.Setter;
import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;
import org.springframework.util.Assert;
import ubic.gemma.persistence.service.TableMaintenanceUtil;

/**
 * @author poirigui
 */
@Setter
public class Ee2AdUpdateJob extends SecureQuartzJobBean implements StatefulJob {

    private TableMaintenanceUtil tableMaintenanceUtil;

    @Override
    protected void executeAsAgent( JobExecutionContext context ) {
        Assert.notNull( tableMaintenanceUtil, "The tableMaintenanceUtil bean was not set." );
        tableMaintenanceUtil.updateExpressionExperiment2ArrayDesignEntries( context.getPreviousFireTime() );
    }
}
