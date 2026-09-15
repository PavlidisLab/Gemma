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
package ubic.gemma.core.ontology.relation;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Writes the {@link ubic.gemma.model.common.description.AnnotationRelationBasis#ONTOLOGY} rows of
 * {@code ANNOTATION_RELATION}: the relations our loaded ontologies already assert.
 *
 * <p>Nothing here is new knowledge. CLO has always stated which disease a cell line derives from and
 * which it models; CHEBI has always stated which pharmacological roles a chemical bears;
 * {@link ubic.gemma.core.ontology.model.OntologyTerm#getRestrictions()} has always been able to read
 * both and was called from nowhere. This is the adoption of a capability that was already built, not
 * the construction of one.</p>
 *
 * <p><b>Read as relations, never folded into the hierarchy.</b> The
 * {@code additionalPropertyUris} mechanism next door does the opposite — it makes a restriction's
 * target a <i>parent</i> of the term — and applying it here would make {@code MCF7 cell} a subclass of
 * adenocarcinoma, so browsing a disease would return cell lines. These relations are stored beside the
 * hierarchy, not in it.</p>
 *
 * @see ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil#updateOntologyRelationEntries(Collection)
 */
public interface OntologyRelationProducer {

    /**
     * Rebuild every source this producer knows how to read.
     *
     * @return how many relation rows were written
     */
    int produce();

    /**
     * Rebuild a subset of the sources.
     *
     * <p>Rebuild, not upsert: the rows for the sources being produced are deleted first. Narrowing the
     * run narrows the delete with it, so producing CLO alone leaves CHEBI's rows where they are — an
     * upsert could only correct rows the new read still produces, and a relation an ontology has since
     * retracted would outlive the axiom it came from.</p>
     *
     * @param sources source names as {@link ubic.gemma.model.common.description.AnnotationRelation#getSource()}
     *                spells them ({@code CLO}, {@code CHEBI}), or null/empty for all of them
     * @return how many relation rows were written
     */
    int produce( @Nullable Collection<String> sources );

    /**
     * The sources this producer can read, in the order it reads them.
     */
    Collection<String> getSupportedSources();

    /**
     * Tokens for the vocabularies the named sources point at but do NOT merge, and therefore cannot
     * name a target out of.
     *
     * <p>🛑 A caller warming ontologies has to include these or the read finds every axiom and drops
     * every row for a null {@code OBJECT_VALUE}, while still reporting success. {@code --source CL}
     * wrote 3 rows of an expected 1,203 that way on 2026-09-02.</p>
     *
     * @param sources source names; unknown ones are ignored
     */
    java.util.Collection<String> getTargetOntologiesFor( java.util.Collection<String> sources );
}
