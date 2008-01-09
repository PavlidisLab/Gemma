/* Ext.Gemma.ExperimentalFactorCombo constructor...
 * 	config is a hash with the following options:
 * 		edId	the id of the ExperimentalDesign whose ExperimentalFactors are displayed in the combo
 */
Ext.Gemma.ExperimentalFactorCombo = function ( config ) {

	this.experimentalDesign = {
		id : config.edId,
		classDelegatingFor : "ExperimentalDesign"
	};
	delete config.edId;

	/* establish default config options...
	 */
	var superConfig = {};
	
	superConfig.store = new Ext.data.Store( {
		proxy : new Ext.data.DWRProxy( ExperimentalDesignController.getExperimentalFactors ),
		reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.ExperimentalFactorGrid.getRecord() ),
		remoteSort : false,
		sortInfo : { field : "name" }
	} );
	superConfig.store.load( { params: [ this.experimentalDesign ] } );
	
	superConfig.displayField = "name";
	superConfig.editable = "false";
	superConfig.mode = "local";
	superConfig.triggerAction = "all";
	
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.ExperimentalFactorCombo.superclass.constructor.call( this, superConfig );
	
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.ExperimentalFactorCombo, Ext.form.ComboBox, {

} );