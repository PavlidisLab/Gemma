/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.context;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.util.BuildInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Methods to create Spring contexts for Gemma manually. This is meant to be used by CLIs only.
 *
 * @author pavlidis
 */
public class SpringContextUtils {

    private static final Log log = LogFactory.getLog( SpringContextUtils.class.getName() );

    /**
     * System property / environment variable that, when set to {@code true}, makes
     * {@link #prepareContext(ApplicationContext)} fail-fast instead of silently falling
     * back to the {@code dev} profile when no environment profile is active.
     * <p>
     * The env-var form ({@code GEMMA_REQUIRE_EXPLICIT_PROFILE}) is intended for container
     * deployments as an extra safety net; the Dockerfile already sets
     * {@code -Dspring.profiles.active=production}, so the fallback path should never be
     * hit in production. The system-property form ({@code gemma.profile.requireExplicit})
     * lets developers opt in locally without polluting their shell environment.
     */
    static final String REQUIRE_EXPLICIT_PROFILE_PROPERTY = "gemma.profile.requireExplicit";
    static final String REQUIRE_EXPLICIT_PROFILE_ENV = "GEMMA_REQUIRE_EXPLICIT_PROFILE";

    /**
     * Obtain an application context for Gemma.
     *
     * @param activeProfiles list of active profiles, for testing use {@link EnvironmentProfiles#TEST}
     * @param additionalConfigurationLocations a list of additional configuration location to load beans from
     * @return a fully initialized {@link ApplicationContext}
     * @throws org.springframework.beans.BeansException if the creation of the context fails
     */
    public static ApplicationContext getApplicationContext( String[] activeProfiles, String... additionalConfigurationLocations ) throws BeansException {
        List<String> paths = new ArrayList<>();

        paths.add( "classpath*:ubic/gemma/applicationContext-*.xml" );

        paths.addAll( Arrays.asList( additionalConfigurationLocations ) );

        StopWatch timer = StopWatch.createStarted();
        try {
            ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext( paths.toArray( new String[0] ), false );
            for ( String activeProfile : activeProfiles ) {
                context.getEnvironment().addActiveProfile( activeProfile );
            }
            prepareContext( context );
            context.refresh();
            return context;
        } finally {
            SpringContextUtils.log.info( "Got Gemma context in " + timer.getTime( TimeUnit.MILLISECONDS ) + " ms." );
        }
    }

    /**
     * @deprecated this method does not support producing Gemma Web contexts, please migrate existing code to use
     * {@link #getApplicationContext(String[], String...)} instead.
     *
     * @param isWebApp If true, a {@link UnsupportedOperationException} will be raised since retrieving the Web
     *                 application context is not supported from here. Use WebApplicationContextUtils.getWebApplicationContext()
     *                 instead. This is only kept for backward-compatibility with external scripts.
     * @see #getApplicationContext(String[], String...)
     */
    @Deprecated
    public static ApplicationContext getApplicationContext( boolean testing, boolean isWebApp, String[] additionalConfigurationLocations ) throws BeansException {
        if ( isWebApp ) {
            throw new UnsupportedOperationException( "The Web app context cannot be retrieved from here, use WebApplicationContextUtils.getWebApplicationContext() instead." );
        }
        return getApplicationContext( testing ? new String[] { EnvironmentProfiles.TEST } : new String[0], additionalConfigurationLocations );
    }

    /**
     * Prepare a given context for prime time.
     * <p>
     * Perform the following steps:
     * <ul>
     * <li>ensure that the security context holder strategy is set to {@link SecurityContextHolder#MODE_INHERITABLETHREADLOCAL}</li>
     * <li>activate the {@code dev} profile as a fallback if no profile are active, unless
     * {@code -Dgemma.profile.requireExplicit=true} or {@code GEMMA_REQUIRE_EXPLICIT_PROFILE=true}
     * is set, in which case startup fails with an {@link IllegalStateException}</li>
     * <li>activate the {@code scheduler} profile if {@code quartzOn} is set</li>
     * <li>verify that exactly one environment profile is active (see {@link EnvironmentProfiles})</li>
     * <li>log an informative message with the context version and active profiles</li>
     * </ul>
     */
    public static void prepareContext( ApplicationContext context ) {
        if ( !SecurityContextHolder.getContextHolderStrategy().getClass().getName().equals( "org.springframework.security.core.context.InheritableThreadLocalSecurityContextHolderStrategy" ) ) {
            log.warn( String.format( "The security context holder strategy is not set to be inherited in new threads. The strategy will be applied manually. Use -D%s=%s explicitly to remove this warning.",
                    SecurityContextHolder.SYSTEM_PROPERTY, SecurityContextHolder.MODE_INHERITABLETHREADLOCAL ) );
            SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_INHERITABLETHREADLOCAL );
        }
        if ( context instanceof ConfigurableApplicationContext ) {
            ConfigurableApplicationContext cac = ( ConfigurableApplicationContext ) context;
            if ( !cac.getEnvironment().acceptsProfiles( EnvironmentProfiles.PRODUCTION, EnvironmentProfiles.DEV, EnvironmentProfiles.TEST ) ) {
                if ( requireExplicitProfile() ) {
                    throw new IllegalStateException( String.format(
                            "No Spring environment profile is active and explicit profiles are required (%s / %s is true). "
                                    + "Set -Dspring.profiles.active=<%s|%s|%s> explicitly.",
                            REQUIRE_EXPLICIT_PROFILE_PROPERTY, REQUIRE_EXPLICIT_PROFILE_ENV,
                            EnvironmentProfiles.PRODUCTION, EnvironmentProfiles.DEV, EnvironmentProfiles.TEST ) );
                }
                log.warn( String.format(
                        "No Spring environment profile is active; falling back to '%s'. "
                                + "This is a foot-gun if Gemma.properties points at a production database. "
                                + "Set -Dspring.profiles.active=<%s|%s|%s> explicitly, or set %s=true (or -D%s=true) to fail-fast instead.",
                        EnvironmentProfiles.DEV,
                        EnvironmentProfiles.PRODUCTION, EnvironmentProfiles.DEV, EnvironmentProfiles.TEST,
                        REQUIRE_EXPLICIT_PROFILE_ENV, REQUIRE_EXPLICIT_PROFILE_PROPERTY ) );
                cac.getEnvironment().addActiveProfile( EnvironmentProfiles.DEV );
            }
            // enable the scheduler profile if quartzOn is set to true
            if ( Settings.getBoolean( "quartzOn" ) && !cac.getEnvironment().acceptsProfiles( EnvironmentProfiles.SCHEDULER ) ) {
                log.warn( "Enabling the Quartz scheduler since quartzOn is set. You should add 'scheduler' to the active profiles instead." );
                cac.getEnvironment().addActiveProfile( EnvironmentProfiles.SCHEDULER );
            }
        }
        long numberOfActiveEnvironmentProfiles = Stream.of( EnvironmentProfiles.PRODUCTION, EnvironmentProfiles.DEV, EnvironmentProfiles.TEST )
                .filter( context.getEnvironment()::acceptsProfiles )
                .count();
        if ( numberOfActiveEnvironmentProfiles == 0 ) {
            throw new IllegalStateException( "The context must contain at least one environment profile." );
        } else if ( numberOfActiveEnvironmentProfiles > 1 ) {
            throw new IllegalStateException( "The context must contain at most one environment profile." );
        }
        if ( context instanceof AbstractXmlApplicationContext ) {
            // never validate in production, it's too slow
            if ( context.getEnvironment().acceptsProfiles( EnvironmentProfiles.PRODUCTION ) ) {
                log.debug( "Disabling XML validation for parsing the context metadata since the production production is enabled." );
                ( ( AbstractXmlApplicationContext ) context ).setValidating( false );
            }
        }
        BuildInfo buildInfo = BuildInfo.fromManifest();
        SpringContextUtils.log.info( String.format( "Loading Gemma %s%s, hold on!",
                buildInfo,
                context.getEnvironment().getActiveProfiles().length > 0 ?
                        " (active profiles: " + String.join( ", ", context.getEnvironment().getActiveProfiles() ) + ")" : "" ) );
    }

    /**
     * @return {@code true} if either the {@link #REQUIRE_EXPLICIT_PROFILE_PROPERTY} system
     * property or the {@link #REQUIRE_EXPLICIT_PROFILE_ENV} environment variable is set to
     * {@code true} (case-insensitive). The system property takes precedence.
     */
    private static boolean requireExplicitProfile() {
        String sys = System.getProperty( REQUIRE_EXPLICIT_PROFILE_PROPERTY );
        if ( sys != null ) {
            return Boolean.parseBoolean( sys );
        }
        String env = System.getenv( REQUIRE_EXPLICIT_PROFILE_ENV );
        return env != null && Boolean.parseBoolean( env );
    }
}