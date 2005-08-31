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
package edu.columbia.gemma.expression.biomaterial;

import java.util.ArrayList;
import java.util.List;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.expression.biomaterial.TreatmentService
 */
public class TreatmentServiceImpl extends edu.columbia.gemma.expression.biomaterial.TreatmentServiceBase {

    /**
     * @see edu.columbia.gemma.expression.biomaterial.TreatmentService#getTreatments()
     */
    @SuppressWarnings("unchecked")
    protected java.util.List handleGetTreatments() throws java.lang.Exception {
        List<Treatment> result = new ArrayList<Treatment>();
        result.addAll( this.getTreatmentDao().loadAll() );
        return result;
    }

    /**
     * @see edu.columbia.gemma.expression.biomaterial.TreatmentService#saveTreatment(edu.columbia.gemma.expression.biomaterial.Treatment)
     */
    protected void handleSaveTreatment( edu.columbia.gemma.expression.biomaterial.Treatment treatment )
            throws java.lang.Exception {
        this.getTreatmentDao().create( treatment );
    }

}