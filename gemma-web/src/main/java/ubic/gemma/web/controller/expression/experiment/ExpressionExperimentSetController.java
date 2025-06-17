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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.SessionBoundExpressionExperimentSetValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetValueObjectHelper;
import ubic.gemma.persistence.util.IdentifiableUtils;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.controller.persistence.SessionListManager;
import ubic.gemma.web.util.EntityNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * For fetching and manipulating ExpressionExperimentSets. Methods take collections to be compatible with Store
 * interfaces.
 *
 * @author paul
 */
@Controller
@RequestMapping("/expressionExperimentSet")
public class ExpressionExperimentSetController extends BaseController {

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private ExpressionExperimentSetValueObjectHelper expressionExperimentSetValueObjectHelper;

    @Autowired
    private SessionListManager sessionListManager;

    /**
     * AJAX adds the Expression Experiment group to the session
     *
     * @param eeSetVos          value object constructed on the client.
     * @param modificationBased whether the set was modified by the user
     * @return collection of added session groups (with updated reference.id etc)
     * @deprecated
     */
    @Deprecated
    public Collection<SessionBoundExpressionExperimentSetValueObject> addSessionGroups(
            Collection<SessionBoundExpressionExperimentSetValueObject> eeSetVos, Boolean modificationBased ) {

        Collection<SessionBoundExpressionExperimentSetValueObject> results = new HashSet<>();

        for ( SessionBoundExpressionExperimentSetValueObject eesvo : eeSetVos ) {

            results.add( this.addSessionGroup( eesvo, modificationBased ) );
        }

        return results;
    }

    /**
     * AJAX adds the Expression Experiment group to the session
     */
    public SessionBoundExpressionExperimentSetValueObject addSessionGroup(
            SessionBoundExpressionExperimentSetValueObject eesvo, Boolean modificationBased ) {
        return sessionListManager.addExperimentSet( eesvo, modificationBased );
    }

    /**
     * AJAX adds the experiment group to the session
     *
     * @return the new gene groups
     */
    public Collection<ExpressionExperimentSetValueObject> addUserAndSessionGroups(
            Collection<ExpressionExperimentSetValueObject> sets ) {

        Collection<ExpressionExperimentSetValueObject> result = new HashSet<>();

        Collection<SessionBoundExpressionExperimentSetValueObject> sessionResult = new HashSet<>();

        for ( ExpressionExperimentSetValueObject eesvo : sets ) {

            if ( eesvo instanceof SessionBoundExpressionExperimentSetValueObject ) {
                sessionResult.add( ( SessionBoundExpressionExperimentSetValueObject ) eesvo );
            } else {
                result.add( eesvo );
            }

        }

        result = this.create( result );

        result.addAll( this.addSessionGroups( sessionResult, true ) );

        return result;

    }

    /**
     * AJAX returns a JSON string encoding whether the current user owns the group and whether the group is db-backed
     */
    public String canCurrentUserEditGroup( ExpressionExperimentSetValueObject eesvo ) {
        boolean userCanEditGroup = false;
        boolean groupIsDBBacked = false;
        if ( !( eesvo instanceof SessionBoundExpressionExperimentSetValueObject ) ) {
            groupIsDBBacked = true;
            try {
                ExpressionExperimentSetValueObject set = expressionExperimentSetService
                        .loadValueObject( expressionExperimentSetService.loadOrFail( eesvo.getId() ) );
                if ( set == null ) {
                    throw new IllegalArgumentException( String.format( "Failed to load VO for experiment set with ID %d", eesvo.getId() ) );
                }
                userCanEditGroup = ( set.getUserCanWrite() && set.isModifiable() );

            } catch ( org.springframework.security.access.AccessDeniedException ade ) {
                return "{groupIsDBBacked:" + true + ",userCanEditGroup:" + false + "}";
            }
        }
        return "{groupIsDBBacked:" + groupIsDBBacked + ",userCanEditGroup:" + userCanEditGroup + "}";
    }

    public Collection<ExpressionExperimentSetValueObject> create(
            Collection<ExpressionExperimentSetValueObject> entities ) {

        Collection<Long> eeSetIds = new HashSet<>();
        for ( ExpressionExperimentSetValueObject ees : entities ) {

            if ( ees.getExpressionExperimentIds() == null || ees.getExpressionExperimentIds().isEmpty() ) {
                throw new IllegalArgumentException(
                        "No expression experiment ids provided. Cannot save an empty set." );
            }
            ExpressionExperimentSet newEESet = this.create( ees );
            eeSetIds.add( newEESet.getId() );
        }
        return this.expressionExperimentSetService.loadValueObjectsByIds( eeSetIds );
    }

    public Collection<Long> getExperimentIdsInSet( Long id ) {

        if ( id == null ) {
            return new ArrayList<>();
        }

        ExpressionExperimentSetValueObject vo = expressionExperimentSetService.loadValueObjectById( id );
        if ( vo == null ) {
            throw new EntityNotFoundException( "No such set with id=" + id );
        }
        // FIXME this is a bit inefficient, for security filtering ... could have an ID-filtering interceptor.
        return IdentifiableUtils.getIds( expressionExperimentSetService.getExperimentValueObjectsInSet( id ) );
    }

    /**
     * @param limit to return only up to a given number of experiments, e.g. for a preview of the set.
     */
    public Collection<ExpressionExperimentDetailsValueObject> getExperimentsInSet( Long groupId, final Integer limit ) {

        Collection<ExpressionExperimentDetailsValueObject> experimentInSet = expressionExperimentSetService
                .getExperimentValueObjectsInSet( groupId );

        if ( limit != null && limit > 0 && limit < experimentInSet.size() ) {
            return CollectionUtils.select( experimentInSet, new Predicate<ExpressionExperimentDetailsValueObject>() {
                int i = 0;

                @Override
                public boolean evaluate( ExpressionExperimentDetailsValueObject object ) {
                    return i++ < limit;
                }
            } );
        }
        return experimentInSet;
    }

    /**
     * AJAX
     *
     * @param id of the set
     * @return the ExpressionExperimentSetValueObject for the id param
     * @throws IllegalArgumentException if the id param is null
     * @throws AccessDeniedException    if the id param is not null but the loading function returns a null value
     */
    public ExpressionExperimentSetValueObject load( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Cannot load an experiment set with a null id." );
        }
        Collection<Long> ids = new ArrayList<>( 1 );
        ids.add( id );

        Collection<ExpressionExperimentSetValueObject> sets = expressionExperimentSetService
                .loadValueObjectsByIds( ids );

        // security.
        if ( sets == null || sets.isEmpty() ) {
            throw new AccessDeniedException(
                    "No experiment set exists with id=" + id + " or you do not have permission to access it." );
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

        return expressionExperimentSetService.loadAllExperimentSetValueObjects( false );
    }

    /**
     * AJAX
     *
     * @return all available session backed sets
     */
    public Collection<SessionBoundExpressionExperimentSetValueObject> loadAllSessionGroups() {

        return sessionListManager.getAllExperimentSets();
    }

    /**
     * AJAX
     *
     * @return all available sets from db and also session backed sets
     */
    public Collection<ExpressionExperimentSetValueObject> loadAllUserAndSessionGroups() {

        Collection<ExpressionExperimentSetValueObject> results = this.loadAll();

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

        Collection<ExpressionExperimentSetValueObject> valueObjects = new ArrayList<>();

        valueObjects.addAll( expressionExperimentSetService.loadMySetValueObjects( false ) );
        valueObjects.addAll( sessionListManager.getAllExperimentSets() );

        return valueObjects;
    }

    public ExpressionExperimentSetValueObject loadByName( String name ) {
        if ( StringUtils.isBlank( name ) ) {
            throw new IllegalArgumentException( "Cannot load an experiment set with a blank name." );
        }

        Collection<ExpressionExperimentSet> sets = expressionExperimentSetService.findByName( name );
        if ( sets.isEmpty() ) {
            throw new AccessDeniedException(
                    "No experiment set exists with name=" + name + " or you do not have permission to access it." );
        }
        return expressionExperimentSetService.loadValueObjectById( sets.iterator().next().getId() );

    }

    /**
     * @return the entities which were removed.
     */
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
    public Collection<ExpressionExperimentSetValueObject> removeUserAndSessionGroups(
            Collection<ExpressionExperimentSetValueObject> vos ) {
        Collection<ExpressionExperimentSetValueObject> removedSets = new HashSet<>();
        Collection<ExpressionExperimentSetValueObject> databaseCollection = new HashSet<>();
        Collection<SessionBoundExpressionExperimentSetValueObject> sessionCollection = new HashSet<>();

        for ( ExpressionExperimentSetValueObject experimentSetValueObject : vos ) {
            if ( experimentSetValueObject instanceof SessionBoundExpressionExperimentSetValueObject ) {
                sessionCollection.add( ( SessionBoundExpressionExperimentSetValueObject ) experimentSetValueObject );
            } else {
                databaseCollection.add( experimentSetValueObject );
            }
        }

        sessionCollection = this.removeSessionGroups( sessionCollection );
        databaseCollection = this.remove( databaseCollection );

        removedSets.addAll( sessionCollection );
        removedSets.addAll( databaseCollection );

        return removedSets;
    }

    @RequestMapping(value = "/showExpressionExperimentSet.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ModelAndView showExpressionExperimentSet( @RequestParam("id") Long id ) {
        StopWatch timer = StopWatch.createStarted();
        ExpressionExperimentSetValueObject eesvo = expressionExperimentSetService.loadValueObjectById( id );
        if ( eesvo == null ) {
            throw new EntityNotFoundException( "No experiment set with ID " + id );
        }
        if ( timer.getTime() > 200 ) {
            log.info( "Show experiment set was slow: id=" + eesvo.getId() + " " + timer.getTime() + "ms" );
        }
        return new ModelAndView( "expressionExperimentSet.detail" )
                .addObject( "eeSet", eesvo );
    }

    /**
     * @return the entities which were updated (even if they weren't actually updated)
     */
    public Collection<ExpressionExperimentSetValueObject> update(
            Collection<ExpressionExperimentSetValueObject> entities ) {
        for ( ExpressionExperimentSetValueObject ees : entities ) {

            if ( ees.getExpressionExperimentIds() == null || ees.getExpressionExperimentIds().isEmpty() ) {
                throw new IllegalArgumentException(
                        "No expression experiment ids provided. Cannot save an empty set." );
            }
            this.update( ees );
        }
        return entities;
    }

    /**
     * AJAX Updates the given group (permission permitting) with the given list of memberIds. Will not allow the same
     * experiment to be added to the set twice. Will not update name or description, just members.
     *
     * @param groupId id of the gene set being updated
     * @return error message or null if no errors
     */
    @SuppressWarnings("unused") // Used in front end
    public String updateMembers( Long groupId, Collection<Long> eeIds ) {
        expressionExperimentSetValueObjectHelper.updateMembers( groupId, eeIds );
        return null; //FIXME the called method never set the string property.

    }

    /**
     * AJAX Updates the database record for the param experiment set value object (permission permitting) with the value
     * object's name and description.
     *
     * @param eeSetVO the value object that represents the database record to update
     * @return a value object for the updated set
     */
    public ExpressionExperimentSetValueObject updateNameDesc( ExpressionExperimentSetValueObject eeSetVO ) {
        return expressionExperimentSetValueObjectHelper.updateNameAndDescription( eeSetVO, false );
    }

    /**
     * AJAX Updates the session group.
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

        Collection<ExpressionExperimentSetValueObject> updatedSets = new HashSet<>();
        Collection<ExpressionExperimentSetValueObject> databaseCollection = new HashSet<>();
        Collection<SessionBoundExpressionExperimentSetValueObject> sessionCollection = new HashSet<>();

        for ( ExpressionExperimentSetValueObject experimentSetValueObject : vos ) {
            if ( experimentSetValueObject instanceof SessionBoundExpressionExperimentSetValueObject ) {
                sessionCollection.add( ( SessionBoundExpressionExperimentSetValueObject ) experimentSetValueObject );
            } else {
                databaseCollection.add( experimentSetValueObject );
            }
        }

        sessionCollection = this.updateSessionGroups( sessionCollection );
        databaseCollection = this.update( databaseCollection );

        updatedSets.addAll( sessionCollection );
        updatedSets.addAll( databaseCollection );

        return updatedSets;

    }

    private ExpressionExperimentSet create( ExpressionExperimentSetValueObject obj ) {

        if ( obj.getId() != null && obj.getId() >= 0 ) {
            throw new IllegalArgumentException( "Should not provide an id for 'create': " + obj.getId() );
        }

        if ( StringUtils.isBlank( obj.getName() ) ) {
            throw new IllegalArgumentException( "You must provide a name" );
        }

        return expressionExperimentSetValueObjectHelper.create( obj );
    }

    /**
     * Delete a EEset from the system.
     *
     * @throws IllegalArgumentException it has analyses associated with it
     */
    private void remove( ExpressionExperimentSetValueObject obj ) {
        try {
            expressionExperimentSetValueObjectHelper.delete( obj );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    private void update( ExpressionExperimentSetValueObject obj ) {
        try {
            expressionExperimentSetValueObjectHelper.update( obj );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }
}
