/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.stereotype.Component;
import ubic.gemma.core.context.EnvironmentProfiles;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fail-fast startup hook for unresolved sentinel ({@code XXXXXX}) placeholder values shipped by
 * {@code default.properties}. Container deployers who forget to set, e.g., {@code GEMMA_DB_PASSWORD}
 * would otherwise see Spring's {@link Value @Value} resolution substitute the literal {@code XXXXXX}
 * into the JDBC URL / security tokens, and the failure would surface deep into the runtime as a
 * confusing MySQL "Access denied" or auth error.
 * <p>
 * This validator runs in {@link InitializingBean#afterPropertiesSet()} (after the
 * {@code Environment} has merged environment-variable overrides, system properties, and on-disk
 * {@code Gemma.properties}), walks a hardcoded list of keys that ship with {@code XXXXXX}-class
 * placeholders, and throws {@link IllegalStateException} naming each unresolved key plus the
 * matching {@code GEMMA_*} environment variable the deployer should set.
 * <p>
 * Active only under {@link EnvironmentProfiles#PRODUCTION} and {@link EnvironmentProfiles#DEV} —
 * tests run under {@link EnvironmentProfiles#TEST} where the sentinel values are tolerated (the
 * keys are either unused or used only for internal token equality checks).
 * <p>
 * Bypass via {@code gemma.sentinels.ignore=true} if a deployer has a justified reason to keep a
 * sentinel value (e.g. a future-deployment placeholder during staging). Off by default.
 *
 * @see <a href="file:../../../../../../../../../../CONTAINER_IMAGE_RECCE.md">CONTAINER_IMAGE_RECCE.md</a> Gap 3
 */
@Slf4j
@Component
@Profile({ EnvironmentProfiles.PRODUCTION, EnvironmentProfiles.DEV })
public class SentinelPropertyValidator implements InitializingBean {

    /**
     * Keys that ship with a sentinel placeholder in {@code default.properties}. Listed in the
     * order they appear in the source file so the resulting error message reads sensibly.
     * <p>
     * Excluded by design:
     * <ul>
     *   <li>{@code gemma.testdb.*} — only consumed under the {@code test}/{@code testdb} profile,
     *       which this validator does not activate under.</li>
     *   <li>{@code mail.password} (ships blank) — empty string is a deliberate "no auth" signal
     *       for {@code mail.host=localhost} SMTP relays.</li>
     * </ul>
     * If a new sentinel key is added to {@code default.properties}, add it here too.
     */
    private static final String[] GUARDED_KEYS = {
            "mail.username",
            "gemma.db.password",
            "gemma.runas.password",
            "gemma.anonymousAuth.key",
            "gemma.agent.password"
    };

    /**
     * Sentinel value patterns to match. A property "passes" the validator iff its resolved value
     * does NOT consist exclusively of these characters (anywhere from 4 to 32 of them). Matching
     * the regex form rather than a fixed string lets the validator catch slight variants
     * ({@code XXXXXX}, {@code XXXXXXX}, {@code XXXXXXXX}) without needing to recite each one.
     */
    private static final java.util.regex.Pattern SENTINEL_PATTERN =
            java.util.regex.Pattern.compile( "X{4,32}" );

    private final PropertySources propertySources;

    @Value("${gemma.sentinels.ignore:false}")
    private boolean ignoreSentinels;

    public SentinelPropertyValidator( @Qualifier("settingsPropertySources") PropertySources propertySources ) {
        this.propertySources = propertySources;
    }

    @Override
    public void afterPropertiesSet() {
        if ( ignoreSentinels ) {
            log.warn( "gemma.sentinels.ignore=true — skipping XXXXXX sentinel check. "
                    + "Unresolved placeholders may cause downstream auth failures." );
            return;
        }

        Map<String, String> offenders = new LinkedHashMap<>();
        for ( String key : GUARDED_KEYS ) {
            String value = lookup( key );
            if ( value != null && SENTINEL_PATTERN.matcher( value ).matches() ) {
                offenders.put( key, value );
            }
        }

        if ( offenders.isEmpty() ) {
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add( "One or more required Gemma properties still resolve to their default "
                + "XXXXXX placeholder. Set the matching environment variable (or override via "
                + "Gemma.properties / -D system property):" );
        for ( Map.Entry<String, String> e : offenders.entrySet() ) {
            lines.add( "  - " + e.getKey() + " = " + e.getValue()
                    + "   (set $" + toEnvVar( e.getKey() ) + ")" );
        }
        lines.add( "To bypass this check (NOT recommended in production), set "
                + "gemma.sentinels.ignore=true." );

        throw new IllegalStateException( String.join( System.lineSeparator(), lines ) );
    }

    /**
     * Look up a key by walking the configured {@link PropertySources} in priority order. Mirrors
     * the resolution that {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer}
     * applies for {@code ${...}} substitution at {@link Value @Value} sites — so this validator's
     * verdict matches what gets injected at runtime.
     */
    private String lookup( String key ) {
        for ( PropertySource<?> ps : propertySources ) {
            Object v = ps.getProperty( key );
            if ( v != null ) {
                return v.toString();
            }
        }
        return null;
    }

    /**
     * Translate a dot-separated Gemma property key to its POSIX-style environment-variable form,
     * matching {@link SettingsConfig#filterEnvironmentVariables(Map)} ({@code gemma.db.password} →
     * {@code GEMMA_DB_PASSWORD}).
     */
    static String toEnvVar( String key ) {
        return key.toUpperCase().replace( '.', '_' );
    }
}
