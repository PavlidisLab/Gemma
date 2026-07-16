/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.apps;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.LegacyCurationFlagMigrator;

/**
 * One-time maintenance: forward-migrate the legacy {@code CurationDetails.troubled}/
 * {@code needsAttention} flags into Tickets (task 11). Run once at deploy after the ticket-derived
 * cache lands; the live hook keeps the columns correct afterwards. Idempotent — safe to re-run.
 */
@Slf4j
public class MigrateCurationFlagsToTicketsCli extends AbstractAuthenticatedCLI {

    @Autowired
    private LegacyCurationFlagMigrator migrator;

    @Autowired
    private UserManager userManager;

    public MigrateCurationFlagsToTicketsCli() {
        setRequireLogin();
    }

    @Override
    public String getCommandName() {
        return "migrateCurationFlagsToTickets";
    }

    @Override
    public String getShortDesc() {
        return "One-time: migrate legacy CurationDetails troubled/needsAttention flags into tickets (task 11).";
    }

    @Override
    protected void doAuthenticatedWork() {
        User operator = userManager.getCurrentUser();
        if ( operator == null ) {
            throw new IllegalStateException( "No authenticated user to record as the migration tickets' reporter." );
        }
        int opened = migrator.migrate( operator );
        log.info( "Legacy curation-flag migration complete: {} tickets opened (reporter={}).",
                opened, operator.getUserName() );
    }
}
