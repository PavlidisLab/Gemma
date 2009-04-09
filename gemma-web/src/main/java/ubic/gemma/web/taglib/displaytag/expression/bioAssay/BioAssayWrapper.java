/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.web.taglib.displaytag.expression.bioAssay;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalEvent;
import ubic.gemma.model.expression.bioAssay.BioAssay;

/**
 * Used to generate hyperlinks in displaytag tables.
 * 
 * @author keshav
 * @version $Id$
 */
public class BioAssayWrapper extends TableDecorator {

    Log log = LogFactory.getLog( this.getClass() );

    /**
     * @return String
     */
    public String getNameLink() {
        BioAssay object = ( BioAssay ) getCurrentRowObject();
        String name = object.getName();

        if ( name == null ) {
            name = "No name";
        }

        return "<a href=\"/Gemma/bioAssay/showBioAssay.html?id=" + object.getId() + "\">" + name + "</a>";
    }

    /**
     * @return
     */
    public String getDelete() {
        BioAssay object = ( BioAssay ) getCurrentRowObject();

        AuditTrail auditTrail = object.getAuditTrail();
        for ( AuditEvent ae : auditTrail.getEvents() ) {
            if ( ae.getEventType() != null && ae.getEventType() instanceof SampleRemovalEvent ) {
                return "Outlier";
            }
        }

        // FIXME wire to AJAX call.
        // confirmAction is stored in global.js
        return "<form action=\"/Gemma/bioAssay/markBioAssayOutlier.html?id=" + object.getId()
                + "\" onSubmit=\"return confirmAction('Are you sure you want to flag Bioassay " + object.getName()
                + " as an outlier?')\" method=\"post\"><input title=\"Mark this sample as an outlier.\"  type=\"submit\"  value=\"Outlier\" /></form>";

    }

}
