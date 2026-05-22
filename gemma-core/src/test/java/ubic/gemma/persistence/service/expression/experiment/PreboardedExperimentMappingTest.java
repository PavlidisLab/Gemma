/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.expression.experiment.PreboardedExperiment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard for the {@code PreboardedExperiment} discriminator mapping
 * added in commit {@code 44bf890061}.
 * <p>
 * The compat patch's only job is to let a hotfix-1.32.7 instance MAP an
 * {@code INVESTIGATION} row whose {@code class} discriminator column is
 * {@code 'PreboardedExperiment'} (written by a phase2 peer against a shared
 * DB) without throwing {@code WrongClassException} or returning {@code null}
 * from polymorphic loads. This test pins the round-trip end-to-end:
 *
 * <ol>
 *     <li>Persist a {@code PreboardedExperiment} via the session.</li>
 *     <li>Evict from the first-level cache.</li>
 *     <li>Reload polymorphically via {@code session.get(Investigation.class, id)}.</li>
 *     <li>Assert the reloaded instance is typed as {@code PreboardedExperiment}
 *         and that the three preboarded-specific fields round-trip.</li>
 * </ol>
 *
 * <p>If the HBM subclass entry is removed, the discriminator value is changed,
 * or the column names drift, this test will fail with a concrete signal
 * (ClassCastException on the assertion, or WrongClassException at load time).
 */
@ContextConfiguration
public class PreboardedExperimentMappingTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class PreboardedExperimentMappingTestContextConfiguration extends BaseDatabaseTestContextConfiguration {
    }

    @Autowired
    private SessionFactory sessionFactory;

    @Test
    public void preboardedRoundTripsThroughHibernate() {
        PreboardedExperiment pb = new PreboardedExperiment();
        pb.setName( "GSE12345 (preboarded)" );
        pb.setAccession( "GSE12345" );
        pb.setSource( "GEO" );
        pb.setIdentifyingMetadata( "{\"title\":\"test\",\"pubmed\":\"1\"}" );

        Session session = sessionFactory.getCurrentSession();
        session.persist( pb );
        Long id = pb.getId();
        assertThat( id ).isNotNull();

        // Drop the first-level cache so the reload actually hits Hibernate's
        // entity loader (rather than returning the same in-memory instance).
        session.flush();
        session.evict( pb );

        // Polymorphic load on the BASE class — this is the path that would
        // throw WrongClassException if the discriminator value were unmapped.
        Investigation reloaded = ( Investigation ) session.get( Investigation.class, id );
        assertThat( reloaded )
                .as( "polymorphic load on Investigation must materialize as PreboardedExperiment" )
                .isInstanceOf( PreboardedExperiment.class );

        PreboardedExperiment rt = ( PreboardedExperiment ) reloaded;
        assertThat( rt.getAccession() ).isEqualTo( "GSE12345" );
        assertThat( rt.getSource() ).isEqualTo( "GEO" );
        assertThat( rt.getIdentifyingMetadata() ).isEqualTo( "{\"title\":\"test\",\"pubmed\":\"1\"}" );
    }
}
