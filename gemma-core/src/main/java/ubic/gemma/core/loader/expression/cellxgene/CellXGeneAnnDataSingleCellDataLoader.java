package ubic.gemma.core.loader.expression.cellxgene;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import ubic.gemma.core.loader.expression.singleCell.AnnDataSingleCellDataLoader;
import ubic.gemma.core.loader.util.mapper.SimpleBioAssayMapper;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class CellXGeneAnnDataSingleCellDataLoader extends AnnDataSingleCellDataLoader {

    private final boolean keepPooledSample;
    private final boolean keepUnknownSample;

    public CellXGeneAnnDataSingleCellDataLoader( Path file, boolean keepPooledSample, boolean keepUnknownSample ) {
        super( file );
        this.keepPooledSample = keepPooledSample;
        this.keepUnknownSample = keepUnknownSample;
        // defaults for CELLxGENE
        setTranspose( true );
        setSampleFactorName( "donor_id" );
        setCellTypeFactorName( "cell_type" );
        setCellTypeUriFactorName( "cell_type_ontology_term_id" );
        setUnknownCellTypeIndicator( "unknown" );
        setBioAssayToSampleNameMapper( new SimpleBioAssayMapper() );
    }

    @Override
    public Set<String> getSampleNames() throws IOException {
        LinkedHashSet<String> sn = new LinkedHashSet<>( super.getSampleNames() );
        if ( !keepPooledSample ) {
            sn.remove( "pooled" );
        }
        if ( !keepUnknownSample ) {
            sn.remove( "unknown" );
        }
        return sn;
    }

    @Override
    public Map<BioMaterial, Set<Characteristic>> getSamplesCharacteristics( Collection<BioAssay> samples ) throws IOException {
        return super.getSamplesCharacteristics( samples ).entrySet().stream()
                .collect( Collectors.toMap( Map.Entry::getKey, e -> mergeOntologyTerms( e.getValue() ) ) );
    }

    /**
     * Ontology terms in CELLxGENE are split in two separate column: one for the label and one for the URI.
     */
    private Set<Characteristic> mergeOntologyTerms( Set<Characteristic> cs ) {
        cs = new HashSet<>( cs );
        Map<String, Set<Characteristic>> characteristicsByCategory = cs.stream()
                .filter( c -> c.getCategory() != null )
                .collect( Collectors.groupingBy( Characteristic::getCategory, Collectors.toSet() ) );
        for ( String category : characteristicsByCategory.keySet() ) {
            if ( category.endsWith( "_ontology_term_id" ) ) {
                if ( characteristicsByCategory.get( category ).size() > 1 ) {
                    log.warn( "Multiple characteristics for category " + category + ", skipping merging ontology terms." );
                    continue;
                }
                Characteristic ontologyTerm = characteristicsByCategory.get( category ).iterator().next();
                String labelColumn = Strings.CS.removeEnd( category, "_ontology_term_id" );
                if ( characteristicsByCategory.containsKey( labelColumn ) ) {
                    if ( characteristicsByCategory.get( labelColumn ).size() > 1 ) {
                        log.warn( "Multiple characteristics for category " + labelColumn + ", skipping merging ontology terms." );
                        continue;
                    }
                    Characteristic ontologyLabel = characteristicsByCategory.get( labelColumn ).iterator().next();
                    if ( ( "na".equals( ontologyLabel.getValue() ) && "na".equals( ontologyTerm.getValue() ) )
                            || ( "unknown".equals( ontologyLabel.getValue() ) && "unknown".equals( ontologyTerm.getValue() ) ) ) {
                        // missing value, drop the characteristic
                        cs.remove( ontologyLabel );
                        cs.remove( ontologyTerm );
                    } else if ( "na".equals( ontologyTerm.getValue() ) || "unknown".equals( ontologyTerm.getValue() ) ) {
                        // treat it as a free-text term
                        ontologyLabel.setValueUri( null );
                        cs.remove( ontologyTerm );
                    } else if ( "||".contains( ontologyTerm.getValue() ) ) {
                        // multi-value, we drop the original label & term, but we create a new characteristic for each
                        // term
                        cs.remove( ontologyLabel );
                        cs.remove( ontologyTerm );
                        for ( String termId : ontologyTerm.getValue().split( "\\|\\|" ) ) {
                            if ( termId.equals( "na" ) || termId.equals( "unknown" ) ) {
                                continue;
                            }
                            cs.add( Characteristic.Factory.newInstance( category, null,
                                    ontologyLabel.getValue(), CellXGeneUtils.getTermUri( termId ) ) );
                        }
                    } else {
                        // move the URI to the label characteristic and drop the term one
                        ontologyLabel.setValueUri( CellXGeneUtils.getTermUri( ontologyTerm.getValue() ) );
                        cs.remove( ontologyTerm );
                    }
                }
            }
        }
        return cs;
    }
}
