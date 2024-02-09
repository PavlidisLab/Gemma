/**
 * @author thea
 *
 */
Ext.namespace( 'Gemma', 'Gemma.AjaxLogin', 'Gemma.Application' );

var externalDatabasesStore = new Ext.data.JsonStore( {
   autoLoad : true,
   url : ctxBasePath + '/rest/v2',
   root : 'data.externalDatabases',
   idProperty : 'id',
   fields : [ 'id', 'name', 'description', 'uri', 'releaseVersion', 'releaseUrl', 'lastUpdated', 'externalDatabases' ]
} );

Gemma.GemmaNavigationHeader = Ext
   .extend(
      Ext.Toolbar,
      {
         height : 60,
         style : 'background:white;border-bottom:1px solid #A9BFD3',
         border : false,
         defaults : {
            // cls:'bigText'
            flex : 0
         },
         layout : 'hbox',
         layoutConfig : {
            align : 'middle'
         },
         doSearchQuery : function() {
            if ( this.inMenuSearchField.getValue().length > 1 ) {
               location.href = ctxBasePath + '/searcher.html?query=' + this.inMenuSearchField.getValue(); // +
               // '&scope=EG'
            }
         },

         /**
          * @memberOf Gemma.GemmaNavigationHeader
          */
         showAbout : function() {
            var w = new Ext.Window(
               {
                  width : 800,
                  height : 600,
                  title : "About Gemma",
                  items : [ {
                     xtype : 'panel',
                     html : '<div style="margin:10px;padding:5px;"><p>Gemma is a web site, database and a set of tools for the meta-analysis, re-use and '
                        + 'sharing of gene expression profiling data. Gemma contains data from thousands '
                        + 'of public studies, referencing thousands of published papers. Users can search, access and visualize the data and differential'
                        + ' expression results. For more information, see the '
                        + '<a href="https://pavlidislab.github.io/Gemma/" target="_blank">help and documentation.&nbsp;<img src="' + ctxBasePath + '/images/icons/link_external_icon_tight.gif"/></a>'
                        + '</p><p>Gemma was developed by the Pavlidis group at UBC '
                        + '(<a href="https://pavlidislab.github.io/Gemma/#credits" target="_blank">credits&nbsp;<img src="' + ctxBasePath + '/images/icons/link_external_icon_tight.gif"/></a>). '
                        + '</p><p>To cite Gemma, please use: <br>'
                        + 'Lim N., et al., Curation of over 10,000 transcriptomic studies to enable data reuse. <em>Database</em>, 2021.'
                        + ' <a href="https://doi.org/10.1093/database/baab006" target="_blank">link&nbsp;<img src="' + ctxBasePath + '/images/icons/link_external_icon_tight.gif"/></a>'
                        + '</p></div>'
                  } ],
                  buttons : [ {

                     text : "OK",
                     handler : function() {
                        w.destroy();
                     },
                     scope : w
                  } ],
                  listeners : {
                     afterrender : function( win ) {
                        var summary = 'Gemma\'s expression platform and gene annotations are powered by:';
                        externalDatabasesStore.data.each( function( ed ) {
                           summary += '<dt>';
                           summary += Ext.util.Format.capitalize( ed.data.name );
                           summary += '</dt>';
                           summary += '<dd>';
                           summary += ed.data.description;
                           if ( ed.data.uri !== null ) {

                              summary += ' <a href="' + ed.data.uri + '" target="_blank">link&nbsp;<img src="' + ctxBasePath + '/images/icons/link_external_icon_tight.gif"/></a>';
                           }
                           summary += '<br>';
                           if ( ed.data.releaseVersion != null ) {
                              summary += 'Release used: ';
                              if ( ed.data.releaseUrl !== null ) {
                                 summary += '<a href="' + ed.data.releaseUrl + '">' + ed.data.releaseVersion + '</a>' + '.<br>';
                              } else {
                                 summary += ed.data.releaseVersion + '.<br>';
                              }
                           }
                           if ( ed.data.lastUpdated !== null ) {
                              summary += 'Last updated on ' + new Date( ed.data.lastUpdated ).toLocaleDateString() + '.';
                           }
                           // extra information from related databases
                           ed.data.externalDatabases.forEach( function( relatedEd ) {
                              summary += '<br>'
                              summary += Ext.util.Format.capitalize( relatedEd.name );
                              if ( relatedEd.lastUpdated != null ) {
                                 if ( relatedEd.releaseUrl != null ) {
                                    summary += ' <a href="' + relatedEd.releaseUrl + '" target="_blank">' + relatedEd.releaseVersion + '&nbsp;<img src="' + ctxBasePath + '/images/icons/link_external_icon_tight.gif"/></a>';
                                 }
                                 summary += ' last updated on ' + new Date( relatedEd.lastUpdated ).toLocaleDateString() + '.';
                                 if ( relatedEd.uri != null ) {
                                    summary += ' <a href="' + relatedEd.uri + '" target="_blank">link&nbsp;<img src="' + ctxBasePath + '/images/icons/link_external_icon_tight.gif"/></a>';
                                 }
                              }
                           } );
                           summary += '</dd>';
                        } );
                        win.add( {
                           xtype : 'panel',
                           html : '<dl style="margin:10px;padding:5px;">' + summary + '</dl>'
                        } );
                     }
                  }
               } );

            w.show();
         },

         initComponent : function() {

            this.inMenuSearchField = new Ext.form.TextField( {
               flex : 1,
               enableKeyEvents : true,
               emptyText : 'Search',
               minLength : 2,
               listeners : {
                  specialkey : function( formField, e ) {
                     // e.HOME, e.END, e.PAGE_UP, e.PAGE_DOWN,
                     // e.TAB, e.ESC, arrow keys: e.LEFT, e.RIGHT, e.UP, e.DOWN
                     if ( e.getKey() === e.ENTER ) {
                        this.doSearchQuery( this.lastQuery );
                     } else if ( e.getKey() === e.ESC ) {
                        formField.setValue( '' );
                     }
                  },
                  scope : this
               }
            } );

            var isAdmin = Gemma.SecurityManager.isAdmin();
            var userLoggedIn = Gemma.SecurityManager.isLoggedIn();

            var menuDefaults = {
               cls : 'x-menu-item-large'
            };
            var searchBtn = new Ext.Button( {
               ref : 'searchBtn',
               text : 'Explore',
               menu : new Ext.menu.Menu( {
                  defaults : menuDefaults,
                  style : 'background:white;',
                  items : [
                  // these items will render as dropdown menu items when the arrow is clicked:
                  {
                     xtype : 'panel',
                     layout : 'hbox',
                     border : false,
                     // style:'width:100%',
                     items : [ this.inMenuSearchField, {
                        xtype : 'button',
                        text : 'Go',
                        handler : function() {
                           this.doSearchQuery();
                        },
                        scope : this,
                        flex : 0
                     } ]
                  },
                  /*
                   * { text : 'Search Analyses', href : ctxBasePath + "/analysesResultsSearch.html", tooltip : "Search for
                   * differential and coexpression patterns" }, '-',
                   */
                  {
                     text : 'Browse Datasets',
                     href : gemBrowUrl,
                     tooltip : "View the list of Gemma's expression data sets"
                  },
                  {
                     text : 'Browse Datasets (legacy)',
                     href : ctxBasePath + "/expressionExperiment/showAllExpressionExperiments.html",
                     tooltip : "View the list of Gemma's expression data sets"
                  },{
                     text : 'Browse Platforms',
                     href : ctxBasePath + "/arrays/showAllArrayDesigns.html",
                     tooltip : "View the list of Gemma's platforms"
                  }
                  /*, {
                     text : 'Search Annotated Papers',
                     href : ctxBasePath + "/bibRef/searchBibRefs.html",
                     tooltip : "Search for papers the Gemma curators have annotated"
                  } */
                  ],
                  listeners : {
                     mouseover : function() {
                        hideTaskSearch.cancel();
                     },
                     mouseout : function() {
                        hideTaskSearch.delay( 250 );
                     }
                  }
               } ),
               listeners : {
                  mouseover : function() {
                     hideTaskSearch.cancel();
                     if ( !this.hasVisibleMenu() ) {
                        this.showMenu();
                     }
                  },
                  mouseout : function() {
                     hideTaskSearch.delay( 250 );
                  }
               }
            } );

            var myGemmaBtn = new Ext.Button( {
               text : 'My Gemma',
               hidden : !userLoggedIn || !isAdmin,
               menu : new Ext.menu.Menu( {
                  defaults : menuDefaults,
                  style : 'background:white',
                  items : [// these items will render as dropdown menu items when the arrow is
                  // clicked:
                   {
                     text : 'Load Data',
                     href : ctxBasePath + "/expressionExperiment/upload.html",
                     tooltip : "Upload your expression data"
                  },  {
                     text : 'Dataset manager',
                     href : ctxBasePath + "/expressionExperiment/showAllExpressionExperimentLinkSummaries.html",
                     tooltip : "Curate your data"
                  }, {
                     text : 'Gene Groups',
                     href : ctxBasePath + "/geneGroupManager.html",
                     tooltip : "Manage your gene groups"
                  }, {
                     text : 'Experiment Groups',
                     href : ctxBasePath + "/expressionExperimentSetManager.html",
                     tooltip : "Manage your dataset / expression experiment groups"
                  }, '-', {
                     text : 'Edit Profile',
                     href : ctxBasePath + "/userProfile.html",
                     tooltip : "Edit your profile"
                  } ],
                  listeners : {
                     mouseover : function() {
                        hideTaskMyGemma.cancel();
                     },
                     mouseout : function() {
                        hideTaskMyGemma.delay( 250 );
                     }
                  }
               } ),
               listeners : {
                  mouseover : function() {
                     hideTaskMyGemma.cancel();
                     if ( !this.hasVisibleMenu() ) {
                        this.showMenu();
                     }
                  },
                  mouseout : function() {
                     hideTaskMyGemma.delay( 250 );
                  }
               }
            } );
            var aboutBtn = new Ext.Button( {
               text : 'About',
               menu : new Ext.menu.Menu( {
                  defaults : menuDefaults,
                  style : 'background:white',
                  items : [ {
                     text : 'About Gemma',
                     handler : function( b, e ) {
                        this.showAbout();
                     },
                     scope : this
                  }, {
                     text : 'Help and Documentation&nbsp;<img src="' + ctxBasePath + '/images/icons/link_external_icon.gif"/>',
                     href : "https://pavlidislab.github.io/Gemma/",
                     tooltip : "Gemma overview and general help",
                     hrefTarget : "_blank"
                  }/*, {
                     text : 'Dataset citations',
                     href : ctxBasePath + "/bibRef/showAllEeBibRefs.html"
                  }, {
                     text : 'QC updates',
                     href : ctxBasePath + "/expressionExperimentsWithQC.html"
                  }*/ ],
                  listeners : {
                     mouseover : function() {
                        hideTaskAbout.cancel();
                     },
                     mouseout : function() {
                        hideTaskAbout.delay( 250 );
                     }
                  }
               } ),
               listeners : {
                  mouseover : function() {
                     hideTaskAbout.cancel();
                     if ( !this.hasVisibleMenu() ) {
                        this.showMenu();
                     }
                  },
                  mouseout : function() {
                     hideTaskAbout.delay( 250 );
                  }
               }
            } );

            var helpBtn = new Ext.Button( {
               text : 'Help',
               menu : new Ext.menu.Menu( {
                  defaults : menuDefaults,
                  style : 'background:white',
                  items : [ {
                     text : 'Documentation',
                     href : "https://pavlidislab.github.io/Gemma/",
                     tooltip : "Gemma documentation"
                  }, {
                     text : 'Contact Us',
                     href : ctxBasePath + "/contactUs.html",
                     tooltip : "Contact Us"
                  } ],
                  listeners : {
                     mouseover : function() {
                        hideTaskHelp.cancel();
                     },
                     mouseout : function() {
                        hideTaskHelp.delay( 250 );
                     }
                  }
               } ),
               listeners : {
                  mouseover : function() {
                     hideTaskHelp.cancel();
                     if ( !this.hasVisibleMenu() ) {
                        this.showMenu();
                     }
                  },
                  mouseout : function() {
                     hideTaskHelp.delay( 250 );
                  }
               }
            } );

            var adminBtn = new Ext.Button( {
               text : 'Admin',
               hidden : !isAdmin,
               menu : new Ext.menu.Menu( {
                  defaults : menuDefaults,
                  style : 'background:white',
                  items : [// these items will render as dropdown menu items when the arrow is clicked:
                  {
                     text : 'Add Data',
                     href : ctxBasePath + "/admin/loadExpressionExperiment.html",
                     tooltip : "Import from GEO or ArrayExpress"
                  }, {
                     text : 'Browse GEO',
                     href : ctxBasePath + "/admin/geoRecordBrowser.html",
                     tooltip : "Browse data sets in GEO"
                  }, {
                     text : 'Search Annotations',
                     href : ctxBasePath + "/characteristicBrowser.html",
                     tooltip : "Search annotations"
                  }, '-', {
                     text : 'Manage Users',
                     href : ctxBasePath + "/admin/userManager.html",
                     tooltip : "Manage users"
                  }, {
                     text : 'View Active Sessions',
                     href : ctxBasePath + "/admin/activeUsers.html",
                     tooltip : "View active users"
                  }, {
                     text : 'System Monitoring',
                     href : ctxBasePath + "/admin/systemStats.html"
                  }, {
                     text : 'Manage Search Indexes',
                     href : ctxBasePath + "/admin/indexer.html"
                  }, {
                     text : 'Manage Maintenance Mode',
                     href : ctxBasePath + "/admin/maintenanceMode.html"
                  }, {
                     text : 'Update "What\'s New"',
                     href : ctxBasePath + "/whatsnew/generateCache.html"
                  }, {
                     text : 'Widget Test Page',
                     href : ctxBasePath + "/admin/widgetTest.html"
                  } ],
                  listeners : {
                     mouseover : function() {
                        hideTaskAdmin.cancel();
                     },
                     mouseout : function() {
                        hideTaskAdmin.delay( 250 );
                     }
                  }
               } ),
               listeners : {
                  mouseover : function() {
                     hideTaskAdmin.cancel();
                     if ( !this.hasVisibleMenu() ) {
                        this.showMenu();
                     }
                  },
                  mouseout : function() {
                     hideTaskAdmin.delay( 250 );
                  }
               }
            } );

            var loggedInAs = Ext.getDom( 'username-logged-in' );
            if ( loggedInAs ) {
               loggedInAs = loggedInAs.value;
            }
            var userBtn = new Ext.Button( {
               text : loggedInAs,
               hidden : !loggedInAs,
               menu : new Ext.menu.Menu( {
                  defaults : menuDefaults,
                  style : 'background:white',
                  items : [ {
                     text : 'Edit your profile',
                     href : ctxBasePath + "/userProfile.html",
                     tooltip : "Change your password"
                  }, {
                     text : 'Log out',
                     handler : Gemma.AjaxLogin.logoutFn,
                     tooltip : "Log out of Gemma"
                  } ],
                  listeners : {
                     mouseover : function() {
                        hideTaskUser.cancel();
                     },
                     mouseout : function() {
                        hideTaskUser.delay( 250 );
                     }
                  }
               } ),
               listeners : {
                  mouseover : function() {
                     hideTaskUser.cancel();
                     if ( !this.hasVisibleMenu() ) {
                        this.showMenu();
                     }
                  },
                  mouseout : function() {
                     hideTaskUser.delay( 250 );
                  }
               }
            } );
            this.loginBtn = new Ext.Button( {
               xtype : 'button',
               text : 'Log In',
               cls : 'mediumText',
               hidden : userLoggedIn,
               handler : function() {

                  Gemma.AjaxLogin.showLoginWindowFn();
               },
               scope : this
            } );

            Gemma.Application.currentUser.on( "logIn", function( userName, isAdmin ) {
               this.loginBtn.hide();
               myGemmaBtn.show();
               if ( this.navToolbar.myGemmaSpacer ) {
                  this.navToolbar.myGemmaSpacer.show();
               }
               adminBtn.setVisible( isAdmin );
               if ( this.navToolbar.adminSpacer ) {
                  this.navToolbar.adminSpacer.setVisible( isAdmin );
               }
               userBtn.setText( userName );
               userBtn.show();
               this.doLayout();
            }, this );

            /*
             * this.logoutBtn = new Ext.Button({ xtype: 'button', text: 'Log out', cls:'smallText', hidden:
             * !userLoggedIn, handler: Gemma.AjaxLogin.logoutFn });
             */

            Gemma.Application.currentUser.on( "logOut", function() {
               this.loginBtn.show();
               userBtn.setText( 'anon' );
               userBtn.hide();
               adminBtn.hide();
               if ( this.navToolbar.adminSpacer ) {
                  this.navToolbar.adminSpacer.hide();
               }
               myGemmaBtn.hide();
               if ( this.navToolbar.myGemmaSpacer ) {
                  this.navToolbar.myGemmaSpacer.hide();
               }
               this.doLayout();
            }, this );

            /*
             * this.userText = new Ext.Panel({ tpl:'Welcome <a href="' + ctxBasePath + '/userProfile.html">{userName}</a>', hidden:
             * !userLoggedIn, data: {userName: loggedInAs}, border:false, style:'font-size:11px' });
             */

            /*
             * these statements (along with mouseover and mouseout listeners) make each button's menu pop-up when the
             * button is hovered over (instead of just when it's clicked)
             * 
             * from post #2 in:
             * http://www.sencha.com/forum/showthread.php?76885-Button-menu-show-on-mouseover-and-hide-on-mouseout
             * 
             */
            var hideTaskSearch = new Ext.util.DelayedTask( searchBtn.hideMenu, searchBtn );
            var hideTaskAbout = new Ext.util.DelayedTask( aboutBtn.hideMenu, aboutBtn );
            var hideTaskHelp = new Ext.util.DelayedTask( helpBtn.hideMenu, helpBtn );
            var hideTaskMyGemma = new Ext.util.DelayedTask( myGemmaBtn.hideMenu, myGemmaBtn );
            var hideTaskAdmin = new Ext.util.DelayedTask( adminBtn.hideMenu, adminBtn );
            var hideTaskUser = new Ext.util.DelayedTask( userBtn.hideMenu, userBtn );

            Gemma.GemmaNavigationHeader.superclass.initComponent.call( this );
            this
               .add( [
                      {
                         xtype : 'box',
                         autoEl : {
                            tag : 'a',
                            href : ctxBasePath + '/home.html',
                            cn : '<img src="' + ctxBasePath + '/images/logo/gemma-headerlogo.png" style="padding-left:10px;padding-bottom:3px"/>'
                         }
                      }, '->', {
                         ref : 'navToolbar',
                         xtype : 'toolbar',
                         height : 23,
                         style : 'background:white;border-style:none',
                         defaults : {
                            cls : 'bigText'
                         },
                         items : [ '->', searchBtn, {
                            xtype : 'tbspacer',
                            width : 15
                         }, aboutBtn, {
                            xtype : 'tbspacer',
                            width : 15
                         }, myGemmaBtn, {
                            ref : 'myGemmaSpacer',
                            xtype : 'tbspacer',
                            width : 15,
                            hidden : !userLoggedIn
                         }, adminBtn, ' ', {
                            ref : 'adminSpacer',
                            xtype : 'tbspacer',
                            width : 15,
                            hidden : !isAdmin
                         }, '-', {
                            xtype : 'tbspacer',
                            width : 15
                         }, this.loginBtn, userBtn ]

                      }, {
                         xtype : 'box',
                         autoEl : {
                            tag : 'a',
                            href : 'https://www.ubc.ca/',
                            cn : '<img src="' + ctxBasePath + '/images/logo/ubcgrey_logo_40.png"/>',
                            style : 'padding-left:15px; padding-right:10px'
                         }
                      } ] );
         }
      } );

Ext.reg( 'gemmaNavHeader', Gemma.GemmaNavigationHeader );
