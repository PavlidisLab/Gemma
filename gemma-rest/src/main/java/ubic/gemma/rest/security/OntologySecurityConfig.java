package ubic.gemma.rest.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Security for the Gemma ontology pages under {@code /ont/**}, served by
 * {@code ubic.gemma.rest.servlet.GemmaOntologyServlet}.
 * <p>
 * Every request runs as the anonymous user, credentials or not: the TGFVO services are
 * {@code @Secured("IS_AUTHENTICATED_ANONYMOUSLY")} and filter factor values by the ACL of their dataset, so they
 * need an authentication in the context, and the pages should be the same public view for everyone (and so
 * cacheable). A factor value of a private dataset is not found. There is no login, session or CSRF.
 * <p>
 * Kept apart from {@link RestSecurityConfig} so the chain can be tested on its own.
 */
@Configuration
public class OntologySecurityConfig {

    @Bean
    @Order(2)
    public SecurityFilterChain ontologySecurityFilterChain( HttpSecurity http ) throws Exception {
        return http
                .securityMatcher( new AntPathRequestMatcher( "/ont/**" ) )
                .authorizeHttpRequests( auth -> auth.anyRequest().permitAll() )
                .anonymous( anon -> anon
                        .authorities( "IS_AUTHENTICATED_ANONYMOUSLY" ) )
                .sessionManagement( s -> s
                        .sessionCreationPolicy( SessionCreationPolicy.STATELESS ) )
                .csrf( csrf -> csrf.disable() )
                .requestCache( cache -> cache.disable() )
                .securityContext( sc -> sc.requireExplicitSave( true ) )
                .build();
    }
}
