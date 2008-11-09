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
package ubic.gemma.model.expression.designElement;

import java.util.Collection;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.designElement.ReporterService
 */
public class ReporterServiceImpl extends ubic.gemma.model.expression.designElement.ReporterServiceBase {

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.expression.designElement.ReporterServiceBase#handleCreate(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleCreate( Collection reporters ) throws Exception {
        return this.getReporterDao().create( reporters );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.designElement.ReporterServiceBase#handleCreate(ubic.gemma.model.expression.designElement
     * .Reporter)
     */
    @Override
    protected Reporter handleCreate( Reporter reporter ) throws Exception {
        return ( Reporter ) this.getReporterDao().create( reporter );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.designElement.ReporterServiceBase#handleFind(ubic.gemma.model.expression.designElement
     * .Reporter)
     */
    @Override
    protected Reporter handleFind( Reporter reporter ) throws Exception {
        return this.getReporterDao().find( reporter );
    }

    @Override
    protected Reporter handleFindOrCreate( Reporter reporter ) throws Exception {
        return this.getReporterDao().findOrCreate( reporter );
    }

    @Override
    protected void handleRemove( Reporter reporter ) throws Exception {
        this.getReporterDao().remove( reporter );

    }

    /**
     * @see ubic.gemma.model.expression.designElement.ReporterService#saveReporter(ubic.gemma.model.expression.designElement.Reporter)
     */
    protected void handleSaveReporter( ubic.gemma.model.expression.designElement.Reporter reporter )
            throws java.lang.Exception {
        this.getReporterDao().create( reporter );
    }

}