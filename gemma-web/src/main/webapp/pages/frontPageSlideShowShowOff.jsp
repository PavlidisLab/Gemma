<%-- jshowoff.css is included in the bundle --%>
<div id="sloganText">
    <h2 style="text-align: center; color: grey; font-size: 1.5em">
        Database of curated and re-analyzed gene expression studies
    </h2>
</div>

<div id="frontPageSlideShow" style="display: flex;flex-direction: row">


    <div style="padding-left:250px;margin:40px">
        <%-- Width here needs to be enough to fit the labels at left and right edges --%>
        <img src="https://chart.apis.google.com/chart?chs=340x240&cht=p&chco=224499&chd=${ googleData}
									&chds=a&chdl=${ googleLabels}&chf=bg,s,FFFFFF00
									&chdlp=b|l&chdls=2f2f2f,13"
                width="340" height="240"
                alt="A pie chart representing proportion of taxa among Gemma datasets." />
        <img style="position: absolute; top: 180px; left: 435px;"
                src="${pageContext.request.contextPath}/images/slideShow/humanOutline_35_3.png"
                alt="An overlay of a human on top of its section of the pie chart."
                width="35" height="53" />
        <img style="position: absolute; top: 300px; left: 420px;"
                src="${pageContext.request.contextPath}/images/slideShow/mouseOutline65_simplified.png"
                alt="An overlay of a mouse on top of its section of the pie chart."
                width="65" height="17" />
        <!-- img style="position:absolute;top:17px;left:300px" src="/Gemma/images/slideShow/rightBrace.png"/-->
     <%--   <div id="featuredNumberOfDatasets" class="slideTextTD"></div>--%>
    </div>
    <div style="width:250px;margin:40px;" id="summaryPanel-div"></div>
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
