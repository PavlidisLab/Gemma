package ubic.gemma.persistence.service.expression.experiment;

import org.apache.commons.lang3.RandomStringUtils;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

public class RandomExpressionExperimentUtils {

    /**
     * Create a random {@link ExpressionExperiment} with a desired number of assays.
     *
     * @param numAssays   number of assays to create (one sample per assay will also be created)
     * @param arrayDesign platform to use for the assays
     */
    public static ExpressionExperiment randomExpressionExperiment( Taxon taxon, int numAssays, ArrayDesign arrayDesign ) {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( RandomStringUtils.insecure().nextAlphanumeric( 10 ) );
        ee.setName( RandomStringUtils.insecure().nextAlphanumeric( 10 ) );
        ee.setTaxon( taxon );
        for ( int i = 0; i < numAssays; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance();
            bm.setName( "bm" + ( i + 1 ) );
            bm.setSourceTaxon( taxon );
            BioAssay ba = BioAssay.Factory.newInstance();
            ba.setName( "ba" + ( i + 1 ) );
            ba.setSampleUsed( bm );
            ba.setArrayDesignUsed( arrayDesign );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }
        ee.setNumberOfSamples( numAssays );
        return ee;
    }
}
