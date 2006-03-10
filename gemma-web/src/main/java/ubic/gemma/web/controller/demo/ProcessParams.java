package ubic.gemma.web.controller.demo;

/**
 * TODO - DOCUMENT ME
 *
 * @author pavlidis
 * @version $Id$
 */
public class ProcessParams {
    double startAt;
    double endAt;
    long sleepTime;
    /**
     * @return Returns the endAt.
     */
    public double getEndAt() {
        return this.endAt;
    }
    /**
     * @param endAt The endAt to set.
     */
    public void setEndAt( double endAt ) {
        this.endAt = endAt;
    }
    /**
     * @return Returns the sleepTime.
     */
    public long getSleepTime() {
        return this.sleepTime;
    }
    /**
     * @param sleepTime The sleepTime to set.
     */
    public void setSleepTime( long sleepTime ) {
        this.sleepTime = sleepTime;
    }
    /**
     * @return Returns the startAt.
     */
    public double getStartAt() {
        return this.startAt;
    }
    /**
     * @param startAt The startAt to set.
     */
    public void setStartAt( double startAt ) {
        this.startAt = startAt;
    }
}