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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.analysis.expression.diff.BaselineSelection;
import ubic.gemma.core.mail.MailEngine;
import ubic.gemma.core.ontology.relation.OntologyRelationProducer;
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

import org.springframework.lang.Nullable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
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
@Slf4j
public class TableMaintenanceUtilImpl implements TableMaintenanceUtil {

    /**
     * Clause for selecting entities updated since a given date.
     */
    private static final String CD_LAST_UPDATED_SINCE = "(CD.LAST_UPDATED is null or :since is null or CD.LAST_UPDATED >= :since)";

    private static final java.io.ObjectInputFilter GENE2CS_DESERIALIZATION_FILTER = java.io.ObjectInputFilter.Config.createFilter(
            "ubic.gemma.**;java.util.**;java.lang.**;java.time.**;java.math.**;java.sql.**;!*" );

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
    // Targets Spring Security 6's canonical four-table ACL schema. The AOI's class name lives in
    // acl_class.class (joined through acl_object_identity.object_id_class), and acl_sid uses a
    // principal=0/1 discriminator with the SID name in the `sid` column.
    private static final String SELECT_ANONYMOUS_MASK =
            "coalesce((select BIT_OR(ACE.mask) "
                    + "from acl_object_identity AOI "
                    + "join acl_class AOC on AOC.id = AOI.object_id_class "
                    + "join acl_entry ACE on ACE.acl_object_identity = AOI.id "
                    + "where AOC.class = 'ubic.gemma.model.expression.experiment.ExpressionExperiment' "
                    + "and AOI.object_id_identity = I.ID "
                    + "and ACE.sid = (select acl_sid.id from acl_sid where acl_sid.principal = 0 and acl_sid.sid = 'IS_AUTHENTICATED_ANONYMOUSLY') "
                    + "group by AOI.id), 0)";

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
                    .createNativeQuery( "delete from EXPRESSION_EXPERIMENT2CHARACTERISTIC where LEVEL = :level" )
                    .addSynchronizedQuerySpace( EE2C_QUERY_SPACE )
                    .setParameter( "level", level )
                    .executeUpdate();
        }
        int updated = sessionFactory.getCurrentSession()
                .createNativeQuery(
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
                    .createNativeQuery( "delete from EXPRESSION_EXPERIMENT2ARRAY_DESIGN" )
                    .addSynchronizedQuerySpace( EE2AD_QUERY_SPACE )
                    .executeUpdate();
        }
        int updated = sessionFactory.getCurrentSession()
                .createNativeQuery( EE2AD_QUERY )
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
                .createNativeQuery( EE2AD_QUERY )
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

    /**
     * Query space for {@code ANNOTATION_RELATION}, so reads over it are invalidated when a rebuild
     * writes to it.
     */
    private static final String AR_QUERY_SPACE = "ANNOTATION_RELATION";

    /**
     * Removes the CURATED rows. 🛑 <b>It no longer writes any.</b>
     *
     * <p>The harvest turned every EE2C row carrying a predicate and an object into a triple keyed by the
     * TERM. A statement a curator wrote about ONE experiment's material therefore came back as a property
     * of that term and landed on every other experiment using it. uib found it on experiment 24976 -- mouse
     * EAE astrocytes under fingolimod -- which rendered three confident chips, none of them true of it:
     * {@code astrocyte --derives from part of--> organoid} from ee 31771,
     * {@code astrocyte --has role--> cell co-culturing} from ee 32195, and {@code nuclear RNA extract
     * --derived from cell--> vasoactive intestinal peptide secreting cell} from ee 32525.</p>
     *
     * <p>🛑 <b>Filtering does not rescue the harvest, and this was measured rather than assumed.</b> 15,007
     * of ~18,000 curated triples on prod were attested by a single experiment. The ones that recurred were
     * the common curation PATTERNS rather than truths -- {@code Trp53 --has_genotype--> Homozygous negative}
     * across 133 experiments, though Trp53 is also overexpressed and heterozygous elsewhere. Adding both
     * breadth bars on top left a set that still read mostly experiment-local: {@code diabetes mellitus
     * --induced by--> streptozocin} holds for STZ models rather than for diabetes, {@code Apoe --has_allele
     * --> APOE4} is simply wrong because Apoe also carries E2 and E3, and {@code induced by} records how one
     * model was made. Paul, 2026-08-28, having read them: <i>"most of these are not good … let's just delete
     * all the curated records. We'll just make sure the ones we add are high quality."</i></p>
     *
     * <p>The delete is kept, and is what removes the rows already in the table. Curated relations are not
     * gone as a concept -- what is gone is deriving them wholesale from annotations that were never
     * assertions about a term. Anything added back has to be curated as a relation in its own right.</p>
     *
     * @return always 0; nothing is written. The count of rows REMOVED goes to the log.
     */
    @Override
    @Timed
    @Transactional
    public int updateAnnotationRelationEntries( @Nullable ExpressionExperiment ee ) {
        StopWatch timer = StopWatch.createStarted();
        String what = ee != null ? " for " + ee : "";
        log.info( String.format( "Updating CURATED ANNOTATION_RELATION entries%s...", what ) );

        // Delete first, then insert. An upsert can only correct rows the new query still produces, so a
        // row whose statement a curator has since deleted would outlive the annotation it came from --
        // the failure mode that left 1,008 uncorrectable rows in EE2C.
        NativeQuery<?> delete = sessionFactory.getCurrentSession()
                .createNativeQuery( "delete from ANNOTATION_RELATION where BASIS = 'CURATED'"
                        + ( ee != null ? " and EXPRESSION_EXPERIMENT_FK = :eeId" : "" ) )
                .addSynchronizedQuerySpace( AR_QUERY_SPACE );
        if ( ee != null ) {
            delete.setParameter( "eeId", ee.getId() );
        }
        int removed = delete.executeUpdate();

        log.info( String.format( "Done removing CURATED ANNOTATION_RELATION entries%s; %d removed in %d ms.",
                what, removed, timer.getTime() ) );
        return 0;
    }

    /**
     * Absent from ontology-free contexts (tests, and any deployment with the ontologies switched off),
     * which is why this is optional rather than required: {@code TableMaintenanceUtil} must still start.
     */
    @Autowired(required = false)
    private OntologyRelationProducer ontologyRelationProducer;

    /**
     * Optional for the same reason as the ontology producer: a context without ontologies must still
     * start, and this one additionally needs MONDO loaded to translate MGI's DOIDs.
     */
    @Autowired(required = false)
    private ubic.gemma.core.ontology.relation.MgiRelationProducer mgiRelationProducer;

    @Autowired(required = false)
    private ubic.gemma.core.ontology.relation.CellosaurusRelationProducer cellosaurusRelationProducer;

    /**
     * Not {@code @Transactional}: the producer spends minutes walking Jena models before it has a row to
     * write, and its own transaction wraps only the delete-and-insert. Holding a connection open across
     * the read would be a maintenance job contending with the application for no reason.
     */
    @Override
    @Timed
    public int updateOntologyRelationEntries( @Nullable Collection<String> sources ) {
        if ( ontologyRelationProducer == null ) {
            log.warn( "No ontology relation producer is wired; ONTOLOGY ANNOTATION_RELATION entries are not updated." );
            return 0;
        }
        StopWatch timer = StopWatch.createStarted();
        String what = sources != null && !sources.isEmpty() ? " for " + sources : "";
        log.info( String.format( "Updating ONTOLOGY ANNOTATION_RELATION entries%s...", what ) );
        int written = ontologyRelationProducer.produce( sources );
        evictAnnotationRelationQueryCache();
        log.info( String.format( "Done updating ONTOLOGY ANNOTATION_RELATION entries%s; %d written in %d ms.",
                what, written, timer.getTime() ) );
        return written;
    }

    /**
     * Not {@code @Transactional}, for the same reason as the ontology pass: the fetch and the parse
     * happen before there is a row to write, and the producer's own transaction wraps only the
     * delete-and-insert.
     */
    @Override
    @Timed
    public int updateExternalRelationEntries() {
        if ( mgiRelationProducer == null ) {
            log.warn( "No MGI relation producer is wired; EXTERNAL ANNOTATION_RELATION entries are not updated." );
            return 0;
        }
        StopWatch timer = StopWatch.createStarted();
        log.info( "Updating EXTERNAL ANNOTATION_RELATION entries..." );
        int written = 0;
        // 🛑 Each source stands or falls alone. Both deletes are scoped to their own SOURCE, so one
        // failing download must not cost the other its rows -- and a failure leaves the existing rows
        // in place rather than emptying them, since rebuilding from nothing is indistinguishable from
        // the source having retracted everything it ever said.
        List<String> failed = new ArrayList<>();
        try {
            written += mgiRelationProducer.produce();
        } catch ( java.io.IOException e ) {
            log.error( "Could not read MGI's reports; its EXTERNAL relation rows are left as they are.", e );
            failed.add( "MGI: " + e.getMessage() );
        }
        if ( cellosaurusRelationProducer != null ) {
            try {
                written += cellosaurusRelationProducer.produce();
            } catch ( java.io.IOException e ) {
                log.error( "Could not read Cellosaurus; its EXTERNAL relation rows are left as they are.", e );
                failed.add( "Cellosaurus: " + e.getMessage() );
            }
        }
        evictAnnotationRelationQueryCache();
        log.info( String.format( "Done updating EXTERNAL ANNOTATION_RELATION entries; %d written in %d ms.",
                written, timer.getTime() ) );
        // 🛑 Both sources are attempted before this throws, which is the point -- isolation is about
        // one source not costing the other its rows, and it says nothing about what the CALLER should
        // be told. On 2026-08-18 MGI failed on a read-only cache path and the command logged
        // "Wrote 243212 EXTERNAL relation rows" and exited 0. Half the job had not run, and the only
        // way to find out was to go and count the table.
        if ( !failed.isEmpty() ) {
            throw new IllegalStateException( "EXTERNAL relation update finished with "
                    + failed.size() + " of its sources failing, and their existing rows untouched: "
                    + String.join( "; ", failed ) + ". " + written + " rows were written by the rest." );
        }
        return written;
    }

    @Override
    public void evictAnnotationRelationQueryCache() {
        sessionFactory.getCache().evictQueryRegion( AR_QUERY_SPACE );
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
    @Nullable
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
        for ( Map.Entry<ArrayDesign, AuditEvent> uoEntry : updatedObj.entrySet() ) {
            ArrayDesign a = uoEntry.getKey();
            AuditEvent ae = uoEntry.getValue();
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
                        .createNativeQuery( "delete from GENE2CS g2s where g2s.AD = :adId" )
                        .addSynchronizedQuerySpace( GENE2CS_QUERY_SPACE )
                        .setParameter( "adId", arrayDesign.getId() )
                        .executeUpdate();
            } else {
                TableMaintenanceUtilImpl.log.info( "Truncating GENE2CS..." );
                sessionFactory.getCurrentSession()
                        .createNativeQuery( "delete from GENE2CS" )
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
                .createNativeQuery( "insert into GENE2CS (GENE, CS, AD) "
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
            ois.setObjectInputFilter( GENE2CS_DESERIALIZATION_FILTER );
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
