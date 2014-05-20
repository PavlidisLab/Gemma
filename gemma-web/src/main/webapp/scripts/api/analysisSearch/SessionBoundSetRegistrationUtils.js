/**
 * Register gene sets on the server, in the user's session.
 * 
 * @author anton
 */
Ext.namespace( 'Gemma' );

Gemma.SessionSetsUtils = {

   /**
    * @memberOf Gemma.SessionSetUtils
    */
   registerGeneSetIfNotRegistered : function( geneSet ) {
      var defered = RSVP.defer();

      // if using a GO group or 'all results' group for a search, register the geneSetValObj as a set in the session

      if ( typeof geneSet !== 'undefined' && (geneSet.id === null || geneSet.id === -1) ) {
         var geneSetClone = Object.clone( geneSet );
         delete geneSetClone.memberIds;
         // no memberIds field in a geneSetValueObject (????)
         // but this object would have the field if it was a GO group object
         // (this is a short cut fix, a better fix would be to make a new GSVO from the fields)

         GeneSetController.addSessionGroup( geneSetClone, false, function( registeredGeneSet ) {
            if ( registeredGeneSet === null ) {
               Ext.Msg.alert( 'Error', 'There was a problem registering the gene set on the server' );
               defered.reject( 'There was a problem registering the gene set on the server' );
            } else {
               defered.resolve( registeredGeneSet );
            }

         } );
      } else {
         defered.resolve( geneSet );
      }

      return defered.promise;
   },

   /**
    * 
    * @param experimentSets
    * @returns
    */
   registerExperimentSetIfNotRegistered : function( experimentSet ) {
      var defered = new RSVP.defer();

      var esvo = experimentSet;
      // if the group has a null value for id, then it hasn't been
      // created as a group in the database nor session
      if ( typeof esvo !== 'undefined' && (esvo.id === -1 || esvo.id === null) ) {

         ExpressionExperimentSetController.addSessionGroup( esvo, false, function( registeredESVO ) {
            if ( registeredESVO === null ) {
               Ext.Msg.alert( 'Error', 'There was a problem registering the experiment set on the server' );
               defered.reject( 'There was a problem registering the experiment set on the server' );
            } else {
               defered.resolve( registeredESVO );
            }
         } );
      } else {
         defered.resolve( esvo );
      }

      return defered.promise;
   }
};