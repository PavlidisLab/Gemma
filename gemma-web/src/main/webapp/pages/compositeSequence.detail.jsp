<%@ include file="/common/taglibs.jsp" %>
<jsp:useBean id="compositeSequence" scope="request"
             class="ubic.gemma.model.expression.designElement.CompositeSequence"/>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<head>
    <title><fmt:message key="compositeSequence.title"/> ${ compositeSequence.name}</title>
    <jwr:script src='/scripts/api/ext/data/DwrProxy.js'/>
    <%-- deprecated. We should replace this with something tidier --%>
    <script type="text/javascript" type="text/javascript">
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
</head>
<body>
<div class="padded">
    <div class="v-padded">
        <h2>
            <fmt:message key="compositeSequence.title"/>
            : ${ compositeSequence.name} on
            <a
                    href="${pageContext.request.contextPath}/arrays/showArrayDesign.html?id=${ compositeSequence.arrayDesign.id }">
                ${compositeSequence.arrayDesign.shortName} </a>
             - ${ compositeSequence.arrayDesign.name}

        </h2>
    </div>

    <table class="detail row-separated pad-cols info-boxes">
        <tr>
            <td valign="top" align="right">
                <b> <fmt:message key="compositeSequence.description"/> :
                </b>
            </td>
            <td>
                <i class="qtp fa fa-question-circle fa-fw"
                   title="Description for the probe, usually provided by the manufacturer. It might not match the sequence annotation!">
                </i>
            </td>
            <td>
                <c:choose>
                    <c:when test="${not empty compositeSequence.description}">${compositeSequence.description}</c:when>
                    <c:otherwise>No description available</c:otherwise>
                </c:choose>
            </td>
        </tr>

        <tr>
            <td valign="top" align="right">
                <b> Taxon : </b>
            </td>
            <td></td>
            <td>
                <c:choose>
                    <c:when
                            test="${not empty compositeSequence.biologicalCharacteristic.taxon}">
                        ${compositeSequence.biologicalCharacteristic.taxon.commonName}
                    </c:when>
                    <c:otherwise>
                        [Taxon missing]
                    </c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr>
            <td valign="top" align="right">
                <b> Sequence Type : </b>
            </td>
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
                        [Not available]
                    </c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr>
            <td valign="top" align="right">
                <b> Sequence name : </b>
            </td>
            <td>
                <i class="qtp fa fa-question-circle fa-fw"
                   title='Name of the sequence in our system.'> </i>
            </td>
            <td>
                <c:choose>
                    <c:when
                            test="${not empty compositeSequence.biologicalCharacteristic.name}">${ compositeSequence.biologicalCharacteristic.name}</c:when>
                    <c:otherwise>No name available</c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr>
            <td valign="top" align="right">
                <b> Sequence description : </b>
            </td>
            <td>
                <i class="qtp fa fa-question-circle fa-fw"
                   title='Description of the sequence in our system.'> </i>
            </td>
            <td>
                <c:choose>
                    <c:when
                            test="${not empty compositeSequence.biologicalCharacteristic.description}">
                        ${ compositeSequence.biologicalCharacteristic.description}
                    </c:when>
                    <c:otherwise>No description available</c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr>
            <td valign="top" align="right">
                <b> Sequence accession : </b>
            </td>
            <td>
                <i class="qtp fa fa-question-circle fa-fw"
                   title='External accession for this sequence, if known'> </i>
            </td>
            <td>
                <c:choose>
                    <c:when test="${not empty compositeSequence.biologicalCharacteristic.sequenceDatabaseEntry}">
                        ${ compositeSequence.biologicalCharacteristic.sequenceDatabaseEntry.accession}</c:when>
                    <c:otherwise>No accession</c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr>
            <td valign="top" align="right">
                <b> Sequence : </b>
            </td>
            <td>
                <i class="qtp fa fa-question-circle fa-fw"
                   title='Sequence, if known'> </i>
            </td>
            <td>
                <c:choose>
                    <c:when test="${not empty compositeSequence.biologicalCharacteristic.sequence}">
                        <textarea class="smaller" style="font-family: monospace" rows="10"
                                  cols="90" readonly="1">${ compositeSequence.biologicalCharacteristic.sequence} </textarea></c:when>
                    <c:otherwise>No sequence</c:otherwise>
                </c:choose>

            </td>
        </tr>
        <tr>
            <td valign="top" align="right">
                <b> Sequence length : </b>
            </td>
            <td></td>
            <td>
                <c:choose>
                    <c:when
                            test="${not empty compositeSequence.biologicalCharacteristic.sequence}">
                        ${fn:length(compositeSequence.biologicalCharacteristic.sequence)}
                    </c:when>
                    <c:otherwise>No sequence available</c:otherwise>
                </c:choose>
            </td>
        </tr>
    </table>

    <hr class="normal">


    <h3 style="padding: 5px;">Alignment information</h3>
    <div style="padding: 10px;" id="probe-details"></div>
    <input type="hidden" name="cs" id="cs" value="${compositeSequence.id}"/>
</div>
</body>
