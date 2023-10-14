package ubic.gemma.core.apps;

import lombok.Value;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.AbstractSpringAwareCLI;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;
import ubic.gemma.persistence.util.EntityUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.stripToNull;

/**
 * Performs the migration of old-style characteristics to statements.
 * <p>
 * The input file describes transition from a bag of {@link ubic.gemma.model.common.description.Characteristic} to more
 * structured {@link ubic.gemma.model.expression.experiment.Statement} which are allowed to have up to two related
 * objects.
 * @deprecated this will be removed as soon as all the old-style characteristics are migrated
 * @author poirigui
 */
@Deprecated
public class FactorValueMigratorCLI extends AbstractSpringAwareCLI {

    private static final String MIGRATION_FILE_OPTION = "migrationFile",
            NOOP_OPTION = "noop";

    @Autowired
    public FactorValueService factorValueService;

    /**
     * A list of migration to perform.
     */
    private List<Migration> migrations;

    private boolean noop;

    @Value
    private static class Migration {
        long lineNumber;
        Long factorValueId;

        @Nullable
        Long oldStyleCharacteristicIdUsedAsSubject;
        String category;
        String categoryUri;
        String subject;
        String subjectUri;

        // first predicate
        String predicate;
        String predicateUri;
        @Nullable
        Long oldStyleCharacteristicIdUsedAsObject;
        String object;
        String objectUri;

        // second predicate
        String secondPredicate;
        String secondPredicateUri;
        @Nullable
        Long oldStyleCharacteristicIdUsedAsSecondObject;
        String secondObject;
        String secondObjectUri;
    }

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
        options.addOption( Option.builder( MIGRATION_FILE_OPTION )
                .type( File.class )
                .desc( "File containing the migration" )
                .hasArg()
                .required()
                .build() );
        options.addOption( NOOP_OPTION, false, "Only validate migrations" );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws Exception {
        File migrationFile = ( File ) commandLine.getParsedOptionValue( MIGRATION_FILE_OPTION );
        // TODO: parse & validate migrations
        migrations = new ArrayList<>();
        try ( CSVParser parser = CSVParser.parse( migrationFile, StandardCharsets.UTF_8, CSVFormat.TDF.withHeader() ) ) {
            for ( CSVRecord row : parser ) {
                migrations.add( new Migration(
                        row.getRecordNumber(),
                        parseLongIfNonBlank( row.get( "FactorValue ID" ) ),
                        parseLongIfNonBlank( row.get( "C1 ID" ) ),
                        stripToNull( row.get( "SubjectCategory" ) ), stripToNull( row.get( "SubjectCategoryURI" ) ),
                        stripToNull( row.get( "Subject" ) ), stripToNull( row.get( "SubjectURI" ) ),
                        stripToNull( row.get( "Predicate" ) ), stripToNull( row.get( "PredicateURI" ) ),
                        parseLongIfNonBlank( row.get( "C2 ID" ) ),
                        stripToNull( row.get( "Object" ) ), stripToNull( row.get( "ObjectURI" ) ),
                        stripToNull( row.get( "SecondPredicate" ) ), stripToNull( row.get( "SecondPredicateURI" ) ),
                        parseLongIfNonBlank( row.get( "C3 ID" ) ),
                        stripToNull( row.get( "SecondObject" ) ), stripToNull( row.get( "SecondObjectURI" ) )
                ) );
            }
        }
        noop = commandLine.hasOption( NOOP_OPTION );
    }

    private Long parseLongIfNonBlank( String s ) {
        return StringUtils.isBlank( s ) ? null : Long.parseLong( StringUtils.strip( s ) );
    }

    @Override
    protected void doWork() throws Exception {
        for ( Migration migration : migrations ) {
            try {
                Statement statement = performMigration( migration );
                addSuccessObject( migration, statement != null ? "Created " + statement : "No statement was created." );
            } catch ( Exception e ) {
                addErrorObject( migration, e );
            }
        }
    }

    @Nullable
    private Statement performMigration( Migration migration ) {
        FactorValue fv = factorValueService.loadWithOldStyleCharacteristics( 1L );

        if ( fv == null ) {
            throw new IllegalArgumentException( String.format( "No FactorValue with ID %d.", migration.factorValueId ) );
        }

        Map<Long, Characteristic> oldStyleCharacteristicsById = EntityUtils.getIdMap( fv.getOldStyleCharacteristics() );

        validateMigration( migration, fv, oldStyleCharacteristicsById );

        Statement statement = new Statement();

        if ( migration.oldStyleCharacteristicIdUsedAsSubject != null ) {
            Characteristic c = oldStyleCharacteristicsById.get( migration.oldStyleCharacteristicIdUsedAsSubject );
            statement.setCategory( c.getCategory() );
            statement.setCategoryUri( c.getCategoryUri() );
            statement.setSubject( c.getValue() );
            statement.setSubjectUri( c.getValueUri() );
            statement.setOriginalValue( c.getOriginalValue() );
            statement.setEvidenceCode( c.getEvidenceCode() );
        }

        // if an old-style characteristic was used as a basis for an existing statement, we use it
        // this handles cases where migration.category, migration.categoryUri, etc. were not supplied
        statement = replaceWithSameSubjectIfExists( fv, statement );

        if ( migration.category != null )
            statement.setCategory( migration.category );
        if ( migration.categoryUri != null )
            statement.setCategoryUri( migration.categoryUri );
        if ( migration.subject != null )
            statement.setSubject( migration.subject );
        if ( migration.subjectUri != null )
            statement.setSubjectUri( migration.subjectUri );

        // if an identical migrated statement exists, we use it
        statement = replaceWithSameSubjectIfExists( fv, statement );

        statement.setSecondPredicate( migration.predicate );
        statement.setSecondPredicateUri( migration.predicateUri );

        if ( migration.oldStyleCharacteristicIdUsedAsObject != null ) {
            Characteristic c = oldStyleCharacteristicsById.get( migration.oldStyleCharacteristicIdUsedAsObject );
            statement.setObject( c.getValue() );
            statement.setObjectUri( c.getValueUri() );
        }

        if ( migration.object != null )
            statement.setObject( migration.object );
        if ( migration.objectUri != null )
            statement.setObjectUri( migration.objectUri );

        statement.setSecondPredicate( migration.secondPredicate );
        statement.setSecondPredicateUri( migration.secondPredicateUri );

        if ( migration.oldStyleCharacteristicIdUsedAsSecondObject != null ) {
            Characteristic c = oldStyleCharacteristicsById.get( migration.oldStyleCharacteristicIdUsedAsSecondObject );
            statement.setSecondObject( c.getValue() );
            statement.setSecondObjectUri( c.getValueUri() );
        }

        if ( migration.secondObject != null )
            statement.setSecondObject( migration.secondObject );
        if ( migration.secondObjectUri != null )
            statement.setSecondObjectUri( migration.secondObjectUri );

        if ( noop )
            return statement;

        return factorValueService.saveStatement( fv, statement );
    }

    /**
     * Replace the given statement with a statement that has the same category and subject from the factor value.
     * <p>
     * If no such statement can be found, the given statement is returned.
     */
    private Statement replaceWithSameSubjectIfExists( FactorValue fv, Statement statement ) {
        for ( Statement s : fv.getCharacteristics() ) {
            if ( ( s.getCategoryUri() != null ? StringUtils.equalsIgnoreCase( s.getCategoryUri(), statement.getCategoryUri() ) : StringUtils.equalsIgnoreCase( s.getCategory(), statement.getCategory() ) )
                    && ( s.getSubjectUri() != null ? StringUtils.equalsIgnoreCase( s.getSubjectUri(), statement.getSubjectUri() ) : StringUtils.equalsIgnoreCase( s.getSubject(), statement.getSubject() ) ) ) {
                log.info( "A statement with the same category and subject already exists, reusing it." );
                // make sure those are not erased
                if ( s.getOriginalValue() != null && !s.getOriginalValue().equalsIgnoreCase( statement.getOriginalValue() ) ) {
                    log.warn( String.format( "%s's original value will change: %s -> %s", s, s.getOriginalValue(), statement.getOriginalValue() ) );
                }
                if ( s.getEvidenceCode() != null && !s.getEvidenceCode().equals( statement.getEvidenceCode() ) ) {
                    log.warn( String.format( "%s's evidence code will change: %s -> %s", s, s.getEvidenceCode(), statement.getEvidenceCode() ) );
                }
                // make sure to copy over cosmetic changes (case, etc.)
                s.setCategory( statement.getCategory() );
                s.setCategoryUri( statement.getCategoryUri() );
                s.setSubject( statement.getSubject() );
                s.setSubjectUri( statement.getSubjectUri() );
                s.setOriginalValue( statement.getOriginalValue() );
                s.setEvidenceCode( statement.getEvidenceCode() );
                return s;
            }
        }
        return statement;
    }

    /**
     * Validate the given migration.
     */
    private void validateMigration( Migration migration, FactorValue fv, Map<Long, Characteristic> oldStyleCharacteristicsById ) {
        if ( migration.oldStyleCharacteristicIdUsedAsSubject != null ) {
            if ( !oldStyleCharacteristicsById.containsKey( migration.oldStyleCharacteristicIdUsedAsSubject ) ) {
                throw new IllegalArgumentException( String.format( "Old-style characteristic with ID %d is not associated with %s.", migration.oldStyleCharacteristicIdUsedAsSubject, fv ) );
            }
            Characteristic c = oldStyleCharacteristicsById.get( migration.oldStyleCharacteristicIdUsedAsObject );
            validateTerm( "category", migration.category != null ? migration.category : c.getCategory(), migration.categoryUri != null ? migration.categoryUri : c.getCategoryUri() );
            validateTerm( "subject", migration.subject != null ? migration.subject : c.getValue(), migration.subjectUri != null ? migration.subjectUri : c.getValueUri() );
        } else {
            // ensure that a valid subject is supplied
            validateTerm( "category", migration.category, migration.categoryUri );
            validateTerm( "subject", migration.subject, migration.subjectUri );
        }

        if ( migration.oldStyleCharacteristicIdUsedAsObject != null ) {
            if ( !oldStyleCharacteristicsById.containsKey( migration.oldStyleCharacteristicIdUsedAsObject ) ) {
                throw new IllegalArgumentException( String.format( "Old-style characteristic with ID %d is not associated with %s.", migration.oldStyleCharacteristicIdUsedAsSubject, fv ) );
            }
            Characteristic c = oldStyleCharacteristicsById.get( migration.oldStyleCharacteristicIdUsedAsObject );
            validateTerm( "predicate", migration.predicate, migration.predicateUri );
            validateTerm( "object", migration.object != null ? migration.object : c.getValue(), migration.objectUri != null ? migration.objectUri : c.getValueUri() );
        } else if ( migration.object != null ) {
            validateTerm( "predicate", migration.predicate, migration.predicateUri );
            validateTerm( "object", migration.object, migration.objectUri );
        } else {
            // statement has no object
            ensureTermIsNull( migration.predicate, migration.predicateUri );
            //noinspection ConstantValue
            ensureTermIsNull( migration.object, migration.objectUri );
        }

        if ( migration.oldStyleCharacteristicIdUsedAsSecondObject != null ) {
            if ( !oldStyleCharacteristicsById.containsKey( migration.oldStyleCharacteristicIdUsedAsSecondObject ) ) {
                throw new IllegalArgumentException( String.format( "Old-style characteristic with ID %d is not associated with %s.", migration.oldStyleCharacteristicIdUsedAsSubject, fv ) );
            }
            Characteristic c = oldStyleCharacteristicsById.get( migration.oldStyleCharacteristicIdUsedAsSecondObject );
            validateTerm( "second predicate", migration.secondPredicate, migration.secondPredicateUri );
            validateTerm( "second object", migration.secondObject != null ? migration.secondObject : c.getValue(), migration.secondObjectUri != null ? migration.secondObjectUri : c.getValueUri() );
        } else if ( migration.secondObject != null ) {
            // ensure that a valid second predicate & object are supplied
            validateTerm( "second predicate", migration.secondPredicate, migration.secondPredicateUri );
            validateTerm( "second object", migration.secondObject, migration.secondObjectUri );
        } else {
            // statement has no second object
            ensureTermIsNull( migration.secondPredicate, migration.secondPredicateUri );
            //noinspection ConstantValue
            ensureTermIsNull( migration.secondObject, migration.secondObjectUri );
        }
    }

    private void validateTerm( String name, String term, String termUri ) {
        if ( StringUtils.isBlank( term ) ) {
            throw new IllegalArgumentException( String.format( "A %s cannot be blank.", name ) );
        }
        if ( termUri != null && StringUtils.isBlank( termUri ) ) {
            throw new IllegalArgumentException( String.format( "A %s URI cannot be blank, although it may be null for free-text %s.", name, pluralize( name ) ) );
        }
        if ( term.length() > 255 ) {
            throw new IllegalArgumentException( String.format( "A %s cannot exceed 255 characters", name ) );
        }
        if ( termUri != null && termUri.length() > 255 ) {
            throw new IllegalArgumentException( String.format( "A %s URI cannot exceed 255 characters", name ) );
        }
    }

    private String pluralize( String s ) {
        if ( s.endsWith( "y" ) ) {
            return s.substring( 0, s.length() - 1 ) + "ies";
        } else {
            return s + "s";
        }
    }

    private void ensureTermIsNull( String term, String termUri ) {
        if ( term != null ) {
            throw new IllegalArgumentException( "Term must be blank." );
        }
        if ( termUri != null ) {
            throw new IllegalArgumentException( "Term URI must be blank." );
        }
    }
}
