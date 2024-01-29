<%-- jshowoff.css is included in the bundle --%>
<div id="sloganText">
    <h2 style="text-align: center; color: grey; font-size: 1.5em;font-family:Avenir,Helvetica,Arial,sans-serif">
        Database of curated and re-analyzed gene expression studies
    </h2>
</div>

<div id="frontPageSlideShow" style="display: flex;flex-direction: row">
    <div style="font-family:Avenir,Helvetica,Arial,sans-serif;font-size:small;width:330px;min-width:250px;padding-left:130px;margin:10px;padding-top:40px">
        Gemma provides expression data, experimental design annotations,
        and differential expression analysis results for thousands of studies (microarrays and RNA-seq).
        We acquire data from public sources (primarily NCBI <a href="http://www.ncbi.nlm.nih.gov/geo/">GEO</a>),
        reanalyze raw
        data, annotate experimental conditions, conduct quality control and compute differential expression using standardized procedures.
        We have especially good coverage of experiments relevant to the nervous system.
        See the <a href="https://pavlidislab.github.io/Gemma/">documentation</a> for more information.
        Gemma was and developed and is maintained by the <a href="https://pavlab.msl.ubc.ca/" target="_blank">Pavlidis group
        at UBC</a>)
    </div>

    <div style="padding-left:10px;margin:40px">
        <%-- Width here needs to be enough to fit the labels at left and right edges --%>
       <%-- <img src="https://chart.apis.google.com/chart?chs=340x240&cht=p&chco=224499&chd=${ googleData}
									&chds=a&chdl=${ googleLabels}&chf=bg,s,FFFFFF00
									&chdlp=b|l&chdls=2f2f2f,13"
                width="340" height="240"
                alt="A pie chart representing proportion of taxa among Gemma datasets." />--%>
            <img style="box-shadow: 5px 5px 5px rgba(0, 0, 0, 0.5);" src="${pageContext.request.contextPath}/images/showoff.png"
                 alt="Example of a dataset view" />
      <%--  <img style="position: absolute; top: 180px; left: 435px;"
                src="${pageContext.request.contextPath}/images/slideShow/humanOutline_35_3.png"
                alt="An overlay of a human on top of its section of the pie chart."
                width="35" height="53" />
        <img style="position: absolute; top: 300px; left: 420px;"
                src="${pageContext.request.contextPath}/images/slideShow/mouseOutline65_simplified.png"
                alt="An overlay of a mouse on top of its section of the pie chart."
                width="65" height="17" />--%>
        <!-- img style="position:absolute;top:17px;left:300px" src="/Gemma/images/slideShow/rightBrace.png"/-->
        <%--   <div id="featuredNumberOfDatasets" class="slideTextTD"></div>--%>
    </div>
    <div style="width:250px;margin:40px" id="summaryPanel-div"></div>
</div>
<%--
<script type="text/javascript">
$.getJSON( '${pageContext.request.contextPath}' + '/rest/v2/datasets/count', function( data ) {
      var numberFormatter = new Intl.NumberFormat();
      var featuredNumberOfDatasets = 1000 * Math.floor( data.data / 1000 );
      document.getElementById( 'featuredNumberOfDatasets' ).innerHTML = 'Over <b>' + numberFormatter.format( featuredNumberOfDatasets ) + '</b> curated data sets';
   }
)
</script>--%>
