/**
 * Register gene sets on the server, in the user's session.
 * 
 * @author anton
 */
Ext.namespace( 'Gemma' );

Gemma.SessionSetsUtils = {

   /**
    * if using a just-made-by-user or GO group, register the geneSetValObj as a set in the session
    * 
    * @memberOf Gemma.SessionSetUtils
    * @returns {Promise}
    * @static
    */
   registerGeneSetIfNotRegistered : function( geneSet ) {
      var defered = RSVP.defer();

      if ( typeof geneSet === 'undefined' || geneSet === null || geneSet.id != -1 ) {
         defered.resolve( geneSet );
      } else {

         GeneSetController.addSessionGroup( geneSet, false, function( registeredGeneSet ) {
            if ( registeredGeneSet === null ) {
               Ext.Msg.alert( 'Error', 'There was a problem registering the gene set on the server' );
               defered.reject( 'There was a problem registering the gene set on the server' );
            } else {
               defered.resolve( registeredGeneSet );
            }

         } );
      }
      return defered.promise;

   },

   /**
    * 
    * @param experimentSet
    * @returns {Promise}
    * @static
    */
   registerExperimentSetIfNotRegistered : function( esvo ) {
      var defered = new RSVP.defer();
      if ( typeof esvo === 'undefined' || esvo === null || esvo.id != -1 ) {
         return defered.resolve( esvo );
      } else {

         ExpressionExperimentSetController.addSessionGroup( esvo, false, function( registeredESVO ) {
            if ( registeredESVO === null ) {
               Ext.Msg.alert( 'Error', 'There was a problem registering the experiment set on the server' );
               defered.reject( 'There was a problem registering the experiment set on the server' );
            } else {
               defered.resolve( registeredESVO );
            }
         } );
      }
      return defered.promise;
   }
};