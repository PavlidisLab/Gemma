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
package ubic.gemma.core.ontology.chembl;

import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.Objects;

/**
 * A compound ChEMBL identifies for a developmental / trial code, together with the provenance of
 * that identification.
 *
 * <p><b>This is not an annotation and carries no URI to commit.</b> It answers "what is
 * {@code WY-14643}", which is a different question from "what should this sample be tagged with".
 * The name it yields is fed back through Gemma's own ontology search, and it is that search's
 * result — a real CHEBI term or nothing — that a curator can act on. Keeping the two apart is the
 * point: ChEMBL is being used as a naming authority, not as a vocabulary Gemma annotates in.</p>
 *
 * <p>Every field except {@link #getMatchedSynonym()} comes straight from ChEMBL, and the
 * provenance fields exist so a downstream reader can tell where the claim came from and when.
 * An identification with no attribution is exactly the kind of unsourced assertion the curation
 * pipeline is trying to stop producing.</p>
 */
public class ChemblCompound implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String code;
    @Nullable
    private final String chemblId;
    @Nullable
    private final String preferredName;
    @Nullable
    private final String matchedSynonym;
    @Nullable
    private final String release;

    /**
     * Sentinel for "ChEMBL was asked and has nothing", so a repeatedly-submitted unknown code is
     * only sent upstream once. Mirrors {@code OlsTerm.notFound}.
     */
    public static ChemblCompound notFound( String code ) {
        return new ChemblCompound( code, null, null, null, null );
    }

    public ChemblCompound( String code, @Nullable String chemblId, @Nullable String preferredName,
            @Nullable String matchedSynonym, @Nullable String release ) {
        this.code = code;
        this.chemblId = chemblId;
        this.preferredName = preferredName;
        this.matchedSynonym = matchedSynonym;
        this.release = release;
    }

    /** The code as asked about. */
    public String getCode() {
        return code;
    }

    /** ChEMBL's accession for the compound, e.g. {@code CHEMBL295416}. */
    @Nullable
    public String getChemblId() {
        return chemblId;
    }

    /** ChEMBL's preferred name, e.g. {@code PIRINIXIC ACID}. Null for compounds ChEMBL has not named. */
    @Nullable
    public String getPreferredName() {
        return preferredName;
    }

    /**
     * The synonym string that actually matched, which is the evidence for the identification.
     * Without it the claim is unverifiable — see {@link ChemblCodeResolver} on why the fuzzy
     * search endpoint is not used.
     */
    @Nullable
    public String getMatchedSynonym() {
        return matchedSynonym;
    }

    /** ChEMBL release the identification was read from, e.g. {@code ChEMBL_37}. */
    @Nullable
    public String getRelease() {
        return release;
    }

    /** Whether ChEMBL identified the code at all. */
    public boolean isFound() {
        return chemblId != null;
    }

    /**
     * The name to hand back to Gemma's ontology search. Falls back to the matched synonym when
     * ChEMBL has an entry but no preferred name — searching for the code again would be circular,
     * so a nameless identification yields nothing to bridge with.
     */
    @Nullable
    public String getSearchableName() {
        return preferredName != null ? preferredName : null;
    }

    /** Stable link to the compound record, for a curator who wants to check the claim. */
    @Nullable
    public String getSourceUrl() {
        return chemblId != null
                ? "https://www.ebi.ac.uk/chembl/compound_report_card/" + chemblId + "/"
                : null;
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof ChemblCompound ) ) {
            return false;
        }
        ChemblCompound that = ( ChemblCompound ) o;
        return Objects.equals( code, that.code ) && Objects.equals( chemblId, that.chemblId );
    }

    @Override
    public int hashCode() {
        return Objects.hash( code, chemblId );
    }

    @Override
    public String toString() {
        return isFound()
                ? "ChemblCompound[" + code + " -> " + chemblId + " (" + preferredName + ")]"
                : "ChemblCompound[" + code + " not found]";
    }
}
