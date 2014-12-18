/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package org.apache.woden.wsdl20.extensions.http;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.wsdl20.extensions.http.HTTPLocation;

/**
 * Functional verification test of HTTPLocation.
 * 
 * @author John Kaputin (jkaputin@apache.org)
 */
public class HTTPLocationTest extends TestCase 
{
    public static Test suite()
    {
        return new TestSuite(HTTPLocationTest.class);
    }
    
    public void testCtor() throws Exception
    {
        HTTPLocation loc;
        
        //empty string
        loc = new HTTPLocation("");
        assertNotNull(loc);
        
        //no curly braces
        loc = new HTTPLocation("/temperature/");
        assertNotNull(loc);
        
        //no curly braces, path and query string
        loc = new HTTPLocation("/temperature/town?month=Jan&day=01");
        assertNotNull(loc);
        
        //one local name
        loc = new HTTPLocation("/temperature/{town}/");
        assertNotNull(loc);
        
        //multiple local names
        loc = new HTTPLocation("/temperature/{town}/{state}/{country}");
        assertNotNull(loc);
        
        //multiple local names, encoded & raw, path & query string
        loc = new HTTPLocation("/temperature/{town}/{!state}/{country}?month={mth}&date={!dt}");
        assertNotNull(loc);
        
        //with double curly braces
        loc = new HTTPLocation("{{XXX}}/temperature/{town}/{{{state}}}/{country}");
        assertNotNull(loc);
        
        //invalid template
        loc = new HTTPLocation("{{XXX}}/te}mp}erature/{town}/{state}/{coun{try}");
        assertNotNull(loc);
        
    }
    
    public void testGetTemplates_noArg() {
        
        HTTPLocation loc;
        loc = new HTTPLocation("/temperature/{town}/{!state}/{country}?temp={tmp}&month={mth}&date={!dt}&place={town}");
        
        HTTPLocationTemplate[] templates = loc.getTemplates();
        assertEquals("Incorrect number of templates", 7, templates.length);
        
        loc = new HTTPLocation("/travel/flight?no=BA6&dep=13.55");
        templates = loc.getTemplates();
        assertEquals("Unexpected templates", 0, templates.length);
        
        loc = new HTTPLocation("/in}valid/abc?{localname}");
        templates = loc.getTemplates();
        assertEquals("Location is invalid, so no templates were expected", 0, templates.length);
    }    
        
    public void testGetTemplatesInPath_noArg() {
        
        HTTPLocation loc;
        loc = new HTTPLocation("/temperature/{town}/{!state}/{country}?temp={tmp}&month={mth}&date={!dt}&place={town}");
        
        HTTPLocationTemplate[] templates = loc.getTemplatesInPath();
        assertEquals("Incorrect number of templates", 3, templates.length);
        for(int i=0; i<templates.length; i++) {
            assertFalse(templates[i].isQuery());
        }
        
        loc = new HTTPLocation("/travel/flight?no=BA6&dep=13.55");
        templates = loc.getTemplatesInPath();
        assertEquals("Unexpected templates", 0, templates.length);
        
        loc = new HTTPLocation("/in}valid/{localname}?abc");
        templates = loc.getTemplatesInPath();
        assertEquals("Location is invalid, so no templates were expected", 0, templates.length);
    }    
        
    public void testGetTemplatesInQuery_noArg() {
        
        HTTPLocation loc;
        loc = new HTTPLocation("/temperature/{town}/{!state}/{country}?temp={tmp}&month={mth}&date={!dt}&place={town}");
        
        HTTPLocationTemplate[] templates = loc.getTemplatesInQuery();
        assertEquals("Incorrect number of templates", 4, templates.length);
        for(int i=0; i<templates.length; i++) {
            assertTrue(templates[i].isQuery());
        }
        
        loc = new HTTPLocation("/travel/flight?no=BA6&dep=13.55");
        templates = loc.getTemplatesInQuery();
        assertEquals("Unexpected templates", 0, templates.length);
        
        loc = new HTTPLocation("/in}valid/abc?{localname}");
        templates = loc.getTemplatesInQuery();
        assertEquals("Location is invalid, so no templates were expected", 0, templates.length);
    }    
        
    public void testGetTemplates_oneArg() {
        
        HTTPLocation loc;
        loc = new HTTPLocation("/temperature/{town}/{!state}/{country}?temp={tmp}&month={mth}&date={!dt}&place={town}");
        
        HTTPLocationTemplate[] templates = loc.getTemplates("town");
        assertEquals("Incorrect number of templates", 2, templates.length);
        
        templates = loc.getTemplates("state");
        assertEquals("Incorrect number of templates", 1, templates.length);
        
        templates = loc.getTemplates("mth");
        assertEquals("Incorrect number of templates", 1, templates.length);
        
        templates = loc.getTemplates("dummy");
        assertEquals("Unexpected templates", 0, templates.length);
        
        loc = new HTTPLocation("/travel/flight?no=BA6&dep=13.55");
        templates = loc.getTemplates("dummy");
        assertEquals("Unexpected templates", 0, templates.length);
        
        loc = new HTTPLocation("/in}valid/abc?{localname}");
        templates = loc.getTemplates("localname");
        assertEquals("Location is invalid, so no templates were expected", 0, templates.length);
    }    
        
    public void testGetTemplatesInPath_oneArg() {
        
        HTTPLocation loc;
        loc = new HTTPLocation("/temperature/{town}/{town}/{!state}/{country}?temp={tmp}&month={mth}&date={!dt}&place={town}");
        
        HTTPLocationTemplate[] templates = loc.getTemplatesInPath("town");
        assertEquals("Incorrect number of templates", 2, templates.length);
        for(int i=0; i<templates.length; i++) {
            assertFalse(templates[i].isQuery());
        }
        
        templates = loc.getTemplatesInPath("country");
        assertEquals("Incorrect number of templates", 1, templates.length);
        assertFalse(templates[0].isQuery());
        
        templates = loc.getTemplatesInPath("dummy");
        assertEquals("Unexpected templates", 0, templates.length);
        
        loc = new HTTPLocation("/travel/flight?no=BA6&dep=13.55");
        templates = loc.getTemplatesInPath("dummy");
        assertEquals("Unexpected templates", 0, templates.length);
        
        loc = new HTTPLocation("/in}valid/{localname}?abc");
        templates = loc.getTemplatesInPath("localname");
        assertEquals("Location is invalid, so no templates were expected", 0, templates.length);
    }    
        
    public void testGetTemplatesInQuery_oneArg() {
        
        HTTPLocation loc;
        loc = new HTTPLocation("/temperature/{town}/{!state}/{country}?temp={tmp}&temp2={tmp}&month={mth}&date={!dt}&place={town}");
        
        HTTPLocationTemplate[] templates = loc.getTemplatesInQuery("tmp");
        assertEquals("Incorrect number of templates", 2, templates.length);
        for(int i=0; i<templates.length; i++) {
            assertTrue(templates[i].isQuery());
        }
        
        templates = loc.getTemplatesInQuery("dt");
        assertEquals("Incorrect number of templates", 1, templates.length);
        assertTrue(templates[0].isQuery());
        
        templates = loc.getTemplatesInQuery("dummy");
        assertEquals("Unexpected templates", 0, templates.length);
        
        loc = new HTTPLocation("/travel/flight?no=BA6&dep=13.55");
        templates = loc.getTemplatesInQuery("dummy");
        assertEquals("Unexpected templates", 0, templates.length);
        
        loc = new HTTPLocation("/in}valid/abc?{localname}");
        templates = loc.getTemplatesInQuery("localname");
        assertEquals("Location is invalid, so no templates were expected", 0, templates.length);
    }    
        
    public void testGetTemplate() {
        
        HTTPLocation loc;
        loc = new HTTPLocation("/temperature/{town}/{!state}/{country}?temp={tmp}&month={mth}&date={!dt}&place={town}");
        
        HTTPLocationTemplate template = loc.getTemplate("town");
        assertNotNull("expected a template", template);
        
        //Where multiple templates exist with the same name, this method should return the first.
        //Check that the template returned is the first (i.e. not the one in the query string).
        assertFalse(template.isQuery());
        
        
        template = loc.getTemplate("dt");
        assertNotNull("expected a template", template);
        
        template = loc.getTemplate("dummy");
        assertNull("Unexpected template", template);
        
        loc = new HTTPLocation("/travel/flight?no=BA6&dep=13.55");
        template = loc.getTemplate("dummy");
        assertNull("Unexpected template", template);
        
        loc = new HTTPLocation("/in}valid/{localname}");
        template = loc.getTemplate("localname");
        assertNull("Location is invalid, so null was expected", template);
    }    
        
    public void testGetTemplateInPath() {
        
        HTTPLocation loc;
        loc = new HTTPLocation("/temperature/{town}/{town}/{!state}/{country}?temp={tmp}&month={mth}&date={!dt}&place={town}");
        
        HTTPLocationTemplate template = loc.getTemplateInPath("town");
        assertNotNull("expected a template", template);
        assertFalse(template.isQuery());
        
        template = loc.getTemplateInPath("state");
        assertNotNull("expected a template", template);
        
        template = loc.getTemplateInPath("mth");
        assertNull("Unexpected template", template);
        
        template = loc.getTemplateInPath("dummy");
        assertNull("Unexpected template", template);
        
        loc = new HTTPLocation("/travel/flight?no=BA6&dep=13.55");
        template = loc.getTemplateInPath("dummy");
        assertNull("Unexpected template", template);
        
        loc = new HTTPLocation("/in}valid/{localname}?abc");
        template = loc.getTemplateInPath("localname");
        assertNull("Location is invalid, so null was expected", template);
    }    
        
    public void testGetTemplateInQuery() {
        
        HTTPLocation loc;
        loc = new HTTPLocation("/temperature/{town}/{!state}/{country}?temp={tmp}&month={mth}&date={!dt}&place={town}&place2={town}");
        
        HTTPLocationTemplate template = loc.getTemplateInQuery("town");
        assertNotNull("expected a template", template);
        assertTrue(template.isQuery());
        
        template = loc.getTemplateInQuery("dt");
        assertNotNull("expected a template", template);
        
        template = loc.getTemplateInQuery("country");
        assertNull("Unexpected template", template);
        
        template = loc.getTemplateInQuery("dummy");
        assertNull("Unexpected template", template);
        
        loc = new HTTPLocation("/travel/flight?no=BA6&dep=13.55");
        template = loc.getTemplateInQuery("dummy");
        assertNull("Unexpected template", template);
        
        loc = new HTTPLocation("/in}valid/abc?{localname}");
        template = loc.getTemplateInQuery("localname");
        assertNull("Location is invalid, so null was expected", template);
    }    
        
    public void testGetTemplateNames() {
        
        HTTPLocation loc;
        loc = new HTTPLocation("/temperature/{town}/{!state}/{country}?temp={tmp}&month={mth}&date={!dt}&place={town}");
        
        String[] names = loc.getTemplateNames();
        assertEquals("Incorrect number of names", 7, names.length);
        assertEquals("Incorrect order of names", "town", names[0]);
        assertEquals("Incorrect order of names", "state", names[1]);
        assertEquals("Incorrect order of names", "country", names[2]);
        assertEquals("Incorrect order of names", "tmp", names[3]);
        assertEquals("Incorrect order of names", "mth", names[4]);
        assertEquals("Incorrect order of names", "dt", names[5]);
        assertEquals("Incorrect order of names", "town", names[6]);
        
        loc = new HTTPLocation("/travel/flight?no=BA6&dep=13.55");
        names = loc.getTemplateNames();
        assertEquals("Unexpected template names", 0, names.length);
        
        loc = new HTTPLocation("/in}valid/{localname}");
        names = loc.getTemplateNames();
        assertEquals("Location is invalid, so no template names were expected", 0, names.length);
    }    
        
    public void testGetTemplateNamesInPath() {
        
        HTTPLocation loc;
        loc = new HTTPLocation("/temperature/{town}/{!state}/{country}?temp={tmp}&month={mth}&date={!dt}&place={town}");
        
        String[] names = loc.getTemplateNamesInPath();
        assertEquals("Incorrect number of names", 3, names.length);
        assertEquals("Incorrect order of names", "town", names[0]);
        assertEquals("Incorrect order of names", "state", names[1]);
        assertEquals("Incorrect order of names", "country", names[2]);
        
        loc = new HTTPLocation("/travel/flight?no=BA6&dep=13.55");
        names = loc.getTemplateNamesInPath();
        assertEquals("Unexpected template names", 0, names.length);
        
        loc = new HTTPLocation("/in}valid/{localname}?abc");
        names = loc.getTemplateNamesInPath();
        assertEquals("Location is invalid, so no template names were expected", 0, names.length);
    }    
        
    public void testGetTemplateNamesInQuery() {
        
        HTTPLocation loc;
        loc = new HTTPLocation("/temperature/{town}/{!state}/{country}?temp={tmp}&month={mth}&date={!dt}&place={town}&temp2={tmp}");
        
        String[] names = loc.getTemplateNamesInQuery();
        assertEquals("Incorrect number of names", 5, names.length);
        assertEquals("Incorrect order of names", "tmp", names[0]);
        assertEquals("Incorrect order of names", "mth", names[1]);
        assertEquals("Incorrect order of names", "dt", names[2]);
        assertEquals("Incorrect order of names", "town", names[3]);
        assertEquals("Incorrect order of names", "tmp", names[4]);
        
        loc = new HTTPLocation("/travel/flight?no=BA6&dep=13.55");
        names = loc.getTemplateNamesInQuery();
        assertEquals("Unexpected template names", 0, names.length);
        
        loc = new HTTPLocation("/in}valid/abc?{localname}");
        names = loc.getTemplateNamesInQuery();
        assertEquals("Location is invalid, so no template names were expected", 0, names.length);
    }    
        
    public void testGetOriginalLocation() {
        
        String origLoc = "/temperature/{town}/{!state}/{country}?temp={tmp}&month={mth}&date={!dt}&place={town}";
        HTTPLocation loc;
        loc = new HTTPLocation(origLoc);
        String returnedLoc = loc.getOriginalLocation();
        assertEquals("Unexpected location value", origLoc, returnedLoc);
        
        origLoc = "/travel/flight?no=BA6&dep=13.55";
        loc = new HTTPLocation(origLoc);
        returnedLoc = loc.getOriginalLocation();
        assertEquals("Unexpected location value", origLoc, returnedLoc);
    }
    
    public void testGetFormattedLocation() {
        
        String origLoc = "/temperature/{town}/{!state}/{country}?temp={tmp}&month={mth}&date={!dt}&place={town}";
        HTTPLocation loc;
        loc = new HTTPLocation(origLoc);
        HTTPLocationTemplate template;
        HTTPLocationTemplate[] templates;
        templates = loc.getTemplates("town");
        templates[0].setValue("Perth");
        template = loc.getTemplate("state");
        template.setValue("WA");
        template = loc.getTemplate("country");
        template.setValue("Australia");
        template = loc.getTemplate("tmp");
        template.setValue("41.5");
        template = loc.getTemplate("mth");
        template.setValue("February");
        template = loc.getTemplate("dt");
        template.setValue("28th");
        templates[1].setValue("Fremantle");
        String formattedLoc = "/temperature/Perth/WA/Australia?temp=41.5&month=February&date=28th&place=Fremantle";
        String returnedLoc = loc.getFormattedLocation();
        assertEquals("Unexpected formatted location value", formattedLoc, returnedLoc);
        
        origLoc = "/temperature/{town}/{!state}/{country}?temp={tmp}&month={mth}&date={!dt}&place={town}";
        loc = new HTTPLocation(origLoc);
        templates = loc.getTemplates("town");
        //templates[0].setValue("Perth"); //if {town} is not substituted, it will be replaced by empty string
        template = loc.getTemplate("state");
        template.setValue("WA");
        template = loc.getTemplate("country");
        template.setValue("Australia");
        template = loc.getTemplate("tmp");
        template.setValue("41.5");
        template = loc.getTemplate("mth");
        template.setValue("February");
        template = loc.getTemplate("dt");
        //template.setValue("28th");  //if {!dt} is not substituted, it will be replaced by empty string
        templates[1].setValue("Fremantle");
        formattedLoc = "/temperature//WA/Australia?temp=41.5&month=February&date=&place=Fremantle";
        returnedLoc = loc.getFormattedLocation();
        assertEquals("Unexpected formatted location value", formattedLoc, returnedLoc);
        
        origLoc = "/travel/flight?no=BA6&dep=13.55";
        loc = new HTTPLocation(origLoc);
        returnedLoc = loc.getFormattedLocation();
        assertEquals("Unexpected location value", origLoc, returnedLoc);
        
        origLoc = "{invalid:name}";
        loc = new HTTPLocation(origLoc);
        returnedLoc = loc.getFormattedLocation();
        assertNull("Location is invalid, so null was expected", returnedLoc);
    }
    
    public void testToString() {
        //behaviour same as getFormattedLocation();
        testGetFormattedLocation();
    }
    
}
