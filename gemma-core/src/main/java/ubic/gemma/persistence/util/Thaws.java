package ubic.gemma.persistence.util;

import org.hibernate.Hibernate;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.FactorValue;

import static ubic.gemma.persistence.service.expression.biomaterial.BioMaterialUtils.visitBioMaterials;

/**
 * Consistent initialization logic for the entity graph.
 */
public class Thaws {

    public static void thawDatabaseEntry( DatabaseEntry databaseEntry ) {
        Hibernate.initialize( databaseEntry );
        Hibernate.initialize( databaseEntry.getExternalDatabase() );
    }

    /**
     * Thaw the {@link CurationDetails} of a curatable entity, <em>including</em> the three
     * {@code last*Event} associations hanging off it.
     * <p>
     * Initializing the {@code CurationDetails} on its own is not enough: {@code lastTroubledEvent},
     * {@code lastNeedsAttentionEvent} and {@code lastNoteUpdateEvent} are separately lazy
     * {@link ubic.gemma.model.common.auditAndSecurity.AuditEvent} references, and every curatable
     * value object reads all three in its constructor (see
     * {@code AbstractCuratableValueObject} and {@code CurationDetailsValueObject}).
     * <p>
     * A REST method that thaws through one {@code @Transactional} service call and then builds the
     * value object through another one spans two sessions, so whatever the thaw left as a proxy is
     * dead by the time the VO is built. That is what made {@code GET /datasets/{id}/refresh} answer
     * {@code Could not initialize proxy [AuditEvent#…] - no session} on every call, which in turn
     * meant the post-write cache eviction the CLI performs never ran.
     */
    public static void thawCurationDetails( Curatable curatable ) {
        CurationDetails curationDetails = curatable.getCurationDetails();
        Hibernate.initialize( curationDetails );
        if ( curationDetails != null ) {
            Hibernate.initialize( curationDetails.getLastTroubledEvent() );
            Hibernate.initialize( curationDetails.getLastNeedsAttentionEvent() );
            Hibernate.initialize( curationDetails.getLastNoteUpdateEvent() );
        }
    }

    public static void thawBibliographicReference( BibliographicReference br ) {
        if ( br.getPubAccession() != null ) {
            thawDatabaseEntry( br.getPubAccession() );
        }
        Hibernate.initialize( br.getMeshTerms() );
        Hibernate.initialize( br.getKeywords() );
        Hibernate.initialize( br.getChemicals() );
    }

    public static void thawBioAssayDimension( BioAssayDimension bioAssayDimension ) {
        bioAssayDimension.getBioAssays().forEach( Thaws::thawBioAssay );
    }

    /**
     * Thaw the given BioAssay.
     * <p>
     * The corresponding biomaterial is also thawed with {@link #thawBioMaterial(BioMaterial)}.
     */
    public static void thawBioAssay( BioAssay ba ) {
        thawBioAssayPlatforms( ba );
        // also initialize the other side of the relationship since we're thawing assays
        thawBioMaterial( ba.getSampleUsed(), true );
    }

    /**
     * Thaw the platform-side associations of a {@link BioAssay} without walking its
     * {@code sampleUsed} {@link BioMaterial} chain.
     * <p>
     * Used by callers that batch-thaw the BioMaterial side via
     * {@code BioMaterialDao#thawBioMaterialsForBioAssays}, where the per-BA
     * source-chain walk in {@link #thawBioAssay(BioAssay)} would re-introduce the N+1
     * pattern the batched call exists to eliminate. The platform-side init remains a
     * cheap proxy/eager touch (one or two round-trips per BA at most).
     */
    public static void thawBioAssayPlatforms( BioAssay ba ) {
        ArrayDesign arrayDesignUsed = ba.getArrayDesignUsed();
        Hibernate.initialize( arrayDesignUsed );
        Hibernate.initialize( arrayDesignUsed.getDesignProvider() );
        ArrayDesign originalPlatform = ba.getOriginalPlatform();
        if ( originalPlatform != null ) {
            Hibernate.initialize( originalPlatform );
            Hibernate.initialize( originalPlatform.getDesignProvider() );
        }
    }

    /**
     * Thaw the given BioMaterial.
     * <p>
     * The following fields are initialized: sourceTaxon, treatments and factorValues.experimentalFactor.
     * <p>
     * If the bioMaterial has a sourceBioMaterial, it is thawed as well, recursively. Circular references are detected
     * and will result in a {@link IllegalStateException}.
     */
    public static void thawBioMaterial( BioMaterial bm2 ) {
        thawBioMaterial( bm2, false );
    }

    private static void thawBioMaterial( BioMaterial bm2, boolean initializeBioAssaysUsedIn ) {
        visitBioMaterials( bm2, bm -> {
            Hibernate.initialize( bm.getSourceTaxon() );
            Hibernate.initialize( bm.getTreatments() );
            for ( FactorValue fv : bm.getFactorValues() ) {
                Hibernate.initialize( fv.getExperimentalFactor() );
            }
            if ( initializeBioAssaysUsedIn ) {
                Hibernate.initialize( bm.getBioAssaysUsedIn() );
            }
        } );
    }

    /**
     * Thaw a single-cell dimension.
     */
    public static void thawSingleCellDimension( SingleCellDimension singleCellDimension ) {
        singleCellDimension.getCellTypeAssignments().forEach( Thaws::thawCellTypeAssignment );
        Hibernate.initialize( singleCellDimension.getCellLevelCharacteristics() );
    }

    public static void thawCellTypeAssignment( CellTypeAssignment cellTypeAssignment ) {
        Hibernate.initialize( cellTypeAssignment.getProtocol() );
    }
}
