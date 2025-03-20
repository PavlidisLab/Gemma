Ext.namespace( 'Gemma' );

Gemma.MetaheatmapFilter = function() {
   this._filterId = 1; // used to generate filter ids (private)
   this._length = 0; // number of filters (private)
   this._order = []; // array of filter ids storing the order the filters should be applied in (private)
   this.filters = {};
   this.params = {};
   /**
    * Adds function to filter. Use index to determine order filters should be applied in.
    * 
    * If you add at index 0, all other filters will be shifted down by one
    * 
    * If you add at index 18 and there are only 2 filters, it will just be added to the end (That means a subsequent
    * addition at index 17 would actually come *after*. Use this.getFilterCount()-x to insert appropriately)
    * 
    * If you just want the filter to be added on to the end, pass in index = null or leave it off
    * 
    * @param {Object}
    *           fn should return true if object should be hidden
    * @param {Object}
    *           index (optional) indicates order in which to run this filter relative to others
    * @return {int} the id of the filter just added (needed for updates and deletes)
    */
   this.add = function( fn, index ) {
      var id = this._filterId++;
      if ( arguments.length == 1 ) {
         index = this.length;
      }
      if ( index >= this.length ) {
         this._order.push( id );
      } else {
         this._order.splice( index, 0, id );
      }
      this._length++;
      this.filters[id] = fn;
      // marks that filter hasn't been used yet
      this.params[id] = null;
      return id;
   };
   this.remove = function( id ) {
      if ( this.filters[id] ) {

         if ( this.filters[id] ) {
            delete this.filters[id];
         }

         if ( this.params[id] ) {
            delete this.params[id];
         }

         // remove from order
         var i;
         var orderIndex = this._order.indexOf( id );
         if ( orderIndex !== -1 ) {
            this._order.splice( orderIndex, 1 );
         }
         this._length--;
      }
   };
   /**
    * updates the parameters of a filter a filter isn't active until it's parameters are set (must not be null)
    * 
    * @param {Object}
    *           id
    * @param {Object}
    *           param array of parameters
    */
   this.update = function( id, param ) {
      if ( this.filters[id] ) {
         this.params[id] = param;
      }
   };
   /**
    * returns true if the param object should be hidden, false otherwise
    * 
    * @param {Object}
    *           o
    */
   this.applyFilters = function( o ) {
      var i;
      var func;
      var param;
      var id;
      // run the object through each filtering function until
      // one returns true (true = the object should be hidden)
      for (i = 0; i < this._order.length; i++) {
         id = this._order[i];
         func = this.filters[id];
         param = this.params[id];
         if ( this.params[id] !== null && func( o, param ) ) {
            return true;
         }
      }
      return false;
   };
   this.getFilterCount = function() {
      // could use this._order.length instead
      return this._length;
   };
   /**
    * clear params so that filters are reset (order and functions maintained)
    */
   this.clearState = function() {
      this.params = {};
   };
}
