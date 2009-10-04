Ext.namespace('Gemma');
Gemma.MAX_LABEL_LENGTH_CHAR = 25;
Gemma.MAX_GENEINFO_LENGTH_CHAR = 125;

/**
 * Configure : readMethod,
 */

Gemma.VectorDisplay = Ext
		.extend(
				Ext.Window,

				{
					width : 420,
					height : 400,
					bodyStyle : "background:white",
					stateful : false,
					noGeneLabel : "[No gene]",

					heatmapConfig : {
						xaxis : {
							noTicks : 0
						},
						yaxis : {
							noTicks : 0
						},
						grid : {
							labelMargin : 0,
							marginColor : "white"
						},
						shadowSize : 0,

						label : true
					},

					show : function(config) {

						this.loadMask = new Ext.LoadMask(Ext.getBody());

						var params = [];
						if (config.params) {
							params = config.params;
						}

						this.dataView.getStore().load( {
							params : params,
							callback : this.dedvCallback.createDelegate(this)
						});

						Gemma.VectorDisplay.superclass.show.call(this);

						this.loadMask.show();

					},

					dedvCallback : function(data) {

						if (!data || data.size() == 0) {
							console.log("No data!");
							this.hide();
							Ext.Msg.alert('Status', 'Data not yet available');
							return;
						}

						this.flotrData = [];
						var coordinateProfile = data[0].data.profiles;

						var geneIds = [];

						for ( var i = 0; i < coordinateProfile.size(); i++) {

							var coordinateObject = coordinateProfile[i].points;

							var probeId = coordinateProfile[i].probe.id;
							var probe = coordinateProfile[i].probe.name;
							var genes = coordinateProfile[i].genes;

							var geneNames = this.noGeneLabel;

							var geneOfficialName = '';

							if (genes && genes.size() > 0 && genes[0].name) {

								geneNames = genes[0].name;
								geneOfficialName = genes[0].officialName == null ? '[?]' : genes[0].officialName;

								for ( var k = 1; k < genes.size(); k++) {

									geneOfficialName = geneOfficialName + ", "
											+ (genes[k].officialName == null ? '[?]' : genes[k].officialName);

									geneIds.push(genes[k].id);

									geneNames = geneNames + ", " + genes[k].name;

								}
							}

							var oneProfile = [];
							for ( var j = 0; j < coordinateObject.size(); j++) {
								var point = [ coordinateObject[j].x, coordinateObject[j].y ];
								oneProfile.push(point);
							}

							var plotConfig = {
								data : oneProfile,
								genes : genes,
								label : " <a  href='/Gemma/compositeSequence/show.html?id=" + probeId
										+ "' target='_blank' ext:qtip= '" + probe + " (" + geneNames + ")" + "'> "
										+ Ext.util.Format.ellipsis(geneNames, Gemma.MAX_LABEL_LENGTH_CHAR)
										+ "</a> &nbsp;&nbsp;&nbsp;"
										+ Ext.util.Format.ellipsis(geneOfficialName, Gemma.MAX_GENEINFO_LENGTH_CHAR),
								labelID : probeId,
								lines : {
									lineWidth : Gemma.LINE_THICKNESS
								},
								// Needs to be added so switching views work
								probe : {
									id : probeId,
									name : probe
								},
								points : coordinateObject

							};

							this.flotrData.push(plotConfig);
						}

						var eevo = data[0].data.eevo;

						Heatmap.draw($(this.body.id), this.flotrData, this.heatmapConfig);

						if (this.loadMask) {
							this.loadMask.hide();
						}

						var downloadLink = String
								.format(
										"<a ext:qtip='Download raw data in a tab delimted format'  target='_blank'  href='/Gemma/dedv/downloadDEDV.html?ee={0} &g={1}' > <img src='/Gemma/images/download.gif'/></a>",
										eevo.id, geneIds);

						this.setTitle(this.title + "&nbsp;&nbsp;" + downloadLink);

						this.doLayout(true, true);

					},

					refresh : function() {
						$(this.body.id).innerHTML = '';
						Heatmap.draw($(this.body.id), this.flotrData, this.heatmapConfig);
					},

					initComponent : function() {

						this.dataView = new Ext.DataView( {
							autoHeight : true,
							emptyText : 'Data not available',
							loadingText : 'Loading...',
							store : new Gemma.VisualizationStore( {
								readMethod : this.readMethod
							})
						});

						Gemma.VectorDisplay.superclass.initComponent.call(this);

						this.on('resize', function(component, width, height) {

							if (!this.flotrData) {
								return;
							}

							this.refresh();

						}.createDelegate(this))

					}

				});
