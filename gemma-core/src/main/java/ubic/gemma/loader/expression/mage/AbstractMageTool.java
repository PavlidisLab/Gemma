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
package ubic.gemma.loader.expression.mage;

import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;

/**
 * Superclass for MageML - related loading tools.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractMageTool {
    protected static Log log = LogFactory.getLog( AbstractMageTool.class.getName() );

    protected String[] mageClasses;

    protected Document simplifiedXml;

    /**
     * 
     */
    public AbstractMageTool() {
        super();
        this.mageClasses = this.getMageClassNames();
    }

    /**
     * 
     *
     */
    protected String[] getMageClassNames() {
        ResourceBundle rb = ResourceBundle.getBundle( "ubic.gemma.loader.expression.mage.mage" );
        String mageClassesString = rb.getString( "mage.classes" );
        return mageClassesString.split( ", " );
    }

    public Document getSimplifiedXml() {
        return this.simplifiedXml;
    }
}
