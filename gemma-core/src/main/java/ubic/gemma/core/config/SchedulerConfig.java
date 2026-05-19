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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.security.core.context.SecurityContext;

import org.quartz.JobDetail;
import org.quartz.Trigger;

import ubic.gemma.core.scheduler.BatchInfoRepopulationJob;
import ubic.gemma.core.scheduler.Ee2AdUpdateJob;
import ubic.gemma.core.scheduler.Ee2cUpdateJob;
import ubic.gemma.core.scheduler.SecureMethodInvokingJobDetailFactoryBean;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.analysis.report.WhatsNewService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Quartz scheduler configuration for Gemma, replacing the deprecated XML
 * {@code applicationContext-schedule.xml}.
 * <p>
 * Profile-gated on {@code scheduler} — only the production node that runs scheduled
 * maintenance jobs activates this profile (see {@code SpringContextUtils} which adds the
 * profile when the {@code quartzOn} setting is true). On other nodes (CLI, REST, dev), the
 * profile is inactive, so this class and all of its {@code @Bean} methods are skipped and
 * none of the Quartz machinery is wired.
 * <p>
 * Each trigger preserves the bean id, cron expression, and job-detail wiring from the legacy
 * XML so external consumers (Quartz job-store records, log scrapes, runbooks) keep working.
 * Bean references such as {@code groupAgentSecurityContext} live in
 * {@code applicationContext-dataSource.xml} and are injected by qualifier here.
 * <p>
 * Note: {@code SecureMethodInvokingJobDetailFactoryBean} requires its {@code targetObject}
 * to be a real bean — services like {@code indexerService} that don't currently exist in this
 * codebase will fail context startup under the {@code scheduler} profile. That is identical
 * to the legacy XML behavior; this migration is intentionally a like-for-like port and does
 * not attempt to repair dead wiring.
 * <p>
 * {@link EnableScheduling} is enabled to preserve {@code <task:annotation-driven/>}
 * semantics, even though the comment in the legacy XML noted Gemma deliberately avoids
 * {@code @Scheduled} / {@code @Async} for security-context reasons. Keeping the annotation
 * on means any future {@code @Scheduled} methods (e.g., in CLI tools) still wire up.
 *
 * @author keshav (original XML)
 */
@Configuration
@Profile("scheduler")
@EnableScheduling
public class SchedulerConfig {

    /**
     * Main Quartz entry point. The trigger list mirrors exactly what the legacy XML wired —
     * the commented-out {@code viewTrigger} and {@code monitorSpaceTrigger} from the XML remain
     * out of scope here.
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(
            @Qualifier("arrayDesignReportTrigger") Trigger arrayDesignReportTrigger,
            @Qualifier("expressionExperimentReportTrigger") Trigger expressionExperimentReportTrigger,
            @Qualifier("whatsNewTrigger") Trigger whatsNewTrigger,
            @Qualifier("gene2CsUpdateTrigger") Trigger gene2CsUpdateTrigger,
            @Qualifier("batchInfoTrigger") Trigger batchInfoTrigger,
            @Qualifier("ee2cExperimentUpdateTrigger") Trigger ee2cExperimentUpdateTrigger,
            @Qualifier("ee2cSampleUpdateTrigger") Trigger ee2cSampleUpdateTrigger,
            @Qualifier("ee2cExperimentalDesignUpdateTrigger") Trigger ee2cExperimentalDesignUpdateTrigger,
            @Qualifier("ee2adUpdateTrigger") Trigger ee2adUpdateTrigger,
            @Qualifier("indexExperimentsTrigger") Trigger indexExperimentsTrigger ) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setTriggers(
                arrayDesignReportTrigger,
                expressionExperimentReportTrigger,
                whatsNewTrigger,
                gene2CsUpdateTrigger,
                batchInfoTrigger,
                ee2cExperimentUpdateTrigger,
                ee2cSampleUpdateTrigger,
                ee2cExperimentalDesignUpdateTrigger,
                ee2adUpdateTrigger,
                indexExperimentsTrigger );
        return factory;
    }

    // -------- Triggers --------
    // Cron field order: Seconds Minutes Hours Day-of-month Month Day-of-week (Year?)

    /** Every day at 00:30. */
    @Bean(name = "batchInfoTrigger")
    public CronTriggerFactoryBean batchInfoTrigger(
            @Qualifier("batchInfoJobDetail") JobDetail jobDetail ) {
        CronTriggerFactoryBean t = new CronTriggerFactoryBean();
        t.setJobDetail( jobDetail );
        t.setCronExpression( "0 30 0 * * ?" );
        return t;
    }

    @Bean(name = "batchInfoJobDetail")
    public JobDetailFactoryBean batchInfoJobDetail(
            ExpressionExperimentService expressionExperimentService,
            ExpressionExperimentReportService expressionExperimentReportService,
            AuditEventService auditEventService,
            @Qualifier("groupAgentSecurityContext") SecurityContext securityContext ) {
        JobDetailFactoryBean jd = new JobDetailFactoryBean();
        jd.setJobClass( BatchInfoRepopulationJob.class );
        Map<String, Object> data = new HashMap<>();
        data.put( "expressionExperimentService", expressionExperimentService );
        data.put( "expressionExperimentReportService", expressionExperimentReportService );
        data.put( "auditEventService", auditEventService );
        data.put( "securityContext", securityContext );
        jd.setJobDataAsMap( data );
        return jd;
    }

    /** Every first of the month at 00:15. */
    @Bean(name = "expressionExperimentReportTrigger")
    public CronTriggerFactoryBean expressionExperimentReportTrigger(
            ExpressionExperimentReportService expressionExperimentReportService,
            @Qualifier("groupAgentSecurityContext") SecurityContext securityContext ) {
        return secureMethodCronTrigger( expressionExperimentReportService, "generateSummaryObjects",
                "0 15 0 1 * ?", securityContext );
    }

    /** Every first of the month at 01:30. */
    @Bean(name = "arrayDesignReportTrigger")
    public CronTriggerFactoryBean arrayDesignReportTrigger(
            ArrayDesignReportService arrayDesignReportService,
            @Qualifier("groupAgentSecurityContext") SecurityContext securityContext ) {
        return secureMethodCronTrigger( arrayDesignReportService, "generateArrayDesignReport",
                "0 30 1 1 * ?", securityContext );
    }

    /** Every day at 00:15. */
    @Bean(name = "whatsNewTrigger")
    public CronTriggerFactoryBean whatsNewTrigger(
            WhatsNewService whatsNewService,
            @Qualifier("groupAgentSecurityContext") SecurityContext securityContext ) {
        return secureMethodCronTrigger( whatsNewService, "generateWeeklyReport",
                "0 15 0 * * ?", securityContext );
    }

    /** Every day at 00:40. */
    @Bean(name = "gene2CsUpdateTrigger")
    public CronTriggerFactoryBean gene2CsUpdateTrigger(
            TableMaintenanceUtil tableMaintenanceUtil,
            @Qualifier("groupAgentSecurityContext") SecurityContext securityContext ) {
        return secureMethodCronTrigger( tableMaintenanceUtil, "updateGene2CsEntries",
                "0 40 0 * * ?", securityContext );
    }

    /** Every working day at 19:00. */
    @Bean(name = "ee2cExperimentUpdateTrigger")
    public CronTriggerFactoryBean ee2cExperimentUpdateTrigger(
            @Qualifier("ee2cExperimentUpdateJobDetail") JobDetail jobDetail ) {
        CronTriggerFactoryBean t = new CronTriggerFactoryBean();
        t.setJobDetail( jobDetail );
        t.setCronExpression( "0 0 19 ? * MON-FRI" );
        return t;
    }

    @Bean(name = "ee2cExperimentUpdateJobDetail")
    public JobDetailFactoryBean ee2cExperimentUpdateJobDetail(
            TableMaintenanceUtil tableMaintenanceUtil,
            @Qualifier("groupAgentSecurityContext") SecurityContext securityContext ) {
        return ee2cUpdateJobDetail( tableMaintenanceUtil, ExpressionExperiment.class.getName(), securityContext );
    }

    /** Every working day at 19:10. */
    @Bean(name = "ee2cSampleUpdateTrigger")
    public CronTriggerFactoryBean ee2cSampleUpdateTrigger(
            @Qualifier("ee2cSampleUpdateJobDetail") JobDetail jobDetail ) {
        CronTriggerFactoryBean t = new CronTriggerFactoryBean();
        t.setJobDetail( jobDetail );
        t.setCronExpression( "0 10 19 ? * MON-FRI" );
        return t;
    }

    @Bean(name = "ee2cSampleUpdateJobDetail")
    public JobDetailFactoryBean ee2cSampleUpdateJobDetail(
            TableMaintenanceUtil tableMaintenanceUtil,
            @Qualifier("groupAgentSecurityContext") SecurityContext securityContext ) {
        return ee2cUpdateJobDetail( tableMaintenanceUtil, BioMaterial.class.getName(), securityContext );
    }

    /** Every working day at 19:20. */
    @Bean(name = "ee2cExperimentalDesignUpdateTrigger")
    public CronTriggerFactoryBean ee2cExperimentalDesignUpdateTrigger(
            @Qualifier("ee2cExperimentalDesignUpdateJobDetail") JobDetail jobDetail ) {
        CronTriggerFactoryBean t = new CronTriggerFactoryBean();
        t.setJobDetail( jobDetail );
        t.setCronExpression( "0 20 19 ? * MON-FRI" );
        return t;
    }

    @Bean(name = "ee2cExperimentalDesignUpdateJobDetail")
    public JobDetailFactoryBean ee2cExperimentalDesignUpdateJobDetail(
            TableMaintenanceUtil tableMaintenanceUtil,
            @Qualifier("groupAgentSecurityContext") SecurityContext securityContext ) {
        return ee2cUpdateJobDetail( tableMaintenanceUtil, ExperimentalDesign.class.getName(), securityContext );
    }

    /** Every working day at 19:30. */
    @Bean(name = "ee2adUpdateTrigger")
    public CronTriggerFactoryBean ee2adUpdateTrigger(
            @Qualifier("ee2adUpdateJobDetail") JobDetail jobDetail ) {
        CronTriggerFactoryBean t = new CronTriggerFactoryBean();
        t.setJobDetail( jobDetail );
        t.setCronExpression( "0 30 19 ? * MON-FRI" );
        return t;
    }

    @Bean(name = "ee2adUpdateJobDetail")
    public JobDetailFactoryBean ee2adUpdateJobDetail(
            TableMaintenanceUtil tableMaintenanceUtil,
            @Qualifier("groupAgentSecurityContext") SecurityContext securityContext ) {
        JobDetailFactoryBean jd = new JobDetailFactoryBean();
        jd.setJobClass( Ee2AdUpdateJob.class );
        Map<String, Object> data = new HashMap<>();
        data.put( "tableMaintenanceUtil", tableMaintenanceUtil );
        data.put( "securityContext", securityContext );
        jd.setJobDataAsMap( data );
        return jd;
    }

    /**
     * Every working day at 23:00. NOTE: depends on an {@code indexerService} bean that is not
     * currently defined anywhere in the codebase — context startup under the {@code scheduler}
     * profile will fail at this bean unless that service is restored. Preserved verbatim from
     * the legacy XML to avoid masking the broken wiring during migration.
     */
    @Bean(name = "indexExperimentsTrigger")
    public CronTriggerFactoryBean indexExperimentsTrigger(
            @Qualifier("indexerService") Object indexerService,
            @Qualifier("groupAgentSecurityContext") SecurityContext securityContext ) {
        SecureMethodInvokingJobDetailFactoryBean mi = new SecureMethodInvokingJobDetailFactoryBean( securityContext );
        mi.setTargetObject( indexerService );
        mi.setTargetMethod( "index" );
        mi.setArguments( new Object[] { ExpressionExperiment.class.getName() } );
        mi.setConcurrent( false );
        try {
            mi.afterPropertiesSet();
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
        JobDetail jobDetail = mi.getObject();

        CronTriggerFactoryBean t = new CronTriggerFactoryBean();
        t.setJobDetail( jobDetail );
        t.setCronExpression( "0 0 23 ? * MON-FRI" );
        return t;
    }

    // -------- Helpers --------

    /**
     * Build a {@link CronTriggerFactoryBean} whose job detail is a non-concurrent
     * {@link SecureMethodInvokingJobDetailFactoryBean} that calls {@code targetMethod}
     * on {@code targetObject} under {@code securityContext}.
     */
    private CronTriggerFactoryBean secureMethodCronTrigger(
            Object targetObject, String targetMethod, String cron, SecurityContext securityContext ) {
        SecureMethodInvokingJobDetailFactoryBean mi = new SecureMethodInvokingJobDetailFactoryBean( securityContext );
        mi.setTargetObject( targetObject );
        mi.setTargetMethod( targetMethod );
        mi.setConcurrent( false );
        try {
            mi.afterPropertiesSet();
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
        JobDetail jobDetail = mi.getObject();

        CronTriggerFactoryBean t = new CronTriggerFactoryBean();
        t.setJobDetail( jobDetail );
        t.setCronExpression( cron );
        return t;
    }

    /**
     * Build a {@link JobDetailFactoryBean} that runs {@link Ee2cUpdateJob} at the given
     * granularity ({@code level} is the fully-qualified class name of the entity whose ee2c
     * association is being refreshed: ExpressionExperiment, BioMaterial, or ExperimentalDesign).
     */
    private JobDetailFactoryBean ee2cUpdateJobDetail( TableMaintenanceUtil tableMaintenanceUtil,
            String level, SecurityContext securityContext ) {
        JobDetailFactoryBean jd = new JobDetailFactoryBean();
        jd.setJobClass( Ee2cUpdateJob.class );
        Map<String, Object> data = new HashMap<>();
        data.put( "tableMaintenanceUtil", tableMaintenanceUtil );
        data.put( "level", level );
        data.put( "securityContext", securityContext );
        jd.setJobDataAsMap( data );
        return jd;
    }
}
