<xsl:stylesheet version = '1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
	<xsl:output method = "xml" indent = "yes" /> 
	
	<xsl:template match = "/" >
		<BioList>
			<xsl:apply-templates select = "//MAGE-ML/BioMaterial_package/BioMaterial_assnlist/BioSource" />
		</BioList>
	</xsl:template> 
	
	<xsl:template match = "BioSource">
		<BioSource>
			<xsl:attribute name = "identifier" ><xsl:value-of select="@identifier" /></xsl:attribute> 
			<xsl:attribute name = "name" ><xsl:value-of select="@name" /></xsl:attribute> 
			<Characteristics>
				<xsl:apply-templates select = "Characteristics_assnlist/OntologyEntry"/>
			</Characteristics>
		</BioSource>
	</xsl:template>
	
	
	<xsl:template match = "Characteristics_assnlist/OntologyEntry">
		<xsl:element name="{@category}">
			<xsl:apply-templates select="descendant::OntologyEntry[@category != @value]" />						
		</xsl:element>
	</xsl:template>


	<xsl:template match= "OntologyEntry[@category != @value]">
		<xsl:variable name="c" select="@category" />
		<xsl:variable name="d" select="@description"/>
		<xsl:choose>
			<xsl:when test="contains($c, 'has_')">		
				<xsl:variable name="e" select="../../../../@category"/>
				<!-- need to replace any / in $e with a _ -->
				<xsl:variable name="elname" select="translate($e,'/ ', '__')"/>
				<xsl:element name="{$elname}">	
					<xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute>
					<xsl:if test="string-length($d)>0">
							<xsl:attribute name="description"><xsl:value-of select="@description"/></xsl:attribute>	
					</xsl:if>
					<xsl:apply-templates select="../../OntologyReference_assn/DatabaseEntry" />
					<xsl:apply-templates select="../../OntologyReference_assn/DatabaseEntry/Database_assnref/Database_ref" />
				</xsl:element>		
			</xsl:when>
			<xsl:otherwise>
				<!-- need to replace any / in $e with a _ -->
				<xsl:variable name="elname" select="translate($c,'/ ', '__')"/>
				<xsl:element name="{$elname}">
					<xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute>
					<xsl:if test="string-length($d)>0">
							<xsl:attribute name="description"><xsl:value-of select="@description"/></xsl:attribute>	
					</xsl:if>
					<xsl:apply-templates select="OntologyReference_assn/DatabaseEntry" />
					<xsl:apply-templates select="OntologyReference_assn/DatabaseEntry/Database_assnref/Database_ref" />		
				</xsl:element>	
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match= "OntologyReference_assn/DatabaseEntry">
		<xsl:variable name="a" select="@accession"/>
		<xsl:if test="not(contains($a, 'has_'))">
			<xsl:attribute name="ValueDatabaseAccession"><xsl:value-of select="@accession" /></xsl:attribute>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match= "OntologyReference_assn/DatabaseEntry/Database_assnref/Database_ref">
		<xsl:attribute name="ValueDatabaseIdentifier"><xsl:value-of select="@identifier" /></xsl:attribute>
	</xsl:template>
	
</xsl:stylesheet>
