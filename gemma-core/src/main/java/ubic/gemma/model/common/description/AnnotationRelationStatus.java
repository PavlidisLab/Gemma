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
package ubic.gemma.model.common.description;

/**
 * Whether a source is stating that a relation holds, or stating that it does not.
 *
 * <p>Almost everything in {@link AnnotationRelation} is an assertion, and for a long time the table
 * could hold nothing else. MGI publishes the other kind — {@code MGI_Geno_NotDiseaseDO.rpt} is 1,211
 * curated, cited rows saying a genotype does <b>not</b> model a disease — and disconfirmation is rare
 * enough that discarding it is the expensive option. A negative result that somebody bothered to
 * record is worth more than its row count suggests: it is the only thing that can stop an inference
 * that everything else supports.</p>
 *
 * <p>🛑 <b>A refuted row is dangerous in a way an absent one is not.</b> Read by anything that does
 * not know this column exists, it says the exact opposite of what its source said — so the column is
 * {@code NOT NULL DEFAULT 'ASSERTED'}, the reads filter to {@link #ASSERTED} unless a caller asks
 * otherwise, and {@link #REFUTED} never licenses an inference. {@code PUBLICATION_ASSOCIATION} took
 * the same shape in V25 after the same reasoning; the difference is only in who the naive reader is
 * (Gemma 1.32.x there, our own read paths here, since 1.32.x does not know this table).</p>
 *
 * <p><b>Not a confidence scale.</b> These are two things a source can say, not two ends of one. A
 * relation nobody has an opinion about is simply absent, which is a third state and is represented by
 * there being no row.</p>
 */
public enum AnnotationRelationStatus {

    /**
     * The source states the relation holds. The default, and what every row written before this
     * column existed means.
     */
    ASSERTED,

    /**
     * The source states the relation does NOT hold — an explicit negative, not a missing positive.
     *
     * <p>Kept out of ordinary reads and out of every inference. Its value is to a caller that asks
     * for it by name: "does anything actively deny this?" is a different question from "does anything
     * support it?", and only this basis can answer the first.</p>
     */
    REFUTED
}
