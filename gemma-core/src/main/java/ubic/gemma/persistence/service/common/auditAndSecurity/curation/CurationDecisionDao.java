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
package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDecision;
import ubic.gemma.persistence.service.BaseDao;

import java.util.List;

/**
 * DAO for {@link CurationDecision} rows -- standing rulings that a change must
 * not be made to an experiment.
 *
 * <p>Rows are append-only, so every read comes in two flavours: the full log,
 * and the latest-wins fold that answers "does a refusal stand". A caller that
 * wants the second must not take the head of the first by hand -- the
 * supersession rule, scope included, lives in one place.</p>
 */
public interface CurationDecisionDao extends BaseDao<CurationDecision> {

    /**
     * Every decision on one experiment, most recent first -- the full log,
     * decisions since reversed included.
     */
    List<CurationDecision> findByInvestigation( Investigation investigation );

    /**
     * The standing decision under each key for one experiment.
     */
    List<CurationDecision> findStandingByInvestigation( Investigation investigation );

    /**
     * Every decision on one key, most recent first. The head is the standing
     * one, if the scopes match.
     */
    List<CurationDecision> findByInvestigationAndKey( Investigation investigation, String decisionKey );
}
