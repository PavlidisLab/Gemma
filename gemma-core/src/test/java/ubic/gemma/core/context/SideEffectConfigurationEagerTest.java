/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.context;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the trap described in {@link LazyInitByDefaultPostProcessor}.
 * <p>
 * A {@code @Configuration} that implements {@link InitializingBean} normally exists for the side
 * effect in {@code afterPropertiesSet()} — registering a Hibernate event listener, priming a
 * registry — and nothing injects it. In CLI contexts {@link LazyInitByDefaultPostProcessor} marks
 * every non-infrastructure definition lazy-init, so such a bean is defined but never instantiated
 * and its side effect never happens. Nothing throws; the capability is just absent.
 * <p>
 * That cost us {@code AclEventListenerConfig} and {@code AuditTrailEventListenerConfig} between
 * 2026-05-18 and 2026-08-08: no ACLs were created for CLI-loaded experiments, no ACLs were removed
 * for CLI-deleted ones, and no CREATE/DELETE audit events were emitted. The fix in both cases was
 * {@code @Lazy(false)}, which the post-processor honours (it skips any definition annotated
 * {@code @Lazy}, whatever the value).
 * <p>
 * This test fails if a new such configuration is added without that annotation.
 */
public class SideEffectConfigurationEagerTest {

    private static final String[] SCANNED_PACKAGES = { "ubic.gemma.core", "ubic.gemma.persistence" };

    @Test
    public void configurationsWithSideEffectsMustOptOutOfLazyByDefault() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider( false );
        scanner.addIncludeFilter( new AnnotationTypeFilter( Configuration.class ) );
        List<String> offenders = new ArrayList<>();
        for ( String pkg : SCANNED_PACKAGES ) {
            for ( BeanDefinition bd : scanner.findCandidateComponents( pkg ) ) {
                Class<?> clazz;
                try {
                    clazz = Class.forName( bd.getBeanClassName() );
                } catch ( Throwable e ) {
                    // optional dependency missing on the test classpath; not our concern here
                    continue;
                }
                if ( !InitializingBean.class.isAssignableFrom( clazz ) ) {
                    continue;
                }
                if ( clazz.isAnnotationPresent( TestComponent.class ) ) {
                    continue;
                }
                Lazy lazy = clazz.getAnnotation( Lazy.class );
                if ( lazy == null || lazy.value() ) {
                    offenders.add( clazz.getName() );
                }
            }
        }
        assertThat( offenders )
                .as( "@Configuration classes implementing InitializingBean must be annotated @Lazy(false), "
                        + "otherwise LazyInitByDefaultPostProcessor makes them dead in CLI contexts and their "
                        + "afterPropertiesSet() side effect silently never runs" )
                .isEmpty();
    }
}
