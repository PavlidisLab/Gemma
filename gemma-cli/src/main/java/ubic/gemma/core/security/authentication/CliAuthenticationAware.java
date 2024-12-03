package ubic.gemma.core.security.authentication;

import org.springframework.security.core.Authentication;

import javax.annotation.Nullable;

/**
 * Interface for a component that should receive the username/password authentication token from the CLI.
 */
public interface CliAuthenticationAware {

    void setAuthentication( @Nullable Authentication authentication );
}
