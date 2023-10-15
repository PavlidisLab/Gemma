package ubic.gemma.persistence.service.expression.experiment;

import lombok.Builder;
import lombok.Value;
import ubic.gemma.model.expression.experiment.Statement;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface FactorValueMigratorService {

    /**
     * Perform the given migration.
     * @param migration a migration to perform
     * @param noop      if true, do not save the resulting statement
     * @return the created or updated statement
     */
    MigrationResult performMigration( Migration migration, boolean noop );

    /**
     * Perform multiple migrations in a single transaction.
     * @param migrations a list of migrations to perform
     * @return a list of created or updated statements
     * @throws MigrationFailedException if any of the migrations fail, the first failed migration is stored in the
     *                                  exception wrapper
     */
    List<MigrationResult> performMultipleMigrations( List<Migration> migrations, boolean noop ) throws MigrationFailedException;

    /**
     * Mark the given old-style characteristics for removal by setting their remove flag.
     *
     * @param fvId
     * @param migratedOldStyleCharacteristicIds
     * @param noop
     * @return list of created or updated statements from the given factor value
     */
    List<MigrationResult> performMigrationOfRemainingOldStyleCharacteristics( Long fvId, Set<Long> migratedOldStyleCharacteristicIds, boolean noop );

    /**
     * Migrate all remaining factor values that have not been migrated yet.
     * @param fvIds IDs of already migrated FVs
     * @return created of updated statements organized by factor value ID
     */
    Map<Long, List<MigrationResult>> performMigrationOfRemainingFactorValues( Set<Long> fvIds, boolean noop );

    @Value
    @Builder
    class Migration {
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

    @Value
    class MigrationResult {
        Long factorValueId;
        Statement statement;
        /**
         * Indicate if the statement was created as part of the migration or simply updated.
         */
        boolean created;
    }

    /**
     * Keep track of the first failed migration when multiple migrations are performed in a single transaction.
     */
    class MigrationFailedException extends RuntimeException {

        private final Migration migration;

        public MigrationFailedException( Migration migration, Throwable cause ) {
            super( cause );
            this.migration = migration;
        }

        public Migration getMigration() {
            return migration;
        }
    }
}
