package ubic.gemma.core.util.test;

public interface TestAuthenticationUtils {
    void runAsAdmin();

    void runAsAgent();

    void runAsUser( String userName, boolean createIfMissing );

    void runAsAnonymous();
}
