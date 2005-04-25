package edu.columbia.gemma.common.auditAndSecurity;

/**
 * An exception that is thrown by classes wanting to trap unique constraint violations. This is used to wrap Spring's
 * DataIntegrityViolationException so it's checked in the web layer.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class UserExistsException extends Exception {
    /**
     * Constructor for UserExistsException.
     * 
     * @param message
     */
    public UserExistsException( String message ) {
        super( message );
    }

}
