/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package edu.columbia.gemma.genome.sequenceAnalysis;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.sequence.sequenceAnalysis.BlatResult
 */
public class BlatResultImpl extends edu.columbia.gemma.genome.sequenceAnalysis.BlatResult {

    private String targetName;
    private String queryName;
    private int querySize;

    /**
     * @deprecated - only here temporarily
     * @return
     */
    public String getQueryName() {
        return this.queryName;
    }

    /**
     * @deprecated - only here temporarily
     * @param queryName
     */
    public void setQueryName( String queryName ) {
        this.queryName = queryName;
    }

    /**
     * @deprecated - only here temporarily
     * @return
     */
    public String getTargetName() {
        return this.targetName;
    }

    /**
     * @deprecated - only here temporarily
     * @param targetName
     */
    public void setTargetName( String targetName ) {
        this.targetName = targetName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.sequence.sequenceAnalysis.BlatResult#score()
     */
    public double score() {
        return ( double ) this.getMatches() / ( double ) this.getQuerySize();
    }

    /**
     * @deprecated - only here temporarily
     * @param i
     */
    public void setQuerySize( int i ) {
        this.querySize = i;

    }

    /**
     * @deprecated - only here temporarily
     * @param i
     */
    public int getQuerySize() {
        return this.querySize;
    }

}