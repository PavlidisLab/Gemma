/**
 * @author thea
 * @version $Id$
 */
Ext.namespace('Gemma','Gemma.AjaxLogin','Gemma.Application');

Gemma.GemmaNavigationHeader = Ext.extend(Ext.Toolbar,{
	height: 60,
	style:'background:white;border-bottom:1px solid #A9BFD3',
	border:false,
	defaults:{
		//cls:'bigText'
		flex:0
	},
	layout:'hbox',
	layoutConfig:{
		align: 'middle'
	},
	doSearchQuery: function(){
		location.href = '/Gemma/searcher.html?query=' + this.inMenuSearchField.getValue() + '&scope=SEGAP'	
	},
	initComponent: function(){
		
	this.inMenuSearchField = new Ext.form.TextField({
		flex: 1,
		enableKeyEvents: true,
		listeners: {
			specialkey: function(formField, e){
				// e.HOME, e.END, e.PAGE_UP, e.PAGE_DOWN,
				// e.TAB, e.ESC, arrow keys: e.LEFT, e.RIGHT, e.UP, e.DOWN
				if (e.getKey() === e.ENTER) {
					this.doSearchQuery(this.lastQuery);
				}
				else 
					if (e.getKey() === e.ESC) {
						formField.setValue('');
					}
			},
			scope: this
		}
	});
	var isAdmin = (Ext.getDom('hasAdmin') && Ext.getDom('hasAdmin').getValue() === 'true')?true:false;
	var userLoggedIn = (Ext.getDom('hasUser') && Ext.getDom('hasUser').getValue() === 'true')?true:false;
	
	var searchBtn = new Ext.Button({
			ref:'searchBtn',
			text: 'Search',
			menu: new Ext.menu.Menu({
			style:'background:white',
				items: [	
				// these items will render as dropdown menu items when the arrow is clicked:
				{
					xtype: 'panel',
					layout:'hbox',
					border:false,
					items: [this.inMenuSearchField, {
						xtype: 'button',
						text: 'Go',
						handler: function(){
							this.doSearchQuery();
						},
						scope: this,
						flex:0
					}]
				},{
					text: 'Search our Database',
					href: "/Gemma/searcher.html",
					tooltip: "Search our database for genes, experiments, array designs, etc."
				}, {
					text: 'Search Analysis Results',
					href: "/Gemma/home.html",
					tooltip: "Search for differential and coexpression patterns"
				}, {
					text: 'Browse Datasets',
					href: "/Gemma/expressionExperiment/showAllExpressionExperiments.html",
					tooltip: "View the list of expression data sets"
				}, {
					text: 'Browse Arrays',
					href: "/Gemma/arrays/showAllArrayDesigns.html",
					tooltip: "View the list of expression platforms"
				}],
				listeners: {
					mouseover: function(){
						hideTaskSearch.cancel();
					},
					mouseout: function(){
						hideTaskSearch.delay(250);
					}
				}
			}),
			listeners: {
				mouseover: function(){
					hideTaskSearch.cancel();
					if (!this.hasVisibleMenu()) {
						this.showMenu();
					}
				},
				mouseout: function(){
					hideTaskSearch.delay(250);
				}
			}
		});
		
		var myGemmaBtn = new Ext.Button({
			text: 'My Gemma',
			hidden: !userLoggedIn,
			menu: new Ext.menu.Menu({
				style:'background:white',
				items: [// these items will render as dropdown menu items when the arrow is clicked:
				{
					text: 'Load Data',
					href : "/Gemma/expressionExperiment/upload.html",
					tooltip:"Upload your expression data"
				}, {
					text: 'My Data Sets',
					href : "/Gemma/expressionExperiment/showAllExpressionExperimentLinkSummaries.html",
					tooltip:"Curate your data"
				},'-', {
					text: 'User Groups',
					href : "/Gemma/manageGroups.html",
					tooltip:"Manage your user groups"
				}, {
					text: 'Gene Groups',
					href : "/Gemma/geneGroupManager.html",
					tooltip:"Manage your gene groups"
				}, {
					text: 'Experiment Groups',
					href : "/Gemma/expressionExperimentSetManager.html",
					tooltip:"Manage your dataset / expression experiment groups"
				},'-', {
					text: 'Edit Profile',
					href : "/Gemma/userProfile.html",
					tooltip:"Edit your profile"
				}],
				listeners: {
					mouseover: function(){
						hideTaskMyGemma.cancel();
					},
					mouseout: function(){
						hideTaskMyGemma.delay(250);
					}
				}
			}),
			listeners: {
				mouseover: function(){
					hideTaskMyGemma.cancel();
					if (!this.hasVisibleMenu()) {
						this.showMenu();
					}
				},
				mouseout: function(){
					hideTaskMyGemma.delay(250);
				}
			}
		});
		
		var aboutBtn = new Ext.Button({
			text: 'About',
			menu: new Ext.menu.Menu({
				style:'background:white',
				items: [
				{
					text: 'About Gemma',
					href : "/Gemma/static/about.html",
					tooltip:"Gemma overview and general help"
				},{
					text: 'Help and Documentation',
					href : "/Gemma/static/help.html",
					tooltip:"Gemma overview and general help"
				}, {
					text: 'Downloads',
					href : "http://www.chibi.ubc.ca/Gemma/resources/downloads.html",
					tooltip: "Download Gemma software and data"
				}, '-' ,{
					text: 'Gemma Classic',
					href: "/Gemma/gemmaClassic.html",
					tooltip: "Use the classic version of Gemma"
				}],
				listeners: {
					mouseover: function(){
						hideTaskAbout.cancel();
					},
					mouseout: function(){
						hideTaskAbout.delay(250);
					}
				}
			}),
			listeners: {
				mouseover: function(){
					hideTaskAbout.cancel();
					if (!this.hasVisibleMenu()) {
						this.showMenu();
					}
				},
				mouseout: function(){
					hideTaskAbout.delay(250);
				}
			}
		});
				
		var helpBtn = new Ext.Button({
			text: 'Help',
			menu: new Ext.menu.Menu({
				style:'background:white',
				items: [
				{
					text: 'Wiki',
					href : "http://www.chibi.ubc.ca/faculty/pavlidis/wiki/display/gemma",
					tooltip: "Gemma wiki"
				}, {
					text: 'For Developers',
					href : "http://www.chibi.ubc.ca/Gemma/resources/",
					tooltip: "About the Gemma framework"
				}, {
					text: 'Contact Us',
					href : "/Gemma/contactUs.html",
					tooltip: "Contact Us"
				}],
				listeners: {
					mouseover: function(){
						hideTaskHelp.cancel();
					},
					mouseout: function(){
						hideTaskHelp.delay(250);
					}
				}
			}),
			listeners: {
				mouseover: function(){
					hideTaskHelp.cancel();
					if (!this.hasVisibleMenu()) {
						this.showMenu();
					}
				},
				mouseout: function(){
					hideTaskHelp.delay(250);
				}
			}
		});
		
		var adminBtn = new Ext.Button({
			text: 'Administration',
			hidden: !isAdmin,
			menu: new Ext.menu.Menu({
				style:'background:white',
				items: [// these items will render as dropdown menu items when the arrow is clicked:
				{
					text: 'Add Data',
					href : "/Gemma/admin/loadExpressionExperiment.html",
					tooltip: "Import from GEO or ArrayExpress"
				}, {
					text: 'Browse GEO',
					href : "/Gemma/admin/geoBrowser/showBatch.html",
					tooltip: "Browse GEO"
				}, {
					text: 'Search Annotations',
					href : "/Gemma/characteristicBrowser.html",
					tooltip: "Search annotations"
				}, '-',{
					text: 'Manage Users',
					href : "/Gemma/admin/userManager.html",
					tooltip: "Manage users"
				}, {
					text: 'View Active Sessions',
					href : "/Gemma/admin/activeUsers.html",
					tooltip: "View active users"
				}, {
					text: 'System Monitoring',
					href : "/Gemma/admin/systemStats.html"
				}, {
					text: 'Index Gemma Database',
					href : "/Gemma/admin/indexer.html"
				}, {
					text: 'Manage Maintenance Mode',
					href : "/Gemma/admin/maintenanceMode.html"
				}, {
					text: 'Update "What\'s New"',
					href : "/Gemma/whatsnew/generateCache.html"
				}, {
					text: 'Widget Test Page',
					href : "/Gemma/admin/widgetTest.html"
				}],
				listeners: {
					mouseover: function(){
						hideTaskAdmin.cancel();
					},
					mouseout: function(){
						hideTaskAdmin.delay(250);
					}
				}
			}),
			listeners: {
				mouseover: function(){
					hideTaskAdmin.cancel();
					if (!this.hasVisibleMenu()) {
						this.showMenu();
					}
				},
				mouseout: function(){
					hideTaskAdmin.delay(250);
				}
			}
		});
		
		
		var loggedInAs = Ext.getDom('username-logged-in');
		if(loggedInAs){
			loggedInAs = loggedInAs.value;
		}
		var userBtn = new Ext.Button({
			text: loggedInAs,
			hidden: !loggedInAs,
			menu: new Ext.menu.Menu({
				style:'background:white',
				items: [
				{
					text: 'Edit your profile',
					href : "/Gemma/userProfile.html",
					tooltip:"Change your password"
				}, {
					text: 'Log out',
					handler: Gemma.AjaxLogin.logoutFn,
					tooltip: "Log out of Gemma"
				}],
				listeners: {
					mouseover: function(){
						hideTaskUser.cancel();
					},
					mouseout: function(){
						hideTaskUser.delay(250);
					}
				}
			}),
			listeners: {
				mouseover: function(){
					hideTaskUser.cancel();
					if (!this.hasVisibleMenu()) {
						this.showMenu();
					}
				},
				mouseout: function(){
					hideTaskUser.delay(250);
				}
			}
		});
		this.loginBtn = new Ext.Button({
			xtype: 'button',
			text: 'Log In',
			cls:'mediumText',
			hidden: userLoggedIn,
			handler: function(){
				
				Gemma.AjaxLogin.showLoginWindowFn();
			},
			scope:this
		});
		
		Gemma.Application.currentUser.on("logIn", function(userName, isAdmin){
			this.loginBtn.hide();
			myGemmaBtn.show();
			if(this.navToolbar.myGemmaSpacer) { this.navToolbar.myGemmaSpacer.show(); }
			adminBtn.setVisible(isAdmin);
			if(this.navToolbar.adminSpacer) { this.navToolbar.adminSpacer.setVisible(isAdmin); }
			userBtn.setText(userName);
			userBtn.show();
			this.doLayout();
		},this);
		
				
		/*this.logoutBtn = new Ext.Button({
			xtype: 'button',
			text: 'Log out',
			cls:'smallText',
			hidden: !userLoggedIn,
			handler: Gemma.AjaxLogin.logoutFn
		});*/
		
		Gemma.Application.currentUser.on("logOut", function(){
			this.loginBtn.show();
			userBtn.setText('anon');
			userBtn.hide();
			adminBtn.hide();
			if(this.navToolbar.adminSpacer) { this.navToolbar.adminSpacer.hide(); }
			myGemmaBtn.hide();
			if(this.navToolbar.myGemmaSpacer) { this.navToolbar.myGemmaSpacer.hide(); }
			this.doLayout();
		},this);
		
		/*this.userText = new Ext.Panel({
			tpl:'Welcome <a href="/Gemma/userProfile.html">{userName}</a>',
			hidden: !userLoggedIn,
			data: {userName: loggedInAs},
			border:false,
			style:'font-size:11px'
		});*/
		
		/* these statements (along with mouseover and mouseout listeners) make each button's
		 * menu pop-up when the button is hovered over (instead of just when it's clicked)
		 * 
		 * from post #2 in:
		 * http://www.sencha.com/forum/showthread.php?76885-Button-menu-show-on-mouseover-and-hide-on-mouseout
		 * 
		 */
		var hideTaskSearch = new Ext.util.DelayedTask(searchBtn.hideMenu, searchBtn);
		var hideTaskAbout = new Ext.util.DelayedTask(aboutBtn.hideMenu, aboutBtn);
		var hideTaskHelp = new Ext.util.DelayedTask(helpBtn.hideMenu, helpBtn);
		var hideTaskMyGemma = new Ext.util.DelayedTask(myGemmaBtn.hideMenu, myGemmaBtn);
		var hideTaskAdmin = new Ext.util.DelayedTask(adminBtn.hideMenu, adminBtn);
		var hideTaskUser = new Ext.util.DelayedTask(userBtn.hideMenu, userBtn);
		
		Gemma.GemmaNavigationHeader.superclass.initComponent.call(this);
		this.add([{
			xtype: 'box',
			autoEl: {
				tag: 'a',
				href: '/Gemma/home.html',
				cn: '<img src="/Gemma/images/logo/gemma-sm52x208_text.png" style="padding-left:10px"/>'
			}
		}, '->', {
				ref: 'navToolbar',
				xtype: 'toolbar',
				height: 23,
				style: 'background:white;border-style:none',
				defaults: {
					cls: 'bigText'
				},
				items: ['->', searchBtn,  {xtype: 'tbspacer', width: 15},  aboutBtn,  {xtype: 'tbspacer', width: 15},   
					 myGemmaBtn, {
					 	ref: 'myGemmaSpacer',
					 	xtype: 'tbspacer',
					 	width: 15,
					 	hidden: !userLoggedIn
					 }, adminBtn, ' ', {
					 	ref: 'adminSpacer',
					 	xtype: 'tbspacer',
					 	width: 15,
					 	hidden: !isAdmin
					 },'-',  
					 {xtype: 'tbspacer', width: 15},  this.loginBtn, userBtn]
			
		},{
				xtype: 'box',
				autoEl: {
					tag: 'a',
					href: 'http://www.ubc.ca/',
					cn: '<img src="/Gemma/images/logo/ubcgrey_logo_40.png"/>',
					style: 'padding-left:15px; padding-right:10px'
				}
			} ]);
	}
});

Ext.reg('gemmaNavHeader', Gemma.GemmaNavigationHeader);


