package ubic.gemma.core.job.grid.util;

import javax.jms.JMSException;

public interface JMSBrokerMonitor {

    int getNumberOfWorkerHosts() throws JMSException;

    boolean isRemoteTasksEnabled();

    boolean canServiceRemoteTasks();
}