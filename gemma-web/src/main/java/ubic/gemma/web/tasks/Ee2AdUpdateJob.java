package ubic.gemma.web.tasks;

import lombok.Setter;
import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;
import org.springframework.util.Assert;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.web.scheduler.SecureQuartzJobBean;

/**
 * @author poirigui
 */
@Setter
public class Ee2AdUpdateJob extends SecureQuartzJobBean implements StatefulJob {

    private TableMaintenanceUtil tableMaintenanceUtil;

    @Override
    protected void executeAs( JobExecutionContext context ) {
        Assert.notNull( tableMaintenanceUtil, "The tableMaintenanceUtil bean was not set." );
        tableMaintenanceUtil.updateExpressionExperiment2ArrayDesignEntries( context.getPreviousFireTime() );
    }
}
