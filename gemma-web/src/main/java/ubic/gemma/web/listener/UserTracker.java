package ubic.gemma.web.listener;

/**
 * TODO Document Me
 * 
 * @author paul
 * @version $Id$
 */
public class UserTracker {

    private static int activeSessions;

    private static int authenticatedUsers;

    public static int decrementSessions() {
        return --activeSessions;
    }

    public static int incrementSessions() {
        return ++activeSessions;
    }

    public static int incrementAuthenticatedUsers() {
        return ++authenticatedUsers;
    }

    public static int decrementAuthenticatedUsers() {
        return --authenticatedUsers;
    }

    public static int getActiveSessions() {
        return activeSessions;
    }

    public static int getAuthenticatedUsers() {
        return authenticatedUsers;
    }

}
