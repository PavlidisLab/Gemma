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
package ubic.gemma.core.loader.expression.geo;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.expression.bioAssay.ExtractedMolecule;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which channel's molecule describes the SAMPLE.
 *
 * @author gembro
 */
public class GeoConverterMoleculeTest {

    @Test
    public void testNoChannelReportsAMolecule() {
        assertThat( GeoConverterImpl.resolveMolecule( Collections.emptySet() ) ).isNull();
    }

    @Test
    public void testChannelsAgreeing() {
        assertThat( GeoConverterImpl.resolveMolecule( set( ExtractedMolecule.totalRNA ) ) )
                .isEqualTo( ExtractedMolecule.totalRNA );
    }

    /**
     * 🛑 The GSE9164 case. A two-colour sample co-hybridized against a genomic-DNA reference disagrees by
     * construction, and only the non-DNA channel is the sample. Taking channel 1 by position labelled all 14
     * of its assays {@code genomicDNA} on prod while every description read "Total RNA ... RNeasy Mini Kit";
     * GEO's GSM230264 has ch1 {@code genomic DNA} (Cy3) and ch2 {@code total RNA} (Cy5).
     * <p>
     * Order must not matter — the reference is not reliably first.
     */
    @Test
    public void testGenomicDnaIsAReferenceChannelAndNotTheSample() {
        assertThat( GeoConverterImpl.resolveMolecule( set( ExtractedMolecule.genomicDNA, ExtractedMolecule.totalRNA ) ) )
                .as( "the sample is the channel that is not the DNA reference" )
                .isEqualTo( ExtractedMolecule.totalRNA );

        assertThat( GeoConverterImpl.resolveMolecule( set( ExtractedMolecule.totalRNA, ExtractedMolecule.genomicDNA ) ) )
                .as( "and channel order does not decide it" )
                .isEqualTo( ExtractedMolecule.totalRNA );

        assertThat( GeoConverterImpl.resolveMolecule( set( ExtractedMolecule.genomicDNA, ExtractedMolecule.polyARNA ) ) )
                .isEqualTo( ExtractedMolecule.polyARNA );
    }

    /**
     * ...but only genomic DNA earns that treatment. Two RNA flavours name no reference, so nothing
     * distinguishes them and asserting either would claim GEO said something it did not.
     */
    @Test
    public void testTwoRnaFlavoursStayUnset() {
        assertThat( GeoConverterImpl.resolveMolecule( set( ExtractedMolecule.totalRNA, ExtractedMolecule.polyARNA ) ) )
                .as( "no reference channel to discard, so no answer" )
                .isNull();
    }

    /** Three disagreeing channels are not the two-colour shape either, DNA present or not. */
    @Test
    public void testThreeMoleculesStayUnset() {
        assertThat( GeoConverterImpl.resolveMolecule(
                set( ExtractedMolecule.genomicDNA, ExtractedMolecule.totalRNA, ExtractedMolecule.protein ) ) )
                .isNull();
    }

    private static Set<ExtractedMolecule> set( ExtractedMolecule... ms ) {
        Set<ExtractedMolecule> s = new LinkedHashSet<>();
        Collections.addAll( s, ms );
        return s;
    }
}
