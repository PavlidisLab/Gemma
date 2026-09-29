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
package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.FileUtils;
import ubic.gemma.core.loader.genome.gene.ncbi.GeneProductChangeTsv;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.genome.gene.GeneProductChange;
import ubic.gemma.persistence.service.genome.gene.GeneProductRemovalOutcome;
import ubic.gemma.persistence.service.genome.gene.GeneWriteService;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static ubic.gemma.cli.util.EntityOptionsUtils.addCommaDelimitedPlatformOption;

/**
 * Apply the gene product removals that {@code geneUpdate -noRemove} recorded in its report instead of making them.
 * <p>
 * Each removal is checked against the database first and skipped if it no longer holds; see
 * {@link GeneWriteService#replayGeneProductRemoval}. Each is applied in its own transaction.
 */
public class ReplayGeneProductRemovalsCli extends AbstractAuthenticatedCLI {

    private static final String REPORT_OPTION = "report";
    private static final String PLATFORMS_OPTION = "platforms";
    private static final String PLATFORM_FILE_OPTION = "platformFile";
    private static final String DRY_RUN_OPTION = "dryRun";

    @Autowired
    private GeneWriteService geneWriteService;
    @Autowired
    private EntityLocator entityLocator;

    private Path reportFile;
    private final Set<String> platformIdentifiers = new LinkedHashSet<>();
    private boolean dryRun;

    public ReplayGeneProductRemovalsCli() {
        setRequireLogin();
    }

    @Override
    public String getCommandName() {
        return "replayGeneProductRemovals";
    }

    @Override
    public String getShortDesc() {
        return "Apply the gene product removals that geneUpdate -noRemove recorded and did not make";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.SYSTEM;
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( Option.builder( REPORT_OPTION ).longOpt( "report" ).hasArg().argName( "file" ).type( Path.class )
                .required()
                .desc( "The TSV file written by geneUpdate -noRemove -report. Its REMOVE rows with applied=false are replayed." )
                .build() );
        addCommaDelimitedPlatformOption( options, PLATFORMS_OPTION, "platforms",
                "Only delete the associations of these platforms' elements with the gene products, and a gene product "
                        + "only once it has no association left. An association is between a sequence and a gene "
                        + "product, so one whose sequence is also used by another platform's elements goes for that "
                        + "platform too." );
        options.addOption( Option.builder( PLATFORM_FILE_OPTION ).longOpt( "platform-file" ).hasArg().argName( "file" )
                .type( Path.class )
                .desc( "File with platform identifiers, one per line, used like -" + PLATFORMS_OPTION + " (and added to it)." )
                .build() );
        options.addOption( DRY_RUN_OPTION, "dry-run", false, "Check each removal and report what it would delete, "
                + "without deleting anything." );
        addBatchOption( options );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        reportFile = commandLine.getParsedOptionValue( REPORT_OPTION );
        if ( commandLine.hasOption( PLATFORMS_OPTION ) ) {
            platformIdentifiers.addAll( Arrays.asList( commandLine.getOptionValues( PLATFORMS_OPTION ) ) );
        }
        if ( commandLine.hasOption( PLATFORM_FILE_OPTION ) ) {
            Path platformFile = commandLine.getParsedOptionValue( PLATFORM_FILE_OPTION );
            try {
                platformIdentifiers.addAll( FileUtils.readListFileToStrings( platformFile ) );
            } catch ( IOException e ) {
                throw new ParseException( "Could not read " + platformFile + ": " + e.getMessage() );
            }
            if ( platformIdentifiers.isEmpty() ) {
                throw new ParseException( platformFile + " lists no platform." );
            }
        }
        dryRun = commandLine.hasOption( DRY_RUN_OPTION );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        GeneProductChangeTsv.Report report;
        try ( Reader in = Files.newBufferedReader( reportFile ) ) {
            report = GeneProductChangeTsv.read( in );
        }
        if ( report.dryRun() ) {
            throw new IllegalArgumentException( reportFile + " was written by a geneUpdate dry run, whose changes were "
                    + "rolled back, so its rows do not describe the database." );
        }
        List<GeneProductChange> removals = report.changes().stream()
                .filter( c -> c.kind() == GeneProductChange.Kind.REMOVE && !c.applied() )
                .collect( Collectors.toList() );

        Collection<ArrayDesign> platforms = null;
        if ( !platformIdentifiers.isEmpty() ) {
            platforms = new ArrayList<>();
            for ( String identifier : platformIdentifiers ) {
                platforms.add( entityLocator.locateArrayDesign( identifier ) );
            }
        }

        log.info( ( dryRun ? "Dry run: checking " : "Replaying " ) + removals.size() + " gene product removals from "
                + reportFile + " (of " + report.changes().size() + " rows)"
                + ( platforms != null ? " on " + platforms.stream().map( ArrayDesign::getShortName ).collect( Collectors.joining( ", " ) ) : "" )
                + "." );

        int applied = 0, failed = 0;
        Map<GeneProductRemovalOutcome.SkipReason, Integer> skipped = new EnumMap<>( GeneProductRemovalOutcome.SkipReason.class );
        long productsDeleted = 0, productsKept = 0, blatAssociationsDeleted = 0, annotationAssociationsDeleted = 0;
        Set<String> affectedPlatforms = new TreeSet<>();
        for ( GeneProductChange removal : removals ) {
            GeneProductRemovalOutcome outcome;
            try {
                outcome = geneWriteService.replayGeneProductRemoval( removal, platforms, dryRun );
            } catch ( Exception e ) {
                failed++;
                addErrorObject( removal.describe(), e );
                continue;
            }
            if ( outcome.isSkipped() ) {
                skipped.merge( outcome.skipReason(), 1, Integer::sum );
                addWarningObject( removal.describe(), "Skipped, " + outcome.skipReason() + ": " + outcome.detail() );
                continue;
            }
            applied++;
            if ( outcome.productDeleted() ) {
                productsDeleted++;
            } else {
                productsKept++;
            }
            blatAssociationsDeleted += outcome.blatAssociationsDeleted();
            annotationAssociationsDeleted += outcome.annotationAssociationsDeleted();
            affectedPlatforms.addAll( outcome.platforms() );
            addSuccessObject( removal.describe(), outcome.detail() );
        }

        String would = dryRun ? " (dry run: nothing was deleted)" : "";
        log.info( "Gene product removals" + would + ":\n"
                + "  applied: " + applied + "\n"
                + "  skipped because they no longer hold: " + skipped.values().stream().mapToInt( Integer::intValue ).sum()
                + ( skipped.isEmpty() ? "" : " " + skipped ) + "\n"
                + "  failed: " + failed + "\n"
                + "  gene products deleted: " + productsDeleted + "\n"
                + "  gene products kept for their associations on other platforms: " + productsKept + "\n"
                + "  BLAT associations deleted: " + blatAssociationsDeleted + "\n"
                + "  annotation associations deleted: " + annotationAssociationsDeleted + "\n"
                + "  platforms whose elements had those associations: " + ( affectedPlatforms.isEmpty() ? "none" : String.join( ", ", affectedPlatforms ) ) );
        if ( !dryRun && blatAssociationsDeleted + annotationAssociationsDeleted > 0 ) {
            log.warn( "GENE2CS keeps the rows of the deleted associations until updateGene2Cs -truncate -force is run. "
                    + "Then regenerate the annotation files of the affected platforms with makePlatformAnnotFiles: "
                    + String.join( ", ", affectedPlatforms ) + "." );
        }
    }
}
