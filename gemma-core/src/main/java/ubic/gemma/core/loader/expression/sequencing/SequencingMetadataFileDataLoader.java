package ubic.gemma.core.loader.expression.sequencing;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Load sequencing metadata from a TSV file.
 * @author poirigui
 * @see SequencingMetadata
 */
@CommonsLog
public class SequencingMetadataFileDataLoader extends AbstractDelegatingSequencingDataLoader {

    @Nullable
    private final Path sequencingMetadataFile;

    @Nullable
    private final SequencingMetadata defaultMetadata;

    private final BioAssayMapper bioAssayMapper;

    private boolean ignoreUnmatchedSamples = true;

    /**
     * @param delegate               the delegate loader which may, or may not implement {@link #getSequencingMetadata(Collection)}
     * @param sequencingMetadataFile path to the TSV file containing the metadata
     * @param defaultMetadata        metadata to fill-in of there are no other values either from the file or supplied
     *                               by the delegate
     * @param bioAssayMapper         a mapper to resolve {@link BioAssay}s from the sample IDs in the file
     */
    public SequencingMetadataFileDataLoader( SequencingDataLoader delegate, @Nullable Path sequencingMetadataFile, @Nullable SequencingMetadata defaultMetadata, BioAssayMapper bioAssayMapper ) {
        super( delegate );
        Assert.isTrue( sequencingMetadataFile != null || defaultMetadata != null,
                "At least a metadata file or some default values must be supplied." );
        this.sequencingMetadataFile = sequencingMetadataFile;
        this.defaultMetadata = defaultMetadata;
        this.bioAssayMapper = bioAssayMapper;
    }

    @Override
    public void setIgnoreUnmatchedSamples( boolean ignoreUnmatchedSamples ) {
        super.setIgnoreUnmatchedSamples( ignoreUnmatchedSamples );
        this.ignoreUnmatchedSamples = ignoreUnmatchedSamples;
    }

    @Override
    public Map<BioAssay, SequencingMetadata> getSequencingMetadata( Collection<BioAssay> bioAssays ) throws IOException {
        Map<BioAssay, SequencingMetadata> result = super.getSequencingMetadata( bioAssays );
        Set<BioAssay> seenAssays = new HashSet<>( bioAssays.size() );
        if ( sequencingMetadataFile != null ) {
            // create a copy since we're going to modify it
            result = new HashMap<>( result );
            try ( CSVParser parser = CSVFormat.TDF.builder().setHeader().setSkipHeaderRecord( true ).build().parse( Files.newBufferedReader( sequencingMetadataFile ) ) ) {
                for ( CSVRecord record : parser ) {
                    String sampleId = record.get( "sample_id" );
                    BioAssay ba = bioAssayMapper.matchOne( bioAssays, sampleId ).orElse( null );
                    if ( ba == null ) {
                        String m = "No assay found for sample ID " + sampleId + ".";
                        if ( ignoreUnmatchedSamples ) {
                            log.warn( m );
                        } else {
                            throw new IllegalStateException( m );
                        }
                        continue;
                    }
                    if ( !seenAssays.add( ba ) ) {
                        throw new IllegalStateException( "The assay for " + sampleId + " was already matched by another identifier from " + sequencingMetadataFile + "." );
                    }
                    SequencingMetadata sm = result.computeIfAbsent( ba, k -> SequencingMetadata.builder().build() );
                    updateSequencingMetadata( sm, record );
                }
            }
        }
        // apply defaults to unseen assays
        if ( defaultMetadata != null ) {
            // create a copy since we're going to modify it
            result = new HashMap<>( result );
            for ( BioAssay ba : bioAssays ) {
                if ( !seenAssays.contains( ba ) ) {
                    SequencingMetadata sm = result.computeIfAbsent( ba, k -> SequencingMetadata.builder().build() );
                    updateSequencingMetadata( sm, null );
                }
            }
        }
        return result;
    }

    private void updateSequencingMetadata( SequencingMetadata sm, @Nullable CSVRecord record ) {
        setField( sm, SequencingMetadata::getReadLength, SequencingMetadata::setReadLength, record, "sequence_read_length", Integer::parseInt );
        setField( sm, SequencingMetadata::getReadCount, SequencingMetadata::setReadCount, record, "sequence_read_count", Long::parseLong );
        setField( sm, SequencingMetadata::getIsPaired, SequencingMetadata::setIsPaired, record, "sequence_is_paired", Boolean::parseBoolean );
    }

    private <T> void setField( SequencingMetadata sm,
            Function<SequencingMetadata, T> extractor, BiConsumer<SequencingMetadata, T> setter,
            @Nullable CSVRecord record, String columnName, Function<String, T> parseFunc ) {
        T previousValue = extractor.apply( sm );
        T newValue;
        if ( record != null && record.isMapped( columnName ) ) {
            if ( StringUtils.isNotBlank( record.get( columnName ) ) ) {
                newValue = parseFunc.apply( StringUtils.strip( record.get( columnName ) ) );
            } else if ( previousValue != null ) {
                newValue = null; // keep the value from the delegate
            } else if ( defaultMetadata != null ) {
                newValue = extractor.apply( defaultMetadata );
            } else {
                newValue = null;
            }
        } else if ( previousValue != null ) {
            newValue = null; // keep the value from the delegate
        } else if ( defaultMetadata != null ) {
            newValue = extractor.apply( defaultMetadata );
        } else {
            newValue = null;
        }
        if ( newValue != null ) {
            setter.accept( sm, newValue );
        }
    }
}

