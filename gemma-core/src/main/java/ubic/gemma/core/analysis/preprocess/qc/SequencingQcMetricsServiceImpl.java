package ubic.gemma.core.analysis.preprocess.qc;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentMetaFileType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Streams {@code report_general_stats_data} out of the MultiQC report and resolves its row keys to
 * bioAssays.
 * <p>
 * Two things about that file drive the implementation:
 * <ul>
 * <li>The whole report is around 5.5 MB, of which the general-stats block is about 1% — the rest is
 * {@code report_plot_data}. It is read with the streaming parser and every other top-level field is
 * skipped, so the plot data is never materialized.
 * <li>Row keys are not uniformly sample accessions. Measured over 80 reports on the production
 * metadata volume: 3055 rows keyed by the bare accession, 1244 by an accession with a run or mate
 * suffix ({@code GSM5029427_1}, {@code GSM5029427_SRR13191146_2}), and 1541 by an SRA run accession
 * alone, which Gemma does not record and so cannot be joined. The split falls along module lines —
 * STAR and RSEM key by sample ({@code uniquely_mapped_percent} was sample-level in 79 of those 80
 * reports), FastQC keys by FASTQ file ({@code percent_duplicates} in 5 of 80).
 * </ul>
 *
 * @author gembro
 */
@Service
public class SequencingQcMetricsServiceImpl implements SequencingQcMetricsService {

    private static final Log log = LogFactory.getLog( SequencingQcMetricsServiceImpl.class );

    /**
     * Metric holding the sample's input read count, used for {@link SequencingQcMetrics.SampleMetrics#getReadCount()}.
     * This is STAR's count of reads handed to the aligner.
     */
    private static final String TOTAL_READS_METRIC = "total_reads";

    /**
     * Characters MultiQC puts between an accession and a run or mate suffix in a row key.
     */
    private static final String KEY_SUFFIX_SEPARATORS = "_.";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Override
    public Optional<SequencingQcMetrics> getSequencingQcMetrics( ExpressionExperiment ee ) throws IOException {
        Collection<BioAssay> bioAssays = ee.getBioAssays();

        GeneralStats stats = readGeneralStats( ee );

        if ( stats == null && bioAssays.stream().allMatch( ba -> ba.getSequenceReadCount() == null ) ) {
            return Optional.empty();
        }

        Map<String, BioAssay> byKey = indexBioAssays( bioAssays );

        // sample-level rows (key IS an assay identifier) and sub-sample rows (key merely starts with one)
        Map<Long, Map<String, Double>> sampleValues = new HashMap<>();
        Map<Long, List<SequencingQcMetrics.RunMetrics>> runValues = new HashMap<>();
        // a key that joins to nothing typically appears in several modules; report it once
        Set<String> unmatchedKeys = new TreeSet<>();
        List<SequencingQcMetrics.MetricDefinition> metrics = Collections.emptyList();

        if ( stats != null ) {
            for ( Map.Entry<String, JsonNode> section : stats.sections.entrySet() ) {
                for ( Iterator<Map.Entry<String, JsonNode>> it = section.getValue().fields(); it.hasNext(); ) {
                    Map.Entry<String, JsonNode> row = it.next();
                    String key = row.getKey();
                    if ( !row.getValue().isObject() ) {
                        continue;
                    }
                    Match match = resolve( key, byKey );
                    if ( match == null ) {
                        unmatchedKeys.add( key );
                        continue;
                    }
                    Map<String, Double> values = readNumericFields( row.getValue() );
                    if ( values.isEmpty() ) {
                        continue;
                    }
                    if ( match.exact ) {
                        Map<String, Double> merged = sampleValues
                                .computeIfAbsent( match.bioAssay.getId(), k -> new LinkedHashMap<>() );
                        // a metric two modules both define keeps the first module's value, which is
                        // the order MultiQC itself lists them in
                        values.forEach( merged::putIfAbsent );
                    } else {
                        runValues.computeIfAbsent( match.bioAssay.getId(), k -> new ArrayList<>() )
                                .add( new SequencingQcMetrics.RunMetrics( key, values ) );
                    }
                }
            }
            metrics = describeMetrics( stats, sampleValues, runValues );
        }

        // bioAssays come out of Hibernate as an unordered set; order by id so the same dataset
        // answers in the same order every time
        List<BioAssay> ordered = new ArrayList<>( bioAssays );
        ordered.sort( Comparator.comparing( BioAssay::getId, Comparator.nullsLast( Comparator.naturalOrder() ) ) );

        List<SequencingQcMetrics.SampleMetrics> samples = new ArrayList<>( ordered.size() );
        for ( BioAssay ba : ordered ) {
            Map<String, Double> values = sampleValues.getOrDefault( ba.getId(), Collections.emptyMap() );
            List<SequencingQcMetrics.RunMetrics> runs = runValues.getOrDefault( ba.getId(), Collections.emptyList() );
            Long readCount = null;
            String readCountSource = null;
            Double reported = values.get( TOTAL_READS_METRIC );
            if ( reported != null && !reported.isNaN() && !reported.isInfinite() ) {
                readCount = reported.longValue();
                readCountSource = "report";
            } else if ( ba.getSequenceReadCount() != null ) {
                readCount = ba.getSequenceReadCount();
                readCountSource = "bioAssay";
            }
            samples.add( new SequencingQcMetrics.SampleMetrics( ba.getId(), accessionOf( ba ), ba.getName(),
                    ba.getIsOutlier(), values, runs, readCount, readCountSource ) );
        }

        return Optional.of( new SequencingQcMetrics( stats != null, metrics, samples,
                new ArrayList<>( unmatchedKeys ) ) );
    }

    /**
     * The general-stats data and headers, each as an ordered module name -&gt; section map.
     */
    private static class GeneralStats {
        final Map<String, JsonNode> sections = new LinkedHashMap<>();
        final Map<String, JsonNode> headers = new LinkedHashMap<>();
    }

    /**
     * Read the two general-stats blocks out of the report, skipping every other top-level field.
     *
     * @return the blocks, or null when the experiment has no readable report
     */
    @Nullable
    private GeneralStats readGeneralStats( ExpressionExperiment ee ) throws IOException {
        try ( LockedPath report = expressionDataFileService
                .getMetadataFile( ee, ExpressionExperimentMetaFileType.RNASEQ_PIPELINE_REPORT_DATA, false )
                .orElse( null ) ) {
            if ( report == null || !Files.isReadable( report.getPath() ) ) {
                return null;
            }
            GeneralStats stats = new GeneralStats();
            try ( JsonParser parser = objectMapper.getFactory().createParser( report.getPath().toFile() ) ) {
                if ( parser.nextToken() != JsonToken.START_OBJECT ) {
                    log.warn( "MultiQC report for " + ee.getShortName() + " is not a JSON object; ignoring it." );
                    return null;
                }
                while ( parser.nextToken() == JsonToken.FIELD_NAME ) {
                    String field = parser.currentName();
                    parser.nextToken();
                    if ( "report_general_stats_data".equals( field ) ) {
                        collectSections( objectMapper.readTree( parser ), stats.sections );
                    } else if ( "report_general_stats_headers".equals( field ) ) {
                        collectSections( objectMapper.readTree( parser ), stats.headers );
                    } else {
                        parser.skipChildren();
                    }
                }
            }
            if ( stats.sections.isEmpty() ) {
                log.warn( "MultiQC report for " + ee.getShortName() + " has no general-stats data." );
                return null;
            }
            return stats;
        }
    }

    /**
     * Normalize the two shapes MultiQC has used for these blocks into a module name -&gt; section map:
     * an array of sections (through MultiQC 1.20), or an object keyed by module name (1.21 onward).
     * Array sections have no module name, so they are numbered.
     */
    private static void collectSections( JsonNode node, Map<String, JsonNode> into ) {
        if ( node.isArray() ) {
            for ( int i = 0; i < node.size(); i++ ) {
                if ( node.get( i ).isObject() ) {
                    into.put( String.valueOf( i ), node.get( i ) );
                }
            }
        } else if ( node.isObject() ) {
            for ( Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> e = it.next();
                if ( e.getValue().isObject() ) {
                    into.put( e.getKey(), e.getValue() );
                }
            }
        }
    }

    /**
     * Index the assays by every identifier a report row might be keyed by. An identifier two assays
     * share is dropped rather than resolved arbitrarily.
     */
    private static Map<String, BioAssay> indexBioAssays( Collection<BioAssay> bioAssays ) {
        Map<String, BioAssay> byKey = new HashMap<>();
        Set<String> ambiguous = new HashSet<>();
        for ( BioAssay ba : bioAssays ) {
            for ( String id : new String[] { accessionOf( ba ), ba.getShortName(), ba.getName() } ) {
                if ( id == null || id.isEmpty() ) {
                    continue;
                }
                BioAssay previous = byKey.put( id, ba );
                if ( previous != null && !previous.equals( ba ) ) {
                    ambiguous.add( id );
                }
            }
        }
        byKey.keySet().removeAll( ambiguous );
        return byKey;
    }

    @Nullable
    private static String accessionOf( BioAssay ba ) {
        return ba.getAccession() != null ? ba.getAccession().getAccession() : null;
    }

    private static class Match {
        final BioAssay bioAssay;
        /** True when the row key IS the assay's identifier rather than a run or mate below it. */
        final boolean exact;

        Match( BioAssay bioAssay, boolean exact ) {
            this.bioAssay = bioAssay;
            this.exact = exact;
        }
    }

    /**
     * Resolve a report row key to an assay, trying the whole key first and then successively shorter
     * prefixes ending at a separator, so {@code GSM5029427_SRR13191146_2} reaches {@code GSM5029427}.
     * Longest first, so an accession that itself contains a separator is not cut short.
     */
    @Nullable
    private static Match resolve( String key, Map<String, BioAssay> byKey ) {
        BioAssay exact = byKey.get( key );
        if ( exact != null ) {
            return new Match( exact, true );
        }
        for ( int i = key.length() - 1; i > 0; i-- ) {
            if ( KEY_SUFFIX_SEPARATORS.indexOf( key.charAt( i ) ) < 0 ) {
                continue;
            }
            BioAssay ba = byKey.get( key.substring( 0, i ) );
            if ( ba != null ) {
                return new Match( ba, false );
            }
        }
        return null;
    }

    /**
     * Pull the numeric fields of a row. MultiQC mixes in strings (tool versions, for one); a caller
     * plotting a strip cannot use those, so they are dropped.
     */
    private static Map<String, Double> readNumericFields( JsonNode row ) {
        Map<String, Double> values = new LinkedHashMap<>();
        for ( Iterator<Map.Entry<String, JsonNode>> it = row.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> e = it.next();
            if ( e.getValue().isNumber() ) {
                double v = e.getValue().doubleValue();
                if ( !Double.isNaN( v ) && !Double.isInfinite( v ) ) {
                    values.put( e.getKey(), v );
                }
            }
        }
        return values;
    }

    /**
     * Describe every metric that actually appears in the resolved rows. MultiQC's general-stats
     * headers only cover the columns it chose to display — a handful of the roughly thirty per
     * module — so a metric with no header entry gets one carrying just its name.
     */
    private List<SequencingQcMetrics.MetricDefinition> describeMetrics( GeneralStats stats,
            Map<Long, Map<String, Double>> sampleValues, Map<Long, List<SequencingQcMetrics.RunMetrics>> runValues ) {
        Set<String> present = new LinkedHashSet<>();
        for ( Map<String, Double> values : sampleValues.values() ) {
            present.addAll( values.keySet() );
        }
        for ( List<SequencingQcMetrics.RunMetrics> runs : runValues.values() ) {
            for ( SequencingQcMetrics.RunMetrics run : runs ) {
                present.addAll( run.getValues().keySet() );
            }
        }
        List<SequencingQcMetrics.MetricDefinition> metrics = new ArrayList<>( present.size() );
        for ( String name : present ) {
            JsonNode header = null;
            String module = null;
            for ( Map.Entry<String, JsonNode> section : stats.headers.entrySet() ) {
                JsonNode candidate = section.getValue().get( name );
                if ( candidate != null && candidate.isObject() ) {
                    header = candidate;
                    module = section.getKey();
                    break;
                }
            }
            metrics.add( new SequencingQcMetrics.MetricDefinition( name,
                    text( header, "title" ),
                    text( header, "description" ),
                    // MultiQC's own namespace names the module properly-cased; the section key is
                    // the fallback, and is only a module name in the object-shaped reports
                    header != null && header.hasNonNull( "namespace" ) ? header.get( "namespace" ).asText()
                            : ( module != null && !isNumeric( module ) ? module : null ),
                    text( header, "suffix" ),
                    number( header, "min" ),
                    number( header, "max" ),
                    header != null && header.path( "hidden" ).asBoolean( false ) ) );
        }
        return metrics;
    }

    @Nullable
    private static String text( @Nullable JsonNode header, String field ) {
        return header != null && header.hasNonNull( field ) ? header.get( field ).asText() : null;
    }

    @Nullable
    private static Double number( @Nullable JsonNode header, String field ) {
        return header != null && header.path( field ).isNumber() ? header.get( field ).doubleValue() : null;
    }

    private static boolean isNumeric( String s ) {
        for ( int i = 0; i < s.length(); i++ ) {
            if ( !Character.isDigit( s.charAt( i ) ) ) {
                return false;
            }
        }
        return !s.isEmpty();
    }
}
