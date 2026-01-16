package ubic.gemma.persistence.util;

import org.hibernate.Hibernate;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
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
        for ( DatabaseEntry de : ba.getOtherAccessions() ) {
            Thaws.thawDatabaseEntry( de );
        }
        Hibernate.initialize( ba.getArrayDesignUsed() );
        Hibernate.initialize( ba.getArrayDesignUsed().getDesignProvider() );
        if ( ba.getOriginalPlatform() != null ) {
            Hibernate.initialize( ba.getOriginalPlatform() );
            Hibernate.initialize( ba.getOriginalPlatform().getDesignProvider() );
        }
        // also initialize the other side of the relationship since we're thawing assays
        thawBioMaterial( ba.getSampleUsed(), true );
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
            for ( DatabaseEntry de : bm.getOtherExternalAccessions() ) {
                Thaws.thawDatabaseEntry( de );
            }
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
