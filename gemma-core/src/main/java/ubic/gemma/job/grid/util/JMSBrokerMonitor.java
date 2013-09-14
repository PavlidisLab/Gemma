package ubic.gemma.job.grid.util;

import javax.jms.JMSException;

public interface JMSBrokerMonitor {

    public int getNumberOfWorkerHosts() throws JMSException;

    public String getTaskSubmissionQueueDiagnosticMessage() throws JMSException;

    public int getTaskSubmissionQueueLength() throws JMSException;

    public boolean isRemoteTasksEnabled();

    boolean canServiceRemoteTasks();
}