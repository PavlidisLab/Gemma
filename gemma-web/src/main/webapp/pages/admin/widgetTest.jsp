<%@ include file="/common/taglibs.jsp" %>

<head>
<title>Widget Tests</title>
<style>
    .widget {
        padding: 10px;
        margin: 5px;
        background-color: #DDDDDD;
    }
</style>
</head>

<div class="padded">

    <h2>Widget Tests</h2>

    <h3>ExpressionExperimentPage</h3>
    <div class="widget" id="eepage"></div>

    <div id="widget-tests-combobox">
        <h3>Combo Boxes</h3>
        <h4>DatasetGroup combo</h4>
        <div class="widget" id="eesetcombo"></div>

        <h4>DatasetGroupComboPanel</h4>
        <div class="widget" id="eesetpanel"></div>
        <h4>TaxonCombo</h4>
        <div class="widget" id="taxoncombo"></div>

        <h4>FactorValueCombo</h4>
        <div class="widget" id="factorValueCombo"></div>

        <h4>Dataset search field</h4>
        <div class="widget" id="datasetsearchfield"></div>
        <div id="dsresults"></div>

        <h4>CharacteristicCombo</h4>
        <div class="widget" id="charCombo"></div>

        <h4>Gene combo</h4>
        <div class="widget" id="genecombo"></div>

        <h4>Gene Group Combo</h4>
        <div class="widget" id="genegroupcombo"></div>

        <h4>MGEDCombo</h4>
        <div class="widget" id="mgedcombo"></div>
        <h4>ArrayDesignCombo</h4>
        <div class="widget" id="adCombo"></div>
    </div>

    <div>
        <h3>Panels</h3>

        <h4>GeneChooserPanel</h4>
        <div class="widget" id="genepicker"></div>

        <h4>DatasetGroupGridPanel with DatasetGroupEditToolbar</h4>
        <div class="widget" id="datasetGroupGrid"></div>

        <h4>ExpressionExperiment Grid</h4>
        <div class="widget" id="eegrid"></div>

        <h4>AuditTrailGrid</h4>
        <div class="widget" id="atGrid"></div>

        <h4>FilesUpload</h4>
        <div class="widget" id="fileUpload"></div>

        <h4>Visualization</h4>
        <div class="widget" id="visualization"></div>

        <h4>AnalysisResultsSearch</h4>
        <div id="analysis-results-search-form" align="center"></div>
        <br>
        <div id="analysis-results-search-form-messages"></div>
        <div id="analysis-results-search-form-results"></div>
    </div>

    <h3>ProgressWidget</h3>
    <div class="widget" id="progressWidget"></div>

    <h3>Widget</h3>
    <div class="widget" id=""></div>

    <h3>Font-awesome test</h3>
    <p>
        <i class="fa fa-camera-retro fa-lg"></i>fa-camera-retro
    </p>
</div>

<script>
Ext.namespace( 'Gemma' );
Ext.BLANK_IMAGE_URL = '${pageContext.request.contextPath}/images/default/s.gif';
Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );

Ext.onReady( function() {
   Ext.QuickTips.init();

   new Gemma.DatasetGroupCombo( {
      renderTo : 'eesetcombo'
   } );

   new Gemma.ExpressionExperimentPage( {
      renderTo : 'eepage',
      eeId : '2763'
   } );

   new Gemma.DatasetGroupComboPanel( {
      renderTo : 'eesetpanel'
   } );

   let k = new Gemma.DatasetSearchField( {
      renderTo : 'datasetsearchfield',
      taxon : {
         id : 1
      },
      initQuery : "liver"
   } );

   k.on( 'aftersearch', function( f, results ) {
      Ext.DomHelper.overwrite( 'dsresults', results.length + " found." );
   } );

   new Gemma.GeneCombo( {
      renderTo : 'genecombo'
   } );

   new Gemma.GeneGroupCombo( {
      renderTo : 'genegroupcombo'
   } );

   new Gemma.TaxonCombo( {
      renderTo : 'taxoncombo'
   } );

   new Gemma.GeneGrid( {
      renderTo : 'genepicker',
      height : 200
   } );

   new Gemma.DatasetGroupGridPanel( {
      renderTo : 'datasetGroupGrid',
      width : 750,
      tbar : new Gemma.DatasetGroupEditToolbar()
   } );

   new Gemma.ExpressionExperimentGrid( {
      renderTo : 'eegrid',
      width : 400,
      height : 200,
      eeids : [ 1, 2, 3, 4, 5, 6 ],
      rowExpander : true
   } );

   new Gemma.ArrayDesignCombo( {
      renderTo : 'adCombo'
   } );

   new Gemma.AuditTrailGrid( {
      renderTo : 'atGrid',
      auditable : {
         classDelegatingFor : "ubic.gemma.model.expression.experiment.ExpressionExperiment",
         id : 1
      },
      height : 200,
      width : 520
   } );

   new Gemma.FactorValueCombo( {
      efId : 1,
      renderTo : 'factorValueCombo'
   } );

   new Gemma.FileUploadForm( {
      title : 'Upload your data file',
      renderTo : 'fileUpload'
   } );

   new Gemma.CharacteristicCombo( {
      renderTo : 'charCombo',
      listeners : {
         afterrender : function( d ) {
            d.focus();
            d.setValue( "urinary" );
            d.getStore().load( {
               params : [ 'urinary' ],
               callback : function( rec, op, success ) {
                  d.focus();
                  d.expand();
                  d.select( 1 );
               }
            } )

         },
      }
   } );
   /*

    var v = new Gemma.ProgressWidget({renderTo : 'progressWidget', width : 400});
    v.allMessages = "Here are some messages for you, widget test.";


    var testdata = Gemma.testVisData();
    var viswin = new Gemma.VisualizationWithThumbsWindow ({
    renderTo : 'visualization',	admin : false,
    id : 'viswin',
    title : "This is a test of the visualization widget",
    tpl : Gemma.getProfileThumbnailTemplate(false, false),
    store : new Gemma.VisualizationStore({
    proxy : new Ext.data.MemoryProxy(testdata)
    })
    }) ;
    viswin.show({});
    */
} );
</script>
