/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
package ubic.gemma.core.loader.genome.gene.ncbi;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.persistence.service.genome.gene.GeneProductChange;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * The TSV report of the gene product changes of a {@code geneUpdate} run: one row per {@link GeneProductChange},
 * platforms joined with commas.
 * <p>
 * An instance writes the report, flushing each row so that a run that fails keeps what it wrote. {@link #read(Reader)}
 * reads one back.
 * <p>
 * A header comment line {@code dryRun=true} marks a report written by a dry run, whose changes were all rolled back.
 */
public class GeneProductChangeTsv implements Consumer<GeneProductChange>, Closeable {

    private static final String[] COLUMNS = { "kind", "applied", "taxon", "gene_ncbi_id", "gene_symbol",
            "to_gene_ncbi_id", "to_gene_symbol", "product_id", "product_name", "product_gi", "blat_associations",
            "annotation_associations", "platforms" };

    private static final String DRY_RUN_COMMENT = "dryRun=";

    private static final CSVFormat READ_FORMAT = CSVFormat.TDF.builder()
            .setCommentMarker( TsvUtils.COMMENT )
            .setHeader()
            .setSkipHeaderRecord( true )
            .get();

    private final CSVPrinter printer;

    /**
     * Write the header of a report.
     *
     * @param dryRun whether the rows will describe a dry run
     */
    public GeneProductChangeTsv( Writer out, boolean dryRun ) throws IOException {
        this.printer = CSVFormat.TDF.builder()
                .setRecordSeparator( '\n' )
                .setCommentMarker( TsvUtils.COMMENT )
                .setHeaderComments( "Gene products that geneUpdate removed (applied=false: would have removed, but "
                                + "removing gene products was off) or moved from one gene to another (SWITCH).",
                        DRY_RUN_COMMENT + dryRun )
                .setHeader( COLUMNS )
                .get()
                .print( out );
        printer.flush();
    }

    /**
     * Write a row and flush it.
     *
     * @throws UncheckedIOException if the row cannot be written
     */
    @Override
    public void accept( GeneProductChange change ) {
        try {
            printer.printRecord(
                    change.kind(),
                    change.applied(),
                    change.taxon(),
                    change.geneNcbiId(),
                    change.geneSymbol(),
                    change.toGeneNcbiId(),
                    change.toGeneSymbol(),
                    change.productId(),
                    change.productName(),
                    change.productGi(),
                    change.blatAssociations(),
                    change.annotationAssociations(),
                    String.join( ",", change.platforms() ) );
            printer.flush();
        } catch ( IOException e ) {
            throw new UncheckedIOException( "Failed to write a gene product change to the report.", e );
        }
    }

    @Override
    public void close() throws IOException {
        printer.close();
    }

    /**
     * @param dryRun  whether the report was written by a dry run
     * @param changes the rows, in the order they were written
     */
    public record Report( boolean dryRun, List<GeneProductChange> changes ) {
    }

    public static Report read( Reader in ) throws IOException {
        try ( CSVParser parser = READ_FORMAT.parse( in ) ) {
            List<String> header = parser.getHeaderNames();
            if ( !header.equals( Arrays.asList( COLUMNS ) ) ) {
                throw new IllegalArgumentException( "Not a gene product change report: expected the columns "
                        + String.join( ", ", COLUMNS ) + ", found " + String.join( ", ", header ) + "." );
            }
            boolean dryRun = false;
            if ( parser.hasHeaderComment() ) {
                for ( String line : parser.getHeaderComment().split( "\n" ) ) {
                    if ( line.trim().startsWith( DRY_RUN_COMMENT ) ) {
                        dryRun = Boolean.parseBoolean( line.trim().substring( DRY_RUN_COMMENT.length() ) );
                    }
                }
            }
            List<GeneProductChange> changes = new ArrayList<>();
            for ( CSVRecord record : parser ) {
                changes.add( new GeneProductChange(
                        GeneProductChange.Kind.valueOf( record.get( "kind" ) ),
                        Boolean.parseBoolean( record.get( "applied" ) ),
                        StringUtils.stripToNull( record.get( "taxon" ) ),
                        TsvUtils.parseInt( record.get( "gene_ncbi_id" ) ),
                        StringUtils.stripToNull( record.get( "gene_symbol" ) ),
                        TsvUtils.parseInt( record.get( "to_gene_ncbi_id" ) ),
                        StringUtils.stripToNull( record.get( "to_gene_symbol" ) ),
                        TsvUtils.parseLong( record.get( "product_id" ) ),
                        StringUtils.stripToNull( record.get( "product_name" ) ),
                        StringUtils.stripToNull( record.get( "product_gi" ) ),
                        Long.parseLong( record.get( "blat_associations" ) ),
                        Long.parseLong( record.get( "annotation_associations" ) ),
                        Arrays.asList( StringUtils.split( record.get( "platforms" ), ',' ) ) ) );
            }
            return new Report( dryRun, changes );
        }
    }
}
