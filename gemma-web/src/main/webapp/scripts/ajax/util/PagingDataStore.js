Ext.namespace('Ext.Gemma');

/* Ext.Gemma.PagingDataStore constructor...
 * 	ds is the backing data store
 * 	config is a hash with the following options:
 * 		...
 */
Ext.Gemma.PagingDataStore = function ( config ) {

	this.currentStartIndex = 0;
	this.pageSize = 10;
	if ( config && config.pageSize ) {
		this.pageSize = config.pageSize;
		delete config.pageSize;
	}
	
	Ext.Gemma.PagingDataStore.superclass.constructor.call( this, config );
};

Ext.extend( Ext.Gemma.PagingDataStore, Ext.data.Store, {

	getAt : function ( index ) {
       return Ext.Gemma.PagingDataStore.superclass.getAt.call( this, this.currentStartIndex + index );
    },
    
    getRange : function ( start, end ) {
	   	var windowStart = this.currentStartIndex + start;
    	var windowEnd = this.currentStartIndex + end;
		if ( windowEnd > this.currentStartIndex + this.pageSize - 1 )
   			windowEnd = this.currentStartIndex + this.pageSize - 1;
		return Ext.Gemma.PagingDataStore.superclass.getRange.call( this, windowStart, windowEnd );
    },
    
    indexOf : function ( record ) {
        var i = this.data.indexOf(record);
        return i - this.currentStartIndex;
    },
    
    indexOfId : function ( id ) {
        var i = this.data.indexOfKey(id);
        return i - this.currentStartIndex;
    },
    
    load : function ( options ) {
		options = options || {};
		if ( options.params.start !== undefined || options.params.limit !== undefined ) {
			if ( this.fireEvent( "beforeload", this, options ) !== false ) {
				if ( options.params.start !== undefined ) 
					this.currentStartIndex = options.params.start;
				if ( options.params.limit !== undefined )
					this.pageSize = options.params.limit;
				var total = this.getTotalCount();
				var records = this.getVisibleRecords();
				this.fireEvent( "datachanged", this );
				this.fireEvent( "load", this, records, options );
			}
		} else {
			this.currentStartIndex = 0;
			Ext.Gemma.PagingDataStore.superclass.load.call( this, options );
		}
    },
    
    getVisibleRecords : function () {
    	return this.getRange( 0, this.pageSize - 1 );
    }

} );