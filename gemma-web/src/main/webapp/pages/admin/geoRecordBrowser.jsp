<%@ include file="/common/taglibs.jsp" %>

<head>
<title>GEO Record browser</title>
<Gemma:script src='/scripts/app/geoBrowse.js' />
</head>

<div id="messages"></div>

<div id="progress-area"></div>

<div class="padded">
    <h2>
        Displaying <span id="numRecords"></span> GEO records.
    </h2>
</div>

<div id="geostuff" class="table-padding"></div>

<br />
<div class="padded">
    <p>
        Note: Records are not shown for taxa not in the Gemma system. If you
        choose to load an experiment, please be careful: experiments that
        have two (or more) array designs should be loaded using the regular
        load form if you need to suppress the sample-matching functions.
        <strong>Click on a row</strong> to display more information about the dataset, if available
        from GEO, including information about platforms. This information is
        often not available for a day or two after the data sets becomes
        publicly available.
    </p>
    <p>
        To search GEO, type your query in the 'Search' field, e.g. <i>Mus+musculus[ORGN]+AND+brain[ALL]</i>.
        For more information on how to construct queries, go to the
        <a href="https://www.ncbi.nlm.nih.gov/geo/info/qqtutorial.html">NCBI website</a>.
    </p>
</div>
<div id="taskId" style="display: none;"></div>

<script type="text/javascript">
Ext.BLANK_IMAGE_URL = '${pageContext.request.contextPath}/images/default/s.gif';

Ext.onReady( function() {

   Ext.QuickTips.init();
   Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );

   var grid = new Gemma.GeoBrowseGrid( {} );

   new Ext.Panel( {
      renderTo : 'geostuff',
      items : [ grid ]
   } )

} );
</script>
