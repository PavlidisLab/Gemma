Ext.namespace("Gemma");

/**
 * Create a progress window or throbber, and fire a 'done' event when done.
 * <p>
 * Here is an example of using it:
 * 
 * <pre>
 * var callParams = [];
 * callParams.push(id);
 * ...
 * callParams.push({
 * 			callback : function(data) { // set this is a callback to your ajax call. Data will be the task id
 * 				var k = new Gemma.WaitHandler({
 * 					throbberEl : myThrobberElId // optional place to show the throbber; otherwise popup progress bar is shown.
 * 				}); // instantiate
 * 				this.relayEvents(k, ['done','fail']); // make sure events make it out; optional
 * 				k.on('done', ...); // optional
 * 				k.on('fail', ...); // optional
 * 				k.handleWait(data, false ); 
 * 				Ext.getBody().unmask();
 * 			}.createDelegate(this),
 * 			errorHandler : function(error) {
 *  Ext.Msg.alert(&quot;Failed&quot;, error);
 *  Ext.getBody().unmask();  
 *  }.createDelegate(this)
 * 		});
 * Ext.getBody().mask();
 * Controller.dosomething.apply(this, callParams);
 * </pre>
 * 
 * @class Gemma.WaitHandler
 * @extends Ext.Component
 */
Gemma.WaitHandler = Ext.extend(Ext.util.Observable, {

			throbberEl : null,

			constructor : function(config) {
				this.addEvents({
							"done" : true,
							"fail" : true,
							"background" : true
						});

				// Copy configured listeners into *this* object so that the base class's
				// constructor will add them.
				if (config) {
					if (config.listeners){
						this.listeners = config.listeners;
					}
					if (config.throbberEl){
						this.throbberEl = config.throbberEl;
					}
				}

				// Call our superclass constructor to complete construction process.
				Gemma.WaitHandler.superclass.constructor.call(config);
			},

			/**
			 * Parameters are passed to ProgressWindow config
			 */
			handleWait : function(taskId, showAllMessages, hideLogsAndCancelButton) {
				try {
					var p = new Gemma.ProgressWindow({
								taskId : taskId,
								callback : function(data) {
									this.fireEvent('done', data);
								}.createDelegate(this),
								errorHandler : function(data) {
									this.fireEvent('fail', data);
								}.createDelegate(this),
								showAllMessages : showAllMessages
							});
					
					if(hideLogsAndCancelButton){						
						p.pBar.hideLogsAndCancelButtons();
					}
					
					

					if (this.throbberEl != undefined) {
						/*
						 * Doesn't work quite right ... implemented for 'report' update.
						 */
						var el = Ext.get(this.throbberEl);

						var id = Ext.id();
						Ext.DomHelper.append(this.throbberEl, '<span id="' + id
										+ '"><img src="/Gemma/images/default/tree/loading.gif"/></span>');

						this.on('done', function(data) {
									Ext.DomHelper.overwrite(id, "");
								});

						p.start();
					} else {
						p.show();
					}
				} catch (e) {
					Ext.Msg.alert("Error", e);
				}
			},

			handleGoBackground : function() {
				this.fireEvent('background', this);
			}

		});