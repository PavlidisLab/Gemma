package ubic.gemma.core.analysis.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.loader.expression.geo.model.GeoLibraryStrategy;
import ubic.gemma.core.security.audit.payload.SampleMetadataPayload;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.ExtractedMolecule;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BioAssayMetadataServiceImpl implements BioAssayMetadataService {

    /**
     * The column is {@code VARCHAR(255)} on all three fields.
     */
    private static final int MAX_LENGTH = 255;

    private static final Set<String> VALID_LIBRARY_STRATEGIES;

    static {
        Set<String> s = new TreeSet<>();
        for ( GeoLibraryStrategy v : GeoLibraryStrategy.values() ) {
            s.add( v.name() );
        }
        // Gemma's own two, for microarray samples: GEO's library_strategy vocabulary covers sequencing only.
        s.add( BioAssay.LIBRARY_STRATEGY_MICROARRAY_ONE_COLOR );
        s.add( BioAssay.LIBRARY_STRATEGY_MICROARRAY_TWO_COLOR );
        VALID_LIBRARY_STRATEGIES = Collections.unmodifiableSet( s );
    }

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private BioAssayMetadataAuditService bioAssayMetadataAuditService;

    @Override
    public Set<String> getValidLibraryStrategies() {
        return VALID_LIBRARY_STRATEGIES;
    }

    @Override
    @Transactional
    public Collection<BioAssay> setLibraryStrategy( ExpressionExperiment ee, Collection<BioAssay> bioAssays, @Nullable String value ) {
        String v = StringUtils.trimToNull( value );
        if ( v != null && !VALID_LIBRARY_STRATEGIES.contains( v ) ) {
            throw new IllegalArgumentException( "Unknown library strategy '" + v + "'. Known values: "
                    + String.join( ", ", VALID_LIBRARY_STRATEGIES ) );
        }
        return apply( ee, bioAssays, "libraryStrategy", v, BioAssay::getLibraryStrategy, BioAssay::setLibraryStrategy );
    }

    @Override
    @Transactional
    public Collection<BioAssay> setLibrarySelection( ExpressionExperiment ee, Collection<BioAssay> bioAssays, @Nullable String value ) {
        String v = StringUtils.trimToNull( value );
        if ( v != null && v.length() > MAX_LENGTH ) {
            throw new IllegalArgumentException( "librarySelection must be at most " + MAX_LENGTH + " characters." );
        }
        return apply( ee, bioAssays, "librarySelection", v, BioAssay::getLibrarySelection, BioAssay::setLibrarySelection );
    }

    @Override
    @Transactional
    public Collection<BioAssay> setExtractedMolecule( ExpressionExperiment ee, Collection<BioAssay> bioAssays, @Nullable String value ) {
        String v = StringUtils.trimToNull( value );
        ExtractedMolecule parsed = null;
        if ( v != null ) {
            for ( ExtractedMolecule m : ExtractedMolecule.values() ) {
                if ( m.name().equals( v ) ) {
                    parsed = m;
                    break;
                }
            }
            if ( parsed == null ) {
                throw new IllegalArgumentException( "Unknown extracted molecule '" + v + "'. Known values: "
                        + Arrays.stream( ExtractedMolecule.values() ).map( Enum::name ).collect( Collectors.joining( ", " ) ) );
            }
        }
        ExtractedMolecule target = parsed;
        return apply( ee, bioAssays, "extractedMolecule", v,
                ba -> ba.getExtractedMolecule() != null ? ba.getExtractedMolecule().name() : null,
                ( ba, ignored ) -> ba.setExtractedMolecule( target ) );
    }

    /**
     * Write {@code value} to every assay that does not already hold it, then audit once against the
     * experiment. Assays already holding the value are skipped so a re-run does not manufacture an audit
     * event, and no event is recorded at all when nothing changed.
     */
    private Collection<BioAssay> apply( ExpressionExperiment ee, Collection<BioAssay> bioAssays, String field,
            @Nullable String value, Function<BioAssay, String> getter, BiConsumer<BioAssay, String> setter ) {
        List<BioAssay> changed = new ArrayList<>();
        for ( BioAssay ba : bioAssays ) {
            if ( Objects.equals( getter.apply( ba ), value ) ) {
                continue;
            }
            setter.accept( ba, value );
            bioAssayService.update( ba );
            changed.add( ba );
        }
        if ( changed.isEmpty() ) {
            return changed;
        }
        List<String> labels = changed.stream().map( BioAssay::toString ).collect( Collectors.toList() );
        String note = ( value != null ? "Set " + field + " = " + value : "Cleared " + field )
                + " on " + changed.size() + " sample" + ( changed.size() == 1 ? "" : "s" );
        bioAssayMetadataAuditService.recordSampleMetadataChange( ee, note, new SampleMetadataPayload( field, value, labels ) );
        return changed;
    }
}
