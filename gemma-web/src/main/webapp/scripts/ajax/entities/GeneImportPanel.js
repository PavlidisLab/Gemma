Ext.Gemma.GeneImportPanel = function ( config ) {
	
	
	
	var superConfig=  { 
		title : "Import multiple genes",
		modal : true,
		layout : 'fit',
		autoHeight : true,
		width : 600,
		closeAction:'hide',
		easing : 3, 
        buttons: [{ 
        	text: 'Close',
        	handler: function(){ 
            	this.hide();  
        	},
        	scope : this
       	}]
	};
	
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	
	Ext.Gemma.GeneImportPanel.superclass.constructor.call( this, superConfig );
}

Ext.extend( Ext.Gemma.GeneImportPanel, Ext.Window, {
});