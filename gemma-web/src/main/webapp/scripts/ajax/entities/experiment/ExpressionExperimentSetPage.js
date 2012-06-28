Ext.namespace('Gemma');

/**
 * 
 * Top level container for all sections of expression experiment group info Sections are: 1. Summary (has editing tools)
 * TODO ...
 * 
 * @class Gemma.ExpressionExperimentPage
 * @extends Ext.TabPanel
 * 
 */
Gemma.ExpressionExperimentSetPage = Ext.extend(Ext.TabPanel, {

			defaults : {
				autoScroll : true
			},
			deferredRender : true,
			listeners : {
				'tabchange' : function(tabPanel, newTab) {
					newTab.fireEvent('tabChanged');
				},
				'beforetabchange' : function(tabPanel, newTab, currTab) {
					// if false is returned, tab isn't changed
					if (currTab) {
						return currTab.fireEvent('leavingTab');
					}
					return true;
				}
			},
			invalidIdHandler : function(msg) {
				this.items.add(new Ext.Panel({
							html : "Error in loading experiment group due to invalid id. " + msg
						}));
			},
			initComponent : function() {

				// get id of set to show
				if (!this.eeSetId && document.URL.indexOf("?") > -1 && (document.URL.indexOf("id=") > -1)) {
					var subsetDetails = document.URL.substr(document.URL.indexOf("?") + 1);
					var param = Ext.urlDecode(subsetDetails);
					if (param.id) {
						var ids = param.id.split(',');
						if (ids.length === 1) {
							this.eeSetId = ids[0];
						} else {
							this.invalidIdHandler("Id was: " + param.id);
							Gemma.ExpressionExperimentSetPage.superclass.initComponent.call(this);
							return;
						}
					} else {
						this.invalidIdHandler("Missing \"id\" parameter.");
						Gemma.ExpressionExperimentSetPage.superclass.initComponent.call(this);
						return;
					}
				}

				var isAdmin = Ext.get("hasAdmin").getValue() == 'true';

				Gemma.ExpressionExperimentSetPage.superclass.initComponent.call(this);
				this.on('render', function() {
							if (!this.loadMask) {
								this.loadMask = new Ext.LoadMask(this.getEl(), {
											msg : "Loading ...",
											msgCls : 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
										});
							}
							this.loadMask.show();

							if (this.eeSetId) {
								ExpressionExperimentSetController.load(this.eeSetId, eeSetCb.createDelegate(this));
							} else if (this.eeSetName) {
								ExpressionExperimentSetController.findByName(this.eeSetName, eeSetCb
												.createDelegate(this));
							} else {
								// panic?
							}
						});
			},

			eeSetCb : function(experimentSetVO) {

				this.experimentSet = experimentSetVO;
				this.editable = experimentSetVO.currentUserHasWritePermission;
				this.loadMask.hide();
				/* DETAILS TAB */
				this.add(new Gemma.ExpressionExperimentSetSummary({
							title : 'Summary',
							experimentSet : experimentSetVO,
							editable : this.editable,
							admin : this.admin
						}));

				this.adjustForIsAdmin(isAdmin, this.editable);

				Gemma.Application.currentUser.on("logIn", function(userName, isAdmin) {
							var appScope = this;
							ExpressionExperimentController.canCurrentUserEditGroup(experimentDetails.id, {
										callback : function(editable) {
											appScope.adjustForIsAdmin(isAdmin, editable);
										},
										scope : appScope
									});

						}, this);

				Gemma.Application.currentUser.on("logOut", function() {
							this.adjustForIsAdmin(false, false);

						}, this);

				this.setActiveTab(0);
			},

			adjustForIsAdmin : function(isAdmin, isEditable) {
				/* HISTORY TAB */

				/* ADMIN TOOLS TAB */

			}
		});
