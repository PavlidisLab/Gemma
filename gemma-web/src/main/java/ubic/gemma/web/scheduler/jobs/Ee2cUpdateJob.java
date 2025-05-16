package ubic.gemma.web.scheduler.jobs;

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
public class Ee2cUpdateJob extends SecureQuartzJobBean implements StatefulJob {

    private TableMaintenanceUtil tableMaintenanceUtil;

    private Class<?> level = null;

    @Override
    public void executeAs( JobExecutionContext context ) {
        Assert.notNull( tableMaintenanceUtil, "The tableMaintenanceUtil bean was not set." );
        if ( level == null ) {
            tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( context.getPreviousFireTime(), false );
        } else {
            tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( level, context.getPreviousFireTime(), false );
        }
    }
}
