/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.loader.expression;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.analysis.expression.AnalysisUtilService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExpressionExperimentPlatformSwitchEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author Paul
 * @version $Id$
 */
@Service
public class ExperimentPlatformSwitchHelperServiceImpl implements ExperimentPlatformSwitchHelperService {
    private static Log log = LogFactory.getLog( ExperimentPlatformSwitchHelperServiceImpl.class );

    @Autowired
    private AnalysisUtilService analysisUtilService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private DesignElementDataVectorService vectorService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.loader.expression.ExperimentPlatformSwitchHelperService#persist(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @Transactional
    public void persist( ExpressionExperiment expExp, ArrayDesign arrayDesign ) {
        analysisUtilService.deleteOldAnalyses( expExp );

        expressionExperimentService.update( expExp );

        update( expExp.getRawExpressionDataVectors() );
        update( expExp.getProcessedExpressionDataVectors() ); // usually shouldn't be needed.

        audit( expExp, "Switch to use " + arrayDesign.getShortName() );

        for ( RawExpressionDataVector v : expExp.getRawExpressionDataVectors() ) {
            assert arrayDesign.equals( v.getDesignElement().getArrayDesign() );
        }

        log.info( "Done switching " + expExp );
    }

    /**
     * @param arrayDesign
     */
    private void audit( ExpressionExperiment ee, String note ) {
        AuditEventType eventType = ExpressionExperimentPlatformSwitchEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( ee, eventType, note );
    }

    private void update( Collection<? extends DesignElementDataVector> vectorsForQt ) {
        vectorService.update( vectorsForQt );
    }

}
