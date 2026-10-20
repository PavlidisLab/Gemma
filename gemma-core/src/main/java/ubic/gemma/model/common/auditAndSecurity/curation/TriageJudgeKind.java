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
package ubic.gemma.model.common.auditAndSecurity.curation;

/**
 * What kind of judge produced an {@link AnnotationSet} triage ruling.
 *
 * <p>Kept beside {@link AnnotationSetTriage#getJudgedBy()} rather than
 * inferred from it. The identity string is a username for a person and a run
 * id for an agent, and telling those apart by shape is the kind of rule that
 * works until someone's username looks like a UUID.</p>
 *
 * <p>It exists because "has any human ever looked at this?" is a real query —
 * it is the difference between an un-reviewed corpus and a reviewed one — and
 * without this column the answer requires knowing every agent run id.</p>
 */
public enum TriageJudgeKind {

    /** An automated run: triage pass, QC gate, audit pipeline. */
    AGENT,

    /** A person. */
    CURATOR;

    public String getDbValue() {
        return name().toLowerCase();
    }

    public static TriageJudgeKind fromDbValue( String v ) {
        return TriageJudgeKind.valueOf( v.toUpperCase() );
    }
}
