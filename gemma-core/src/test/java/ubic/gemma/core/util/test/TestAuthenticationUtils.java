package ubic.gemma.core.util.test;

public interface TestAuthenticationUtils {
    void runAsAdmin();

    void runAsUser( String userName );

    void runAsAnonymous();
}
