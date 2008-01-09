/* Ext.Gemma.FactorValueCombo constructor...
 * 	config is a hash with the following options:
 * 		efId	the id of the ExperimentalFactor whose FactorValues are displayed in the combo
 */
Ext.Gemma.FactorValueCombo = function ( config ) {

	this.experimentalFactor = {
		id : config.efId,
		classDelegatingFor : "ExperimentalFactor"
	};
	delete config.efId;

	/* establish default config options...
	 */
	var superConfig = {};
	
	superConfig.store = new Ext.data.Store( {
		proxy : new Ext.data.DWRProxy( ExperimentalDesignController.getFactorValues ),
		reader : new Ext.data.ListRangeReader( {id:"factorValueId"}, Ext.Gemma.FactorValueGrid.getRecord() ),
		remoteSort : false,
		sortInfo : { field : "factorValueId" }
	} );
	if ( this.experimentalFactor.id ) {
		superConfig.store.load( { params: [ this.experimentalFactor ] } );
	}
	
	superConfig.displayField = "factorValueString";
	superConfig.editable = "false";
	superConfig.mode = "local";
	superConfig.triggerAction = "all";
	
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.FactorValueCombo.superclass.constructor.call( this, superConfig );
	
	this.on( "select", function ( combo, record, index ) {
		this.selectedValue = record.data;
	} );
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.FactorValueCombo, Ext.form.ComboBox, {

	setExperimentalFactor : function ( efId ) {
		this.experimentalFactor.id = efId;
		this.store.load( { params: [ this.experimentalFactor ] } );
	},
	
	getFactorValue : function () {
		return this.selectedValue;
	}
	
} );