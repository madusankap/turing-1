/*
 *  Copyright WSO2 Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.apimgt.impl;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.APIStore;
import org.wso2.carbon.apimgt.impl.dto.Environment;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global API Manager configuration. This is generally populated from a special XML descriptor
 * file at system startup. Once successfully populated, this class does not allow more parameters
 * to be added to the configuration. The design of this class has been greatly inspired by
 * the ServerConfiguration class in Carbon core. This class uses a similar '.' separated
 * approach to keep track of XML parameters.
 */
public class APIManagerConfiguration {
    
    private Map<String,List<String>> configuration = new ConcurrentHashMap<String, List<String>>();

    private SecretResolver secretResolver;

    private boolean initialized;

    private List<Environment> apiGatewayEnvironments = new ArrayList<Environment>();
    private Set<APIStore> externalAPIStores = new HashSet<APIStore>();

    /**
     * Populate this configuration by reading an XML file at the given location. This method
     * can be executed only once on a given APIManagerConfiguration instance. Once invoked and
     * successfully populated, it will ignore all subsequent invocations.
     *
     * @param filePath Path of the XML descriptor file
     * @throws APIManagementException If an error occurs while reading the XML descriptor
     */
    public void load(String filePath) throws APIManagementException {
        if (initialized) {
            return;
        }
        InputStream in = null;
        try {
            in = FileUtils.openInputStream(new File(filePath));
            StAXOMBuilder builder = new StAXOMBuilder(in);
            secretResolver = SecretResolverFactory.create(builder.getDocumentElement(), true);
            readChildElements(builder.getDocumentElement(), new Stack<String>());
            initialized = true;
        } catch (IOException e) {
            throw new APIManagementException("I/O error while reading the API manager " +
                    "configuration: " + filePath, e);
        } catch (XMLStreamException e) {
            throw new APIManagementException("Error while parsing the API manager " +
                    "configuration: " + filePath, e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public String getFirstProperty(String key) {
        List<String> value = configuration.get(key);
        if (value == null) {
            return null;
        }
        return value.get(0);
    }
    
    public List<String> getProperty(String key) {
        return configuration.get(key);
    }
    
    public void reloadSystemProperties() {
        for (Map.Entry<String,List<String>> entry : configuration.entrySet()) {
            List<String> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                String text = list.remove(i);
                list.add(i, replaceSystemProperty(text));
            }
        }
    }

    private void readChildElements(OMElement serverConfig,
                                   Stack<String> nameStack) {
        for (Iterator childElements = serverConfig.getChildElements(); childElements
                .hasNext();) {
            OMElement element = (OMElement) childElements.next();
            String localName = element.getLocalName();
            nameStack.push(localName);
            if (elementHasText(element)) {
                String key = getKey(nameStack);
                String value = element.getText();
                if (secretResolver.isInitialized() && secretResolver.isTokenProtected(key)) {
                    value = secretResolver.resolve(key);
                }
                addToConfiguration(key, replaceSystemProperty(value));
            }
            else if("Environments".equals(localName)){
                Iterator environmentIterator = element.getChildrenWithLocalName("Environment");
                apiGatewayEnvironments = new ArrayList<Environment>();

                while(environmentIterator.hasNext()){
                    Environment environment = new Environment();
                    OMElement environmentElem = (OMElement)environmentIterator.next();
                    environment.setType(environmentElem.getAttributeValue(new QName("type")));
                    environment.setName(replaceSystemProperty(
                                        environmentElem.getFirstChildWithName(new QName("Name")).getText()));
                    environment.setServerURL(replaceSystemProperty(
                                        environmentElem.getFirstChildWithName(new QName(
                                                APIConstants.API_GATEWAY_SERVER_URL)).getText()));
                    environment.setUserName(replaceSystemProperty(

                                        environmentElem.getFirstChildWithName(new QName(
                                                APIConstants.API_GATEWAY_USERNAME)).getText()));

                    String key = APIConstants.API_GATEWAY + APIConstants.API_GATEWAY_PASSWORD;
                    String value;
                    if (secretResolver.isInitialized() && secretResolver.isTokenProtected(key)) {
                        value = secretResolver.resolve(key);
                    }
                    else{
                        value = environmentElem.getFirstChildWithName(new QName(
                                                            APIConstants.API_GATEWAY_PASSWORD)).getText();
                    }
                    environment.setPassword(replaceSystemProperty(value));
                    environment.setApiGatewayEndpoint(replaceSystemProperty(
                            environmentElem.getFirstChildWithName(new QName(
                                                            APIConstants.API_GATEWAY_ENDPOINT)).getText()));
                    apiGatewayEnvironments.add(environment);
                }
            }else if(APIConstants.EXTERNAL_API_STORES.equals(localName)){  //Initialize 'externalAPIStores' config elements
                Iterator apistoreIterator = element.getChildrenWithLocalName("ExternalAPIStore");
                externalAPIStores = new HashSet<APIStore>();
                while(apistoreIterator.hasNext()){
                    APIStore store=new APIStore();
                    OMElement storeElem = (OMElement)apistoreIterator.next();
                    String type=storeElem.getAttributeValue(new QName(APIConstants.EXTERNAL_API_STORE_TYPE));
                    store.setType(type); //Set Store type [eg:wso2]
                    String name=storeElem.getAttributeValue(new QName(APIConstants.EXTERNAL_API_STORE_ID));
                    if(name==null){
                        try {
                            throw new APIManagementException("The ExternalAPIStore name attribute is not defined in api-manager.xml.");
                        } catch (APIManagementException e) {
                            //ignore
                        }
                    }
                    store.setName(name); //Set store name
                    OMElement configDisplayName=storeElem.getFirstChildWithName(new QName(APIConstants.EXTERNAL_API_STORE_DISPLAY_NAME));
                    String displayName=(configDisplayName!=null)?replaceSystemProperty(
                            configDisplayName.getText()):name;
                    store.setDisplayName(displayName);//Set store display name
                    store.setEndpoint(replaceSystemProperty(
                            storeElem.getFirstChildWithName(new QName(
                                    APIConstants.EXTERNAL_API_STORE_ENDPOINT)).getText())); //Set store endpoint,which is used to publish APIs
                    store.setPublished(false);
                    if(APIConstants.WSO2_API_STORE_TYPE.equals(type)){
                    OMElement password=storeElem.getFirstChildWithName(new QName(
                                APIConstants.EXTERNAL_API_STORE_PASSWORD));
                    if(password!=null){
                    String key = APIConstants.EXTERNAL_API_STORES+"."+APIConstants.EXTERNAL_API_STORE+"."+APIConstants.EXTERNAL_API_STORE_PASSWORD+'_'+name;//Set store login password [optional]
                    String value;
                    if (secretResolver.isInitialized() && secretResolver.isTokenProtected(key)) {
                        value = secretResolver.resolve(key);
                    }
                    else{

                        value = password.getText();
                    }
                    store.setPassword(replaceSystemProperty(value));
                    store.setUsername(replaceSystemProperty(
                            storeElem.getFirstChildWithName(new QName(
                                    APIConstants.EXTERNAL_API_STORE_USERNAME)).getText())); //Set store login username [optional]
                    }else{
                        try {
                            throw new APIManagementException("The user-credentials of API Publisher is not defined in the <ExternalAPIStore> config of api-manager.xml.");
                        } catch (APIManagementException e) {
                            //ignore
                        }
                    }
                    }
                    externalAPIStores.add(store);
                }
            }
            readChildElements(element, nameStack);
            nameStack.pop();
        }
    }

    private String getKey(Stack<String> nameStack) {
        StringBuffer key = new StringBuffer();
        for (int i = 0; i < nameStack.size(); i++) {
            String name = nameStack.elementAt(i);
            key.append(name).append(".");
        }
        key.deleteCharAt(key.lastIndexOf("."));

        return key.toString();
    }

    private boolean elementHasText(OMElement element) {
        String text = element.getText();
        return text != null && text.trim().length() != 0;
    }

    private void addToConfiguration(String key, String value) {
        List<String> list = configuration.get(key);
        if (list == null) {
            list = new ArrayList<String>();
            list.add(value);
            configuration.put(key, list);
        } else {
            list.add(value);
        }
    }

    private String replaceSystemProperty(String text) {
        int indexOfStartingChars = -1;
        int indexOfClosingBrace;

        // The following condition deals with properties.
        // Properties are specified as ${system.property},
        // and are assumed to be System properties
        while (indexOfStartingChars < text.indexOf("${")
                && (indexOfStartingChars = text.indexOf("${")) != -1
                && (indexOfClosingBrace = text.indexOf('}')) != -1) { // Is a
            // property
            // used?
            String sysProp = text.substring(indexOfStartingChars + 2,
                    indexOfClosingBrace);
            String propValue = System.getProperty(sysProp);
            if (propValue != null) {
                text = text.substring(0, indexOfStartingChars) + propValue
                        + text.substring(indexOfClosingBrace + 1);
            }
            if (sysProp.equals("carbon.home") && propValue != null
                    && propValue.equals(".")) {

                text = new File(".").getAbsolutePath() + File.separator + text;

            }
        }
        return text;
    }

    public List<Environment> getApiGatewayEnvironments() {
        return apiGatewayEnvironments;
    }

    public Set<APIStore> getExternalAPIStores() {  //Return set of APIStores
        return externalAPIStores;
    }

    public APIStore getExternalAPIStore(String storeName) { //Return APIStore object,based on store name/Here we assume store name is unique.
        for (APIStore apiStore : externalAPIStores) {
            if (apiStore.getName().equals(storeName)) {
                return apiStore;
            }
        }
        return null;
    }

}
