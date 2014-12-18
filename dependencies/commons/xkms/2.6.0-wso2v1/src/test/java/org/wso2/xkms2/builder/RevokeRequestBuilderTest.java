/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.xkms2.builder;

import org.apache.axiom.om.OMElement;
import org.wso2.xkms2.RevokeRequest;
import org.wso2.xkms2.XKMSException;
import org.wso2.xkms2.XKRRSTestCase;

public class RevokeRequestBuilderTest extends XKRRSTestCase {
    
    public RevokeRequestBuilderTest() {
        super("RecoverRequestBuilderTest");
    }

    public void testBuild() throws XKMSException {
        OMElement revokeRequestElem = getResourceAsElement("T5_RevokeRequest-http.xml");
        RevokeRequest revokeRequest = (RevokeRequest) RevokeRequestBuilder.INSTANCE.buildElement(revokeRequestElem);
        assertNotNull(revokeRequest);
    }
}
