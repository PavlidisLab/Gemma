/*
 * The Gemma_sec1 project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.apps;

import java.util.Collection;

import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService;
import ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.common.auditAndSecurity.Person;
import ubic.gemma.model.common.auditAndSecurity.PersonService;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserService;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.description.LocalFileService;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.protocol.ProtocolService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Touch all Securables to trigger initialization of ACLs.
 * 
 * @author paul
 * @version $Id$
 */
public class TouchAclCli extends AbstractSpringAwareCLI {

    /*
     * (non-Javadoc)
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {

        super.processCommandLine( "foo", args );

        DifferentialExpressionAnalysisService deas = this.ctx.getBean( DifferentialExpressionAnalysisService.class );
        Collection<DifferentialExpressionAnalysis> diffs = deas.loadAll();
        for ( DifferentialExpressionAnalysis o : diffs ) {
            log.info( "Touch: " + o );
            deas.update( o );
        }

        /*
         * Get all the services we need..
         */
        ExpressionExperimentService e = this.ctx.getBean( ExpressionExperimentService.class );
        Collection<ExpressionExperiment> ees = e.loadAll();
        for ( ExpressionExperiment o : ees ) {
            log.info( "Touch: " + o );
            e.update( o );

            for ( ExpressionAnalysisResultSet a : deas.getResultSets( o ) ) {
                log.info( "Touch: " + o );
                deas.update( a );
            }

        }

        ArrayDesignService a = this.ctx.getBean( ArrayDesignService.class );
        Collection<ArrayDesign> as = a.loadAll();
        for ( ArrayDesign o : as ) {
            log.info( "Touch: " + o );
            a.update( o );
        }

        UserService u = this.ctx.getBean( UserService.class );
        Collection<User> us = u.loadAll();
        for ( User o : us ) {
            log.info( "Touch: " + o );
            u.update( o );
        }

        PersonService p = this.ctx.getBean( PersonService.class );
        Collection<Person> ps = p.loadAll();
        for ( Person pr : ps ) {
            log.info( "Touch: " + pr );
            p.update( pr );
        }

        GeneCoexpressionAnalysisService gcas = this.ctx.getBean( GeneCoexpressionAnalysisService.class );
        Collection<GeneCoexpressionAnalysis> analyses = gcas.loadAll();
        for ( GeneCoexpressionAnalysis o : analyses ) {
            log.info( "Touch: " + o );
            gcas.update( o );
        }

        ProbeCoexpressionAnalysisService pcas = this.ctx.getBean( ProbeCoexpressionAnalysisService.class );
        Collection<ProbeCoexpressionAnalysis> panalyses = pcas.loadAll();
        for ( ProbeCoexpressionAnalysis o : panalyses ) {
            log.info( "Touch: " + o );
            pcas.update( o );
        }

        LocalFileService l = this.ctx.getBean( LocalFileService.class );
        Collection<LocalFile> lf = l.loadAll();
        for ( LocalFile o : lf ) {
            log.info( "Touch: " + o );
            l.update( o );
        }

        ProtocolService prs = this.ctx.getBean( ProtocolService.class );
        Collection<Protocol> pr = prs.loadAll();
        for ( Protocol o : pr ) {
            log.info( "Touch: " + o );
            prs.update( o );
        }

        ExpressionExperimentSetService eess = this.ctx.getBean( ExpressionExperimentSetService.class );
        Collection<ExpressionExperimentSet> esets = eess.loadAll();
        for ( ExpressionExperimentSet o : esets ) {
            log.info( "Touch: " + o );
            eess.update( o );
        }

        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        TouchAclCli c = new TouchAclCli();
        c.doWork( args );

    }

}
