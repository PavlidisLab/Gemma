package ubic.gemma.job.grid.util;

public interface SpaceMonitor {

    public abstract void disable();

    /**
     * @return the lastStatusMessage
     */
    public abstract String getLastStatusMessage();

    /**
     * @return the lastStatusWasOK
     */
    public abstract Boolean getLastStatusWasOK();

    /**
     * @return how many times ping has run so far.
     */
    public abstract Integer getNumberOfPings();

    public abstract Integer getNumberOfBadPings();

    /**
     * This will be fired by quartz. Sends notifications if the space isn't functioning as expected.
     * 
     * @return true if everything is nominal. Note that this return value doesn't really do anything when triggered by
     *         quartz.
     */
    public abstract boolean ping();

}