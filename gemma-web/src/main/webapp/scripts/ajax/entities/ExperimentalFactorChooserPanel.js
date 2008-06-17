Ext.namespace('Gemma');

/**
 * User interface for selecting experimental factors.
 * 
 * @class Gemma.ExperimentalFactorChooserPanel
 * @extends Ext.Window
 *
 * @author keshav
 * @version $Id$
 */
Gemma.ExperimentalFactorChooserPanel = Ext.extend(Ext.Window, {
	id : 'factor-chooser',
	layout : 'border',
	width : 800,
	height : 500,
	closeAction : 'hide',
	constrainHeader : true,

	onCommit : function() {
		
		this.fireEvent("factors-chosen");
		
		this.hide();
	},

	initComponent : function() {

		Ext.apply(this, {
			buttons : [{
				id : 'done-selecting-button',
				text : "Done",
				handler : this.onCommit.createDelegate(this),
				scope : this
			}]
		});

		this.addEvents({
			"factors-chosen" : true
		});

		Gemma.ExperimentalFactorChooserPanel.superclass.initComponent.call(this);

	},

	show : function(config) {

		Gemma.ExperimentalFactorChooserPanel.superclass.show.call(this);
	},

	onRender : function(ct, position) {
		Gemma.ExperimentalFactorChooserPanel.superclass.onRender.call(this, ct, position);

		var admin = dwr.util.getValue("hasAdmin");
	}

});