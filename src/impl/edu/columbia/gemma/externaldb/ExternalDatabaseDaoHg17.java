package edu.columbia.gemma.externaldb;

import java.sql.SQLException;
import java.util.Collection;

import net.sf.hibernate.HibernateException;

/**
 * 
 *
 * <hr>
 * <p>Copyright (c) 2004 - 2005 Columbia University
 * @author keshav
 * @version $Id$
 */
public interface ExternalDatabaseDaoHg17 {
    
    public boolean connectToDatabase() throws HibernateException, SQLException;
    
    public Collection retreiveFromDatabase() throws HibernateException, SQLException;
}