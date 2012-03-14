package net.thornydev.staxxas;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.sun.xml.internal.ws.streaming.XMLStreamWriterException;

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
     * 
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
    * <p>When set as the "current", the namespace will be used as a prefix for element 
    * tags unless it has been set as the "default " namespace via {@link setDefaultNamespace}.
    * </p> 
    * 
    * <p>This method needs to be called once for each namespace mapping before calling
    * any of the "write" or "start" methods.  Or you can pass in all namespace 
    * mappings to the constructor that accepts a Map argument.</p>
    * 
    * @param ns String namespace
    * @param uri String uri for that namespace
    */
   public void mapNamespaceToUri(String ns, String uri) {
   	m.put(ns, uri);
   }

   public void setDefaultNamespace(String nsUri) {
         defaultNamespace = nsUri;
   }
   
   public void setCurrentNamespace(String ns) {
   	if (!m.containsKey(ns)) {
   		throw new IllegalArgumentException("Namespace " + ns + " has not been mapped to a uri");
   	}
   	currNamespace = ns;
   }

    
    
   /* ---[ "Writer" delegating functions ]--- */

   /**
    * Start an XML document. This should only be called once before
    * calling any other "write" methods. It delegates to 
    * {@link XMLStreamWriter#writeStartDocument()}
    * 
    * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
    * throws an XMLStreamException 
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
     * End an XML document. This should only be called once at the end of
     * calling all other "write" methods. Unlike, the 
     * {@link XMLStreamWriter#writeEndDocument()} method, this method also flushes 
     * and closes the XMLStreamWriter by delegating to {@link XMLStreamWriter#close()}
     * and {@link XMLStreamWriter#flush()}.
     * 
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
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
     * 
     * @param localName
     * @param nt
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     */
    private void setPrefix(String localName) {
    	String uri = m.get(currNamespace);
    	if (currNamespace != null) {
    		try {
    			w.setPrefix(currNamespace, uri);
    		} catch (XMLStreamException e) {
    			throw new StaxxasStreamWriterException("startElement", "setPrefix", e);    					
    		}	
    	}
    }

    public StaxxasStreamWriter startRootElement(String localName) {
		setPrefix(localName);
		writeElement(localName);
		writeRootNamespaces(localName);
		return this;    	
    }
    
    /**
     * Starts a new XML element of localName. If you specify that the
     * ElementType is ElementType.ROOT, then it will also write the namespace
     * to uri mappings in this node.  Thus if you are using namespaces and 
     * you are starting the ROOT node, you should call this method as:
     * {@code startElement("eltname", StaxxasStreamWriter.ElementType.ROOT)}.
     * Otherwise, it is easier just to call {@link startElement(String localName)}
     * 
     * @param localName name of XML element to start
     * @param nt ElementType node type
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     */
    public StaxxasStreamWriter startElement(String localName) {
		setPrefix(localName);
		writeElement(localName);
		return this;
    }

    /**
     * Starts a new XML element of localName.
     * <p>
     * This element will be prefixed with a namespace if:
     * <ol>
     *   <li>You have specified a current namespace</li>
     *   <li>The current namespace is not the "default " namespace</li>
     * </ol>
     * </p>
     * @param localName
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     */
//    public StaxxasStreamWriter startElement(String localName) { 
//    	return startElement(localName, ElementType.CHILD);
//    }
    
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
     * {@link XMLStreamWriter#writeAttribute(String, String)a}
     * @param localName
     * @param value
     * @return
     * @throws StaxxasStreamWriterException
     */
    public StaxxasStreamWriter writeAttribute(String localName, String value) {
    	try {
			w.writeAttribute(localName, value);
			return this;
		} catch (XMLStreamException e) {
    		throw new StaxxasStreamWriterException("startElement", "writeNamespace", e);
		}
    }
}
