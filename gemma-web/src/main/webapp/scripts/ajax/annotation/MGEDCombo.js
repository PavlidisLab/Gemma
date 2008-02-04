Ext.namespace('Ext.Gemma');

/* Ext.Gemma.MGEDCombo constructor...
 */
Ext.Gemma.MGEDCombo = function ( config ) {

	if ( config.termKey ) {
		this.dwrMethod = MgedOntologyService.getMgedTermsByKey;
		this.dwrParams = [ config.termKey ];
	} else {
		this.dwrMethod = MgedOntologyService.getUsefulMgedTerms;
		this.dwrParams = [ ];
	}
	delete config.termKey;

	/* establish default config options...
	 */
	var superConfig = {
		displayField : 'term',
		editable : false,
		mode : 'local',
		selectOnFocus : true,
		triggerAction : 'all',
		typeAhead : true
	};
	
	if ( Ext.Gemma.MGEDCombo.record === undefined ) {
		Ext.Gemma.MGEDCombo.record = Ext.data.Record.create( [
			{ name:"id", type:"int" },
			{ name:"uri", type:"string" },
			{ name:"term", type:"string" }
		] );
	}
	superConfig.store = new Ext.data.Store( {
		proxy : new Ext.data.DWRProxy( this.dwrMethod ),
		reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.MGEDCombo.record ),
		remoteSort : false,
		sortInfo : { field : "term" }
	} );
	superConfig.store.load( { params: this.dwrParams } );

	/* apply user-defined config options and call the superclass constructor...
	 */
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.MGEDCombo.superclass.constructor.call( this, superConfig );
	
	this.on( "select", function ( combo, record, index ) {
		this.selectedTerm = record.data;
	} );
};

/* other public methods...
 */
Ext.extend( Ext.Gemma.MGEDCombo, Ext.form.ComboBox, {
	
	getTerm : function () {
		return this.selectedTerm;
	}
	
} );