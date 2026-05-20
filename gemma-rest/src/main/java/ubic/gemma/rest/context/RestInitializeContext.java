package ubic.gemma.rest.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import static ubic.gemma.core.context.SpringContextUtils.prepareContext;

/**
 * Standalone gemma-rest counterpart to {@link ubic.gemma.web.context.InitializeContext} (which lives in
 * gemma-web and pulls in JSP / theme / servlet-context configuration that the REST WAR has no use for).
 * <p>
 * Responsibilities reduced to what the standalone {@code gemma-rest.war} actually needs:
 * <ul>
 *     <li>Activate the {@code web} Spring profile so beans gated on {@code @Profile("web")} (notably
 *     {@link ubic.gemma.rest.analytics.ga4.AnalyticsConfig} and the {@code <beans profile="web">}
 *     wrapper in {@code applicationContext-analytics.xml}) become active. The profile name is kept
 *     as {@code web} rather than introducing a fresh {@code rest} profile so existing
 *     {@code @Profile("web")} call sites in gemma-rest do not need re-gating.</li>
 *     <li>Delegate to {@link ubic.gemma.core.context.SpringContextUtils#prepareContext} for the
 *     environment-profile-validation + {@code SecurityContextHolder} strategy work that every
 *     Gemma context needs.</li>
 * </ul>
 * <p>
 * Wired in {@code gemma-rest/src/main/webapp/WEB-INF/web.xml} via:
 * <pre>{@code
 * <context-param>
 *     <param-name>contextInitializerClasses</param-name>
 *     <param-value>ubic.gemma.rest.context.RestInitializeContext</param-value>
 * </context-param>
 * }</pre>
 *
 * @see ubic.gemma.web.context.InitializeContext gemma-web's full-fat equivalent (kept in place for the
 * legacy UI's lifecycle; both classes co-exist during the gemma-web → gemma-rest cutover)
 */
@Slf4j
public class RestInitializeContext implements ApplicationContextInitializer<ConfigurableWebApplicationContext> {

    @Override
    public void initialize( ConfigurableWebApplicationContext applicationContext ) {
        activateWebProfile( applicationContext );
        prepareContext( applicationContext );
    }

    /**
     * Activate the {@code web} Spring profile. See class javadoc for rationale.
     */
    private void activateWebProfile( ConfigurableWebApplicationContext cac ) {
        cac.getEnvironment().addActiveProfile( "web" );
    }
}
