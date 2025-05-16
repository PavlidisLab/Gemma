package ubic.gemma.model.expression.bioAssay;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialUtils;

import java.util.*;
import java.util.stream.Collectors;

@CommonsLog
public class BioAssayUtils {

    /**
     * Create a mapping between assays and their source assays from a given {@link BioAssaySet}.
     * <p>
     * If an assay has more than one corresponding source assay or no source assay at all, it is ignored and logged as a
     * warning.
     */
    public static Map<BioAssay, BioAssay> createBioAssayToSourceBioAssayMap( BioAssaySet sourceAssaySet, Collection<BioAssay> bas ) {
        Map<BioAssay, BioAssay> assay2sourceAssayMap = new HashMap<>();
        for ( BioAssay ba : bas ) {
            if ( ba.getSampleUsed().getSourceBioMaterial() == null ) {
                log.warn( ba + " does not have a source assay in " + sourceAssaySet + "." );
                continue;
            }

            // collect all BMs in the hierarchy
            Set<BioMaterial> sourceBms = new HashSet<>();
            BioMaterialUtils.visitBioMaterials( ba.getSampleUsed(), sourceBms::add );

            // only retain assays that are in the source assay set
            Set<BioAssay> sourceAssays = sourceBms.stream()
                    .flatMap( bm -> bm.getBioAssaysUsedIn().stream() )
                    .filter( sourceAssaySet.getBioAssays()::contains )
                    .collect( Collectors.toSet() );

            if ( sourceAssays.size() == 1 ) {
                assay2sourceAssayMap.put( ba, sourceAssays.iterator().next() );
            } else if ( sourceAssays.isEmpty() ) {
                log.warn( ba + " does not have a source assay in " + sourceAssaySet + "." );
            } else {
                log.warn( ba + " has more than one source assay in " + sourceAssaySet + "." );
            }
        }
        return assay2sourceAssayMap;
    }
}
