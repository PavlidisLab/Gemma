package ubic.gemma.core.util.test;

import org.springframework.context.ApplicationContext;

public interface AuthenticationTestingUtil {

    void grantAdminAuthority( ApplicationContext ctx );

    void logOut( ApplicationContext ctx );

    void switchToUser( ApplicationContext ctx, String username );
}
