package ubic.gemma.core.apps;

import lombok.Value;
import org.apache.commons.cli.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.persistence.service.expression.experiment.FactorValueMigratorService;

import javax.annotation.CheckReturnValue;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang.StringUtils.stripToNull;

/**
 * Performs the migration of old-style characteristics to statements.
 * <p>
 * The input file describes transition from a bag of {@link ubic.gemma.model.common.description.Characteristic} to more
 * structured {@link ubic.gemma.model.expression.experiment.Statement} which are allowed to have up to two related
 * objects.
 * <p>
 * All unmentioned characteristics will be migrated to subject-only statements.
 * @deprecated this will be removed as soon as all the old-style characteristics are migrated
 * @author poirigui
 */
@Deprecated
public class FactorValueMigratorCLI extends AbstractAuthenticatedCLI {

    private static final String
            MIGRATION_FILE_OPTION = "migrationFile",
            MIGRATE_REMAINING_CHARACTERISTICS_OPTION = "migrateRemainingCharacteristics",
            MIGRATE_REMAINING_FACTOR_VALUES_OPTION = "migrateRemainingFactorValues",
            MIGRATE_NON_TRIVIAL_CASES_OPTION = "migrateNonTrivialCases",
            NOOP_OPTION = "noop";

    @Autowired
    private FactorValueMigratorService factorValueMigratorService;

    /**
     * A list of migrations to perform.
     */
    private List<MigrationWithLineNumber> migrations;
    private boolean migrateRemainingCharacteristics;
    private boolean migrateRemainingFactorValues;
    private boolean migrateNonTrivialCases;
    private boolean noop;

    @Override
    public String getCommandName() {
        return "migrateFactorValues";
    }

    @Override
    public String getShortDesc() {
        return "Perform the migration of old-style characteristics to statements";
    }

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.MISC;
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( Option.builder( MIGRATION_FILE_OPTION ).hasArg().type( File.class ).desc( "File containing factor value migrations" ).build() );
        options.addOption( MIGRATE_REMAINING_CHARACTERISTICS_OPTION, false, "Migrate remaining characteristics of factor values that were mentioned in the migration file. The affected factor values will be marked as needs attention." );
        options.addOption( MIGRATE_REMAINING_FACTOR_VALUES_OPTION, false, "Migrate remaining factor values that weren't mentioned in the migration file." );
        options.addOption( MIGRATE_NON_TRIVIAL_CASES_OPTION, false, "Migrate non-trivial cases (i.e. 2 or more old-style characteristics) to subject-only statements. The affected factor values will be marked as needs attention." );
        options.addOption( NOOP_OPTION, false, "Only validate migrations; no statements will be saved" );
        addBatchOption( options );
    }

    @Value
    static class MigrationWithLineNumber {
        long lineNumber;
        FactorValueMigratorService.Migration migration;

        @Override
        public String toString() {
            return String.format( "[%d] %s", lineNumber, migration );
        }
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        if ( !commandLine.hasOption( MIGRATION_FILE_OPTION ) && !commandLine.hasOption( MIGRATE_REMAINING_FACTOR_VALUES_OPTION ) ) {
            throw new MissingOptionException( String.format( "At least one of -%s or -%s must be specified.",
                    MIGRATION_FILE_OPTION, MIGRATE_REMAINING_FACTOR_VALUES_OPTION ) );
        }
        if ( !commandLine.hasOption( MIGRATION_FILE_OPTION ) && commandLine.hasOption( MIGRATE_REMAINING_CHARACTERISTICS_OPTION ) ) {
            throw new MissingOptionException( String.format( "-%s must be specified if -%s is used.",
                    MIGRATION_FILE_OPTION, MIGRATE_REMAINING_CHARACTERISTICS_OPTION ) );
        }
        if ( !commandLine.hasOption( MIGRATE_REMAINING_FACTOR_VALUES_OPTION ) && commandLine.hasOption( MIGRATE_NON_TRIVIAL_CASES_OPTION ) ) {
            throw new MissingOptionException( String.format( "-%s must be specified if -%s is used.",
                    MIGRATE_REMAINING_FACTOR_VALUES_OPTION, MIGRATE_NON_TRIVIAL_CASES_OPTION ) );
        }
        migrations = new ArrayList<>();
        if ( commandLine.hasOption( MIGRATION_FILE_OPTION ) ) {
            File migrationFile = ( File ) commandLine.getParsedOptionValue( MIGRATION_FILE_OPTION );
            try ( CSVParser parser = CSVParser.parse( migrationFile, StandardCharsets.UTF_8, CSVFormat.TDF.withFirstRecordAsHeader() ) ) {
                boolean hasSecondObjectColumns = CollectionUtils.containsAny( parser.getHeaderNames(),
                        "SecondObjectID", "SecondObjectURI", "SecondObject" );
                boolean hasThirdObjectColumns = CollectionUtils.containsAny( parser.getHeaderNames(),
                        "ThirdObjectID", "ThirdObjectURI", "ThirdObject" );
                for ( CSVRecord row : parser ) {
                    FactorValueMigratorService.Migration migration;
                    try {
                        FactorValueMigratorService.Migration.MigrationBuilder migrationBuilder = FactorValueMigratorService.Migration.builder()
                                .factorValueId( parseLongIfNonBlank( row.get( "FactorValueID" ) ) )
                                .category( stripToNull( row.get( "Category" ) ) ).categoryUri( stripToNull( row.get( "CategoryURI" ) ) )
                                .oldStyleCharacteristicIdUsedAsSubject( parseLongIfNonBlank( row.get( "SubjectID" ) ) )
                                .subject( stripToNull( row.get( "Subject" ) ) ).subjectUri( stripToNull( row.get( "SubjectURI" ) ) )
                                .predicate( stripToNull( row.get( "Predicate" ) ) ).predicateUri( stripToNull( row.get( "PredicateURI" ) ) )
                                .oldStyleCharacteristicIdUsedAsObject( parseLongIfNonBlank( row.get( "ObjectID" ) ) )
                                .object( stripToNull( row.get( "Object" ) ) ).objectUri( stripToNull( row.get( "ObjectURI" ) ) );
                        if ( hasSecondObjectColumns ) {
                            // optionally...
                            migrationBuilder
                                    .secondPredicate( stripToNull( row.get( "SecondPredicate" ) ) ).secondPredicateUri( stripToNull( row.get( "SecondPredicateURI" ) ) )
                                    .oldStyleCharacteristicIdUsedAsSecondObject( parseLongIfNonBlank( row.get( "SecondObjectID" ) ) )
                                    .secondObject( stripToNull( row.get( "SecondObject" ) ) ).secondObjectUri( stripToNull( row.get( "SecondObjectURI" ) ) );
                        }
                        if ( hasThirdObjectColumns && ( StringUtils.isNotBlank( row.get( "ThirdObjectID" ) ) || StringUtils.isNotBlank( row.get( "ThirdObjectURI" ) ) || StringUtils.isNotBlank( row.get( "ThirdObject" ) ) ) ) {
                            throw new IllegalArgumentException( "Statements do not support a third object, make sure that any of the columns related to a third object are left blank." );
                        }
                        migration = migrationBuilder.build();
                    } catch ( Exception e ) {
                        throw new RuntimeException( String.format( "The following migration is invalid:\n\t[%d] %s\n\t%s",
                                row.getRecordNumber(), Arrays.toString( row.values() ), ExceptionUtils.getRootCauseMessage( e ) ), e );
                    }
                    migrations.add( new MigrationWithLineNumber( row.getRecordNumber(), migration ) );
                }
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
            if ( migrations.isEmpty() ) {
                log.warn( String.format( "The migration file %s is empty.", migrationFile.getAbsolutePath() ) );
            }
        }
        migrateRemainingCharacteristics = commandLine.hasOption( MIGRATE_REMAINING_CHARACTERISTICS_OPTION );
        migrateRemainingFactorValues = commandLine.hasOption( MIGRATE_REMAINING_FACTOR_VALUES_OPTION );
        migrateNonTrivialCases = commandLine.hasOption( MIGRATE_NON_TRIVIAL_CASES_OPTION );
        noop = commandLine.hasOption( NOOP_OPTION );
    }

    private Long parseLongIfNonBlank( String s ) {
        return StringUtils.isBlank( s ) ? null : Long.parseLong( StringUtils.strip( s ) );
    }

    @Override
    protected void doWork() throws Exception {
        if ( noop ) {
            log.info( "Noop mode enabled, no statements will be saved." );
        }
        if ( migrateRemainingFactorValues ) {
            promptConfirmationOrAbort( "Migrating remaining factor values will create a lot of statements." );
        }
        if ( migrateNonTrivialCases ) {
            promptConfirmationOrAbort( "Migrating non-trivial cases will create a lot of statements and needs attention events on the affected datasets." );
        }
        Map<Long, List<MigrationWithLineNumber>> migrationsByFactorValue = migrations.stream()
                .collect( Collectors.groupingBy( migration -> migration.getMigration().getFactorValueId(), Collectors.toList() ) );
        for ( Map.Entry<Long, List<MigrationWithLineNumber>> entry : migrationsByFactorValue.entrySet() ) {
            Long fvId = entry.getKey();
            List<MigrationWithLineNumber> ms = entry.getValue();
            // all the mentioned old-style characteristics will be marked for removal and the remaining ones will be
            // ported to simple subject-only statements
            Set<Long> mentionedOldStyleCharacteristicIds = ms.stream()
                    .map( MigrationWithLineNumber::getMigration )
                    .flatMap( m -> Stream.of( m.getOldStyleCharacteristicIdUsedAsSubject(), m.getOldStyleCharacteristicIdUsedAsObject(), m.getOldStyleCharacteristicIdUsedAsSecondObject() ) )
                    .filter( Objects::nonNull )
                    .collect( Collectors.toSet() );
            if ( ms.size() == 1 ) {
                MigrationWithLineNumber m = ms.iterator().next();
                try {
                    List<FactorValueMigratorService.MigrationResult> results = new ArrayList<>();
                    results.add( factorValueMigratorService.performMigration( m.getMigration(), noop ) );
                    if ( migrateRemainingCharacteristics ) {
                        results.addAll( factorValueMigratorService.performMigrationOfRemainingOldStyleCharacteristics( fvId, mentionedOldStyleCharacteristicIds, noop ) );
                    }
                    addSuccessObject( "FactorValue #" + fvId, summarizeMigrationResults( results ) );
                } catch ( IllegalArgumentException e ) {
                    String summary = String.format( "[%d] %s", m.getLineNumber(), m.getMigration() );
                    addErrorObject( "FactorValue #" + fvId,
                            "The following migration failed:\n\t" + summary + "\n\t" + ExceptionUtils.getRootCauseMessage( e ) );
                } catch ( Exception e ) {
                    throw interruptMigrationProcess( m, e );
                }
            } else {
                try {
                    List<FactorValueMigratorService.Migration> fvm = ms.stream()
                            .map( MigrationWithLineNumber::getMigration )
                            .collect( Collectors.toList() );
                    List<FactorValueMigratorService.MigrationResult> results = new ArrayList<>( fvm.size() );
                    results.addAll( factorValueMigratorService.performMultipleMigrations( fvm, noop ) );
                    if ( migrateRemainingCharacteristics ) {
                        results.addAll( factorValueMigratorService.performMigrationOfRemainingOldStyleCharacteristics( fvId, mentionedOldStyleCharacteristicIds, noop ) );
                    }
                    addSuccessObject( "FactorValue #" + fvId, summarizeMigrationResults( results ) );
                } catch ( FactorValueMigratorService.MigrationFailedException e ) {
                    // skip the migrations that succeeded from appearing in the error message
                    int successfulMigrationsToSkip = 0;
                    for ( MigrationWithLineNumber m : ms ) {
                        if ( m.getMigration() == e.getMigration() ) {
                            break;
                        }
                        successfulMigrationsToSkip++;
                    }
                    // match it with the migration with line number
                    String summary = ms.stream()
                            .skip( successfulMigrationsToSkip )
                            .map( m -> String.format( "[%d] %s", m.getLineNumber(), m.getMigration() ) )
                            .collect( Collectors.joining( "\n\t" ) );
                    if ( e.getCause() instanceof IllegalArgumentException ) {
                        addErrorObject( "FactorValue #" + fvId,
                                "One or more of the following migrations failed:\n\t" + summary + "\n\t" + ExceptionUtils.getRootCauseMessage( e ) );
                    } else {
                        throw interruptMigrationProcess( ms.get( successfulMigrationsToSkip ), e );
                    }
                }
            }
        }
        if ( migrateRemainingFactorValues ) {
            try {
                factorValueMigratorService.performMigrationOfRemainingFactorValues( migrationsByFactorValue.keySet(), migrateNonTrivialCases, noop )
                        .forEach( ( fvId, stmts ) -> {
                            addSuccessObject( "FactorValue #" + fvId,
                                    summarizeMigrationResults( stmts ) );
                        } );
            } catch ( IllegalArgumentException e ) {
                addErrorObject( "Remaining FactorValues", "Failed to migrate the remaining factor values.", e );
            }
        }
    }

    private String summarizeMigrationResults( List<FactorValueMigratorService.MigrationResult> results ) {
        if ( results.isEmpty() ) {
            return "No statements were created or updated.";
        } else if ( results.size() <= 5 ) {
            return results.stream()
                    .map( r -> String.format( "%s %s", r.isCreated() ? "Created" : "Updated", r.getStatement() ) )
                    .collect( Collectors.joining( "\n" ) );
        } else {
            long created = results.stream()
                    .filter( FactorValueMigratorService.MigrationResult::isCreated )
                    .count();
            long updated = ( results.size() - created );
            if ( created > 0 && updated > 0 ) {
                return "Created " + created + " and updated " + updated + " statements";
            } else if ( created > 0 ) {
                return "Created " + created + " statements";
            } else if ( updated > 0 ) {
                return "Updated " + updated + " statements";
            } else {
                return "No statements were created or updated.";
            }
        }
    }

    @CheckReturnValue
    private Exception interruptMigrationProcess( MigrationWithLineNumber m, Exception cause ) {
        log.fatal( "A " + cause.getClass().getName() + " exception occurred, the migration process will not continue." );
        String summary = String.format( "[%d] %s", m.getLineNumber(), m.getMigration() );
        return new Exception( "The following migration failed:\n\t" + summary, cause );
    }
}
