<jsp:useBean id="compositeSequence" scope="request"
        type="ubic.gemma.model.expression.designElement.CompositeSequence" />
<%@ include file="/WEB-INF/common/taglibs.jsp" %>

<head>
<title>${fn:escapeXml(compositeSequence.name)}<c:if test="${fn:length(compositeSequence.description) > 0}">
    - ${fn:escapeXml(compositeSequence.description)}</c:if></title>
<meta name="description" content="${fn:escapeXml(compositeSequence.description)}" />
</head>

<input type="hidden" name="cs" id="cs" value="${compositeSequence.id}" />

<div class="padded">
    <div class="v-padded">
        <h2>
            ${fn:escapeXml(compositeSequence.name)} on
            <a href="${pageContext.request.contextPath}/arrays/showArrayDesign.html?id=${compositeSequence.arrayDesign.id}">${compositeSequence.arrayDesign.shortName}</a>
        </h2>
    </div>

    <table class="detail row-separated pad-cols info-boxes">
        <tr>
            <td><b><fmt:message key="compositeSequence.description" />:</b></td>
            <td>
                <i class="qtp fa fa-question-circle fa-fw"
                        title="Description for the probe, usually provided by the manufacturer. It might not match the sequence annotation!">
                </i>
            </td>
            <td>
                <c:choose>
                    <c:when test="${not empty compositeSequence.description}">
                        ${fn:escapeXml(compositeSequence.description)}
                    </c:when>
                    <c:otherwise><i>No description available</i></c:otherwise>
                </c:choose>
            </td>
        </tr>

        <tr>
            <td><b>Taxon:</b></td>
            <td></td>
            <td>
                <c:choose>
                    <c:when test="${not empty compositeSequence.biologicalCharacteristic.taxon}">
                        ${compositeSequence.biologicalCharacteristic.taxon.commonName}
                    </c:when>
                    <c:otherwise>
                        <i>No taxon available</i>
                    </c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr>
            <td><b>Sequence type:</b></td>
            <td>
                <i class="qtp fa fa-question-circle fa-fw"
                        title="The type of this sequence as recorded in our system"> </i>
            </td>
            <td>
                <c:choose>
                    <c:when
                            test="${not empty compositeSequence.biologicalCharacteristic}">
                        <spring:bind
                                path="compositeSequence.biologicalCharacteristic.type">
                            <spring:transform
                                    value="${compositeSequence.biologicalCharacteristic.type}">
                            </spring:transform>
                        </spring:bind>
                    </c:when>
                    <c:otherwise>
                        <i>No sequence type available</i>
                    </c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr>
            <td><b>Sequence name:</b></td>
            <td>
                <i class="qtp fa fa-question-circle fa-fw"
                        title='Name of the sequence in our system.'> </i>
            </td>
            <td>
                <c:choose>
                    <c:when test="${not empty compositeSequence.biologicalCharacteristic.name}">
                        ${fn:escapeXml(compositeSequence.biologicalCharacteristic.name)}
                    </c:when>
                    <c:otherwise>
                        <i>No sequence name available</i>
                    </c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr>
            <td><b>Sequence description:</b></td>
            <td>
                <i class="qtp fa fa-question-circle fa-fw"
                        title='Description of the sequence in our system.'> </i>
            </td>
            <td>
                <c:choose>
                    <c:when test="${not empty compositeSequence.biologicalCharacteristic.description}">
                        ${fn:escapeXml(compositeSequence.biologicalCharacteristic.description)}
                    </c:when>
                    <c:otherwise>
                        <i>No sequence description available</i>
                    </c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr>
            <td><b>Sequence accession:</b></td>
            <td>
                <i class="qtp fa fa-question-circle fa-fw"
                        title='External accession for this sequence, if known'> </i>
            </td>
            <td>
                <c:choose>
                    <c:when test="${not empty compositeSequence.biologicalCharacteristic.sequenceDatabaseEntry}">
                        ${compositeSequence.biologicalCharacteristic.sequenceDatabaseEntry.accession}
                    </c:when>
                    <c:otherwise><i>No accession available</i></c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr>
            <td><label for="sequence"><b>Sequence:</b></label></td>
            <td>
                <i class="qtp fa fa-question-circle fa-fw"
                        title='Sequence, if known'> </i>
            </td>
            <td>
                <c:choose>
                    <c:when test="${not empty compositeSequence.biologicalCharacteristic.sequence}">
                        <textarea id="sequence" class="smaller" style="font-family: monospace" rows="10"
                                cols="90"
                                readonly>${fn:escapeXml(compositeSequence.biologicalCharacteristic.sequence)}</textarea></c:when>
                    <c:otherwise><i>No sequence available</i></c:otherwise>
                </c:choose>

            </td>
        </tr>
        <tr>
            <td><b>Sequence length:</b></td>
            <td></td>
            <td>
                <c:choose>
                    <c:when test="${not empty compositeSequence.biologicalCharacteristic.sequence}">
                        ${fn:length(compositeSequence.biologicalCharacteristic.sequence)}
                    </c:when>
                    <c:otherwise><i>No sequence available</i></c:otherwise>
                </c:choose>
            </td>
        </tr>
    </table>

    <hr class="normal">


    <h3 style="padding: 5px;">Alignment information</h3>
    <div style="padding: 10px;" id="probe-details"></div>
</div>

<%-- deprecated. We should replace this with something tidier --%>
<script type="text/javascript">
Ext.onReady( function() {
   Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );
   Ext.QuickTips.init();

   var csid = Ext.get( "cs" ) ? Ext.get( "cs" ).getValue() : null;
   this.detailsGrid = new Gemma.GenomeAlignmentsGrid( {
      renderTo : "probe-details",
      height : 100,
      width : 620
   } );

   this.detailsGrid.getStore().load( {
      params : [ {
         id : csid
      } ]
   } );

} );

$( document ).ready( function() {
   $( 'i[title]' ).qtip();
} );
</script>
