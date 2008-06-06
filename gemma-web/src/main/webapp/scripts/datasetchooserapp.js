Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

Ext.namespace('Gemma.DatasetChooser');

Gemma.DatasetChooser.app = function() {

	var btn;
	var dcp;

	return {
		init : function() {

			Ext.QuickTips.init();

			Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

			// dcp = new Ext.Gemma.DatasetChooserPanel();
			//
			// gp = new Ext.Gemma.GeneChooserPanel({
			// renderTo : 'panel',
			// frame : true,
			// width : 500
			// });
			//
			dcp = new Ext.Gemma.ExpressionExperimentSetPanel({
				renderTo : 'but'
			});

			dcp.on("datasets-selected", function(e) {
				Ext.Msg.alert("Yay", "You got "
						+ e.selected.get("expressionExperimentIds").length
						+ " ee ids");
			});

			// dcp.show();
		}
	};
}();