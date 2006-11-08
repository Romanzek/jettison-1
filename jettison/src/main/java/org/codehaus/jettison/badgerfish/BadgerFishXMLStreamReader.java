/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.codehaus.jettison.badgerfish;


import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;

import org.codehaus.jettison.AbstractXMLStreamReader;
import org.codehaus.jettison.Node;
import org.codehaus.jettison.util.FastStack;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BadgerFishXMLStreamReader extends AbstractXMLStreamReader {
    private static final BadgerFishConvention CONVENTION = new BadgerFishConvention();
    private FastStack nodes;
    private String currentText;
    
    public BadgerFishXMLStreamReader(JSONObject obj) 
        throws JSONException, XMLStreamException {
        String rootName = (String) obj.keys().next();
        
        this.node = new Node(rootName, obj.getJSONObject(rootName), CONVENTION);
        this.nodes = new FastStack();
        nodes.push(node);
        event = START_DOCUMENT;
    }

    public int next() throws XMLStreamException {
        if (event == START_DOCUMENT) {
            event = START_ELEMENT;
        } else {
            if (event == END_ELEMENT && nodes.size() != 0) {
                node = (Node) nodes.peek();
            }
            
            if (node.getArray() != null 
                && node.getArray().length() > node.getArrayIndex()) {
                Node arrayNode = node;
                int idx = arrayNode.getArrayIndex();
                
                try {
                    Object o = arrayNode.getArray().get(idx);
                    processKey(node.getCurrentKey(), o);
                } catch (JSONException e) {
                    throw new XMLStreamException(e);
                }
                
                idx++;
                arrayNode.setArrayIndex(idx);
            } else if (node.getKeys() != null && node.getKeys().hasNext()) {
                processElement();
            } else {
                event = END_ELEMENT;
                if (nodes.size() != 0) {
                    node = (Node) nodes.pop();
                }
            }
        }
         
        return event;
    }
    
    private void processElement() throws XMLStreamException {
        try {
            String nextKey = (String) node.getKeys().next();
            
            Object newObj = node.getObject().get(nextKey);
            
            processKey(nextKey, newObj);
        } catch (JSONException e) {
            throw new XMLStreamException(e);
        }
    }

    private void processKey(String nextKey, Object newObj) throws JSONException, XMLStreamException {
        if (nextKey.equals("$")) {
            event = CHARACTERS;
            // TODO I think there is a possibility this could be array
            currentText = (String) newObj;
            return;
        } else if (newObj instanceof JSONObject) {
            node = new Node(nextKey, (JSONObject) newObj, CONVENTION);
            nodes.push(node);
            event = START_ELEMENT;
            return;
        } else if (newObj instanceof JSONArray) {
            JSONArray arr = (JSONArray) newObj;

            if (arr.length() == 0) {
                next();
                return;
            }
            
            // save some state information...
            node.setArray(arr);
            node.setArrayIndex(1);
            node.setCurrentKey(nextKey);
            
            processKey(nextKey, arr.get(0));
        }
    }

    public void close() throws XMLStreamException {
    }

    public String getAttributeType(int arg0) {
        return null;
    }

    public String getCharacterEncodingScheme() {
        return null;
    }

    public String getElementText() throws XMLStreamException {
        return currentText;
    }

    public NamespaceContext getNamespaceContext() {
        return null;
    }

    public String getText() {
        return currentText;
    }  
}
