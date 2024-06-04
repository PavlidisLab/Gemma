package ubic.gemma.core.loader.expression.arrayDesign;

import ubic.gemma.core.analysis.sequence.Blat;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

import java.util.Collection;

public interface ArrayDesignSequenceAlignmentService {

    /**
     * Run blat on all sequences on the array design. For arrays with sequences from multiple taxa, BLAT is run
     * appropriately assuming sequences are available for all the represented taxa.
     *
     * @param ad        ad
     * @param sensitive if true, blat will be run in a more sensitive mode, if available.
     * @return blat results
     */
    Collection<BlatResult> processArrayDesign( ArrayDesign ad, boolean sensitive );

    /**
     * @param ad             ad
     * @param taxon          (to allow for the possibility of multiple taxa for the array) - if not given, attempt to infer from
     *                       ad.
     * @param rawBlatResults , assumed to be from alignments to correct taxon Typically these would have been read in
     *                       from a file.
     * @return persisted BlatResults.
     */
    Collection<BlatResult> processArrayDesign( ArrayDesign ad, Taxon taxon, Collection<BlatResult> rawBlatResults );

    /**
     * If no taxon is supplied then infer it from array. If more than one taxa is on array then stop processing as blat
     * file details should relate to one taxon
     *
     * @param arrayDesign Array design to process
     * @param taxon       Taxon supplied
     * @return taxon
     */
    Taxon validateTaxaForBlatFile( ArrayDesign arrayDesign, Taxon taxon );

    @SuppressWarnings("unused")
        // Possible external use
    Collection<BlatResult> processArrayDesign( ArrayDesign design );

    Collection<BlatResult> processArrayDesign( ArrayDesign ad, Blat blat );

}