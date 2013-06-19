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
package ubic.gemma.web.controller.expression.experiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.expression.experiment.SessionBoundExpressionExperimentSetValueObject;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.persistence.SessionListManager;

/**
 * For fetching and manipulating ExpressionExperimentSets
 * 
 * @author paul
 * @version $Id$
 */
@Controller
@RequestMapping("/expressionExperimentSet")
public class ExpressionExperimentSetController extends BaseController {

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private SessionListManager sessionListManager;

    /**
     * AJAX adds the Expression Experiment group to the session
     * 
     * @param eeSetVos value object constructed on the client.
     * @param modificationBased whether the set was modified by the user
     * @return collection of added session groups (with updated reference.id etc)
     */
    public Collection<SessionBoundExpressionExperimentSetValueObject> addSessionGroups(
            Collection<SessionBoundExpressionExperimentSetValueObject> eeSetVos, Boolean modificationBased ) {

        Collection<SessionBoundExpressionExperimentSetValueObject> results = new HashSet<SessionBoundExpressionExperimentSetValueObject>();

        for ( SessionBoundExpressionExperimentSetValueObject eesvo : eeSetVos ) {

            results.add( sessionListManager.addExperimentSet( eesvo, modificationBased ) );
        }

        return results;
    }

    /**
     * AJAX adds the experiment group to the session
     * 
     * @param geneSetVo value object constructed on the client.
     * @return the new gene groups
     */
    public Collection<ExpressionExperimentSetValueObject> addUserAndSessionGroups(
            Collection<ExpressionExperimentSetValueObject> sets ) {

        Collection<ExpressionExperimentSetValueObject> result = new HashSet<ExpressionExperimentSetValueObject>();

        Collection<SessionBoundExpressionExperimentSetValueObject> sessionResult = new HashSet<SessionBoundExpressionExperimentSetValueObject>();

        for ( ExpressionExperimentSetValueObject eesvo : sets ) {

            if ( eesvo instanceof SessionBoundExpressionExperimentSetValueObject ) {
                sessionResult.add( ( SessionBoundExpressionExperimentSetValueObject ) eesvo );
            } else {
                result.add( eesvo );
            }

        }

        result = create( result );

        result.addAll( addSessionGroups( sessionResult, true ) );

        return result;

    }

    /**
     * AJAX returns a JSON string encoding whether the current user owns the group and whether the group is db-backed
     * 
     * @param ref reference for a gene set
     * @return
     */
    public String canCurrentUserEditGroup( ExpressionExperimentSetValueObject eesvo ) {
        boolean userCanEditGroup = false;
        boolean groupIsDBBacked = false;
        if ( !( eesvo instanceof SessionBoundExpressionExperimentSetValueObject ) ) {
            groupIsDBBacked = true;
            try {
                ExpressionExperimentSetValueObject set = expressionExperimentSetService.loadValueObject( eesvo.getId() );
                userCanEditGroup = ( set.getUserCanWrite() && set.isModifiable() );

            } catch ( org.springframework.security.access.AccessDeniedException ade ) {
                return "{groupIsDBBacked:" + groupIsDBBacked + ",userCanEditGroup:" + false + "}";
            }
        }
        return "{groupIsDBBacked:" + groupIsDBBacked + ",userCanEditGroup:" + userCanEditGroup + "}";
    }

    /**
     * @param entities
     * @return
     */
    public Collection<ExpressionExperimentSetValueObject> create(
            Collection<ExpressionExperimentSetValueObject> entities ) {

        Collection<Long> eeSetIds = new HashSet<Long>();
        for ( ExpressionExperimentSetValueObject ees : entities ) {

            if ( ees.getExpressionExperimentIds() == null || ees.getExpressionExperimentIds().isEmpty() ) {
                throw new IllegalArgumentException( "No expression experiment ids provided. Cannot save an empty set." );
            }
            ExpressionExperimentSet newEESet = this.create( ees );
            eeSetIds.add( newEESet.getId() );
        }
        return this.expressionExperimentSetService.loadValueObjects( eeSetIds );
    }

    /**
     * @param id
     * @return
     */
    public Collection<Long> getExperimentIdsInSet( Long id ) {
        ExpressionExperimentSetValueObject vo = expressionExperimentSetService.loadValueObject( id );
        if ( vo == null ) {
            throw new IllegalArgumentException( "No such set with id=" + id );
        }
        // note that this is a bit inefficient....
        return EntityUtils.getIds( expressionExperimentSetService.getExperimentValueObjectsInSet( id ) );
    }

    /**
     * AJAX
     * 
     * @param id of the set
     * @return the ExpressionExperimentSetValueObject for the id param
     * @throws IllegalArgumentException if the id param is null
     * @throws AccessDeniedException if the id param is not null but the loading function returns a null value
     */
    public ExpressionExperimentSetValueObject load( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Cannot load an experiment set with a null id." );
        }
        Collection<Long> ids = new ArrayList<Long>( 1 );
        ids.add( id );

        Collection<ExpressionExperimentSetValueObject> sets = expressionExperimentSetService.loadValueObjects( ids );

        // security.
        if ( sets == null || sets.isEmpty() ) {
            throw new AccessDeniedException( "No experiment set exists with id=" + id
                    + " or you do not have permission to access it." );
        } else if ( sets.size() > 1 ) {
            // this really shouldn't happen
            throw new AccessDeniedException( "More than one experiment set exists with id=" + id + "." );
        }
        return sets.iterator().next();
    }

    /**
     * AJAX returns all available sets that have a taxon value (so not really all) sets can have *any* number of
     * experiments
     * 
     * @return all available sets that have a taxon value
     */
    public Collection<ExpressionExperimentSetValueObject> loadAll() {
        Collection<ExpressionExperimentSetValueObject> sets = expressionExperimentSetService
                .loadAllExperimentSetValueObjects();

        return sets;
    }

    /**
     * AJAX
     * 
     * @return all available session backed sets
     */
    public Collection<SessionBoundExpressionExperimentSetValueObject> loadAllSessionGroups() {

        Collection<SessionBoundExpressionExperimentSetValueObject> sessionResults = sessionListManager
                .getAllExperimentSets();

        return sessionResults;
    }

    /**
     * AJAX
     * 
     * @return all available sets from db and also session backed sets
     */
    public Collection<ExpressionExperimentSetValueObject> loadAllUserAndSessionGroups() {

        Collection<ExpressionExperimentSetValueObject> results = loadAll();

        Collection<SessionBoundExpressionExperimentSetValueObject> sessionResults = sessionListManager
                .getAllExperimentSets();

        results.addAll( sessionResults );

        return results;
    }

    /**
     * AJAX
     * 
     * @return all available sets that have a taxon value from db and also session backed sets
     */
    public Collection<ExpressionExperimentSetValueObject> loadAllUserOwnedAndSessionGroups() {

        Collection<ExpressionExperimentSetValueObject> valueObjects = new ArrayList<ExpressionExperimentSetValueObject>();

        valueObjects.addAll( expressionExperimentSetService.loadMySetValueObjects() );
        valueObjects.addAll( sessionListManager.getAllExperimentSets() );

        return valueObjects;
    }

    /**
     * @param name
     * @return
     */
    public ExpressionExperimentSetValueObject loadByName( String name ) {
        if ( StringUtils.isBlank( name ) ) {
            throw new IllegalArgumentException( "Cannot load an experiment set with a blank name." );
        }

        Collection<ExpressionExperimentSet> sets = expressionExperimentSetService.findByName( name );
        if ( sets.isEmpty() ) {
            throw new AccessDeniedException( "No experiment set exists with name=" + name
                    + " or you do not have permission to access it." );
        }
        return expressionExperimentSetService.loadValueObject( sets.iterator().next().getId() );

    }

    /**
     * @param entities
     * @return the entities which were removed.
     */
    // TODO returning the entities that were removed is weird?
    public Collection<ExpressionExperimentSetValueObject> remove(
            Collection<ExpressionExperimentSetValueObject> entities ) {
        for ( ExpressionExperimentSetValueObject ees : entities ) {
            this.remove( ees );
        }
        return entities;
    }

    /**
     * AJAX Given a valid experiment group will remove it from the session.
     */
    public Collection<SessionBoundExpressionExperimentSetValueObject> removeSessionGroups(
            Collection<SessionBoundExpressionExperimentSetValueObject> vos ) {
        for ( SessionBoundExpressionExperimentSetValueObject experimentSetValueObject : vos ) {
            sessionListManager.removeExperimentSet( experimentSetValueObject );
        }

        return vos;
    }

    /**
     * AJAX Given valid experiment groups will remove them from the session or the database appropriately.
     */
    // TODO returning the entities that were removed is weird?
    public Collection<ExpressionExperimentSetValueObject> removeUserAndSessionGroups(
            Collection<ExpressionExperimentSetValueObject> vos ) {
        Collection<ExpressionExperimentSetValueObject> removedSets = new HashSet<ExpressionExperimentSetValueObject>();
        Collection<ExpressionExperimentSetValueObject> databaseCollection = new HashSet<ExpressionExperimentSetValueObject>();
        Collection<SessionBoundExpressionExperimentSetValueObject> sessionCollection = new HashSet<SessionBoundExpressionExperimentSetValueObject>();

        for ( ExpressionExperimentSetValueObject experimentSetValueObject : vos ) {
            if ( experimentSetValueObject instanceof SessionBoundExpressionExperimentSetValueObject ) {
                sessionCollection.add( ( SessionBoundExpressionExperimentSetValueObject ) experimentSetValueObject );
            } else {
                databaseCollection.add( experimentSetValueObject );
            }
        }

        sessionCollection = removeSessionGroups( sessionCollection );
        databaseCollection = remove( databaseCollection );

        removedSets.addAll( sessionCollection );
        removedSets.addAll( databaseCollection );

        return removedSets;
    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @RequestMapping(value = "/showExpressionExperimentSet.html", method = RequestMethod.GET)
    public ModelAndView showExpressionExperimentSet( HttpServletRequest request, HttpServletResponse response ) {

        ModelAndView mav = new ModelAndView( "expressionExperimentSet.detail" );
        StopWatch timer = new StopWatch();
        timer.start();

        ExpressionExperimentSetValueObject eesvo = getExpressionExperimentSetFromRequest( request );

        mav.addObject( "eeSetId", eesvo.getId() );
        mav.addObject( "eeSetName", eesvo.getName() );

        if ( timer.getTime() > 200 ) {
            log.info( "Show experiment set was slow: id=" + eesvo.getId() + " " + timer.getTime() + "ms" );
        }

        return mav;
    }

    /**
     * @param entities
     * @return the entities which were updated (even if they weren't actually updated)
     */
    public Collection<ExpressionExperimentSetValueObject> update(
            Collection<ExpressionExperimentSetValueObject> entities ) {
        for ( ExpressionExperimentSetValueObject ees : entities ) {

            if ( ees.getExpressionExperimentIds() == null || ees.getExpressionExperimentIds().isEmpty() ) {
                throw new IllegalArgumentException( "No expression experiment ids provided. Cannot save an empty set." );
            }
            update( ees );
        }
        return entities;
    }

    /**
     * AJAX Updates the given group (permission permitting) with the given list of memberIds. Will not allow the same
     * experiment to be added to the set twice. Will not update name or description, just members.
     * 
     * @param groupId id of the gene set being updated
     * @param eeIds
     * @return error message or null if no errors
     */
    public String updateMembers( Long groupId, Collection<Long> eeIds ) {

        return expressionExperimentSetService.updateDatabaseEntityMembers( groupId, eeIds );

    }

    /**
     * AJAX Updates the database record for the param experiment set value object (permission permitting) with the value
     * object's name and description.
     * 
     * @param eeSetVO the value object that represents the database record to update
     * @return a value object for the updated set
     */
    public ExpressionExperimentSetValueObject updateNameDesc( ExpressionExperimentSetValueObject eeSetVO ) {

        return expressionExperimentSetService.updateDatabaseEntityNameDesc( eeSetVO );

    }

    /**
     * AJAX Updates the session group. TODO move to service?
     */
    public Collection<SessionBoundExpressionExperimentSetValueObject> updateSessionGroups(
            Collection<SessionBoundExpressionExperimentSetValueObject> vos ) {
        for ( SessionBoundExpressionExperimentSetValueObject expressionExperimentSetValueObject : vos ) {
            sessionListManager.updateExperimentSet( expressionExperimentSetValueObject );
        }
        return vos;
    }

    /**
     * AJAX Updates the session group and user database groups.
     */
    public Collection<ExpressionExperimentSetValueObject> updateUserAndSessionGroups(
            Collection<ExpressionExperimentSetValueObject> vos ) {

        Collection<ExpressionExperimentSetValueObject> updatedSets = new HashSet<ExpressionExperimentSetValueObject>();
        Collection<ExpressionExperimentSetValueObject> databaseCollection = new HashSet<ExpressionExperimentSetValueObject>();
        Collection<SessionBoundExpressionExperimentSetValueObject> sessionCollection = new HashSet<SessionBoundExpressionExperimentSetValueObject>();

        for ( ExpressionExperimentSetValueObject experimentSetValueObject : vos ) {
            if ( experimentSetValueObject instanceof SessionBoundExpressionExperimentSetValueObject ) {
                sessionCollection.add( ( SessionBoundExpressionExperimentSetValueObject ) experimentSetValueObject );
            } else {
                databaseCollection.add( experimentSetValueObject );
            }
        }

        sessionCollection = updateSessionGroups( sessionCollection );
        databaseCollection = update( databaseCollection );

        updatedSets.addAll( sessionCollection );
        updatedSets.addAll( databaseCollection );

        return updatedSets;

    }

    /**
     * @param obj
     * @return
     */
    private ExpressionExperimentSet create( ExpressionExperimentSetValueObject obj ) {

        if ( obj.getId() != null && obj.getId() >= 0 ) {
            throw new IllegalArgumentException( "Should not provide an id for 'create': " + obj.getId() );
        }

        if ( StringUtils.isBlank( obj.getName() ) ) {
            throw new IllegalArgumentException( "You must provide a name" );
        }

        return expressionExperimentSetService.createFromValueObject( obj );
    }

    /**
     * @param request
     * @return
     * @throws IllegalArgumentException if a matching EE can't be loaded
     */
    private ExpressionExperimentSetValueObject getExpressionExperimentSetFromRequest( HttpServletRequest request ) {

        ExpressionExperimentSetValueObject set = null;
        Long id = null;

        if ( request.getParameter( "id" ) != null ) {
            try {
                id = Long.parseLong( request.getParameter( "id" ) );
            } catch ( NumberFormatException e ) {
                throw new IllegalArgumentException( "You must provide a valid numerical identifier" );
            }
            set = expressionExperimentSetService.loadValueObject( id );

            if ( set == null ) {
                throw new IllegalArgumentException( "Unable to access experiment set with id=" + id );
            }
        } else {
            throw new IllegalArgumentException( "You must provide an id" );
        }
        return set;
    }

    /**
     * Delete a EEset from the system.
     * 
     * @param obj
     * @return true if it was deleted.
     * @throw IllegalArgumentException it has analyses associated with it
     */
    private boolean remove( ExpressionExperimentSetValueObject obj ) {
        try {
            expressionExperimentSetService.deleteDatabaseEntity( obj );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
        return true;
    }

    /**
     * @param obj
     */
    private void update( ExpressionExperimentSetValueObject obj ) {
        try {
            expressionExperimentSetService.updateDatabaseEntity( obj );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

    }

}
