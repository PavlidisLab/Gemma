Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

Ext.namespace('Gemma.DatasetChooser');

Gemma.DatasetChooser.app = function() {

	var btn;
	var dcp;

	return {
		init : function() {

			Ext.QuickTips.init();

			dcp = new Ext.Gemma.DatasetChooserPanel();

			btn = new Ext.Button({
				renderTo : 'but',
				text : "Show picker",
				handler : function() {
					dcp.show();
				}
			});

			// Ext.state.Manager.setProvider(new Ext.state.SessionProvider({
			// state : Ext.appState
			// }));

			dcp.on("datasets-selected", function(e) {
				Ext.Msg.alert("Yay", "You got " + e.eeIds.length + " ee ids");
			});

			// dcp.show();
		}
	};
}();