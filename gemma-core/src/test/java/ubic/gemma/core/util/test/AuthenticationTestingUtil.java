package ubic.gemma.core.util.test;

public interface AuthenticationTestingUtil {

    void grantAdminAuthority();

    void logOut();

    void switchToUser( String username );
}
