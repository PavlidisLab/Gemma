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
package ubic.gemma.model.expression.experiment;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.bioAssayData.ProcessedDataVectorCache;
import ubic.gemma.visualization.ExperimentalDesignVisualizationService;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.expression.experiment.ExperimentalDesignService
 */
@Service
public class ExperimentalDesignServiceImpl extends ubic.gemma.model.expression.experiment.ExperimentalDesignServiceBase {

    @Autowired
    private ExperimentalDesignVisualizationService experimentalDesignVisualizationService = null;
    
    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;
    
    @Autowired
    private ProcessedDataVectorCache processedDataVectorCache = null;


    private Log log = LogFactory.getLog( ExperimentalDesignServiceImpl.class.getName() );
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExperimentalDesignServiceBase#handleCreate(ubic.gemma.model.expression
     * .experiment.ExperimentalDesign)
     */
    @Override
    protected ExperimentalDesign handleCreate( ExperimentalDesign experimentalDesign ) {
        return this.getExperimentalDesignDao().create( experimentalDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.expression.experiment.ExperimentalDesignServiceBase#handleFind(ubic.gemma.model.expression.
     * experiment.ExperimentalDesign)
     */
    @Override
    protected ExperimentalDesign handleFind( ExperimentalDesign experimentalDesign ) {
        return this.getExperimentalDesignDao().find( experimentalDesign );
    }

    @Override
    protected ExperimentalDesign handleFindByName( String name ) {
        return this.getExperimentalDesignDao().findByName( name );
    }

    @Override
    protected ExperimentalDesign handleFindOrCreate( ExperimentalDesign experimentalDesign ) {
        return this.getExperimentalDesignDao().findOrCreate( experimentalDesign );
    }

    @Override
    protected ExpressionExperiment handleGetExpressionExperiment( ExperimentalDesign experimentalDesign ) {
        return this.getExperimentalDesignDao().getExpressionExperiment( experimentalDesign );
    }

    @Override
    protected ExperimentalDesign handleLoad( Long id ) {
        return this.getExperimentalDesignDao().load( id );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExperimentalDesignService#getExperimentalDesigns()
     */
    @Override
    protected java.util.Collection<ExperimentalDesign> handleLoadAll() {
        return ( Collection<ExperimentalDesign> ) this.getExperimentalDesignDao().loadAll();
    }

    @Override
    protected void handleUpdate( ExperimentalDesign experimentalDesign ) {
        this.getExperimentalDesignDao().update( experimentalDesign );
    }
    
    
    /**
     * Clear entries in caches relevant to experimental design for the experiment passed in. 
     * The caches cleared are the processedDataVectorCache and the caches held in 
     * ExperimentalDesignVisualizationService
     * @param eeId
     * @return msg if error occurred or empty string if successful
     */
    @Override
    public String clearDesignCaches( Long eeId ){

        if( eeId == null ) return "parameter was null";
        ExpressionExperiment ee = expressionExperimentService.load( eeId );
        if( ee == null ) return "you do not have permission to view this experiment.";
        
        log.info( "Clearing design caches for experiment: "+ee.toString() );
        
        processedDataVectorCache.clearCache( eeId );
        experimentalDesignVisualizationService.clearCaches( ee );
        return "";
    }
    
    /**
     * Clear entries in caches relevant to experimental design for the experiment passed in. 
     * The caches cleared are the processedDataVectorCache and the caches held in 
     * ExperimentalDesignVisualizationService
     * @param ee
     */
    @Override
    public void clearDesignCaches( ExpressionExperiment ee ){
        
        if( ee == null ) return;
        log.info( "Clearing design caches for experiment: "+ee.toString() );
        
        processedDataVectorCache.clearCache( ee.getId() );
        experimentalDesignVisualizationService.clearCaches( ee );
    }
    
    /**
     * Clear all entries in caches relevant to experimental design. 
     * The caches cleared are the processedDataVectorCache and the caches held in 
     * ExperimentalDesignVisualizationService
     */
    @Override
    public void clearDesignCaches( ){
        
        log.info( "Clearing all design caches." );
        
        processedDataVectorCache.clearCache();
        experimentalDesignVisualizationService.clearCaches();
    }

}