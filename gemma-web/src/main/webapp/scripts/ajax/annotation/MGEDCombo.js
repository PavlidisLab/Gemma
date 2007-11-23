Ext.namespace('Ext.Gemma');

/* Ext.Gemma.MGEDCombo constructor...
 */
Ext.Gemma.MGEDCombo = function ( config ) {

	Ext.Gemma.MGEDCombo.superclass.constructor.call( this, config );
	
	if ( Ext.Gemma.MGEDCombo.record == undefined ) {
		Ext.Gemma.MGEDCombo.record = Ext.data.Record.create( [
			{ name:"id", type:"int" },
			{ name:"uri", type:"string" },
			{ name:"term", type:"string" },
		] );
	}
	this.store = config.store || new Ext.data.Store( {
		proxy : new Ext.data.DWRProxy( MgedOntologyService.getUsefulMgedTerms ),
		reader : new Ext.data.ListRangeReader( {id:"id"}, Ext.Gemma.MGEDCombo.record ),
		remoteSort : false,
		sortInfo : { field : "term" }
	} );
	this.store.load();
	
	this.displayField = config.displayField || "term";
	this.editable = config.editable || "false";
	this.mode = config.mode || "local";
	this.triggerAction = config.triggerAction || "all";
	this.typeAhead = config.typeAhead || true;
	
	this.on( "select", function ( combo, record, index ) {
		this.selectedTerm = record.data;
	} );
}

/* other public methods...
 */
Ext.extend( Ext.Gemma.MGEDCombo, Ext.form.ComboBox, {
	
	getTerm : function () {
		return this.selectedTerm;
	}
	
} );