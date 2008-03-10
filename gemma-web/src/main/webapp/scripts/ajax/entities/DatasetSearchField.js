
/* Ext.Gemma.DatasetSearchField constructor...
 */
Ext.namespace('Ext.Gemma');

Ext.Gemma.DatasetSearchField = function ( config ) {

	this.loadMask = config.loadMask; delete config.loadMask;
	this.eeIds = [];

	Ext.Gemma.DatasetSearchField.superclass.constructor.call( this, config );
	
	this.on( 'beforesearch', function( field, query ) {
		if ( this.loadMask ) {
			this.loadMask.show();
		}
	} );
	this.on( 'aftersearch', function( field, results ) {
		if ( this.loadMask ) {
			this.loadMask.hide();
		}
	} );
};

/* other public methods...
 */
Ext.extend( Ext.Gemma.DatasetSearchField, Ext.form.TextField, {

	initComponent : function() {
        Ext.Gemma.DatasetSearchField.superclass.initComponent.call(this);
        
        this.addEvents(
            'beforesearch',
            'aftersearch'
        );
    },

	initEvents : function() {
		Ext.Gemma.DatasetSearchField.superclass.initEvents.call(this);
		
		var queryTask = new Ext.util.DelayedTask( this.findDatasets, this );
		this.el.on( "keyup", function( e ) { queryTask.delay( 500 ); } );
	},
	
	findDatasets : function () {
		var params = [ this.getValue(), this.taxon ? this.taxon.id : -1 ];
		if ( params == this.lastParams ) {
			return;
		}
		if ( this.fireEvent('beforesearch', this, params ) !== false ) {
			this.lastParams = params;
			ExtCoexpressionSearchController.findExpressionExperiments( params[0], params[1], this.foundDatasets.bind( this ) );
        }
	},
	
	foundDatasets : function ( results ) {
		this.eeIds = results;
		this.fireEvent( 'aftersearch', this, results );
	},
	
	getEeIds : function () {
		return this.eeIds;
	},
	
	taxonChanged : function ( taxon, doSearch ) {
		this.taxon = taxon;
		if ( doSearch ) {
			this.findDatasets();
		}
	}
	
} );