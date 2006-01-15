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
package edu.columbia.gemma.expression.designElement;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.expression.designElement.ReporterService
 */
public class ReporterServiceImpl extends edu.columbia.gemma.expression.designElement.ReporterServiceBase {

    /**
     * @see edu.columbia.gemma.expression.designElement.ReporterService#saveReporter(edu.columbia.gemma.expression.designElement.Reporter)
     */
    protected void handleSaveReporter( edu.columbia.gemma.expression.designElement.Reporter reporter )
            throws java.lang.Exception {
        this.getReporterDao().create( reporter );
    }

    @Override
    protected Reporter handleFindOrCreate( Reporter reporter ) throws Exception {
        return this.getReporterDao().findOrCreate( reporter );
    }

    @Override
    protected void handleRemove( Reporter reporter ) throws Exception {
        this.getReporterDao().remove( reporter );

    }

}