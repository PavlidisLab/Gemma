/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubic.gemma.persistence.service.maintenance;

import io.micrometer.core.annotation.Timed;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.mail.MailEngine;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEvent;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.genome.GeneDao;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Functions for maintaining the database. This is intended for denormalized tables and statistics tables that need to
 * be generated periodically.
 *
 * @author jsantos
 * @author paul
 */
@Service
@CommonsLog
public class TableMaintenanceUtilImpl implements TableMaintenanceUtil {

    /**
     * Clause for selecting entities updated since a given date.
     */
    private static final String CD_LAST_UPDATED_SINCE = "(CD.LAST_UPDATED is null or :since is null or CD.LAST_UPDATED >= :since)";

    /**
     * The query used to repopulate the contents of the GENE2CS table.
     */
    private static final String GENE2CS_REPOPULATE_QUERY = "select gene.ID, cs.ID, cs.ARRAY_DESIGN_FK "
            + "from CHROMOSOME_FEATURE as gene, CHROMOSOME_FEATURE as geneprod, BIO_SEQUENCE2_GENE_PRODUCT as bsgp, COMPOSITE_SEQUENCE cs "
            + "where geneprod.GENE_FK = gene.ID and bsgp.GENE_PRODUCT_FK = geneprod.ID and bsgp.BIO_SEQUENCE_FK = cs.BIOLOGICAL_CHARACTERISTIC_FK "
            + "group by gene.ID, cs.ID, cs.ARRAY_DESIGN_FK";

    /**
     * The query used to repopulate the contents of the GENE2CS table.
     */
    private static final String GENE2CS_REPOPULATE_SINCE_LAST_UPDATE_QUERY = "select gene.ID, cs.ID, cs.ARRAY_DESIGN_FK "
            + "from CHROMOSOME_FEATURE as gene, CHROMOSOME_FEATURE as geneprod, BIO_SEQUENCE2_GENE_PRODUCT as bsgp, COMPOSITE_SEQUENCE cs "
            + "join ARRAY_DESIGN ad on ad.ID = cs.ARRAY_DESIGN_FK "
            + "join CURATION_DETAILS CD on CD.ID = ad.CURATION_DETAILS_FK "
            + "where geneprod.GENE_FK = gene.ID and bsgp.GENE_PRODUCT_FK = geneprod.ID and bsgp.BIO_SEQUENCE_FK = cs.BIOLOGICAL_CHARACTERISTIC_FK "
            + "and " + CD_LAST_UPDATED_SINCE
            + "group by gene.ID, cs.ID, cs.ARRAY_DESIGN_FK";

    private static final String GENE2CS_REPOPULATE_BY_ARRAY_DESIGN_QUERY = "select gene.ID, cs.ID, cs.ARRAY_DESIGN_FK "
            + "from CHROMOSOME_FEATURE as gene, CHROMOSOME_FEATURE as geneprod, BIO_SEQUENCE2_GENE_PRODUCT as bsgp, COMPOSITE_SEQUENCE cs "
            + "where geneprod.GENE_FK = gene.ID and bsgp.GENE_PRODUCT_FK = geneprod.ID and bsgp.BIO_SEQUENCE_FK = cs.BIOLOGICAL_CHARACTERISTIC_FK "
            + "and cs.ARRAY_DESIGN_FK = :ad "
            + "group by gene.ID, cs.ID, cs.ARRAY_DESIGN_FK";

    /**
     * Select the bitmask of permissions that applies to the {@code IS_AUTHENTICATED_ANONYMOUSLY} granted authority. If
     * more than one ACL entry are present, they are combined with a bitwise OR.
     * <p>
     * If no ACL entries exist for the anonymous SID, 0 is returned which effectively grants no permission at all.
     */
    private static final String SELECT_ANONYMOUS_MASK =
            "coalesce((select BIT_OR(ACE.MASK) "
                    + "from ACLOBJECTIDENTITY AOI "
                    + "join ACLENTRY ACE on ACE.OBJECTIDENTITY_FK = AOI.ID "
                    + "where AOI.OBJECT_CLASS = 'ubic.gemma.model.expression.experiment.ExpressionExperiment' "
                    + "and AOI.OBJECT_ID = I.ID "
                    + "and ACE.SID_FK = (select ACLSID.ID from ACLSID where ACLSID.GRANTED_AUTHORITY = 'IS_AUTHENTICATED_ANONYMOUSLY') "
                    + "group by AOI.ID), 0)";

    /**
     * Clause for selecting a particular {@link ExpressionExperiment}
     */
    private static final String EE_EQUALS = "(I.ID = :eeId or :eeId is null)";

    private static final String EE2C_EE_QUERY =
            "select C.ID, C.NAME, C.DESCRIPTION, C.CATEGORY, C.CATEGORY_URI, C.`VALUE`, C.VALUE_URI, C.PREDICATE, C.PREDICATE_URI, C.OBJECT, C.OBJECT_URI, C.SECOND_PREDICATE, C.SECOND_PREDICATE_URI, C.SECOND_OBJECT, C.SECOND_OBJECT_URI, C.ORIGINAL_VALUE, C.EVIDENCE_CODE, I.ID, (" + SELECT_ANONYMOUS_MASK + "), 'ubic.gemma.model.expression.experiment.ExpressionExperiment' "
                    + "from INVESTIGATION I "
                    + "join CURATION_DETAILS CD on I.CURATION_DETAILS_FK = CD.ID "
                    + "join CHARACTERISTIC C on I.ID = C.INVESTIGATION_FK "
                    + "where I.class = 'ExpressionExperiment' "
                    + "and " + EE_EQUALS + " "
                    + "and " + CD_LAST_UPDATED_SINCE + " "
                    + "group by I.ID, COALESCE(C.CATEGORY_URI, C.CATEGORY), COALESCE(C.VALUE_URI, C.`VALUE`)";

    private static final String EE2C_BM_QUERY =
            "select C.ID, C.NAME, C.DESCRIPTION, C.CATEGORY, C.CATEGORY_URI, C.`VALUE`, C.VALUE_URI, C.PREDICATE, C.PREDICATE_URI, C.OBJECT, C.OBJECT_URI, C.SECOND_PREDICATE, C.SECOND_PREDICATE_URI, C.SECOND_OBJECT, C.SECOND_OBJECT_URI, C.ORIGINAL_VALUE, C.EVIDENCE_CODE, I.ID, (" + SELECT_ANONYMOUS_MASK + "), 'ubic.gemma.model.expression.biomaterial.BioMaterial' "
                    + "from INVESTIGATION I "
                    + "join CURATION_DETAILS CD on I.CURATION_DETAILS_FK = CD.ID "
                    + "join BIO_ASSAY BA on I.ID = BA.EXPRESSION_EXPERIMENT_FK "
                    + "join BIO_MATERIAL BM on BA.SAMPLE_USED_FK = BM.ID "
                    + "join CHARACTERISTIC C on BM.ID = C.BIO_MATERIAL_FK "
                    + "where I.class = 'ExpressionExperiment' "
                    + "and " + EE_EQUALS + " "
                    + "and " + CD_LAST_UPDATED_SINCE + " "
                    + "group by I.ID, COALESCE(C.CATEGORY_URI, C.CATEGORY), COALESCE(C.VALUE_URI, C.`VALUE`)";

    private static final String EE2C_CTA_QUERY =
            "select C.ID, C.NAME, C.DESCRIPTION, C.CATEGORY, C.CATEGORY_URI, C.`VALUE`, C.VALUE_URI, C.PREDICATE, C.PREDICATE_URI, C.OBJECT, C.OBJECT_URI, C.SECOND_PREDICATE, C.SECOND_PREDICATE_URI, C.SECOND_OBJECT, C.SECOND_OBJECT_URI, C.ORIGINAL_VALUE, C.EVIDENCE_CODE, I.ID, (" + SELECT_ANONYMOUS_MASK + "), 'ubic.gemma.model.expression.bioAssayData.CellTypeAssignment' "
                    + "from INVESTIGATION I "
                    + "join CURATION_DETAILS CD on I.CURATION_DETAILS_FK = CD.ID "
                    + "join BIO_ASSAY BA on I.ID = BA.EXPRESSION_EXPERIMENT_FK "
                    + "join BIO_ASSAYS2SINGLE_CELL_DIMENSIONS B2SCD on BA.ID = B2SCD.BIO_ASSAYS_FK "
                    + "join SINGLE_CELL_DIMENSION SCD on SCD.ID = B2SCD.SINGLE_CELL_DIMENSIONS_FK "
                    + "join ANALYSIS CTA on SCD.ID = CTA.SINGLE_CELL_DIMENSION_FK "
                    + "join CHARACTERISTIC C on CTA.ID = C.CELL_TYPE_ASSIGNMENT_FK "
                    + "where I.class = 'ExpressionExperiment' and CTA.class = 'CellTypeAssignment' "
                    + "and " + EE_EQUALS + " "
                    + "and " + CD_LAST_UPDATED_SINCE + " "
                    + "group by I.ID, COALESCE(C.CATEGORY_URI, C.CATEGORY), COALESCE(C.VALUE_URI, C.`VALUE`)";

    private static final String EE2C_CLC_QUERY =
            "select C.ID, C.NAME, C.DESCRIPTION, C.CATEGORY, C.CATEGORY_URI, C.`VALUE`, C.VALUE_URI, C.PREDICATE, C.PREDICATE_URI, C.OBJECT, C.OBJECT_URI, C.SECOND_PREDICATE, C.SECOND_PREDICATE_URI, C.SECOND_OBJECT, C.SECOND_OBJECT_URI, C.ORIGINAL_VALUE, C.EVIDENCE_CODE, I.ID, (" + SELECT_ANONYMOUS_MASK + "), 'ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics' "
                    + "from INVESTIGATION I "
                    + "join CURATION_DETAILS CD on I.CURATION_DETAILS_FK = CD.ID "
                    + "join BIO_ASSAY BA on I.ID = BA.EXPRESSION_EXPERIMENT_FK "
                    + "join BIO_ASSAYS2SINGLE_CELL_DIMENSIONS B2SCD on BA.ID = B2SCD.BIO_ASSAYS_FK "
                    + "join SINGLE_CELL_DIMENSION SCD on SCD.ID = B2SCD.SINGLE_CELL_DIMENSIONS_FK "
                    + "join CELL_LEVEL_CHARACTERISTICS CLC on SCD.ID = CLC.SINGLE_CELL_DIMENSION_FK "
                    + "join CHARACTERISTIC C on CLC.ID = C.CELL_LEVEL_CHARACTERISTICS_FK "
                    + "where I.class = 'ExpressionExperiment' "
                    + "and " + EE_EQUALS + " "
                    + "and " + CD_LAST_UPDATED_SINCE + " "
                    + "group by I.ID, COALESCE(C.CATEGORY_URI, C.CATEGORY), COALESCE(C.VALUE_URI, C.`VALUE`)";

    /**
     * @deprecated this is deprecated because {@link ExperimentalFactor#getAnnotations()} is also deprecated. However,
     * there's a possibility that this will be repurposed for annotating continuous FVs, see <a href="https://github.com/PavlidisLab/Gemma/issues/950">#950</a>
     * for more details.
     */
    @Deprecated
    private static final String EE2C_ED_FACTOR_ANNOTATIONS_QUERY =
            "select C.ID, C.NAME, C.DESCRIPTION, C.CATEGORY, C.CATEGORY_URI, C.`VALUE`, C.VALUE_URI, C.PREDICATE, C.PREDICATE_URI, C.OBJECT, C.OBJECT_URI, C.SECOND_PREDICATE, C.SECOND_PREDICATE_URI, C.SECOND_OBJECT, C.SECOND_OBJECT_URI, C.ORIGINAL_VALUE, C.EVIDENCE_CODE, I.ID, (" + SELECT_ANONYMOUS_MASK + "), 'ubic.gemma.model.expression.experiment.ExperimentalDesign' "
                    + "from INVESTIGATION I "
                    + "join CURATION_DETAILS CD on I.CURATION_DETAILS_FK = CD.ID "
                    + "join EXPERIMENTAL_DESIGN ED on I.EXPERIMENTAL_DESIGN_FK = ED.ID "
                    + "join EXPERIMENTAL_FACTOR EF on ED.ID = EF.EXPERIMENTAL_DESIGN_FK "
                    + "join CHARACTERISTIC C on C.EXPERIMENTAL_FACTOR_FK = EF.ID "
                    + "where I.class = 'ExpressionExperiment' "
                    + "and " + EE_EQUALS + " "
                    + "and " + CD_LAST_UPDATED_SINCE + " "
                    + "group by I.ID, COALESCE(C.CATEGORY_URI, C.CATEGORY), COALESCE(C.VALUE_URI, C.`VALUE`)";

    private static final String EE2C_ED_FACTOR_VALUE_CHARACTERISTICS_QUERY =
            "select C.ID, C.NAME, C.DESCRIPTION, C.CATEGORY, C.CATEGORY_URI, C.`VALUE`, C.VALUE_URI, C.PREDICATE, C.PREDICATE_URI, C.OBJECT, C.OBJECT_URI, C.SECOND_PREDICATE, C.SECOND_PREDICATE_URI, C.SECOND_OBJECT, C.SECOND_OBJECT_URI, C.ORIGINAL_VALUE, C.EVIDENCE_CODE, I.ID, (" + SELECT_ANONYMOUS_MASK + "), 'ubic.gemma.model.expression.experiment.ExperimentalDesign' "
                    + "from INVESTIGATION I "
                    + "join CURATION_DETAILS CD on I.CURATION_DETAILS_FK = CD.ID "
                    + "join EXPERIMENTAL_DESIGN on I.EXPERIMENTAL_DESIGN_FK = EXPERIMENTAL_DESIGN.ID "
                    + "join EXPERIMENTAL_FACTOR EF on EXPERIMENTAL_DESIGN.ID = EF.EXPERIMENTAL_DESIGN_FK "
                    + "join FACTOR_VALUE FV on FV.EXPERIMENTAL_FACTOR_FK = EF.ID "
                    + "join CHARACTERISTIC C on FV.ID = C.FACTOR_VALUE_FK "
                    + "where I.class = 'ExpressionExperiment' "
                    // remove C.class = 'Statement' once the old-style characteristics are removed (see https://github.com/PavlidisLab/Gemma/issues/929 for details)
                    + "and C.class = 'Statement' "
                    + "and " + EE_EQUALS + " "
                    + "and " + CD_LAST_UPDATED_SINCE + " "
                    + "group by I.ID, COALESCE(C.CATEGORY_URI, C.CATEGORY), COALESCE(C.VALUE_URI, C.`VALUE`) ";

    private static final String EE2C_ED_QUERY = EE2C_ED_FACTOR_ANNOTATIONS_QUERY
            + " union "
            + EE2C_ED_FACTOR_VALUE_CHARACTERISTICS_QUERY;

    private static final String EE2AD_QUERY = "insert into EXPRESSION_EXPERIMENT2ARRAY_DESIGN (EXPRESSION_EXPERIMENT_FK, ARRAY_DESIGN_FK, IS_ORIGINAL_PLATFORM, ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK) "
            + "select I.ID, AD.ID, FALSE, (" + SELECT_ANONYMOUS_MASK + ") from INVESTIGATION I "
            + "join CURATION_DETAILS CD on I.CURATION_DETAILS_FK = CD.ID "
            + "join BIO_ASSAY BA on I.ID = BA.EXPRESSION_EXPERIMENT_FK "
            + "join ARRAY_DESIGN AD on BA.ARRAY_DESIGN_USED_FK = AD.ID "
            + "where I.class = 'ExpressionExperiment' "
            + "and COALESCE(CD.LAST_UPDATED, 0) >= COALESCE(:since, 0) "
            + "group by I.ID, AD.ID "
            + "union "
            + "select I.ID, AD.ID, TRUE, (" + SELECT_ANONYMOUS_MASK + ") from INVESTIGATION I "
            + "join CURATION_DETAILS CD on I.CURATION_DETAILS_FK = CD.ID "
            + "join BIO_ASSAY BA on I.ID = BA.EXPRESSION_EXPERIMENT_FK "
            + "join ARRAY_DESIGN AD on BA.ORIGINAL_PLATFORM_FK = AD.ID "
            + "where I.class = 'ExpressionExperiment' "
            + "and " + EE_EQUALS + " "
            + "and " + CD_LAST_UPDATED_SINCE + " "
            + "group by I.ID, AD.ID "
            + "on duplicate key update ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK = VALUES(ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK)";

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private MailEngine mailEngine;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private SessionFactory sessionFactory;

    @Value("${gemma.gene2cs.path}")
    private Path gene2CsInfoPath;

    private boolean sendEmail = true;

    @Override
    @Transactional
    @Timed
    public int updateGene2CsEntries() {
        return updateGene2CsEntries( null, false, false );
    }

    @Override
    @Transactional
    @Timed
    public int updateGene2CsEntries( ArrayDesign arrayDesign, boolean force ) {
        return updateGene2CsEntries( arrayDesign, null, false, force );
    }

    @Override
    @Transactional
    @Timed
    public int updateGene2CsEntries( @Nullable Date sinceLastUpdate, boolean truncate, boolean force ) {
        return updateGene2CsEntries( null, sinceLastUpdate, truncate, force );
    }

    private int updateGene2CsEntries( @Nullable ArrayDesign arrayDesign, @Nullable Date sinceLastUpdate, boolean truncate, boolean force ) {
        Assert.isTrue( sinceLastUpdate == null || !truncate, "Cannot perform a partial update with sinceLastUpdate with truncate." );
        try {
            String annotation;
            if ( ( annotation = needsToRefreshGene2Cs( force ) ) == null ) {
                TableMaintenanceUtilImpl.log.info( "No update of GENE2CS needed." );
                return 0;
            }
            TableMaintenanceUtilImpl.log.info( "Updating the GENE2CS table..." );
            int updated = this.generateGene2CsEntries( arrayDesign, sinceLastUpdate, truncate );
            String extra = "";
            if ( arrayDesign != null ) {
                extra += " for " + arrayDesign;
            }
            if ( sinceLastUpdate != null ) {
                extra += " since " + sinceLastUpdate;
            }
            if ( updated > 0 ) {
                annotation += "\n\n" + "Updated " + updated + " entries";
                annotation += extra;
                annotation += ".";
            }
            TableMaintenanceUtilImpl.log.info( String.format( "Done regenerating the GENE2CS table%s; %d entries were updated.", extra, updated ) );
            Gene2CsStatus updatedStatus;
            updatedStatus = createUpdateStatus( annotation, null );
            updateGene2csExternalDatabaseLastUpdated( updatedStatus );
            writeGene2CsUpdateStatusToDisk( updatedStatus );
            sendGene2CsUpdateStatusAdminEmail( updatedStatus );
            return updated;
        } catch ( Exception e ) {
            Gene2CsStatus updatedStatus;
            updatedStatus = createUpdateStatus( "An error occurred while attempting to update the GENE2CS table.", e );
            writeGene2CsUpdateStatusToDisk( updatedStatus );
            sendGene2CsUpdateStatusAdminEmail( updatedStatus );
            throw e;
        }
    }

    @Override
    @Timed
    @Transactional
    public int updateExpressionExperiment2CharacteristicEntries( @Nullable Date sinceLastUpdate, boolean truncate ) {
        return updateExpressionExperiment2CharacteristicEntries( null, null, sinceLastUpdate, truncate );
    }

    @Override
    @Timed
    @Transactional
    public int updateExpressionExperiment2CharacteristicEntries( Class<?> level, @Nullable Date sinceLastUpdate, boolean truncate ) {
        return updateExpressionExperiment2CharacteristicEntries( null, level, sinceLastUpdate, truncate );
    }

    @Override
    @Timed
    @Transactional
    public int updateExpressionExperiment2CharacteristicEntries( ExpressionExperiment ee, @Nullable Class<?> level ) {
        return updateExpressionExperiment2CharacteristicEntries( ee, level, null, false );
    }

    private int updateExpressionExperiment2CharacteristicEntries( @Nullable ExpressionExperiment ee, @Nullable Class<?> level, @Nullable Date sinceLastUpdate, boolean truncate ) {
        Assert.isTrue( sinceLastUpdate == null || !truncate, "Cannot perform a partial update with sinceLastUpdate with truncate." );
        StopWatch timer = StopWatch.createStarted();
        String query;
        if ( level == null ) {
            query = EE2C_EE_QUERY
                    + " union "
                    + EE2C_BM_QUERY
                    + " union "
                    + EE2C_CTA_QUERY
                    + " union "
                    + EE2C_CLC_QUERY
                    + " union "
                    + EE2C_ED_QUERY;
        } else if ( level.equals( ExpressionExperiment.class ) ) {
            query = EE2C_EE_QUERY;
        } else if ( level.equals( BioMaterial.class ) ) {
            query = EE2C_BM_QUERY;
        } else if ( level.equals( ExperimentalDesign.class ) ) {
            query = EE2C_ED_QUERY;
        } else if ( level.equals( CellTypeAssignment.class ) ) {
            query = EE2C_CTA_QUERY;
        } else if ( level.equals( CellLevelCharacteristics.class ) ) {
            query = EE2C_CLC_QUERY;
        } else {
            throw new IllegalArgumentException( "Level must be one of ExpressionExperiment, BioMaterial, ExperimentalDesign, CellTypeAssignment or CellLevelCharacteristics." );
        }
        String what = String.format( "%s%s%s",
                level != null ? " at " + level.getSimpleName() + " level" : "",
                ee != null ? " for " + ee : "",
                sinceLastUpdate != null ? " since " + sinceLastUpdate : "" );
        log.info( String.format( "Updating the EXPRESSION_EXPERIMENT2CHARACTERISTIC table%s...", what ) );
        if ( truncate ) {
            log.info( "Truncating EXPRESSION_EXPERIMENT2CHARACTERISTIC" + what + "..." );
            sessionFactory.getCurrentSession()
                    .createSQLQuery( "delete from EXPRESSION_EXPERIMENT2CHARACTERISTIC where LEVEL = :level" )
                    .addSynchronizedQuerySpace( EE2C_QUERY_SPACE )
                    .setParameter( "level", level )
                    .executeUpdate();
        }
        int updated = sessionFactory.getCurrentSession()
                .createSQLQuery(
                        "insert into EXPRESSION_EXPERIMENT2CHARACTERISTIC (ID, NAME, DESCRIPTION, CATEGORY, CATEGORY_URI, `VALUE`, VALUE_URI, PREDICATE, PREDICATE_URI, OBJECT, OBJECT_URI, SECOND_PREDICATE, SECOND_PREDICATE_URI, SECOND_OBJECT, SECOND_OBJECT_URI, ORIGINAL_VALUE, EVIDENCE_CODE, EXPRESSION_EXPERIMENT_FK, ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK, LEVEL) "
                                + query + " "
                                + "on duplicate key update NAME = VALUES(NAME), DESCRIPTION = VALUES(DESCRIPTION), "
                                + "CATEGORY = VALUES(CATEGORY), CATEGORY_URI = VALUES(CATEGORY_URI), "
                                + "`VALUE` = VALUES(`VALUE`), VALUE_URI = VALUES(VALUE_URI), "
                                + "PREDICATE = VALUES(PREDICATE), PREDICATE_URI = VALUES(PREDICATE_URI), "
                                + "OBJECT = VALUES(OBJECT), OBJECT_URI = VALUES(OBJECT_URI), "
                                + "SECOND_PREDICATE = VALUES(SECOND_PREDICATE), SECOND_PREDICATE_URI = VALUES(SECOND_PREDICATE_URI), "
                                + "SECOND_OBJECT = VALUES(SECOND_OBJECT), SECOND_OBJECT_URI = VALUES(SECOND_OBJECT_URI), "
                                + "ORIGINAL_VALUE = VALUES(ORIGINAL_VALUE), EVIDENCE_CODE = VALUES(EVIDENCE_CODE), "
                                + "ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK = VALUES(ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK), LEVEL = VALUES(LEVEL)" )
                .addSynchronizedQuerySpace( EE2C_QUERY_SPACE )
                .setParameter( "eeId", ee != null ? ee.getId() : null )
                .setParameter( "since", sinceLastUpdate )
                .executeUpdate();
        log.info( String.format( "Done updating the EXPRESSION_EXPERIMENT2CHARACTERISTIC table%s; %d entries were updated in %d ms.",
                what, updated, timer.getTime() ) );
        return updated;
    }

    @Override
    @Timed
    @Transactional
    public int updateExpressionExperiment2ArrayDesignEntries( @Nullable Date sinceLastUpdate, boolean truncate ) {
        StopWatch timer = StopWatch.createStarted();
        log.info( String.format( "Updating the EXPRESSION_EXPERIMENT2ARRAY_DESIGN table%s...",
                sinceLastUpdate != null ? " since " + sinceLastUpdate : "" ) );
        if ( truncate ) {
            log.info( "Truncating EXPRESSION_EXPERIMENT2ARRAY_DESIGN..." );
            sessionFactory.getCurrentSession()
                    .createSQLQuery( "delete from EXPRESSION_EXPERIMENT2ARRAY_DESIGN" )
                    .addScalar( EE2AD_QUERY_SPACE )
                    .executeUpdate();
        }
        int updated = sessionFactory.getCurrentSession()
                .createSQLQuery( EE2AD_QUERY )
                .addSynchronizedQuerySpace( EE2AD_QUERY_SPACE )
                .setParameter( "eeId", null )
                .setParameter( "since", sinceLastUpdate )
                .executeUpdate();
        log.info( String.format( "Done updating the EXPRESSION_EXPERIMENT2ARRAY_DESIGN table; %d entries were updated%s in %d ms.",
                updated, sinceLastUpdate != null ? " since " + sinceLastUpdate : "", timer.getTime() ) );
        return updated;
    }

    @Override
    @Timed
    @Transactional
    public int updateExpressionExperiment2ArrayDesignEntries( ExpressionExperiment ee ) {
        StopWatch timer = StopWatch.createStarted();
        int updated = sessionFactory.getCurrentSession()
                .createSQLQuery( EE2AD_QUERY )
                .addSynchronizedQuerySpace( EE2AD_QUERY_SPACE )
                .setParameter( "eeId", ee.getId() )
                .setParameter( "since", null )
                .executeUpdate();
        log.info( String.format( "Done updating the EXPRESSION_EXPERIMENT2ARRAY_DESIGN table for %s; %d entries were updated in %d ms.",
                ee, updated, timer.getTime() ) );
        return updated;
    }

    @Override
    public void evictGene2CsQueryCache() {
        sessionFactory.getCache().evictQueryRegion( GENE2CS_QUERY_SPACE );
    }

    @Override
    public void evictEe2CQueryCache() {
        sessionFactory.getCache().evictQueryRegion( EE2C_QUERY_SPACE );
    }

    @Override
    public void evictEe2AdQueryCache() {
        sessionFactory.getCache().evictQueryRegion( EE2AD_QUERY_SPACE );
    }

    /**
     * For use in tests.
     */
    @Override
    public void disableEmail() {
        this.sendEmail = false;
    }

    /**
     * Check if the GENE2CS table needs to be updated.
     * @param force force-update the GENE2CS table
     * @return the reason for updating, or null not to update
     */
    private String needsToRefreshGene2Cs( boolean force ) {
        if ( force ) {
            return "Force-updating the GENE2CS table.";
        }

        TableMaintenanceUtilImpl.log.info( "Running Gene2CS status check..." );
        Gene2CsStatus status = this.getLastGene2CsUpdateStatus();
        if ( status == null ) {
            return "No Gene2Cs status exists on disk.";
        }

        // check if the last attempt failed, in ths case it will be retried
        if ( status.getError() != null ) {
            return "Last GENE2CS update attempt failed, retrying...";
        }

        // check if new platforms have been added
        Collection<ArrayDesign> newObj = auditEventService.getNewSinceDate( ArrayDesign.class, status.getLastUpdate() );
        for ( ArrayDesign a : newObj ) {
            String annotation = a + " is new since " + status.getLastUpdate();
            TableMaintenanceUtilImpl.log.debug( annotation );
            return annotation;
        }

        // check if any platform has had gene mapping update since the last GENE2CS update
        Map<ArrayDesign, AuditEvent> updatedObj = auditEventService.getLastEvents( ArrayDesign.class, ArrayDesignGeneMappingEvent.class );
        for ( ArrayDesign a : updatedObj.keySet() ) {
            AuditEvent ae = updatedObj.get( a );
            // not be needed any more
            if ( ae.getDate().after( status.getLastUpdate() ) ) {
                String annotation = a + " had probe mapping done since: " + status.getLastUpdate();
                TableMaintenanceUtilImpl.log.debug( annotation );
                return annotation;
            }
        }

        return null;
    }

    /**
     * Function to regenerate the GENE2CS entries. Gene2Cs is a denormalized join table that allows for a quick link
     * between Genes and CompositeSequences
     *
     * @see GeneDao for where the GENE2CS table is used extensively.
     */
    private int generateGene2CsEntries( @Nullable ArrayDesign arrayDesign, @Nullable Date sinceLastUpdate, boolean truncate ) {
        StopWatch timer = StopWatch.createStarted();
        if ( truncate ) {
            if ( arrayDesign != null ) {
                TableMaintenanceUtilImpl.log.info( "Truncating GENE2CS for " + arrayDesign + "..." );
                sessionFactory.getCurrentSession()
                        .createSQLQuery( "delete from GENE2CS g2s where g2s.AD = :adId" )
                        .addSynchronizedQuerySpace( GENE2CS_QUERY_SPACE )
                        .setParameter( "adId", arrayDesign.getId() )
                        .executeUpdate();
            } else {
                TableMaintenanceUtilImpl.log.info( "Truncating GENE2CS..." );
                sessionFactory.getCurrentSession()
                        .createSQLQuery( "delete from GENE2CS" )
                        .addSynchronizedQuerySpace( GENE2CS_QUERY_SPACE )
                        .executeUpdate();
            }
        }
        TableMaintenanceUtilImpl.log.info( "Updating the GENE2CS table..." );
        String query;
        if ( arrayDesign != null ) {
            query = TableMaintenanceUtilImpl.GENE2CS_REPOPULATE_BY_ARRAY_DESIGN_QUERY;
        } else if ( sinceLastUpdate != null ) {
            query = TableMaintenanceUtilImpl.GENE2CS_REPOPULATE_SINCE_LAST_UPDATE_QUERY;
        } else {
            query = TableMaintenanceUtilImpl.GENE2CS_REPOPULATE_QUERY;
        }
        Query queryObject = this.sessionFactory.getCurrentSession()
                .createSQLQuery( "insert into GENE2CS (GENE, CS, AD) "
                        + query + " "
                        // duplicate keys should never happen, so this is a no-op
                        + "on duplicate key update GENE = GENE, CS = CS, AD = AD" )
                .addSynchronizedQuerySpace( GENE2CS_QUERY_SPACE );
        if ( arrayDesign != null ) {
            queryObject.setParameter( "ad", arrayDesign.getId() );
        }
        if ( sinceLastUpdate != null ) {
            queryObject.setParameter( "since", sinceLastUpdate );
        }
        int updated = queryObject.executeUpdate();
        TableMaintenanceUtilImpl.log.info( String.format( "Done regenerating the GENE2CS table; %d entries were updated in %d ms.", updated, timer.getTime() ) );
        return updated;
    }

    /**
     * Reads previous run information from disk.
     *
     * @return null if there is no update information available.
     */
    @Nullable
    private Gene2CsStatus getLastGene2CsUpdateStatus() {
        try ( ObjectInputStream ois = new ObjectInputStream( Files.newInputStream( gene2CsInfoPath ) ) ) {
            return ( Gene2CsStatus ) ois.readObject();
        } catch ( NoSuchFileException e ) {
            return null;
        } catch ( IOException | ClassNotFoundException e ) {
            throw new RuntimeException( "Failed to obtain last gene2cs update status.", e );
        }
    }

    private Gene2CsStatus createUpdateStatus( String annotation, @Nullable Exception e ) {
        Gene2CsStatus status = new Gene2CsStatus();
        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        status.setLastUpdate( date );
        status.setError( e );
        status.setAnnotation( annotation );
        return status;
    }

    /**
     * Update the last updated date of the GENE2CS {@link ExternalDatabase}.
     */
    private void updateGene2csExternalDatabaseLastUpdated( Gene2CsStatus status ) {
        ExternalDatabase ed = externalDatabaseService.findByNameWithAuditTrail( ExternalDatabases.GENE2CS );
        if ( ed == null ) {
            log.error( String.format( "External database with name %s is missing, no audit event will be recorded.", ExternalDatabases.GENE2CS ) );
            return;
        }
        externalDatabaseService.updateReleaseLastUpdated( ed, status.getAnnotation(), status.getLastUpdate() );
    }

    /**
     * Write a GENE2CS update status to disk.
     */
    private void writeGene2CsUpdateStatusToDisk( Gene2CsStatus status ) {
        try {
            PathUtils.createParentDirectories( gene2CsInfoPath );
            try ( ObjectOutputStream oos = new ObjectOutputStream( Files.newOutputStream( gene2CsInfoPath ) ) ) {
                oos.writeObject( status );
            }
        } catch ( IOException e2 ) {
            log.error( "Failed to update gene2cs update status.", e2 );
            // not rethrowing, or else the update itself would be rolled back
        }
    }

    /**
     * Send an email to the admin with the status of the GENE2CS update.
     */
    private void sendGene2CsUpdateStatusAdminEmail( Gene2CsStatus updatedStatus ) {
        if ( !sendEmail ) {
            return;
        }
        try {
            mailEngine.sendMessageToAdmin( "Gene2Cs update status.", "Gene2Cs updating was run.\n" + updatedStatus.getAnnotation() );
        } catch ( Exception e ) {
            log.error( "Failed to send email about Gene2Cs update status.", e );
            // not rethrowing, or else the update itself would be rolled back
        }
    }
}
