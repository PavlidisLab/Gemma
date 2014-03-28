/**
 * TODO document me
 * 
 * @author anton
 */
Ext.namespace( 'Gemma' );

Gemma.SessionSetsUtils = {

   registerGeneSetsIfNotRegistered : function( geneSets ) {
      var defered = RSVP.defer();

      // if using a GO group or 'all results' group for a search, register the geneSetValObj as a set in the session
      var geneSetsAlreadyRegistered = [];
      var geneSetsToRegister = [];
      var i, j;
      var geneSet;
      for (i = 0; i < geneSets.length; i++) {
         geneSet = geneSets[i];
         if ( typeof geneSet !== 'undefined' && (geneSet.id === null || geneSet.id === -1) ) {
            // addSessionGroups() takes a geneSetValueObject (and boolean for isModificationBased)
            var geneSetClone = Object.clone( geneSet );
            delete geneSetClone.memberIds;
            // no memberIds field in a geneSetValueObject
            // but this object would have the field if it was a GO group object
            // (this is a short cut fix, a better fix would be to make a new GSVO from the fields)
            geneSetsToRegister.push( geneSetClone );
         } else {
            geneSetsAlreadyRegistered.push( geneSet );
         }
      }
      if ( geneSetsToRegister.length > 0 ) {
         geneSets = geneSetsAlreadyRegistered;
         GeneSetController.addSessionGroups( geneSetsToRegister, false, function( registeredGeneSets ) {
            // should be at least one geneset
            if ( registeredGeneSets === null || registeredGeneSets.length === 0 ) {
               // TODO error message
               return;
            } else {
               for (j = 0; j < registeredGeneSets.length; j++) {
                  geneSets.push( registeredGeneSets[j] );
               }
            }
            defered.resolve( geneSets );
         } );
      } else {
         defered.resolve( geneSets );
      }

      return defered.promise;
   },

   registerExperimentSetsIfNotRegistered : function( experimentSets ) {
      var defered = new RSVP.defer();

      var experimentGroupsToRegister = [];
      var experimentGroupsAlreadyRegistered = [];
      var i, j;
      var esvo;
      for (i = 0; i < experimentSets.length; i++) {
         esvo = experimentSets[i];
         // if the group has a null value for id, then it hasn't been
         // created as a group in the database nor session
         if ( typeof esvo !== 'undefined' && (esvo.id === -1 || esvo.id === null) ) {
            // addSessionGroups() takes an experimentSetValueObject (and boolean for isModificationBased)

            experimentGroupsToRegister.push( esvo );
         } else {
            experimentGroupsAlreadyRegistered.push( esvo );
         }
      }
      if ( experimentGroupsToRegister.length > 0 ) {
         experimentSets = experimentGroupsAlreadyRegistered;
         ExpressionExperimentSetController.addSessionGroups( experimentGroupsToRegister, false, function(
            registeresDatasetSets ) {
            // should be at least one datasetSet
            if ( registeresDatasetSets === null || registeresDatasetSets.length === 0 ) {
               // TODO error message
               return;
            } else {
               for (j = 0; j < registeresDatasetSets.length; j++) {
                  experimentSets.push( registeresDatasetSets[j] );
               }
            }
            defered.resolve( experimentSets );
         } );
      } else {
         defered.resolve( experimentSets );
      }

      return defered.promise;
   }
};