package edu.columbia.gemma.loader.genome.gene;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class ParserUtils {
    /**
     * @return
     * @throws ConfigurationException
     */
    // TODO generalize this with a parameter.
    public Collection initializeFileTypes( String[] keys ) throws ConfigurationException {
        Collection possibleFiles = new HashSet();
        // TODO don't hardcode.
        for ( int i = 0; i < keys.length; i++ ) {
            possibleFiles.add( keys[i] );
        }
        return possibleFiles;
    }

    /**
     * Determines if parser exists for this type of file.
     * 
     * @param filename
     * @param fileTypes
     * @return
     */
    public boolean validFile( String filename, Collection fileTypes ) {
        String[] f = StringUtils.split( filename, "\\" );
        return fileTypes.contains( f[f.length - 1] );
    }
}
