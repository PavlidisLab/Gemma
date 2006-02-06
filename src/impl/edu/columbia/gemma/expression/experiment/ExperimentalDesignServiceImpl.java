/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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
package edu.columbia.gemma.expression.experiment;

/**
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.expression.experiment.ExperimentalDesignService
 */
public class ExperimentalDesignServiceImpl extends
        edu.columbia.gemma.expression.experiment.ExperimentalDesignServiceBase {

    /**
     * @see edu.columbia.gemma.expression.experiment.ExperimentalDesignService#getExperimentalDesigns()
     */
    @Override
    protected java.util.Collection handleLoadAll() throws java.lang.Exception {
        return this.getExperimentalDesignDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.experiment.ExperimentalDesignServiceBase#handleCreate(edu.columbia.gemma.expression.experiment.ExperimentalDesign)
     */
    @Override
    protected ExperimentalDesign handleCreate( ExperimentalDesign experimentalDesign ) throws Exception {
        return ( ExperimentalDesign ) this.getExperimentalDesignDao().create( experimentalDesign );
    }
}