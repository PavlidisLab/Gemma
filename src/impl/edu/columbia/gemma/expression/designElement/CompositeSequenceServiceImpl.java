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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package edu.columbia.gemma.expression.designElement;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @see edu.columbia.gemma.expression.designElement.CompositeSequenceService
 */
public class CompositeSequenceServiceImpl extends
        edu.columbia.gemma.expression.designElement.CompositeSequenceServiceBase {
    private static final Log log = LogFactory
            .getLog( edu.columbia.gemma.expression.designElement.CompositeSequenceServiceImpl.class );
    
    /**
     * @see edu.columbia.gemma.expression.designElement.CompositeSequenceService#saveCompositeSequence(edu.columbia.gemma.expression.designElement.CompositeSequence)
     */
    protected void handleSaveCompositeSequence(
            edu.columbia.gemma.expression.designElement.CompositeSequence compositeSequence )
            throws java.lang.Exception {
        // @todo implement protected void
        // handleSaveCompositeSequence(edu.columbia.gemma.expression.designElement.CompositeSequence compositeSequence)
        throw new java.lang.UnsupportedOperationException(
                "edu.columbia.gemma.expression.designElement.CompositeSequenceService.handleSaveCompositeSequence(edu.columbia.gemma.expression.designElement.CompositeSequence compositeSequence) Not implemented!" );
    }
    
    @Override
    protected Collection handleGetAllCompositeSequences() throws Exception {
        return this.getCompositeSequenceDao().findAllCompositeSequences();
    }

}