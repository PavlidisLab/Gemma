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
package ubic.gemma.web.taglib.displaytag.diff;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.util.AnchorTagUtil;

/**
 * Rendering of differential expression results for displaytag tables.
 * 
 * @author paul
 * @version $Id$
 */
@Deprecated
public class DiffExResultDecorator extends TableDecorator {

    public String getP() {
        DifferentialExpressionValueObject o = ( DifferentialExpressionValueObject ) getCurrentRowObject();
        return String.format( "%.3g", o.getP() );
    }

    public String getShortName() {
        DifferentialExpressionValueObject o = ( DifferentialExpressionValueObject ) getCurrentRowObject();
        return AnchorTagUtil.getExpressionExperimentLink( o.getExpressionExperiment().getId(), o
                .getExpressionExperiment().getShortName() );
    }

    public String getName() {
        DifferentialExpressionValueObject o = ( DifferentialExpressionValueObject ) getCurrentRowObject();
        return StringUtils.abbreviate( o.getExpressionExperiment().getName(), 70 );
    }

    public String getProbe() {
        DifferentialExpressionValueObject o = ( DifferentialExpressionValueObject ) getCurrentRowObject();
        return AnchorTagUtil.getProbeLink( o.getProbeId(), o.getProbe() );
    }

    public String getExperimentalFactors() {
        DifferentialExpressionValueObject o = ( DifferentialExpressionValueObject ) getCurrentRowObject();
        if ( o.getExperimentalFactors() == null ) return "";
        List<String> f = new ArrayList<String>();
        for ( ExperimentalFactorValueObject ef : o.getExperimentalFactors() ) {
            f.add( ef.getName() );
        }
        return StringUtils.join( f, ',' );
    }

}
