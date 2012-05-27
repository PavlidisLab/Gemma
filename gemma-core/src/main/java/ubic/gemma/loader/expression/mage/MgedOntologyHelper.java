/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.loader.expression.mage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biomage.Common.Extendable;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * This is based entirely on the OntologyHelper from MAGE STK, though the code has been significantly cleaned up and
 * modified.
 * <p>
 * Limitation: some properties point to classes that are external to the ontology. those are not parsed properly,
 * therefore, they are not included in the tree
 * <p>
 * 
 * @author Stathis Sideris 5/12/2003 email: sideris at biochem.ucl.ac.uk
 * @author pavlidis (modifications, tidying )
 * @version $Id$
 * @deprecated We are still using the OntologyHelper, this class isn't necessary. In addition, functionality we need in
 *             this class could be merged with our own MgedOntologyService.
 */
@Deprecated
public class MgedOntologyHelper {

    private static int enumID = 0;

    private static Log log = LogFactory.getLog( MgedOntologyHelper.class.getName() );
    protected static final String PARSER_NAME = "org.apache.xerces.parsers.SAXParser";

    private org.biomage.Description.Database mgedMO = null;
    // Added exception map
    private Map<String, String> modelToOntologyClassMap = new HashMap<String, String>();

    Map<String, ClassInformation> classIndex = new HashMap<String, ClassInformation>();
    Collection<InstanceInformation> instanceIndex = new HashSet<InstanceInformation>();
    Collection<PropertyInformation> propertyIndex = new HashSet<PropertyInformation>();

    private Collection<ClassInformation> rootClasses = new HashSet<ClassInformation>();

    /**
     * Get a list of all classes.
     * 
     * @return
     */
    public Collection<String> getClassNames() {
        return this.classIndex.keySet();
    }

    /**
     * Given a candidate instance name, return the class it belongs to.
     * 
     * @param instanceName
     * @return a String name of the class, null if it is not found in any class.
     */
    public String getClassNameForInstance( String instanceName ) {
        for ( ClassInformation clazz : this.classIndex.values() ) {
            if ( this.getInstanceNamesForClass( clazz.getName() ).contains( instanceName ) ) return clazz.getName();
        }
        return null;
    }

    /**
     * @param uri
     */
    public MgedOntologyHelper( String uri ) {
        this.parse( uri );

        // An empty Database object to hold the MAGE Ontology identifere for MO references
        // Added by Kjell, see below
        mgedMO = new org.biomage.Description.Database();
        mgedMO.setIdentifier( "MO" );
        org.biomage.Common.NameValueType emptyObjectTag = new org.biomage.Common.NameValueType();
        emptyObjectTag.setName( "Placeholder" );
        mgedMO.addToPropertySets( emptyObjectTag );

        // Initialising look-up exception map
        modelToOntologyClassMap.put( "Value", "Measurement" ); // FactorValue

    }

    /**
     * Not sure what this does.
     * 
     * @param referingClassName
     * @param ontEntryName
     * @return
     */
    public String checkEntryName( String referingClassName, String ontEntryName ) {
        String retName = null;

        // Direct lookup, MGED Ontology class name equals refering objects class name + ontologyEntryName
        String query = new String( referingClassName + ontEntryName );
        if ( classExists( query ) ) {
            retName = query;
        }

        // Remove last 's' in case of plural 's' before comparing
        String withoutPluralS = null;
        if ( retName == null && ontEntryName.endsWith( "s" ) ) {
            withoutPluralS = ontEntryName.substring( 0, ontEntryName.length() - 1 );
            query = referingClassName + withoutPluralS;
            if ( classExists( query ) ) {
                retName = query;
            }
        }

        // Try Lookup via referingObject's equiv. MGED Ontology class
        if ( retName == null ) {
            if ( classExists( referingClassName ) ) {
                // NB: Only gets props inherited from superclasses one level up
                Map<String, String> props = this.getPropertiesInfo( referingClassName, true );

                Iterator<String> it = props.keySet().iterator();
                while ( it.hasNext() ) {
                    String propName = it.next();
                    // Compare ontEntryName directly with end of propName
                    if ( propName.toLowerCase().endsWith( ontEntryName.toLowerCase() ) ) {
                        retName = props.get( propName );
                    }
                    // Remove last 's' in case of plural 's' before comparing
                    if ( retName == null && withoutPluralS != null ) {
                        if ( propName.toLowerCase().endsWith( withoutPluralS.toLowerCase() ) ) {
                            retName = props.get( propName );
                        }
                    }
                }

                // Try lookup via MO equiv. of referingObject's parents recursively
                if ( retName == null ) {
                    ClassInformation classInfo = classIndex.get( referingClassName );

                    Collection<ClassInformation> parents = classInfo.getParents();
                    if ( parents != null && parents.size() > 0 ) {
                        Iterator<ClassInformation> cit = parents.iterator();
                        while ( it.hasNext() && retName == null ) {
                            String parentName = cit.next().getName();
                            retName = checkEntryName( parentName, ontEntryName );
                        }
                    }
                }
            }
        }

        return retName;
    }

    /**
     * @param className
     * @return true if className is a valid MO class.
     */
    public boolean classExists( String className ) {
        return classIndex.containsKey( className );
    }

    /**
     * Return a list of names (Strings) for possible enum values of this MGED Ontology property
     * 
     * @param mgedPropertyName
     * @return
     */
    public Collection<String> getEnumValues( String mgedPropertyName ) {
        Iterator<PropertyInformation> it = propertyIndex.iterator();

        PropertyInformation propInfo = null;
        while ( it.hasNext() && propInfo == null ) {
            propInfo = it.next();
            if ( !propInfo.name.equals( mgedPropertyName ) ) {
                propInfo = null;
            }
        }

        if ( propInfo != null ) {
            if ( propInfo.typeName.equals( "enum" ) ) {
                return propInfo.enumValues;
            }
            return null;

        }
        return null;

    }

    /**
     * Return a collection of names (Strings) for possible instances of this MGED Ontology Class
     * 
     * @param mgedClassName
     * @return
     */
    public Collection<String> getInstanceNamesForClass( String mgedClassName ) {
        ClassInformation classInfo = classIndex.get( mgedClassName );

        if ( classInfo != null ) {
            return classInfo.getInstancesNames();
        }
        // Given class name not an MGED Ontology Class...
        return null;

    }

    /**
     * Find corresponding MGED Ontology Class to the OntologyEntry in question
     * 
     * @param term
     * @return
     */
    public org.biomage.Description.DatabaseEntry getOntologyReference( String term ) {
        // Create an empty DatabaseEntry
        org.biomage.tools.ontology.MGEDOntologyEntry.OntologyDatabaseEntry dbEntry = new org.biomage.tools.ontology.MGEDOntologyEntry.OntologyDatabaseEntry();

        // String termEsc = org.biomage.tools.ontology.StringManipHelpers.escapeXMLSensitiveCharacters(term);

        // Add accession and URI
        dbEntry.setAccession( "#" + term );
        // dbEntry.setURI(ontologyURL+"#"+term);
        dbEntry.setURI( "http://mged.sourceforge.net/ontologies/MGEDontology.php#" + term );

        // Reference the empty DataBase object that holds the MO id
        dbEntry.setDatabase( mgedMO );

        return dbEntry;
    }

    /**
     * @param classInfo
     * @param propertyName
     * @return
     */
    public Collection<String> getPossibleNames( ClassInformation classInfo, String propertyName ) {
        if ( classInfo.hasProperty( propertyName ) ) {
            PropertyInformation property = classInfo.getProperty( propertyName );
            if ( property.isPrimitive ) {
                return null;
            }
            ClassInformation typeInfo = property.typeInfo;
            if ( typeInfo.hasInstances() ) {
                return typeInfo.getInstancesNames();
            }
            return null;

        }
        // it does not have the property, we have to look at its parents
        Iterator<ClassInformation> it = classInfo.getParents().iterator();
        while ( it.hasNext() ) {
            ClassInformation parent = it.next();
            Collection<String> results = getPossibleNames( parent, propertyName );
            if ( results != null ) {
                return results;
            }
        }

        return null;
    }

    /**
     * @param object
     * @param propertyName
     * @return
     */
    public Collection<String> getPossibleNames( Extendable object, String propertyName ) {
        String className = object.getClass().getName();
        className = className.substring( 12 ); // get rid of "org.biomage."
        String packageName = className.substring( 0, className.indexOf( "." ) );
        packageName += "Package";
        className = object.getModelClassName();

        if ( !classExists( packageName ) ) {
            return null;
        }
        if ( !classExists( className ) ) {
            return null;
        }

        ClassInformation classInfo = classIndex.get( className );
        return getPossibleNames( classInfo, propertyName );
    }

    /**
     * Find corresponding MGED Ontology Class to the OntologyEntry in question
     * 
     * @param mgedClassName
     * @return
     */
    public Map<String, String> getPropertiesInfo( String mgedClassName ) {
        return getPropertiesInfo( mgedClassName, false );
    }

    /**
     * Return a map of property name,type pairs for the given MGED Ontology Class - alternatively collect property info
     * from parents also (not grand parents and above)
     * 
     * @param mgedClassName
     * @param lookInParentsAlso
     * @return
     */
    public Map<String, String> getPropertiesInfo( String mgedClassName, boolean lookInParentsAlso ) {
        ClassInformation classInfo = classIndex.get( mgedClassName );

        if ( classInfo != null ) {
            java.util.Stack<ClassInformation> classInfosStack = new java.util.Stack<ClassInformation>();
            Map<String, String> propNameType = new HashMap<String, String>();

            classInfosStack.push( classInfo );

            // Had to stop the backwards traversing after 1 step:
            // 2 problems: getting over in MO specific container classes
            // and seem to be a loop up there somewhere...

            while ( !classInfosStack.empty() ) {
                classInfo = classInfosStack.pop();
                Map<Object, PropertyInformation> propInfos = classInfo.getProperties();

                Iterator<Object> it = propInfos.keySet().iterator();
                while ( it.hasNext() ) {
                    String propName = ( String ) it.next();
                    String propType = propInfos.get( propName ).typeName;

                    // Check that property doesn't allready exist
                    // i.e. the case of subclass overriding parent.
                    if ( propNameType.get( propName ) == null ) {
                        propNameType.put( propName, propType );
                    }
                }

                // Look for properties of parent classes
                if ( lookInParentsAlso ) {
                    List<ClassInformation> parentsClassInfos = classInfo.getParents();
                    Iterator<ClassInformation> mit = parentsClassInfos.iterator();
                    while ( mit.hasNext() ) {
                        classInfosStack.push( mit.next() );
                    }
                    lookInParentsAlso = false; // To not proceed recursively up the hierarchy
                }

            }
            return propNameType;
        }
        // Given class name not an MGED Ontology Class...
        return null;

    }

    /**
     * @param ontClass
     * @return
     */
    public Collection<String> getSubClassNames( String ontClass ) {
        ClassInformation classInfo = classIndex.get( ontClass );
        Collection<ClassInformation> subClasses = classInfo.getSubClasses();

        Collection<String> subClassNames = new HashSet<String>( subClasses.size() );

        Iterator<ClassInformation> it = subClasses.iterator();
        while ( it.hasNext() ) {
            ClassInformation subClassInfo = it.next();
            subClassNames.add( subClassInfo.getName() );
        }

        return subClassNames;
    }

    /**
     * Check if given MGED Ontology Class is instantiable
     * 
     * @param mgedClassName
     * @return
     */
    public boolean isInstantiable( String mgedClassName ) {
        ClassInformation classInfo = classIndex.get( mgedClassName );

        if ( classInfo != null ) {
            if ( classInfo.getInstances().size() == 0 ) {
                return false;
            }
            return true;

        }
        // Given class name not an MGED Ontology Class...
        log.warn( mgedClassName + " not a MGED Ontology class..." );
        return false;

    }

    /**
     * @param uri
     */
    public void parse( String uri ) {
        XMLReader parser = null;

        try {
            parser = XMLReaderFactory.createXMLReader( PARSER_NAME );
            OntologyDAMLHandler theHandler = new OntologyDAMLHandler();
            parser.setContentHandler( theHandler );
            parser.setErrorHandler( theHandler );
            parser.parse( uri );
        } catch ( SAXException e ) {
            log.error( e, e );
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            log.error( e, e );
            throw new RuntimeException( e );
        }

        buildClassTree();
    }

    /**
     * @param referingObject
     * @param ontologyEntryName
     * @return
     */
    public String resolveOntologyClassNameFromModel( Extendable referingObject, String ontologyEntryName ) {
        String retName = null;

        String initCapEntryName = org.biomage.tools.helpers.StringOutputHelpers.initialCap( ontologyEntryName );

        // Direct lookup, OntologyEntry name equals MGED Ontology class name
        if ( classExists( initCapEntryName ) ) {
            retName = initCapEntryName;
        }

        // Direct lookup without plural 's' if present
        if ( retName == null && initCapEntryName.endsWith( "s" ) ) {
            String withoutPluralS = initCapEntryName.substring( 0, initCapEntryName.length() - 1 );
            if ( classExists( withoutPluralS ) ) {
                retName = withoutPluralS;
            }
        }

        // Try to infer MGED Ontology class name from combination of MAGE class name and the OntologyEntry name
        if ( retName == null ) {
            retName = checkEntryName( referingObject.getModelClassName(), initCapEntryName );
        }

        // Check exception map
        if ( retName == null ) {
            if ( modelToOntologyClassMap.containsKey( ontologyEntryName ) ) {
                retName = modelToOntologyClassMap.get( ontologyEntryName );
            }
        }

        return retName;
    }

    /**
     * 
     *
     */
    private void buildClassTree() {

        // assign parents and subclasses
        Iterator<String> it = classIndex.keySet().iterator();
        while ( it.hasNext() ) {
            ClassInformation currentClass = classIndex.get( it.next() );
            Iterator<String> it2 = currentClass.getParentNames().iterator();
            while ( it2.hasNext() ) {
                ClassInformation parent = classIndex.get( it2.next() );
                if ( !currentClass.getParents().contains( parent ) ) {
                    currentClass.addToParents( parent );
                }
                if ( !parent.getSubClasses().contains( currentClass ) ) {
                    parent.addToSubClasses( currentClass );
                }
            }
            currentClass.getParentNames().clear(); // no longer needed
        }

        // assign instances
        // it = instanceIndex.keySet().iterator();
        Iterator<InstanceInformation> iit = instanceIndex.iterator();
        while ( it.hasNext() ) {
            InstanceInformation instance = iit.next();
            if ( classIndex.get( instance.className ) != null ) {
                instance.classInfo = classIndex.get( instance.className );
                instance.classInfo.addToInstances( instance );
            }
            instance.className = null; // no longer needed
        }

        // assign properties
        Iterator<PropertyInformation> pit = propertyIndex.iterator();
        while ( pit.hasNext() ) {
            PropertyInformation property = pit.next();
            if ( property.typeName == null ) {
                System.out.println( "property " + property.name + " has no typeName" );
                continue;
            }
            if ( property.typeName.equals( "enum" ) ) {
                String enumClassName = "enum" + enumID;
                enumID++;
                ClassInformation enumClass = new ClassInformation( enumClassName );
                classIndex.put( enumClassName, enumClass );
                Iterator<String> it2 = property.enumValues.iterator();
                while ( it2.hasNext() ) {
                    InstanceInformation instance = new InstanceInformation( it2.next() );
                    enumClass.addToInstances( instance );
                    instance.classInfo = enumClass;
                }
                property.typeInfo = enumClass;
            } else if ( classIndex.get( property.typeName ) != null ) {
                property.typeInfo = classIndex.get( property.typeName );
                // property.classInfo.addToProperties(property);
            }
        }

        // find root classes
        it = classIndex.keySet().iterator();
        while ( it.hasNext() ) {
            ClassInformation currentClass = classIndex.get( it.next() );
            if ( currentClass.getParents().isEmpty() ) {
                rootClasses.add( currentClass );
            }
        }

        /*
         * it = rootClasses.iterator(); while(it.hasNext()){ ClassInformation rootClass = (ClassInformation) it.next();
         * rootClass.print(0); }
         */

    }

    private static class ClassInformation {
        private Collection<InstanceInformation> instances = new HashSet<InstanceInformation>();
        private String name;

        private Collection<String> parentNames = new HashSet<String>();
        private List<ClassInformation> parents = new ArrayList<ClassInformation>();
        private Map<Object, PropertyInformation> properties = new HashMap<Object, PropertyInformation>();
        private Collection<ClassInformation> subClasses = new HashSet<ClassInformation>();

        public ClassInformation( String _name ) {
            name = _name;
        }

        public void addToInstances( InstanceInformation instance ) {
            instances.add( instance );
        }

        public void addToParentNames( String v ) {
            parentNames.add( v );
        }

        public void addToParents( ClassInformation parent ) {
            parents.add( parent );
        }

        public void addToProperties( PropertyInformation v ) {
            properties.put( v.name, v );
        }

        public void addToSubClasses( ClassInformation classInfo ) {
            subClasses.add( classInfo );
        }

        public Collection<InstanceInformation> getInstances() {
            return instances;
        }

        public Collection<String> getInstancesNames() {
            if ( instances == null ) {
                return null;
            }

            Iterator<InstanceInformation> it = instances.iterator();
            Collection<String> names = new HashSet<String>();

            while ( it.hasNext() ) {
                InstanceInformation instance = it.next();
                names.add( instance.name );
            }

            return names;
        }

        public String getName() {
            return name;
        }

        public Collection<String> getParentNames() {
            return parentNames;
        }

        public List<ClassInformation> getParents() {
            return parents;
        }

        public Map<Object, PropertyInformation> getProperties() {
            return properties;
        }

        public PropertyInformation getProperty( String propertyName ) {
            return properties.get( propertyName );
        }

        public Collection<ClassInformation> getSubClasses() {
            return subClasses;
        }

        public boolean hasInstances() {
            return !( instances.isEmpty() );
        }

        public boolean hasProperty( String propertyName ) {
            return properties.containsKey( propertyName );
        }

        /**
         * @param depth
         */
        public void print( int depth ) {
            System.out.print( makeIndent( depth ) + name );
            if ( !parents.isEmpty() ) {
                System.out.println( " (parent: " + parents.get( 0 ).getName() + ")" );
            } else {
                System.out.println( "" );
            }
            printProperties( depth + 1 );
            printInstances( depth + 1 );
            Iterator<ClassInformation> it = subClasses.iterator();
            while ( it.hasNext() ) {
                ClassInformation subClass = it.next();
                // System.out.println(makeIndent(depth)+ subClass.getName());
                subClass.print( depth + 1 );
            }
        }

        /**
         * @param depth
         */
        public void printInstances( int depth ) {
            Iterator<InstanceInformation> it = instances.iterator();
            while ( it.hasNext() ) {
                InstanceInformation instance = it.next();
                System.out.println( makeIndent( depth ) + "[" + instance.name + "]" );
            }
        }

        /**
         * @param depth
         */
        public void printProperties( int depth ) {
            Iterator<PropertyInformation> it = properties.values().iterator();
            while ( it.hasNext() ) {
                PropertyInformation property = it.next();
                System.out.print( makeIndent( depth ) + "." + property.name );
                if ( property.typeName == null ) {
                    System.out.print( " of NO TYPE!" );
                    continue;
                }
                if ( property.typeName.equals( "any" ) ) {
                    System.out.println( " of any type" );
                } else if ( property.isPrimitive ) {
                    System.out.println( " of type " + property.typeName + " PRIMITIVE" );
                } else {
                    System.out.println( " of type " + property.typeInfo.getName() );
                }
            }
        }

        public void setName( String v ) {
            name = v;
        }

        private String makeIndent( int depth ) {
            String result = "";
            for ( int i = 0; i < depth; i++ ) {
                result += "\t";
            }
            return result;
        }

    }

    static class InstanceInformation {
        public ClassInformation classInfo;
        public String className;
        public String name;

        public InstanceInformation( String _name ) {
            name = _name;
        }
    }

    class OntologyDAMLHandler implements org.xml.sax.ContentHandler, org.xml.sax.ErrorHandler {
        private ClassInformation currentClass = null;
        private String currentClassName = null;
        private InstanceInformation currentInstance = null;
        private PropertyInformation currentProperty = null;
        private String currentPropertyName = null;
        private String currentResourceName = null;
        private PathStack path = new PathStack();

        private boolean withinEnum = false;

        @Override
        public void characters( char[] ch, int start, int length ) {
        }

        @Override
        public void endDocument() {
        }

        @Override
        public void endElement( String namespaceURI, String localName, String qName ) {
            path.pop();
        }

        @Override
        public void endPrefixMapping( String prefix ) {
        }

        @Override
        public void error( SAXParseException exception ) {
        }

        @Override
        public void fatalError( SAXParseException exception ) {
        }

        @Override
        public void ignorableWhitespace( char[] ch, int start, int length ) {
        }

        @Override
        public void processingInstruction( String target, String data ) {
        }

        @Override
        public void setDocumentLocator( Locator locator ) {
        }

        @Override
        public void skippedEntity( String name ) {
        }

        @Override
        public void startDocument() {
        }

        @Override
        public void startElement( String namespaceURI, String localName, String qName, Attributes atts ) {
            path.push( qName );
            if ( qName.equals( "daml:Class" ) ) {
                if ( path.getParent().equals( "rdfs:subClassOf" ) ) {
                    String temp = null;
                    if ( ( temp = atts.getValue( "rdf:about" ) ) != null ) {
                        String parentName = extractClassName( temp );
                        currentClass.addToParentNames( parentName );
                    }
                } else if ( path.getParent().equals( "rdf:RDF" ) ) {
                    String temp = null;
                    if ( ( temp = atts.getValue( "rdf:about" ) ) != null ) {
                        currentClassName = extractClassName( temp );
                        ClassInformation classInfo = new ClassInformation( currentClassName );
                        classIndex.put( currentClassName, classInfo );
                        currentClass = classInfo;
                    }
                } else if ( path.endsWith( "rdf:Description/rdf:type/daml:Class" ) ) {
                    String temp = null;
                    if ( ( temp = atts.getValue( "rdf:about" ) ) != null ) {
                        currentInstance = new InstanceInformation( currentResourceName );
                        currentInstance.name = currentResourceName;
                        // instanceIndex.put(currentResourceName, currentInstance);
                        instanceIndex.add( currentInstance );
                        currentInstance.className = extractClassName( temp );
                    }
                }
            } else if ( qName.equals( "rdf:Description" ) ) {
                String temp = null;
                if ( ( temp = atts.getValue( "rdf:about" ) ) != null ) {
                    currentResourceName = extractClassName( temp );
                }
            } else if ( qName.equals( "daml:onProperty" ) ) {
                String temp = null;
                if ( ( temp = atts.getValue( "rdf:resource" ) ) != null ) {
                    currentPropertyName = extractClassName( temp );
                    currentProperty = new PropertyInformation( currentPropertyName );
                    currentClass.addToProperties( currentProperty );
                    propertyIndex.add( currentProperty );
                }
            } else if ( qName.equals( "daml:nil" ) ) { // list ends
                withinEnum = false;
            } else if ( qName.equals( "daml:Thing" ) && withinEnum ) {
                String temp = null;
                if ( ( temp = atts.getValue( "rdf:about" ) ) != null ) {
                    currentProperty.enumValues.add( extractClassName( temp ) );
                }
                // System.out.println("Added to "+currentProperty.name+": "+extractClassName(temp));
            }

            // Primitive property
            if ( path.endsWith( "daml:Class/rdfs:subClassOf/daml:Restriction/daml:hasClass" ) ) {
                String temp = null;
                if ( ( temp = atts.getValue( "rdf:resource" ) ) != null ) {
                    currentProperty.typeName = extractClassName( temp );
                    currentProperty.isPrimitive = true;
                }
            }

            // Property with filler
            if ( path.endsWith( "daml:Class/rdfs:subClassOf/daml:Restriction/daml:hasClass/daml:Class" ) ) {
                // System.out.println("yes");
                String temp = null;
                if ( ( temp = atts.getValue( "rdf:about" ) ) != null ) {
                    currentProperty.typeName = extractClassName( temp );
                    /*
                     * System.out.println("Adding property " +currentProperty.name +" to class "
                     * +currentProperty.typeName);
                     */
                } else { // enum
                    withinEnum = true;
                    currentProperty.typeName = "enum";
                    currentProperty.enumValues = new HashSet<String>();
                }
            }

            if ( path.endsWith( "daml:Class/rdfs:subClassOf/daml:Restriction/daml:hasClass/daml:Thing" ) ) {
                currentProperty.typeName = "any";
            }
        }

        @Override
        public void startPrefixMapping( String prefix, String uri ) {
        }

        @Override
        public void warning( SAXParseException exception ) {
        }

        private String extractClassName( String uri ) {
            return uri.substring( uri.indexOf( "#" ) + 1 );
        }
    }

    static class PathStack {
        private List<String> list = new ArrayList<String>();

        public boolean endsWith( String ending ) {
            return this.getAsString().endsWith( ending );
        }

        public boolean equals( String string ) {
            return string.equals( this.getAsString() );
        }

        public String getAncestor( int i ) {
            if ( i < 1 ) {
                return null;
            }
            return list.get( list.size() - 1 - i );
        }

        public String getAsString() {
            String result = "";
            Iterator<String> it = list.iterator();
            while ( it.hasNext() ) {
                result += it.next();
                if ( it.hasNext() ) {
                    result += "/";
                }
            }
            return result;
        }

        public String getCurrent() {
            return list.get( list.size() - 1 );
        }

        public String getGrandParent() {
            return list.get( list.size() - 1 - 2 );
        }

        public String getParent() {
            return list.get( list.size() - 1 - 1 );
        }

        public String pop() {
            return list.remove( list.size() - 1 );
        }

        public void push( String current ) {
            list.add( current );
        }
    }

    static class PropertyInformation {
        public Collection<String> enumValues;
        public boolean isPrimitive = false;
        public String name;
        public ClassInformation typeInfo;
        public String typeName;

        public PropertyInformation( String _name ) {
            name = _name;
        }
    }
}
