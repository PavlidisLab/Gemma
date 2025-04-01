package ubic.gemma.cli.authentication;

import org.springframework.security.core.Authentication;

/**
 * Interface for a component that should receive the username/password authentication token from the CLI.
 */
public interface CLIAuthenticationAware {

    void setAuthentication( Authentication authentication );

    void clearAuthentication();
}
