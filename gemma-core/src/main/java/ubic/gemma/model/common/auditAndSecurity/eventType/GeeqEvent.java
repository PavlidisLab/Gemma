package ubic.gemma.model.common.auditAndSecurity.eventType;

public class GeeqEvent extends AuditEventType {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 6621758826080039878L;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public GeeqEvent() {
    }

    public static final class Factory {

        public static GeeqEvent newInstance() {
            return new GeeqEvent();
        }

    }

}
