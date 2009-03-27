Ext.namespace("Gemma");

/**
 * Create a progress window, and fire a 'done' event when done.
 * <p>
 * Here is an example of using it:
 * 
 * <pre>
 * var callParams = [];
 * callParams.push(id);
 * ...
 * callParams.push({
 * 			callback : function(data) { // set this is a callback to your ajax call. Data will be the task id
 * 				var k = new Gemma.WaitHandler(); // instantiate
 * 				this.relayEvents(k, ['done']); // make sure events make it out
 * 				k.handleWait(data, false); // start wait
 * 				k.on('done', function(payload) {
 * 							this.handleSucess(payload); // you define this.
 * 						});
 * 			}.createDelegate(this)
 * 		});
 * 
 * Controller.dosomething.apply(this, callParams);
 * </pre>
 * 
 * @class Gemma.WaitHandler
 * @extends Ext.Component
 */
Gemma.WaitHandler = Ext.extend(Ext.Component, {
			initComponent : function() {
				Gemma.WaitHandler.superclass.initComponent.call(this);
				this.addEvents('done');
			},

			/**
			 * Parameters are passed to ProgressWindow config
			 */
			handleWait : function(taskId, showAllMessages) {
				try {
					var p = new Gemma.ProgressWindow({
								taskId : taskId,
								callback : function(data) {
									this.fireEvent('done', data);
								}.createDelegate(this),
								showAllMessages : showAllMessages
							});

					p.show();
				} catch (e) {
					Ext.Msg.alert("Error", e);
				}
			}

		});