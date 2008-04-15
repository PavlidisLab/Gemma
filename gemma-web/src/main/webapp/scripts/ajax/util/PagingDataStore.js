Ext.namespace('Ext.Gemma');

/* Ext.Gemma.PagingDataStore constructor...
 * 	ds is the backing data store
 * 	config is a hash with the following options:
 * 		pageSize is the number of rows to show on each page.
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
    
    getCount : function () {
    	return this.getVisibleRecords().length;
    },
    
    getRange : function ( start, end ) {
	   	var windowStart = this.currentStartIndex + start;
    	var windowEnd = this.currentStartIndex + end;
		if ( windowEnd > this.currentStartIndex + this.pageSize - 1 ) {
   			windowEnd = this.currentStartIndex + this.pageSize - 1;
   		}
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
    
    add : function(records) {
    	Ext.Gemma.PagingDataStore.superclass.add.call( this, records );
    	this.totalLength = this.data.length;
    },
    
    load : function ( options ) {
		options = options || {};
		if ( options.params !== undefined && ( options.params.start !== undefined || options.params.limit !== undefined ) ) {
			if ( this.fireEvent( "beforeload", this, options ) !== false ) {
				if ( options.params.start !== undefined ) {
					this.currentStartIndex = options.params.start;
				}
				if ( options.params.limit !== undefined ) {
					this.pageSize = options.params.limit;
				}
				var total = this.getTotalCount();
				var records = this.getVisibleRecords();
				this.fireEvent( "datachanged", this );
				this.fireEvent( "load", this, records, options );
			}
		} else {
			// not resetting to the first page by default as per bug 1072
			if ( options.resetPage ) {
				this.currentStartIndex = 0;
			}
			Ext.Gemma.PagingDataStore.superclass.load.call( this, options );
		}
    },
    
    loadRecords : function (o, options, success) {
    	Ext.Gemma.PagingDataStore.superclass.loadRecords.call( this, o, options, success );
    	
    	this.checkStartIndex();
    },
    
    remove : function (record) {
    	Ext.Gemma.PagingDataStore.superclass.remove.call( this, record );
    	
    	// no idea why I should have to do this...
    	this.totalLength = this.data.length;
    	
    	this.checkStartIndex();
    },

    
    removeAll : function () {
    	Ext.Gemma.PagingDataStore.superclass.removeAll.call( this );
    	
    	// no idea why I should have to do this...
    	this.totalLength = this.data.length;
    	
    	this.checkStartIndex();
    },
    
    checkStartIndex : function () {
    	var previousIndex = this.currentStartIndex;
    	while ( this.currentStartIndex >= this.totalLength ) {
    		this.currentStartIndex -= this.pageSize;
    	}
    	if ( this.currentStartIndex < 0 ) {
    		this.currentStartIndex = 0;
    	}
    	if ( this.currentStartIndex != previousIndex ) {
    		// update the paging toolbar...
    		this.fireEvent( "load", this, [], { params : { start : this.currentStartIndex } } );
    	}
    },
    
    getVisibleRecords : function () {
    	return this.getRange( 0, this.pageSize - 1 );
    }

} );