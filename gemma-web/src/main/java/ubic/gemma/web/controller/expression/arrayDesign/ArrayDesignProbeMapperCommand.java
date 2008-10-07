/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.controller.expression.arrayDesign;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.web.controller.BaseCommand;

/**
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignProbeMapperCommand extends BaseCommand {

    /**
     * 
     */
    private static final long serialVersionUID = 8176595155628608117L;

    private boolean forceAnalysis = false;

    private ArrayDesign arrayDesign = null;

    public boolean isForceAnalysis() {
        return forceAnalysis;
    }

    public void setForceAnalysis( boolean forceAnalysis ) {
        this.forceAnalysis = forceAnalysis;
    }

    public ArrayDesign getArrayDesign() {
        return arrayDesign;
    }

    public void setArrayDesign( ArrayDesign arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

}
