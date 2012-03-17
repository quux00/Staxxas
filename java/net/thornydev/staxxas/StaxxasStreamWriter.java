package net.thornydev.staxxas;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * TODO: fill in more
 * 
 * <strong>This class is not thread-safe.</strong> I could have designed the 
 * Staxxas class to be immutable by proper use of constructor params, but like 
 * a {@link java.util.StringBuilder} the underlying XMLStreamWriter is a state machine 
 * that cannot be used successfully by more than one thread at a time.
 * @author midpeter444
 *
 */
public class StaxxasStreamWriter {
    private final Map<String,String> m;
    private final XMLStreamWriter w;
    private String currNamespace;
    private String defaultNamespace;

    /**
     * Enum to specify whether the type of XML element being written
     * with respect to whether it is a root node or not (child)
     */
    public enum ElementType {
      ROOT, CHILD
    }

    /* ---[ Constructors ]--- */
    
    /**
     * Creates a StaxxasStreamWriter wrapping a StAX XMLStreamWriter and
     * also with a pre-filled in set of mappings of namespaces to URIs.
     * Alternatively, you can use the constructor that takes just an 
     * XMLStreamWriter and call {@link mapNamespaceToUri} once for each
     * mapping.
     *      
     * @param sw JAXP StAX XMLStreamWriter that is used to do the actual
     * writing of the XML elements and content
     * @param nsToUri Map of each namespace for the document to its corresponding
     * URI
     */
    public StaxxasStreamWriter(XMLStreamWriter sw, Map<String,String> nsToUri) {
    	w = sw;
    	if (nsToUri == null) m = new HashMap<String,String>();
    	else 	             m = nsToUri;
    }

    /**
     * TODO: FILL OUT!
     * 
     * @param sw JAXP StAX XMLStreamWriter that is used to do the actual
     * writing of the XML elements and content
     * @param nsToUri Map of each namespace for the document to its corresponding URI
     * @param default Ns The default  namespace of the document - when set this
     * namespace will <strong>not</strong> be used as a prefix for XML docs.
     * Can be null. Or you can call the other constructor that leaves this argument off.
     */
    public StaxxasStreamWriter(XMLStreamWriter sw) {
    	this(sw, null);
    }

    /* ---[ Helper setter/mapper functions ]--- */
    
    /**
     * Maps a namespace (e.g., "foo") to a URI (e.g, "http://example.com/foo")
     * 
     * <p>This method needs to be called once for each namespace mapping before calling
     * {@link startDoc}.  Or you can pass in all namespace mappings to the constructor  
     * that accepts a Map argument or the other {@code mapNamespaceToUri} method</p>
     * 
     * <p>When set as the "current", the namespace will be used as a prefix for element 
     * tags unless it has been set as the "default " namespace via {@link setDefaultNamespace}.
     * </p> 
     * 
     * @param ns String namespace
     * @param uri String uri for that namespace
     */
    public void mapNamespaceToUri(String ns, String uri) {
    	m.put(ns, uri);
    }

    /**
     * Maps a namespace (e.g., "foo") to a URI (e.g, "http://example.com/foo") using all the 
     * entries in the provided map.
     * 
     * See the other {@link mapNamespaceToUri} method for more documentation.
     * 
     * @param nsToUri Map<String,String> containing one or more entries mapping an XML
     * namespace to a URI
     */
    public void mapNamespaceToUri(Map<String,String> nsToUri) {
    	m.putAll(nsToUri);
    }

    /**
     * A default namespace will be written as a non-prefixed attributed in the root node
     * element, like so: {@literal <foo xmlns="http://www.example.com/myURI">}
     * 
     * It is thus the default namespace for the XML document and the non-prefixed elements
     * will be assigned to that namespace.
     * 
     * Call this method before calling {@link startDoc}.
     * 
     * @param nsUri
     */
    public void setDefaultNamespace(String nsUri) {
    	defaultNamespace = nsUri;
    }
    
    /**
     * As you are writing the XML document, you may need to change the current namespace for the next
     * node or set of nodes.  Call this method to adjust what the current namespace is.  This method
     * can be called any time (except after {@link closeDoc()} of course) after you have specified
     * a namespace prefix to URI mapping via mapNamespaceToUri (or in the constructor that accepts a map).
     * 
     * <p>If you pass in a namespace prefix that has not been 'registered' via mapNamespaceToUri
     * an IllegalArgumentException will be thrown.</p>
     * 
     * <p>Example: 
     * {@literal
     * sx.mapNamespaceToUri(mymap);   // mymap assumed to have been populated already
     * sx.setCurrentNamespace("foo"); // prefix "foo" 
     * sx.startDoc();
     * sx.startElement("root");       // will be written with "foo" namespace
     * sx.setCurrentNamespace("bar"); 
     * sx.startElement("child");      // will be written with "bar" namespace
     * ...
     * }
     * </p>
     * 
     * @param ns String namespace prefix that has already been registered to a URI
     * @throws IllegalArgumentException if namespace passed in has not already been registered to a URI
     */
    public void setCurrentNamespace(String ns) {
    	if (!m.containsKey(ns)) {
    		throw new IllegalArgumentException("Namespace " + ns + " has not been mapped to a uri");
    	}
    	currNamespace = ns;
    }

    
    
   /* ---[ "Writer" delegating functions ]--- */

    /**
     * Start an XML document. This method should only be called once before
     * calling any other "write" methods. It delegates to 
     * {@link XMLStreamWriter#writeStartDocument()}
     * 
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter startDoc() {
        try {
          w.writeStartDocument();
          return this;
          
        } catch (XMLStreamException e) {
            throw new StaxxasStreamWriterException("startDoc", "writeStartDocument", e);
        }
      }

    /**
     * Start an XML document. This method should only be called once before
     * calling any other "write" methods. It delegates to 
     * {@link XMLStreamWriter#writeStartDocument(version)}
     * 
     * <p>This constructor allows you to specify an XML version number.  The {@code startDoc()}
     * that take no args defaults to version 1.0.</p>
     * 
     * @param version String referring to a version number to write to the XML processing
     * instruction
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     */
    public StaxxasStreamWriter startDoc(String version) {
        try {
          w.writeStartDocument(version);
          return this;
          
        } catch (XMLStreamException e) {
            throw new StaxxasStreamWriterException("startDoc", "writeStartDocument", e);
        }
      }

    /**
     * Start an XML document. This method should only be called once before
     * calling any other "write" methods. It delegates to 
     * {@link XMLStreamWriter#writeStartDocument(version)}
     * 
     * <p>This constructor allows you to specify an XML version number.  The {@code startDoc()}
     * that take no args defaults to version 1.0.</p>
     * 
     * @param version String referring to a version number to write to the XML processing
     * instruction
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter startDoc(String encoding, String version) {
        try {
          w.writeStartDocument(encoding, version);
          return this;
          
        } catch (XMLStreamException e) {
            throw new StaxxasStreamWriterException("startDoc", "writeStartDocument", e);
        }
      }

    /**
     * End an XML document. This should only be called once at the end of
     * calling all other "write" methods. Unlike, the 
     * {@link XMLStreamWriter#writeEndDocument()} method, this method also flushes 
     * and closes the XMLStreamWriter by delegating to {@link XMLStreamWriter#close()}
     * and {@link XMLStreamWriter#flush()}.
     * 
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter endDoc() {
    	try {
    		w.writeEndDocument();
    		w.flush();
    		w.close();
    		return this;
    	} catch (XMLStreamException e) {
            throw new StaxxasStreamWriterException("endDoc", 
            		"writeEndDocument/flush/close", e);    		
    	}
    }


    /**
     * Starts a new XML root element of localName. If you are creating a root element
     * and you are using namespaces, you should call this method not {@code startElement}.
     * This methods will write the namespace to URI mappings into the node's attributes.
     * 
     * <p>If you are not using namespaces, then using this method for the root element
     * is also fine.</p> 
     * 
     * @param localName name of XML element to start
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter startRootElement(String localName) {
		setPrefix();
		writeElement(localName);
		writeRootNamespaces(localName);
		return this;    	
    }
    
    /**
     * Starts a new XML element of localName. If you are creating a root element
     * and you are using namespaces, you should call {@code startRootElement}.
     * This methods will <strong>not</strong> write the namespace to URI mappings.
     * 
     * <p>If you are not using namespaces, then using this method for the root element
     * is fine.</p> 
     * 
     * @param localName name of XML element to start
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter startElement(String localName) {
		setPrefix();
		writeElement(localName);
		return this;
    }

    /**
     * Writes an empty XML element as a single tag: {@literal <foo/>}.
     * 
     * @param localName String name of element
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter emptyElement(String localName) {
    	setPrefix();
    	writeEmptyElement(localName);
    	return this;
    }

    /**
     * Convenience method to write an end tag. It can be self-documenting to say which tag
     * you are closing if the {@codestartElement()} call is far away.  The string passed in is 
     * ignored and not validated in any way.  It is only there to have self-documenting code.
     * 
     * @param localName ignored - for documentation purposes only
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter endElement(String localName) {
    	return endElement();
    }
    
    /**
     * Write the end tag (element) of the last tag that was started.
     * 
     * @param localName String name of element
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter endElement() {
    	try {
    		w.writeEndElement();
    		return this;
    	} catch (XMLStreamException e) {
            throw new StaxxasStreamWriterException("endDoc", "writeEndElement", e);
    	}    	
    }

    /**
     * Writes an attribute to the last element started. Functionality is the same as
     * the XMLStreamWriter function by the same name: 
     * {@link XMLStreamWriter#writeAttribute(String, String)}.
     * 
     * <p>Attributes written with this method will <strong>not</strong> be prefixed with
     * a namespace prefix even if you have set a current namespace.  In order prefix an
     * attribute you must pass in the prefix to the other {@attribute()} method.</p>
     * 
     * @param localName name of attribute
     * @param value of attribute
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter attribute(String localName, String value) {
    	try {
			w.writeAttribute(localName, value);
			return this;
		} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("writeAttribute", "writeAttribute", e);
		}
    }

    public StaxxasStreamWriter prefixedAttribute(String prefix, 
    		String localName, String value) {
    	try {
			w.writeAttribute(m.get(prefix), localName, value);
    		return this;
		} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("writePrefixedAttribute", "writeAttribute", e);
		}
    }

    public StaxxasStreamWriter characters(String text) {
    	try {
    		w.writeCharacters(text);
			return this;
		} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("characters", "writeCharacters", e);
		}
    }
    
    public StaxxasStreamWriter characters(char[] text, int start, int len) {
    	try {
    		w.writeCharacters(text, start, len);
			return this;
		} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("characters", "writeCharacters", e);
		}
    }

    public StaxxasStreamWriter comment(String text) {
    	try {
    		w.writeComment(text);
			return this;
		} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("comment", "writeComment", e);
		}
    }
    
    
    public StaxxasStreamWriter processingInstruction(String target) {
    	try {
    		w.writeProcessingInstruction(target);
			return this;
		} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("processingInstruction", 
					"writeProcessingInstruction", e);
		}    	
    }
    
    public StaxxasStreamWriter processingInstruction(String target, String data) {
    	try {
    		w.writeProcessingInstruction(target, data);
			return this;
		} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("processingInstruction", 
					"writeProcessingInstruction", e);
		}    	
    }
    
    public StaxxasStreamWriter entityRef(String name) {
    	try {
    		w.writeEntityRef(name);
    		return this;
    	} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("entityRef", "writeEntityRef", e);
		}
    }
    
    public StaxxasStreamWriter dtd(String dtd) {
    	try {
    		w.writeDTD(dtd);
    		return this;
    	} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("dtd", "writeDTD", e);
		}
    }
    
    public StaxxasStreamWriter cdata(String cdata) {
    	try {
    		w.writeCData(cdata);
    		return this;
    	} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("cdata", "writeCData", e);
		}
    }
    
    /* ---[ private methods ]--- */
    
    private void writeRootNamespaces(String name) {
		try {
			if (defaultNamespace != null) {
				w.writeDefaultNamespace(defaultNamespace);
			}
			for (String ns: m.keySet()) {
				w.writeNamespace(ns, m.get(ns));
			}
		} catch (XMLStreamException e) {
    		throw new StaxxasStreamWriterException("writeRootNamespaces", "writeNamespace", e);
		}
    }    

    private void writeEmptyElement(String name) {
    	try {
    		if (currNamespace != null) {
    			w.writeEmptyElement(m.get(currNamespace), name);
    		} else {
    			w.writeEmptyElement(name);
    		}    		
		} catch (XMLStreamException e) {
    		throw new StaxxasStreamWriterException("emptyElement", "writeEmptyElement", e);    		
		}
    }
    
    private void writeElement(String name) {
    	try {
    		if (currNamespace != null) {
    			w.writeStartElement(m.get(currNamespace), name);
    		} else {
    			w.writeStartElement(name);
    		}
    	} catch (XMLStreamException e) {
    		throw new StaxxasStreamWriterException("startElement", "writeStartElement", e);    		
    	}
    }

    /**
     * Sets the prefix on the XMLStreamWriter state machine so it has a
     * mapping of the namespace ("prefix") to the appropriate URI
     * 
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     */
    private void setPrefix() {
    	String uri = m.get(currNamespace);
    	if (currNamespace != null) {
    		try {
    			w.setPrefix(currNamespace, uri);
    		} catch (XMLStreamException e) {
    			throw new StaxxasStreamWriterException("startElement", "setPrefix", e);    					
    		}	
    	}
    }
}
