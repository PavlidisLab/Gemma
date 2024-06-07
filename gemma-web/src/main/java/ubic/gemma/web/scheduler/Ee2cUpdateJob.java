package ubic.gemma.web.scheduler;

import lombok.Setter;
import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;
import org.springframework.util.Assert;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;

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
