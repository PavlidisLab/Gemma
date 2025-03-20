package ubic.gemma.model.common.auditAndSecurity;

public enum AuditAction {
    /**
     * Create
     */
    C,
    /**
     * Read
     */
    R,
    /**
     * Update
     */
    U,
    /**
     * Delete
     */
    D;

    /**
     * Aliases, for readability.
     */
    public static final AuditAction
            CREATE = C,
            READ = R,
            UPDATE = U,
            DELETE = D;
}