package org.apache.woden.wsdl20.xml;

import java.net.URI;

import org.apache.woden.WSDLException;
import org.apache.woden.WSDLFactory;
import org.apache.woden.types.NamespaceDeclaration;
import org.apache.woden.wsdl20.xml.DescriptionElement;
import org.apache.woden.wsdl20.xml.InterfaceElement;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class WSDLElementTest extends TestCase {
    private String prefix1 = "prefix1";
    private String prefix2 = "prefix2";
    private String prefix3 = "prefix3";
    private String prefix4 = "prefix4";
    private String prefix5 = "prefix5";
    private String prefix6 = "prefix6";
    private String nonExistantPrefix = "nonExistantPrefix";
    private URI namespace1 = null; 
    private URI namespace2 = null;
    private URI namespace3 = null;
    private URI namespace4 = null;
    private URI namespace5 = null;
    private URI namespace6 = null;
    private URI nonExistantNamespace = null;
    private WSDLFactory factory = null;
    
    public static Test suite()
    {
       return new TestSuite(WSDLElementTest.class);
    }
       
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
        super.setUp();
        
        try {
            factory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            fail("Can't instantiate the WSDLFactory object.");
        }
        
        namespace1 = new URI("http://apache.org/namespaceURIa");
        namespace2 = new URI("http://apache.org/namespaceURIb");
        namespace3 = new URI("http://apache.org/namespaceURIc");
        namespace4 = new URI("http://apache.org/namespaceURId");
        namespace5 = new URI("http://apache.org/namespaceURIe");
        namespace6 = new URI("http://apache.org/namespaceURIf");
        nonExistantNamespace = new URI("http://apache.org/nonExistantNamespace");
    }
    
    public void testAddGetNamespace() {
        DescriptionElement descriptionElement = factory.newDescription();
        descriptionElement.addNamespace(prefix1,namespace1);
        descriptionElement.addNamespace(null, namespace2);
        
            //Get namespace and prefix on description
            URI uri = descriptionElement.getNamespaceURI(prefix1);
            assertEquals("Retrieved NamespaceURI does not match that set", namespace1, uri);
    
            String prefix = descriptionElement.getNamespacePrefix(namespace1);
            assertEquals("Retrieved NamespacePrefix does not match that set", prefix1, prefix);
            
            //Get default namespace
            uri = descriptionElement.getNamespaceURI("");
            assertEquals("Default NamespaceURI does not match that set", namespace2, uri);
    
            //Get undefined namespace and prefix.
            assertNull("Null was not returned when a non-existent namespace prefix was given", descriptionElement.getNamespaceURI(nonExistantPrefix));
            assertNull("Null was not returned when a non-existent namespace URI was given", descriptionElement.getNamespacePrefix(nonExistantNamespace));
    
        InterfaceElement interfaceElement = descriptionElement.addInterfaceElement();
        interfaceElement.addNamespace(prefix3, namespace3);
        
            //Get namespace and prefix on description
            uri = interfaceElement.getNamespaceURI(prefix1);
            assertEquals("Retrieved NamespaceURI does not match that set", namespace1, uri);
    
            prefix = interfaceElement.getNamespacePrefix(namespace1);
            assertEquals("Retrieved NamespacePrefix does not match that set", prefix1, prefix);
            
            //Get default namespace on description
            uri = interfaceElement.getNamespaceURI("");
            assertEquals("Default NamespaceURI does not match that set", namespace2, uri);
            
            //Get namespace and prefix on interface
            uri = interfaceElement.getNamespaceURI(prefix3);
            assertEquals("Retrieved NamespaceURI does not match that set", namespace3, uri);
    
            prefix = interfaceElement.getNamespacePrefix(namespace3);
            assertEquals("Retrieved NamespacePrefix does not match that set", prefix3, prefix);
    
            //Get undefined namespace and prefix.
            assertNull("Null was not returned when a non-existent namespace prefix was given", interfaceElement.getNamespaceURI(nonExistantPrefix));
            assertNull("Null was not returned when a non-existent namespace URI was given", interfaceElement.getNamespacePrefix(nonExistantNamespace));
        
        InterfaceFaultElement interfaceFaultElement = interfaceElement.addInterfaceFaultElement();
        interfaceFaultElement.addNamespace(prefix4, namespace4);
        
            //Get namespace and prefix on description
            uri = interfaceFaultElement.getNamespaceURI(prefix1);
            assertEquals("Retrieved NamespaceURI does not match that set", namespace1, uri);
    
            prefix = interfaceFaultElement.getNamespacePrefix(namespace1);
            assertEquals("Retrieved NamespacePrefix does not match that set", prefix1, prefix);
            
            //Get default namespace on description
            uri = interfaceFaultElement.getNamespaceURI("");
            assertEquals("Default NamespaceURI does not match that set", namespace2, uri);
            
            //Get namespace and prefix on interface
            uri = interfaceFaultElement.getNamespaceURI(prefix3);
            assertEquals("Retrieved NamespaceURI does not match that set", namespace3, uri);
    
            prefix = interfaceFaultElement.getNamespacePrefix(namespace3);
            assertEquals("Retrieved NamespacePrefix does not match that set", prefix3, prefix);
            
            //Get namespace and prefix on interface fault
            uri = interfaceFaultElement.getNamespaceURI(prefix4);
            assertEquals("Retrieved NamespaceURI does not match that set", namespace4, uri);
    
            prefix = interfaceFaultElement.getNamespacePrefix(namespace4);
            assertEquals("Retrieved NamespacePrefix does not match that set", prefix4, prefix);
    
            //Get undefined namespace and prefix.
            assertNull("Null was not returned when a non-existent namespace prefix was given", interfaceFaultElement.getNamespaceURI(nonExistantPrefix));
            assertNull("Null was not returned when a non-existent namespace URI was given", interfaceFaultElement.getNamespacePrefix(nonExistantNamespace));
        
        BindingElement bindingElement = descriptionElement.addBindingElement();
        bindingElement.addNamespace(prefix5, namespace5);
        
            //Get namespace and prefix on description
            uri = bindingElement.getNamespaceURI(prefix1);
            assertEquals("Retrieved NamespaceURI does not match that set", namespace1, uri);
    
            prefix = bindingElement.getNamespacePrefix(namespace1);
            assertEquals("Retrieved NamespacePrefix does not match that set", prefix1, prefix);
            
            //Get default namespace on description
            uri = bindingElement.getNamespaceURI("");
            assertEquals("Default NamespaceURI does not match that set", namespace2, uri);
            
            //Get namespace and prefix on binding
            uri = bindingElement.getNamespaceURI(prefix5);
            assertEquals("Retrieved NamespaceURI does not match that set", namespace5, uri);
    
            prefix = bindingElement.getNamespacePrefix(namespace5);
            assertEquals("Retrieved NamespacePrefix does not match that set", prefix5, prefix);
    
            //Get undefined namespace and prefix.
            assertNull("Null was not returned when a non-existent namespace prefix was given", bindingElement.getNamespaceURI(nonExistantPrefix));
            assertNull("Null was not returned when a non-existent namespace URI was given", bindingElement.getNamespacePrefix(nonExistantNamespace));
        
        BindingFaultElement bindingFaultElement = bindingElement.addBindingFaultElement();
        bindingFaultElement.addNamespace(prefix6, namespace6);
        
            //Get namespace and prefix on description
            uri = bindingFaultElement.getNamespaceURI(prefix1);
            assertEquals("Retrieved NamespaceURI does not match that set", namespace1, uri);
    
            prefix = bindingFaultElement.getNamespacePrefix(namespace1);
            assertEquals("Retrieved NamespacePrefix does not match that set", prefix1, prefix);
            
            //Get default namespace on description
            uri = bindingFaultElement.getNamespaceURI("");
            assertEquals("Default NamespaceURI does not match that set", namespace2, uri);
            
            //Get namespace and prefix on binding
            uri = bindingFaultElement.getNamespaceURI(prefix5);
            assertEquals("Retrieved NamespaceURI does not match that set", namespace5, uri);
    
            prefix = bindingFaultElement.getNamespacePrefix(namespace5);
            assertEquals("Retrieved NamespacePrefix does not match that set", prefix5, prefix);
            
            //Get namespace and prefix on binding fault
            uri = bindingFaultElement.getNamespaceURI(prefix6);
            assertEquals("Retrieved NamespaceURI does not match that set", namespace6, uri);
    
            prefix = bindingFaultElement.getNamespacePrefix(namespace6);
            assertEquals("Retrieved NamespacePrefix does not match that set", prefix6, prefix);
    
            //Get undefined namespace and prefix.
            assertNull("Null was not returned when a non-existent namespace prefix was given", bindingFaultElement.getNamespaceURI(nonExistantPrefix));
            assertNull("Null was not returned when a non-existent namespace URI was given", bindingFaultElement.getNamespacePrefix(nonExistantNamespace));
    }
    
    public void testRemoveNamespace() {
        //Build up wsdl20 element model.
        DescriptionElement descriptionElement = factory.newDescription();
        descriptionElement.addNamespace(prefix1,namespace1);
        assertNotNull(descriptionElement.getNamespaceURI(prefix1));
        assertNotNull(descriptionElement.getNamespacePrefix(namespace1));
        
        TypesElement typesElement = null;
        try {
            typesElement = descriptionElement.addTypesElement();
        } catch (WSDLException e) {
            //This will never happen as I've only added one types element to the description in since creating it 8 lines above.
            fail("Unexpected exception was thrown: " + e.getMessage()); //But in case...
        }
        typesElement.addNamespace(prefix2, namespace2);
        assertNotNull(typesElement.getNamespaceURI(prefix2));
        assertNotNull(typesElement.getNamespacePrefix(namespace2));
        assertNotNull(typesElement.getNamespaceURI(prefix1));
        assertNotNull(typesElement.getNamespacePrefix(namespace1));
        
        DocumentationElement documentationElement = typesElement.addDocumentationElement();
        documentationElement.addNamespace(prefix3, namespace3);
        assertNotNull(documentationElement.getNamespaceURI(prefix3));
        assertNotNull(documentationElement.getNamespacePrefix(namespace3));
        assertNotNull(documentationElement.getNamespaceURI(prefix2));
        assertNotNull(documentationElement.getNamespacePrefix(namespace2));
        assertNotNull(documentationElement.getNamespaceURI(prefix1));
        assertNotNull(documentationElement.getNamespacePrefix(namespace1));

        //Remove non existant namespace.
        typesElement.removeNamespace(nonExistantPrefix);
        assertNotNull(documentationElement.getNamespaceURI(prefix3));
        assertNotNull(documentationElement.getNamespacePrefix(namespace3));
        assertNotNull(documentationElement.getNamespaceURI(prefix2));
        assertNotNull(documentationElement.getNamespacePrefix(namespace2));
        assertNotNull(documentationElement.getNamespaceURI(prefix1));
        assertNotNull(documentationElement.getNamespacePrefix(namespace1));
        
        descriptionElement.removeNamespace(nonExistantPrefix);
        assertNotNull(documentationElement.getNamespaceURI(prefix3));
        assertNotNull(documentationElement.getNamespacePrefix(namespace3));
        assertNotNull(documentationElement.getNamespaceURI(prefix2));
        assertNotNull(documentationElement.getNamespacePrefix(namespace2));
        assertNotNull(documentationElement.getNamespaceURI(prefix1));
        assertNotNull(documentationElement.getNamespacePrefix(namespace1));
        
        documentationElement.removeNamespace(nonExistantPrefix);
        assertNotNull(documentationElement.getNamespaceURI(prefix3));
        assertNotNull(documentationElement.getNamespacePrefix(namespace3));
        assertNotNull(documentationElement.getNamespaceURI(prefix2));
        assertNotNull(documentationElement.getNamespacePrefix(namespace2));
        assertNotNull(documentationElement.getNamespaceURI(prefix1));
        assertNotNull(documentationElement.getNamespacePrefix(namespace1));
        
        //Remove namespaces one at a time.
        descriptionElement.removeNamespace(prefix1);
        descriptionElement.removeNamespace(prefix2);
        descriptionElement.removeNamespace(prefix3);
        assertNotNull(documentationElement.getNamespaceURI(prefix3));
        assertNotNull(documentationElement.getNamespacePrefix(namespace3));
        assertNotNull(documentationElement.getNamespaceURI(prefix2));
        assertNotNull(documentationElement.getNamespacePrefix(namespace2));
        assertNull(documentationElement.getNamespaceURI(prefix1));
        assertNull(documentationElement.getNamespacePrefix(namespace1));

        typesElement.removeNamespace(prefix2);
        typesElement.removeNamespace(prefix3);
        assertNotNull(documentationElement.getNamespaceURI(prefix3));
        assertNotNull(documentationElement.getNamespacePrefix(namespace3));
        assertNull(documentationElement.getNamespaceURI(prefix2));
        assertNull(documentationElement.getNamespacePrefix(namespace2));
        assertNull(documentationElement.getNamespaceURI(prefix1));
        assertNull(documentationElement.getNamespacePrefix(namespace1));

        documentationElement.removeNamespace(prefix3);
        assertNull(documentationElement.getNamespaceURI(prefix3));
        assertNull(documentationElement.getNamespacePrefix(namespace3));
        assertNull(documentationElement.getNamespaceURI(prefix2));
        assertNull(documentationElement.getNamespacePrefix(namespace2));
        assertNull(documentationElement.getNamespaceURI(prefix1));
        assertNull(documentationElement.getNamespacePrefix(namespace1));
    }
    
    public void testGetDeclaredAndInScopeNamespaces() {
        //Test flags
        boolean ns1;
        boolean ns2;
        boolean ns3;
        boolean ns4;
        boolean ns5;
        boolean ns6;
        
        NamespaceDeclaration[] namespaces;
        
        //Description with two namespaces.
        DescriptionElement descriptionElement = factory.newDescription();
        descriptionElement.addNamespace(prefix1,namespace1);
        descriptionElement.addNamespace(prefix2,namespace2);
        //Service on description with two name spaces.
        ServiceElement serviceElement = descriptionElement.addServiceElement();
        serviceElement.addNamespace(prefix3, namespace3);
        serviceElement.addNamespace(prefix4, namespace4);
        //Endpoint on description with two namespaces
        EndpointElement endpointElement = serviceElement.addEndpointElement();
        endpointElement.addNamespace(prefix5, namespace5);
        endpointElement.addNamespace(prefix6, namespace6);
        
        //Get description declared name spaces.
        namespaces = descriptionElement.getDeclaredNamespaces();
        assertEquals("Expected 2 namespaces", namespaces.length, 2);
        
        ns1 = false;
        ns2 = false;
        for(int i=0; i<namespaces.length; i++) {
            if (namespaces[i].getNamespaceURI().equals(namespace1) && namespaces[i].getPrefix().equals(prefix1))
                ns1 = true;
            if (namespaces[i].getNamespaceURI().equals(namespace2) && namespaces[i].getPrefix().equals(prefix2))
                ns2 = true;
        }

        assertTrue("Expected NamespaceURI not found", ns1);
        assertTrue("Expected NamespaceURI not found", ns2);
        
        //Get description in scope namespaces.
        namespaces = descriptionElement.getInScopeNamespaces();
        assertEquals("Expected 2 namespaces", namespaces.length, 2);
        
        ns1 = false;
        ns2 = false;
        for(int i=0; i<namespaces.length; i++) {
            if (namespaces[i].getNamespaceURI().equals(namespace1) && namespaces[i].getPrefix().equals(prefix1))
                ns1 = true;
            if (namespaces[i].getNamespaceURI().equals(namespace2) && namespaces[i].getPrefix().equals(prefix2))
                ns2 = true;
        }

        assertTrue("Expected NamespaceURI not found", ns1);
        assertTrue("Expected NamespaceURI not found", ns2);
        
        //Get service declared name spaces.
        namespaces = serviceElement.getDeclaredNamespaces();
        assertEquals("Expected 2 namespaces", namespaces.length, 2);
        
        ns3 = false;
        ns4 = false;
        for(int i=0; i<namespaces.length; i++) {
            if (namespaces[i].getNamespaceURI().equals(namespace3) && namespaces[i].getPrefix().equals(prefix3))
                ns3 = true;
            if (namespaces[i].getNamespaceURI().equals(namespace4) && namespaces[i].getPrefix().equals(prefix4))
                ns4 = true;
        }

        assertTrue("Expected NamespaceURI not found", ns3);
        assertTrue("Expected NamespaceURI not found", ns4);
        
        //Get service in scope namespaces.
        namespaces = serviceElement.getInScopeNamespaces();
        assertEquals("Expected 4 namespaces", namespaces.length, 4);
 
        ns1 = false;
        ns2 = false;
        ns3 = false;
        ns4 = false;
        for(int i=0; i<namespaces.length; i++) {
            if (namespaces[i].getNamespaceURI().equals(namespace1) && namespaces[i].getPrefix().equals(prefix1))
                ns1 = true;
            if (namespaces[i].getNamespaceURI().equals(namespace2) && namespaces[i].getPrefix().equals(prefix2))
                ns2 = true;
            if (namespaces[i].getNamespaceURI().equals(namespace3) && namespaces[i].getPrefix().equals(prefix3))
                ns3 = true;
            if (namespaces[i].getNamespaceURI().equals(namespace4) && namespaces[i].getPrefix().equals(prefix4))
                ns4 = true;
        }

        assertTrue("Expected NamespaceURI not found", ns1);
        assertTrue("Expected NamespaceURI not found", ns2);
        assertTrue("Expected NamespaceURI not found", ns3);
        assertTrue("Expected NamespaceURI not found", ns4);
        
        //Get endpoint declared name spaces.
        namespaces = endpointElement.getDeclaredNamespaces();
        assertEquals("Expected 2 namespaces", namespaces.length, 2);
        
        ns5 = false;
        ns6 = false;
        for(int i=0; i<namespaces.length; i++) {
            if (namespaces[i].getNamespaceURI().equals(namespace5) && namespaces[i].getPrefix().equals(prefix5))
                ns5 = true;
            if (namespaces[i].getNamespaceURI().equals(namespace6) && namespaces[i].getPrefix().equals(prefix6))
                ns6 = true;
        }

        assertTrue("Expected NamespaceURI not found", ns5);
        assertTrue("Expected NamespaceURI not found", ns6);
        
        //Get endpoint in scope namespaces.
        namespaces = endpointElement.getInScopeNamespaces();
        assertEquals("Expected 6 namespaces", namespaces.length, 6);
 
        ns1 = false;
        ns2 = false;
        ns3 = false;
        ns4 = false;
        ns5 = false;
        ns6 = false;
        
        for(int i=0; i<namespaces.length; i++) {
            if (namespaces[i].getNamespaceURI().equals(namespace1) && namespaces[i].getPrefix().equals(prefix1))
                ns1 = true;
            if (namespaces[i].getNamespaceURI().equals(namespace2) && namespaces[i].getPrefix().equals(prefix2))
                ns2 = true;
            if (namespaces[i].getNamespaceURI().equals(namespace3) && namespaces[i].getPrefix().equals(prefix3))
                ns3 = true;
            if (namespaces[i].getNamespaceURI().equals(namespace4) && namespaces[i].getPrefix().equals(prefix4))
                ns4 = true;
            if (namespaces[i].getNamespaceURI().equals(namespace5) && namespaces[i].getPrefix().equals(prefix5))
                ns5 = true;
            if (namespaces[i].getNamespaceURI().equals(namespace6) && namespaces[i].getPrefix().equals(prefix6))
                ns6 = true;
        }

        assertTrue("Expected NamespaceURI not found", ns1);
        assertTrue("Expected NamespaceURI not found", ns2);
        assertTrue("Expected NamespaceURI not found", ns3);
        assertTrue("Expected NamespaceURI not found", ns4);
        assertTrue("Expected NamespaceURI not found", ns5);
        assertTrue("Expected NamespaceURI not found", ns6);
    }
}
