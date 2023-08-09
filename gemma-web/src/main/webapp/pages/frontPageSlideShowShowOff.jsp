<%-- jshowoff.css is included in the bundle --%>
<div id="sloganText">
	<h2 style="text-align: center; color: grey; font-size: 1.5em">
		Database of curated and re-analyzed gene expression studies
	</h2>
</div>

<div id="frontPageSlideShow" align="center">
	<div id="features" style="overflow: hidden;">
		<div id="dataChart">
			<table>
				<tr>
					<td class="slideImageTD">
						<div style="position: relative;">
							<%-- Width here needs to be enough to fit the labels at left and right edges --%>
							<img src="https://chart.apis.google.com/chart?chs=340x240&cht=p&chco=224499&chd=${ googleData}
									&chds=a&chdl=${ googleLabels}&chf=bg,s,FFFFFF00
									&chdlp=b|l&chdls=2f2f2f,13"
								 width="340" height="240"
								 alt="A pie chart representing proportion of taxa among Gemma datasets."/>
							<img style="position: absolute; top: 30px; left: 150px;"
								 src="${pageContext.request.contextPath}/images/slideShow/humanOutline_35_3.png"
								 alt="An overlay of a human on top of its section of the pie chart."
								 width="35" height="53"/>
							<img style="position: absolute; top: 150px; left: 130px;"
								 src="${pageContext.request.contextPath}/images/slideShow/mouseOutline65_simplified.png"
								 alt="An overlay of a mouse on top of its section of the pie chart."
								 width="65" height="17"/>
							<!-- img style="position:absolute;top:17px;left:300px" src="/Gemma/images/slideShow/rightBrace.png"/-->
						</div>
					</td>
					<td id="featuredNumberOfDatasets" class="slideTextTD"></td>
				</tr>
			</table>
		</div>
<!--		<div title="2">
			<table>
				<tr>
					<td class="slideImageTD"><img src="${pageContext.request.contextPath}/images/slideShow/diffEx.png" style="margin-top: 10px" /></td>
					<td align=center class="slideTextTD">
						Search and visualise <b>differential expression</b> patterns across genes and conditions
					</td>
				</tr>
			</table>
		</div>
		<div title="3">
			<table>
				<tr>
					<td class="slideImageTD"><img class="rounded" src="${pageContext.request.contextPath}/images/slideShow/coex.png" style="margin-top: 15px;">
					</td>
					<td class="slideTextTD">
						Search <b>coexpression</b> patterns across studies and visualise the results in a network
					</td>
				</tr>
			</table>
		</div>
		<div title="4">
			<table>
				<tr>
					<td class="slideImageTD"><img src="${pageContext.request.contextPath}/images/slideShow/myGemma2.png" style="padding: 15px"></td>
					<td class="slideTextTD">
						<b>Create and share</b> your own gene and experiment groups
					</td>
				</tr>
			</table>
		</div>
		<div title="5">
			<table>
				<tr>
					<td class="slideImageTD"><img src="${pageContext.request.contextPath}/images/slideShow/neurocarta.png" style="padding: 15px"></td>
					<td class="slideTextTD">
						<div>
							<img src="${pageContext.request.contextPath}/images/logo/phenocarta-45p.png">
						</div> 
						Browse over 60,000 evidence-based <b>gene-to-phenotype associations</b> and create your own
					</td>
				</tr>
			</table>
		</div> -->
	</div>
</div>
<script type="text/javascript">
   $.getJSON( '${pageContext.request.contextPath}' + '/rest/v2/datasets', function( data ) {
         var numberFormatter = new Intl.NumberFormat();
         var featuredNumberOfDatasets = 1000 * Math.floor( data.totalElements / 1000 );
         document.getElementById( 'featuredNumberOfDatasets' ).innerHTML = 'Over <b>' + numberFormatter.format( featuredNumberOfDatasets ) + '</b> curated data sets';
      }
   )
</script>