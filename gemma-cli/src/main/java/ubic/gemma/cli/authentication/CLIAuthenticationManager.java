package ubic.gemma.cli.authentication;

import gemma.gsec.authentication.ManualAuthenticationService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.List;

/**
 * Manages authentication from the CLI.
 * @author poirigui
 */
public class CLIAuthenticationManager {

    private final ManualAuthenticationService manualAuthenticationService;

    private final List<CLIAuthenticationAware> cliAuthenticationAwareList;

    public CLIAuthenticationManager( ManualAuthenticationService manualAuthenticationService, List<CLIAuthenticationAware> cliAuthenticationAwareList ) {
        this.manualAuthenticationService = manualAuthenticationService;
        this.cliAuthenticationAwareList = cliAuthenticationAwareList;
    }

    public Authentication authenticate( String username, String password ) throws AuthenticationException {
        try {
            Authentication result = manualAuthenticationService.authenticate( username, password );
            // the authentication manager erases credentials, so we need to create it again
            UsernamePasswordAuthenticationToken authToPass = new UsernamePasswordAuthenticationToken( username, password );
            cliAuthenticationAwareList.forEach( a -> a.setAuthentication( authToPass ) );
            return result;
        } catch ( AuthenticationException e ) {
            cliAuthenticationAwareList.forEach( CLIAuthenticationAware::clearAuthentication );
            throw e;
        }
    }

    public Authentication authenticateAnonymously() {
        try {
            Authentication result = manualAuthenticationService.authenticateAnonymously();
            cliAuthenticationAwareList.forEach( a -> a.setAuthentication( result ) );
            return result;
        } catch ( AuthenticationException e ) {
            cliAuthenticationAwareList.forEach( CLIAuthenticationAware::clearAuthentication );
            throw e;
        }
    }
}
