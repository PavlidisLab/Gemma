package ubic.gemma.persistence.service.expression.experiment;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.util.IdentifiableUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@CommonsLog
@Deprecated
public class FactorValueMigratorServiceImpl implements FactorValueMigratorService {

    @Autowired
    private FactorValueService factorValueService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Override
    @Transactional
    public MigrationResult performMigration( Migration migration, boolean noop ) {
        if ( migration.getFactorValueId() == null ) {
            throw new IllegalArgumentException( "The FactorValue ID cannot be null." );
        }

        FactorValue fv = factorValueService.loadWithOldStyleCharacteristics( migration.getFactorValueId(), noop );

        if ( fv == null ) {
            throw new IllegalArgumentException( String.format( "No FactorValue with ID %d.", migration.getFactorValueId() ) );
        }

        Map<Long, Characteristic> oldStyleCharacteristicsById = IdentifiableUtils.getIdMap( fv.getOldStyleCharacteristics() );

        validateMigration( migration, fv, oldStyleCharacteristicsById );

        Statement statement;

        if ( migration.getOldStyleCharacteristicIdUsedAsSubject() != null ) {
            Characteristic c = oldStyleCharacteristicsById.get( migration.getOldStyleCharacteristicIdUsedAsSubject() );
            // if the characteristic was already migrated
            statement = findStatementByCategoryAndSubject( fv, c )
                    .orElseGet( Statement::new );
            if ( c.isMigratedToStatement() && statement.getId() == null ) {
                log.warn( c + " claims to be migrated to statement, but there is no statement with its category/subject." );
            }
            populateFromOldStyleCharacteristic( statement, c );
            c.setMigratedToStatement( true );
            log.debug( c + " has been migrated to a statement subject." );
        } else {
            statement = new Statement();
        }

        if ( migration.getCategory() != null ) {
            statement.setCategory( migration.getCategory() );
            statement.setCategoryUri( migration.getCategoryUri() );
        } else if ( migration.getCategoryUri() != null ) {
            statement.setCategoryUri( migration.getCategoryUri() );
        }

        if ( migration.getSubject() != null ) {
            statement.setSubject( migration.getSubject() );
            statement.setSubjectUri( migration.getSubjectUri() );
        } else if ( migration.getSubjectUri() != null ) {
            statement.setSubjectUri( migration.getSubjectUri() );
        }

        // if an identical statement exists, reuse it instead of creating a duplicate
        statement = findStatementByCategoryAndSubject( fv, statement ).orElse( statement );

        statement.setPredicate( migration.getPredicate() );
        statement.setPredicateUri( migration.getPredicateUri() );

        if ( migration.getOldStyleCharacteristicIdUsedAsObject() != null ) {
            Characteristic c = oldStyleCharacteristicsById.get( migration.getOldStyleCharacteristicIdUsedAsObject() );
            statement.setObject( c.getValue() );
            statement.setObjectUri( c.getValueUri() );
            c.setMigratedToStatement( true );
            log.debug( c + " has been migrated to a statement object." );
        }

        if ( migration.getObject() != null ) {
            statement.setObject( migration.getObject() );
            statement.setObjectUri( migration.getObjectUri() );
        } else if ( statement.getObjectUri() != null ) {
            statement.setObjectUri( migration.getObjectUri() );
        }

        statement.setSecondPredicate( migration.getSecondPredicate() );
        statement.setSecondPredicateUri( migration.getSecondPredicateUri() );

        if ( migration.getOldStyleCharacteristicIdUsedAsSecondObject() != null ) {
            Characteristic c = oldStyleCharacteristicsById.get( migration.getOldStyleCharacteristicIdUsedAsSecondObject() );
            statement.setSecondObject( c.getValue() );
            statement.setSecondObjectUri( c.getValueUri() );
            c.setMigratedToStatement( true );
            log.debug( c + " has been migrated to a statement second object." );
        }

        if ( migration.getSecondObject() != null ) {
            statement.setSecondObject( migration.getSecondObject() );
            statement.setSecondObjectUri( migration.getSecondObjectUri() );
        } else if ( migration.getSecondObjectUri() != null ) {
            statement.setSecondObjectUri( migration.getSecondObjectUri() );
        }

        boolean isTransient = statement.getId() == null;

        log.debug( String.format( "FactorValue #%d: %s %s", fv.getId(), isTransient ? "Created" : "Updated", statement ) );

        return new MigrationResult( fv.getId(), noop ? statement : factorValueService.saveStatementIgnoreAcl( fv, statement ), isTransient );
    }

    @Override
    @Transactional
    public List<MigrationResult> performMultipleMigrations( List<Migration> migrations, boolean noop ) throws MigrationFailedException {
        List<MigrationResult> result = new ArrayList<>( migrations.size() );
        for ( Migration migration : migrations ) {
            try {
                result.add( performMigration( migration, noop ) );
            } catch ( Exception e ) {
                throw new MigrationFailedException( migration, e );
            }
        }
        return result;
    }

    @Override
    @Transactional
    public List<MigrationResult> performMigrationOfRemainingOldStyleCharacteristics( Long fvId, Set<Long> migratedOldStyleCharacteristicIds, boolean noop ) {
        FactorValue fv = factorValueService.loadWithOldStyleCharacteristics( fvId, noop );

        if ( fv == null ) {
            throw new IllegalArgumentException( String.format( "No FactorValue with ID %d.", fvId ) );
        }

        List<MigrationResult> results = performMigrationOfRemainingOldStyleCharacteristics( fv, migratedOldStyleCharacteristicIds, false, noop );

        if ( !results.isEmpty() && !fv.getNeedsAttention() ) {
            String note = "Marked as needs attention because some if its old-style characteristics were automatically migrated.";
            if ( !noop ) {
                factorValueService.markAsNeedsAttention( fv, note );
            }
            log.warn( "FactorValue #" + fv.getId() + ": " + note );
        }

        return results;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public Map<Long, List<MigrationResult>> performMigrationOfRemainingFactorValues( Set<Long> migratedFactorValueIds, boolean migrateNonTrivialCases, boolean noop ) {
        Map<Long, List<MigrationResult>> result = new HashMap<>();
        long total = factorValueService.countAll();
        // this way we avoid loading old-style characteristics unless we need to;
        if ( total > 0 ) {
            log.info( String.format( "Loading %d factor values that haven't been migrated yet...",
                    total - migratedFactorValueIds.size() ) );
        }
        Map<Long, Integer> fvs = factorValueService.loadIdsWithNumberOfOldStyleCharacteristics( migratedFactorValueIds );
        if ( fvs.isEmpty() ) {
            log.info( "There are no more factor values to migrate." );
            return result;
        }
        TransactionTemplate tt = new TransactionTemplate( platformTransactionManager );
        int index = 0;
        int done = 0;
        int remaining = fvs.size();
        Map<Integer, List<Long>> fvToWarnAboutBySize = new HashMap<>();
        StopWatch timer = StopWatch.createStarted();
        log.info( String.format( "Migrating %d out of %d remaining factor values...", remaining, total ) );
        for ( Map.Entry<Long, Integer> e : fvs.entrySet() ) {
            Long fvId = e.getKey();
            Integer numberOfOldStyleCharacteristics = e.getValue();
            if ( numberOfOldStyleCharacteristics == 0 ) {
                log.debug( String.format( "FactorValue #%d doesn't have any old-style characteristics, no migration needed!", fvId ) );
                remaining--;
            } else if ( numberOfOldStyleCharacteristics == 1 ) {
                result.computeIfAbsent( fvId, k -> new ArrayList<>() )
                        .addAll( tt.execute( ts -> {
                            FactorValue fv = factorValueService.loadWithOldStyleCharacteristics( fvId, noop );
                            if ( fv == null ) {
                                log.warn( String.format( "No FactorValue with ID %d.", fvId ) );
                                return Collections.emptyList();
                            }
                            return performMigrationOfRemainingOldStyleCharacteristics( fv, null, false, noop );
                        } ) );
                done++;
            } else if ( migrateNonTrivialCases ) {
                result.computeIfAbsent( fvId, k -> new ArrayList<>() )
                        .addAll( tt.execute( ts -> {
                            FactorValue fv = factorValueService.loadWithOldStyleCharacteristics( fvId, noop );
                            if ( fv == null ) {
                                log.warn( String.format( "No FactorValue with ID %d.", fvId ) );
                                return Collections.emptyList();
                            }
                            // we need to skip characteristics flagged as already migrated because they might have
                            // already been covered by a curator-supplied migration
                            List<MigrationResult> results = performMigrationOfRemainingOldStyleCharacteristics( fv, null, true, noop );
                            if ( !results.isEmpty() && !fv.getNeedsAttention() ) {
                                String note = String.format( "Marked as needs attention because %d of its old-style characteristics were automatically migrated to subject-only statements.", results.size() );
                                if ( !noop ) {
                                    factorValueService.markAsNeedsAttention( fv, note );
                                }
                                log.warn( "FactorValue #" + fv.getId() + ": " + note );
                            }
                            return results;
                        } ) );
            } else {
                // 2 or more old-style characteristics, those should have been in the migration file
                log.debug( String.format( "FactorValue #%d has %d old-style characteristics and was missing from the migration file.",
                        fvId, numberOfOldStyleCharacteristics ) );
                fvToWarnAboutBySize
                        .computeIfAbsent( numberOfOldStyleCharacteristics, k -> new ArrayList<>() )
                        .add( fvId );
                remaining--;
            }
            if ( ( ++index % 100 ) == 0 ) {
                warnUnsupportedFactorValues( fvToWarnAboutBySize );
                log.info( String.format( "Migrated %d/%d factor values so far %f fv/s...", done, remaining,
                        1000.0 * done / timer.getTime( TimeUnit.MILLISECONDS ) ) );
            }
        }
        warnUnsupportedFactorValues( fvToWarnAboutBySize );
        log.info( String.format( "Migrated %d factor values in %d ms.", done, timer.getTime( TimeUnit.MILLISECONDS ) ) );
        return result;
    }

    /**
     * Perform the migration of the remaining old-style characteristics of a given factor value.
     * <p>
     * This is an automated step: each old-style characteristic is migrated as a subject-only statement.
     * @param fv                                a factor value to migrate
     * @param migratedOldStyleCharacteristicIds a set of already migrated old-style characteristics to be ignored, a
     *                                          warning is produced if an old-style characteristic is marked as migrated
     *                                          to statement and not present in that set, or null to ignore
     * @param skipAlreadyMigrated               skip characteristics that are flagged as already migrated to statement
     *                                          as per {@link Characteristic#isMigratedToStatement()}
     * @param noop                              noop mode: the factor value and characteristics are loaded in read-only
     *                                          mode and no changes are persisted
     * @return the result of the migration of each old-style characteristic
     */
    private List<MigrationResult> performMigrationOfRemainingOldStyleCharacteristics( FactorValue fv, @Nullable Set<Long> migratedOldStyleCharacteristicIds, boolean skipAlreadyMigrated, boolean noop ) {
        List<MigrationResult> result = new ArrayList<>();
        for ( Characteristic c : fv.getOldStyleCharacteristics() ) {
            if ( skipAlreadyMigrated && c.isMigratedToStatement() ) {
                log.debug( "FactorValue #" + fv.getId() + ": Ignoring " + c + " as it was already migrated to a statement." );
                continue;
            }
            if ( migratedOldStyleCharacteristicIds != null && migratedOldStyleCharacteristicIds.contains( c.getId() ) ) {
                log.debug( "FactorValue #" + fv.getId() + ": Ignoring " + c + " as it was already migrated to a statement." );
                continue;
            }
            if ( migratedOldStyleCharacteristicIds != null && c.isMigratedToStatement() ) {
                log.warn( "FactorValue #" + fv.getId() + ": " + c + " has already been migrated to a statement, but its ID is not in the set of migrated old-style characteristics." );
            }
            // convert to simple statement
            Statement statement = findStatementByCategoryAndSubject( fv, c )
                    .orElseGet( Statement::new );
            populateFromOldStyleCharacteristic( statement, c );
            c.setMigratedToStatement( true );
            boolean isTransient = fv.getId() == null;
            log.debug( "FactorValue #" + fv.getId() + ": " + ( isTransient ? "Created" : "Updated" ) + statement );
            result.add( new MigrationResult( fv.getId(), noop ? statement : factorValueService.saveStatementIgnoreAcl( fv, statement ), isTransient ) );
        }
        if ( result.isEmpty() ) {
            log.debug( "FactorValue #" + fv.getId() + ": No old-style characteristics to migrate." );
        }
        return result;
    }

    private void warnUnsupportedFactorValues( Map<Integer, List<Long>> fvToWarnAboutBySize ) {
        for ( Map.Entry<Integer, List<Long>> entry : fvToWarnAboutBySize.entrySet() ) {
            if ( !entry.getValue().isEmpty() ) {
                log.warn( String.format( "%d factor values have %d old-style characteristics and were not already migrated: %s",
                        entry.getValue().size(), entry.getKey(),
                        entry.getValue().stream().map( String::valueOf ).collect( Collectors.joining( ", " ) ) ) );
                entry.getValue().clear();
            }
        }
    }

    /**
     * Populate a statement with the data from an old-style characteristic.
     */
    private void populateFromOldStyleCharacteristic( Statement statement, Characteristic c ) {
        // make sure those are not erased
        if ( statement.getOriginalValue() != null && !StringUtils.equalsIgnoreCase( statement.getOriginalValue(), c.getOriginalValue() ) ) {
            log.warn( String.format( "%s's original value will change: %s -> %s", statement, statement.getOriginalValue(), c.getOriginalValue() ) );
        }
        if ( statement.getEvidenceCode() != null && !Objects.equals( statement.getEvidenceCode(), c.getEvidenceCode() ) ) {
            log.warn( String.format( "%s's evidence code will change: %s -> %s", statement, statement.getEvidenceCode(), c.getEvidenceCode() ) );
        }
        // make sure to copy over cosmetic changes (case, etc.)
        statement.setCategory( c.getCategory() );
        statement.setCategoryUri( c.getCategoryUri() );
        statement.setSubject( c.getValue() );
        statement.setSubjectUri( c.getValueUri() );
        statement.setOriginalValue( c.getOriginalValue() );
        statement.setEvidenceCode( c.getEvidenceCode() );
    }

    /**
     * Find a statement that shares the same category/subject of a given old-style characteristic.
     */
    private Optional<Statement> findStatementByCategoryAndSubject( FactorValue fv, Characteristic c ) {
        for ( Statement s : fv.getCharacteristics() ) {
            if ( ( s.getCategoryUri() != null ? StringUtils.equalsIgnoreCase( s.getCategoryUri(), c.getCategoryUri() ) : StringUtils.equalsIgnoreCase( s.getCategory(), c.getCategory() ) )
                    && ( s.getSubjectUri() != null ? StringUtils.equalsIgnoreCase( s.getSubjectUri(), c.getValueUri() ) : StringUtils.equalsIgnoreCase( s.getSubject(), c.getValue() ) ) ) {
                log.debug( "FactorValue #" + fv.getId() + ": A statement with the same category and subject already exists, reusing it." );
                return Optional.of( s );
            }
        }
        return Optional.empty();
    }

    /**
     * Validate the given migration.
     */
    private void validateMigration( Migration migration, FactorValue fv, Map<Long, Characteristic> oldStyleCharacteristicsById ) {
        if ( migration.getOldStyleCharacteristicIdUsedAsSubject() != null ) {
            if ( !oldStyleCharacteristicsById.containsKey( migration.getOldStyleCharacteristicIdUsedAsSubject() ) ) {
                throw new IllegalArgumentException( String.format( "Old-style characteristic with ID %d is not associated with %s.", migration.getOldStyleCharacteristicIdUsedAsSubject(), fv ) );
            }
            Characteristic c = oldStyleCharacteristicsById.get( migration.getOldStyleCharacteristicIdUsedAsSubject() );
            validateTerm( migration.getCategory() != null ? migration.getCategory() : c.getCategory(), migration.getCategoryUri() != null ? migration.getCategoryUri() : c.getCategoryUri(), "category" );
            validateTerm( migration.getSubject() != null ? migration.getSubject() : c.getValue(), migration.getSubjectUri() != null ? migration.getSubjectUri() : c.getValueUri(), "subject" );
        } else {
            // ensure that a valid subject is supplied
            validateTerm( migration.getCategory(), migration.getCategoryUri(), "category" );
            validateTerm( migration.getSubject(), migration.getSubjectUri(), "subject" );
        }

        if ( migration.getOldStyleCharacteristicIdUsedAsObject() != null ) {
            if ( !oldStyleCharacteristicsById.containsKey( migration.getOldStyleCharacteristicIdUsedAsObject() ) ) {
                throw new IllegalArgumentException( String.format( "Old-style characteristic with ID %d is not associated with %s.", migration.getOldStyleCharacteristicIdUsedAsObject(), fv ) );
            }
            Characteristic c = oldStyleCharacteristicsById.get( migration.getOldStyleCharacteristicIdUsedAsObject() );
            validateTerm( migration.getPredicate(), migration.getPredicateUri(), "predicate" );
            validateTerm( migration.getObject() != null ? migration.getObject() : c.getValue(), migration.getObjectUri() != null ? migration.getObjectUri() : c.getValueUri(), "object" );
        } else if ( migration.getObject() != null ) {
            validateTerm( migration.getPredicate(), migration.getPredicateUri(), "predicate" );
            validateTerm( migration.getObject(), migration.getObjectUri(), "object" );
        } else {
            // statement has no object
            ensureTermIsNull( migration.getPredicate(), migration.getPredicateUri(), "There is no object, the %s must be blank.", "predicate" );
        }

        if ( migration.getOldStyleCharacteristicIdUsedAsSecondObject() != null ) {
            if ( !oldStyleCharacteristicsById.containsKey( migration.getOldStyleCharacteristicIdUsedAsSecondObject() ) ) {
                throw new IllegalArgumentException( String.format( "Old-style characteristic with ID %d is not associated with %s.", migration.getOldStyleCharacteristicIdUsedAsSecondObject(), fv ) );
            }
            Characteristic c = oldStyleCharacteristicsById.get( migration.getOldStyleCharacteristicIdUsedAsSecondObject() );
            validateTerm( migration.getSecondPredicate(), migration.getSecondPredicateUri(), "second predicate" );
            validateTerm( migration.getSecondObject() != null ? migration.getSecondObject() : c.getValue(), migration.getSecondObjectUri() != null ? migration.getSecondObjectUri() : c.getValueUri(), "second object" );
        } else if ( migration.getSecondObject() != null ) {
            // ensure that a valid second predicate & object are supplied
            validateTerm( migration.getSecondPredicate(), migration.getSecondPredicateUri(), "second predicate" );
            validateTerm( migration.getSecondObject(), migration.getSecondObjectUri(), "second object" );
        } else {
            // statement has no second object
            ensureTermIsNull( migration.getSecondPredicate(), migration.getSecondPredicateUri(), "There is no second object, the %s must be blank.", "second predicate" );
        }
    }

    private void validateTerm( String term, @Nullable String termUri, String name ) {
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

    private void ensureTermIsNull( @Nullable String term, @Nullable String termUri, String message, String name ) {
        if ( term != null ) {
            throw new IllegalArgumentException( String.format( message, name ) );
        }
        if ( termUri != null ) {
            throw new IllegalArgumentException( String.format( message, name + " URI" ) );
        }
    }
}
