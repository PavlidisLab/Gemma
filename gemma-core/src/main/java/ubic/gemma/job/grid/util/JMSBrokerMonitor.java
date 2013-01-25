package ubic.gemma.job.grid.util;

import javax.jms.JMSException;

public interface JMSBrokerMonitor {

    public boolean isRemoteTasksEnabled();

    public int getNumberOfWorkerHosts() throws JMSException;
    public int getTaskSubmissionQueueLength() throws JMSException;

    public String getTaskSubmissionQueueDiagnosticMessage() throws JMSException;

    boolean canServiceRemoteTasks();
}