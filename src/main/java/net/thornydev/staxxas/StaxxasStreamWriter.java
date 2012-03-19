package net.thornydev.staxxas;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * The Staxxas library provides a facade that wraps the JAXP 
 * {@link javax.xml.stream.XMLStreamWriter}. All the writing to the underlying output 
 * source or stream is still done by the {@link javax.xml.stream.XMLStreamWriter} 
 * object that is passed into the constructor of this class. 
 * 
 * <p>The Staxxas API simplifies the {@link javax.xml.stream.XMLStreamWriter} API and 
 * wraps methods so that the API user does not have to handle the checked 
 * {@link javax.xml.stream.XMLStreamException} that is thrown by the XMLStreamWriter
 * "write" methods.
 *   
 * <p>In particular, this facade provides three primary 
 * improvements over the native {@link javax.xml.stream.XMLStreamWriter} API:
 * <ol>
 *   <li>The "write" method names are less verbose, you can do method call
 *   chaining and in some cases call fewer methods to get the document written</li>
 *   <li>Only unchecked exceptions are thrown, which wrap any checked exceptions thrown
 *   by the underlying library</li>
 *   <li>Namespace handling is simplified</li>
 * </ol>
 * </p>	
 * 
 * <h6>Less verbosity, method call chaining, fewer methods calls.</h6>
 * <p>To help decrease the verbosity of the XMLStreamWriter API, any method that 
 * starts with "write", such as <code>writeEmptyElement</code>, has been
 * shortened by removing the "write" prefix, such as <code>emptyElement</code>.
 * Those name-shortened methods are just wrappers around the longer methods
 * of the XMLStreamWriter object. </p>
 * 
 * <p>Another aspect of the verbosity of the StAX API is having to pass in the 
 * full URI of the namespace whenever you need to associate a namespace with
 * an XML element.  This requirement has been removed - see the <strong>
 * Simplified namespace handling</strong> section below.  
 * </p>
 * 
 * <p>In the case of finishing off the XML doc writing, the XMLStreamWriter
 * API writer requires you to call <em>three</em> methods: 
 * {@link XMLStreamWriter#writeEndDocument()}, followed by {@link XMLStreamWriter#flush()},
 * followed by {@link XMLStreamWriter#close()}.  The Staxxas API only requires that
 * {@link StaxxasStreamWriter#endDoc()} be called and it calls <code>flush()</code>
 * and <code>close()</code> on the underlying writer.
 * </p>
 * 
 * <h6>Only unchecked exceptions</h6>
 * <p>In <em>Effective Java, 2nd edition</em>, Josh Bloch argues that checked 
 * exceptions are only appropriate when two conditions hold:
 * <ol>
 *   <li>The exception cannot be prevented by proper use of the API</li>
 *   <li>The user of the API can take some useful action to recover
 *   when the exception occurs.</li>
 * </ol>
 * </p>
 * <p>All of the "write" methods of XMLStreamWriter throw a checked exception. Those
 * exceptions can arise even if the API is used properly, so Bloch's first condition
 * applies, but in that case it likely to be a problem with the file system or
 * stream to which the XML is being written, which a Java programmer has little
 * control over. Thus, the second condition does not apply and the exceptions
 * thrown here should be unchecked.
 * </p>
 * 
 * <p>In the tradition of the Spring framework, all these checked exceptions
 * have been wrapped and are rethrown as an unchecked exception of type
 * {@link StaxxasStreamWriterException}.</p>
 * 
 * <h6>Simplified namespace handling</h6>
 * <p>Lastly, the namepace handling in the XMLStreamWriter is rather ghastly
 * and unnecessarily verbose.  In the Staxxas API, you basically only have
 * to map all your namespace prefixes to their corresponding namespace APIs
 * at the beginning and then call {@code setCurrentNamespace(prefix)} 
 * passing in the prefix that is current.  All subsequent {@code startElement()}
 * calls will use that namespace until {@code setCurrentNamespace(prefix)} 
 * is called with a different current namespace.
 * </p>
 * 
 * <p>A default namespace can also be specified and any unprefixed
 * XML elements will be associated with that default namespace.  A unprefixed
 * XML element can be set by calling {@code setCurrentNamespace(null)} 
 * or {@code startElement("myEltName", null)}.</p>
 * 
 * <h6>Thread Safety</h6>
 * <p><strong>This class is not thread-safe.</strong> I could have designed the 
 * Staxxas class to be immutable by proper use of constructor params, but like 
 * a {@link java.util.StringBuilder}, the underlying 
 * {@link javax.xml.stream.XMLStreamWriter} is a state machine that cannot be 
 * used successfully by more than one thread at a time.</p>
 * 
 * @author midpeter444
 *
 */
public class StaxxasStreamWriter {
	/**
	 * The JAXP XMLStreamWriter - it does all the actual writing of the XML doc.
	 */
	private final XMLStreamWriter w;

	/**
	 * The java.io.Writer underlying the XMLStreamWriter.
	 * Keep a reference to this if it is passed to the constructor, so we
	 * can close it when endDoc() is called.  Optional.
	 */
	private final Writer writer;

	/**
	 * Map of namespace prefixes to URIs that should be written to the document.
	 * This map will <strong>not</strong> include the defaultNamespace, since
	 * that namespace has no prefix.
	 */
    private final Map<String,String> m;

    /** 
     * If the user is using namespaces, this field holds the current namespace
     * that is in play for writing XML nodes. When set, any elements that are
     * started will be prefixed with this namespace prefix.
     */
    private String currNamespace;
    
    /**
     * If the user is using a default namespace (which has no prefix), then this
     * field is set to the URI of that prefix-less "namespace".  This namespace 
     * will not appear in the Map instance variable that holds mappings of namespace 
     * prefixes to URIs. 
     */
    private String defaultNamespace;

    /* ---[ Constructors ]--- */
    
    /**
     * Creates a StaxxasStreamWriter wrapping a StAX {@link javax.xml.stream.XMLStreamWriter}
     * and accepting a filled out set of mappings of namespace prefixes to namespace URIs.
     * Alternatively, you can use the constructor that takes just an 
     * XMLStreamWriter and call {@link StaxxasStreamWriter#mapNamespaceToUri(String, String)} 
     * once for each mapping or {@link StaxxasStreamWriter#mapNamespaceToUri(Map)}
     * with a filled out Map object.
     * 
     * @param sw JAXP StAX {@link XMLStreamWriter} that is used to do the actual
     * writing of the XML elements and content
     * @param nsToUri Map of each namespace (prefix) to its corresponding URI
     */
    public StaxxasStreamWriter(XMLStreamWriter sw, Map<String,String> nsToUri) {
    	w = sw;
    	writer = null;
    	if (nsToUri == null) m = new HashMap<String,String>();
    	else 	             m = nsToUri;
    }

    /**
     * Creates a StaxxasStreamWriter with a Writer that gets passed to a StAX 
     * {@link javax.xml.stream.XMLStreamWriter} and accepting a filled out set 
     * of mappings of namespace prefixes to namespace URIs.  By passing in a 
     * {@link java.io.Writer}, this method will create the {@code XMLStreamWriter}
     * with it.
     * 
     * <p>The advantages of using this constructor are that it wraps all 
     * the constructor of the {@code XMLStreamWriter} and when {@code endDoc()}
     * is called it will also flush and close the {@code Writer}, not just the 
     * {@code XMLStreamWriter}.</p>
     * 
     * <p>The disadvantage of using this constructor is that the caller will 
     * not have access to the {@link XMLOutputFactory} that creates the 
     * {@code XMLStreamWriter}, and so cannot modify any settings on those objects.
     * </p>
     * 
     * @param writer the Writer object to write the XML document to
     * writing of the XML elements and content
     * @param nsToUri Map of each namespace (prefix) to its corresponding URI
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying 
     * StAX library throws an {@code XMLStreamException} or the {@link XMLOutputFactory}
     * throws a {@link FactoryConfigurationError} when creating the 
     * {@code XMLStreamWriter}.
     */
    public StaxxasStreamWriter(Writer writer, Map<String,String> nsToUri) {
    	try {
    		this.writer = writer;
			w = XMLOutputFactory.newFactory().createXMLStreamWriter(writer);
		} catch (XMLStreamException e) {
    		throw new StaxxasStreamWriterException("StaxxasStreamWriter constructor",
    				"XMLOutputFactory.newFactory().createXMLStreamWriter(writer)", e);
		} catch (FactoryConfigurationError e) {
    		throw new StaxxasStreamWriterException("StaxxasStreamWriter constructor",
    				"XMLOutputFactory.newFactory().createXMLStreamWriter(writer)", e);
		}
    	if (nsToUri == null) m = new HashMap<String,String>();
    	else 	             m = nsToUri;
    }

    /**
     * Creates a StaxxasStreamWriter with a Writer that gets passed to a StAX 
     * {@link javax.xml.stream.XMLStreamWriter}.
     * 
     * <p>See additional documentation in the other constructor that takes
     * a Writer.</p>
     * 
     * @param writer the Writer object to write the XML document to
     * writing of the XML elements and content
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying 
     * StAX library throws an {@code XMLStreamException} or the {@link XMLOutputFactory}
     * throws a {@link FactoryConfigurationError} when creating the 
     * {@code XMLStreamWriter}.
     */    
    public StaxxasStreamWriter(Writer writer) {
    	this(writer, null);
    }
    
    /**
     * Creates a StaxxasStreamWriter wrapping a StAX {@link javax.xml.stream.XMLStreamWriter}.
     * 
     * <p> If prefixed namespaces will be used when writing the XML document, use
     * the other constructor or one of the {@code mapNamespaceToUri} methods, such as
     * {@link StaxxasStreamWriter#mapNamespaceToUri(String, String)}.</p>
     * 
     * @param sw JAXP StAX XMLStreamWriter that is used to do the actual
     * writing of the XML elements and content
     */
    public StaxxasStreamWriter(XMLStreamWriter sw) {
    	this(sw, null);
    }

    /* ---[ Helper setter/mapper functions ]--- */
    
    /**
     * Sets us a mappping betwee a namespace (e.g., "foo") to a URI 
     * (e.g, "http://example.com/foo")
     * 
     * <p>This method does not write the namespace to the XML document. It only registers 
     * the namespace-prefix to the namespace-URI. Thus, it needs to be called once for each 
     * namespace mapping before calling {@link StaxxasStreamWriter#startDoc()}.  Or you 
     * can pass in all namespace mappings to the constructor that accepts a Map argument 
     * or the other {@code mapNamespaceToUri} method.</p>
     * 
     * <p>When set as "current", the namespace will be used as a prefix for element 
     * tags unless it has been set as the "default" namespace via 
     * {@link StaxxasStreamWriter#setDefaultNamespace(String)}, in which case no prefix
     * will be used.
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
     * <p>See the other {@link StaxxasStreamWriter#mapNamespaceToUri(String, String)} method 
     * for more documentation.</p>
     * 
     * @param nsToUri Map<String,String> containing one or more entries mapping an XML
     * namespace to a URI
     */
    public void mapNamespaceToUri(Map<String,String> nsToUri) {
    	m.putAll(nsToUri);
    }

    /**
     * Sets the default (non-prefixed) namespace for the entire XML document.
     * A default namespace will be written as a non-prefixed attributed in the root node
     * element, like so: {@literal <foo xmlns="http://www.example.com/myURI">}
     * 
     * <p>As the default namespace for the XML document, all non-prefixed elements
     * will be assigned to that namespace.</p>
     * 
     * <p>Call this method only once and before calling 
     * {@link StaxxasStreamWriter#startDoc()}.</p>
     * 
     * @param nsUri a URI that is the default namespace
     */
    public void setDefaultNamespace(String nsUri) {
    	defaultNamespace = nsUri;
    }
    
    /**
     * Sets the current prefixed namespace for the XML document.
     * 
     * <p>As you are writing the XML document, you may need to change the current 
     * namespace for the next node or set of nodes.  Call this method to adjust 
     * what the current namespace is.  This method can be called any time 
     * (except after {@link StaxxasStreamWriter#endDoc()} of course) after you have 
     * specified a namespace prefix to URI mapping via mapNamespaceToUri (or in the 
     * constructor that accepts a map).
     * 
     * <p>You may pass in null in order to "unset" the current namespace in order
     * to use the default namespace or just have an unprefixed element.
     * </p>
     * 
     * <p>If you pass in an argument (other than {@code null}) that has not been 
     * 'registered' via {@code mapNamespaceToUri} an {@code IllegalArgumentException} 
     * will be thrown.
     * </p>
     * 
     * <p><strong>Example:</strong> 
     * {@literal
     *     sx.mapNamespaceToUri(mymap);   // mymap assumed to have been populated already
     *     sx.setCurrentNamespace("foo"); // prefix "foo" 
     *     sx.startDoc();
     *     sx.startElement("root");       // will be written with "foo" namespace
     *     sx.setCurrentNamespace("bar"); 
     *     sx.startElement("child");      // will be written with "bar" namespace
     *     ...
     * }
     * </p>
     * 
     * @param ns String namespace prefix that has already been registered to a URI
     * @throws IllegalArgumentException if namespace passed in has not already been 
     * registered to a URI
     */
    public void setCurrentNamespace(String ns) {
    	if (ns == null) {
    		currNamespace = null;
    	} else if (!m.containsKey(ns)) {
    		throw new IllegalArgumentException("Namespace " + ns + " has not been mapped to a uri");
    	}
    	currNamespace = ns;
    }

    
    
   /* ---[ "Writer" delegating functions ]--- */

    /**
     * Start an XML document by writing the XML declaration.
     * 
     * <p>This method should only be called once before
     * calling any other "write" methods. It delegates to 
     * {@link XMLStreamWriter#writeStartDocument()}</p>
     * 
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying 
     * StAX library throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter startDoc() {
    	try {
          w.writeStartDocument();
          setPrefixes();
          return this;
          
        } catch (XMLStreamException e) {
            throw new StaxxasStreamWriterException("startDoc", "writeStartDocument", e);
        }
      }

    /**
     * Start an XML document by writing the XML declaration.  
     * 
     * <p>This method should only be called once before
     * calling any other "write" methods. It delegates to 
     * {@link XMLStreamWriter#writeStartDocument(String)}, which takes the same
     * arguments as this method.</p>
     * 
     * <p>This constructor allows you to specify an XML version number.  The {@code startDoc()}
     * method that take no args defaults to version 1.0.</p>
     * 
     * @param version version of the xml document
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     */
    public StaxxasStreamWriter startDoc(String version) {
        try {
          w.writeStartDocument(version);
          setPrefixes();
          return this;
          
        } catch (XMLStreamException e) {
            throw new StaxxasStreamWriterException("startDoc", "writeStartDocument", e);
        }
      }

    /**
     * Start an XML document by writing the XML declaration. This method should only 
     * be called once before calling any other "write" methods. It delegates to 
     * {@link XMLStreamWriter#writeStartDocument(String, String)}, which takes the 
     * same arguments as this method.
     * 
     * <p>This constructor allows you to specify an XML version number and an
     * encoding (e.g., "UTF-8").  The {@code startDoc()} method that take no args 
     * defaults to version 1.0 and does not set an encoding.</p>
     * 
     * <p><strong>Note on encoding from the 
     * {@link XMLStreamWriter#writeStartDocument(String, String)} javadoc:</strong>
     * 
     * <blockquote>Note that the encoding parameter does not set the actual 
     * encoding of the underlying output. That must be set when the instance 
     * of the XMLStreamWriter is created using the XMLOutputFactory.</blockquote>
     * 
     * @param encoding encoding of the xml declaration
     * @param version version of the xml document
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter startDoc(String encoding, String version) {
        try {
          w.writeStartDocument(encoding, version);
          setPrefixes();
          return this;
          
        } catch (XMLStreamException e) {
            throw new StaxxasStreamWriterException("startDoc", "writeStartDocument", e);
        }
      }

    /**
     * End an XML document. This should only be called once at the end of
     * calling all other "write" methods. Unlike, the 
     * {@link XMLStreamWriter#writeEndDocument()} method, this method also flushes 
     * and closes the XMLStreamWriter by calling the {@link XMLStreamWriter#close()}
     * and {@link XMLStreamWriter#flush()} methods.
     * 
     * <p>While not best practice, this method will also close opened tags that 
     * you have not explicitly closed with {@link StaxxasStreamWriter#endElement()}.</p>
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
    		// if user gave us the java.io.Writer directly, we also 
    		// flush and close that
    		if (writer != null) {
    			writer.flush();
    			writer.close();
    		}
    		return this;
    	} catch (XMLStreamException e) {
            throw new StaxxasStreamWriterException("endDoc", 
            		"writeEndDocument/flush/close", e);    		
    	} catch (IOException e) {
            throw new StaxxasStreamWriterException("endDoc", 
            		"writeEndDocument/flush/close", e);    		
		}
    }


    /**
     * Starts a new XML root element with the name passed in. If you are creating
     * a root element and you are using namespaces, you should call this method, not 
     * {@link StaxxasStreamWriter#startElement(String)}. This method will write the
     * namespaceURI mappings into the root node's attributes.
     * 
     * <p>If you are not using namespaces, then using either this method or 
     * {@link StaxxasStreamWriter#startElement(String)} is fine for the root 
     * element.</p>
     * 
     * @param localName name of XML element to start
     * @throws StaxxasStreamWriterException (RuntimeException) if the 
     * underlying StAX library throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter startRootElement(String localName) {
		writeElement(localName);
		writeRootNamespaces(localName);
		return this;    	
    }
    
    /**
     * Starts a new XML element with the name passed in.  If you are creating a 
     * root element and you are using namespaces, you should call 
     * {@code startRootElement}. This method will <strong>not</strong> write 
     * the namespace to URI mappings into the root element.
     * 
     * <p>If you are not using namespaces, then using this method for writing the root 
     * element is fine.</p> 
     * 
     * @param localName name of XML element to start
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying 
     * StAX library throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter startElement(String localName) {
		writeElement(localName);
		return this;
    }

    /**
     * Starts a new XML element with the element name and the namespace
     * prefix passed in.  This is an alternative to calling 
     * {@code setCurrentNamespace()} before calling {@code startElement}.
     * 
     * <p>The namespace prefix passed in only applies to this element and
     * does not change the current namespace for other elements. It is
     * allowable to pass in {@code null} for the nsPrefix parameter; doing
     * so will unset the current namespace (for this element only) and 
     * write an unprefixed element associated with the default namespace
     * (if any).<p>
     * 
     * @param localName name of XML element to start
     * @param nsPrefix registered prefix to use for this element. <code>null</code>
     * is allowed to map to the default namespace.
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying 
     * StAX library throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter startElement(String localName, String nsPrefix) {
		String hold = currNamespace;
		currNamespace = nsPrefix;
    	writeElement(localName);
    	currNamespace = hold;
		return this;
    }

    /**
     * Writes an empty XML element as a single tag: e.g., 
     * <code>{@literal <foo/>}</code>.
     * 
     * @param localName name of element
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying 
     * StAX library throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter emptyElement(String localName) {
    	writeEmptyElement(localName);
    	return this;
    }

    /**
     * Convenience method to write a closing tag. It can be self-documenting to say which 
     * tag you are closing if the {@code startElement()} call is far away.  The string
     * passed in is ignored and not validated in any way.  It is only there to have 
     * self-documenting code.
     * 
     * @param localName ignored: It is for documentation purposes only.
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying 
     * StAX library throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter endElement(String localName) {
    	return endElement();
    }
    
    /**
     * Write the end tag (element) of the last tag that was started.
     * 
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying 
     * StAX library throws an XMLStreamException 
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
     * Writes an attribute into the last element started. Functionality is the same as
     * the XMLStreamWriter function: 
     * {@link XMLStreamWriter#writeAttribute(String, String)}.
     * 
     * <p>Attributes written with this method will <strong>not</strong> be prefixed with
     * a namespace prefix even if you have set a current namespace by calling 
     * {@link StaxxasStreamWriter#setCurrentNamespace(String)}.  In order to prefix 
     * an attribute you must pass in the prefix to the 
     * {@link StaxxasStreamWriter#prefixedAttribute(String, String, String)} method.</p>
     * 
     * @param localName name of attribute
     * @param value value of attribute
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

    /**
     * Writes a prefixed attribute into the last element started. 
     * 
     * <p>Attributes written with this method will be prefixed with
     * a namespace prefix you pass in, which can differ from current namespace set by calling 
     * {@link StaxxasStreamWriter#setCurrentNamespace(String)}.</p> 
     *		
     * <p>The namespace prefix and its corresponding URI <strong>must</strong> have already 
     * been registered with this StaxxasStreamWriter. This can be done via one
     * of the {@code mapNamespaceToUri} methods or via the constructor that accepts
     * a Map parameter.  If the prefix cannot be found in mappings already registered, then
     * a StaxxasStreamWriterException will be thrown.</p>
     * 
     * @param prefix namespace prefix
     * @param localName name of attribute
     * @param value value of attribute
     * @return this StaxxasStreamWriter in order to allow method chaining
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     */
    public StaxxasStreamWriter prefixedAttribute(String prefix, 
    		String localName, String value) {
    	try {
			w.writeAttribute(m.get(prefix), localName, value);
    		return this;
		} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("writePrefixedAttribute", "writeAttribute", e);
		}
    }

    /**
     * Writes text content into the most recently opened XML element.
     * 
     * @param text String text to write inside the most recently started XML element
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying
     * StAX library throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter characters(String text) {
    	try {
    		w.writeCharacters(text);
			return this;
		} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("characters", "writeCharacters", e);
		}
    }
    
    /**
     * Writes the subset of characters specified by the parameters as text content inside 
     * the most recently opened XML element.
     * 
     * @param text character array to pull character data from
     * @param start the first position in the char array to start pulling from
     * @param len the number of characters to pull from the char array, starting from position
     * specified by the previous param
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter characters(char[] text, int start, int len) {
    	try {
    		w.writeCharacters(text, start, len);
			return this;
		} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("characters", "writeCharacters", e);
		}
    }

    /**
     * Writes an XML comment inside the most recently started XML element (or at the top
     * level if no XML element is started). 
     * 
     * @param text String comment to write
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     * @return this StaxxasStreamWriter in order to allow method chaining
     */
    public StaxxasStreamWriter comment(String text) {
    	try {
    		w.writeComment(text);
			return this;
		} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("comment", "writeComment", e);
		}
    }
    
    
    /**
     * Writes a processing instruction.  This version can only write the processing instruction
     * target name without any additional "attributes" or qualifiers.
     * 
     * <p><strong>Example:</strong> When using this method if you call
     * <br/><code>{@literal processingInstruction("foo")}</code> 
     * <br/>then it will write<br/> 
     * <code>{@literal <?foo?>}</code>
     * </p>
     * 
     * @param target name/target of the processing instruction
     * @return this StaxxasStreamWriter in order to allow method chaining
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     */    
    public StaxxasStreamWriter processingInstruction(String target) {
    	try {
    		w.writeProcessingInstruction(target);
			return this;
		} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("processingInstruction", 
					"writeProcessingInstruction", e);
		}    	
    }
    
    /**
     * Writes a processing instruction.
     * 
     * <p><strong>Example:</strong> If you want a processing instruction like <br/> 
     * <code>{@literal <?foo bar="quux"?>}</code>
     * <br/>then you need to call this method like this:<br/>
     * <code>{@literal processingInstruction("foo", "bar=\"quux\"")}</code>
     * </p>
     * 
     * @param target name/target of the processing instruction
     * @param data the data (name and value pair as a String) to add to the PI
     * @return this StaxxasStreamWriter in order to allow method chaining
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     */    
    public StaxxasStreamWriter processingInstruction(String target, String data) {
    	try {
    		w.writeProcessingInstruction(target, data);
			return this;
		} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("processingInstruction", 
					"writeProcessingInstruction", e);
		}    	
    }
    
    /**
     * Writes an entity reference to the XML document. The text you supply will 
     * be wrapped have a {@code &} at the start and {@code ;} at the end. So if 
     * you supply "apos", it will write {@code &apos;}.
     * 
     * @param eref core of the entity reference to write (minus the ampersand and semicolon)
     * @return this StaxxasStreamWriter in order to allow method chaining
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     */    
    public StaxxasStreamWriter entityRef(String eref) {
    	try {
    		w.writeEntityRef(eref);
    		return this;
    	} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("entityRef", "writeEntityRef", e);
		}
    }
    
    /**
     * Writes a DTD to the XML doc.
     * 
     * @param dtd DTD to write to the XML doc
     * @return this StaxxasStreamWriter in order to allow method chaining
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     */    
    public StaxxasStreamWriter dtd(String dtd) {
    	try {
    		w.writeDTD(dtd);
    		return this;
    	} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("dtd", "writeDTD", e);
		}
    }
    
    /**
     * Writes a CDATA section to the element currently opened. 
     * 
     * @param cdata CDATA text to write inside the most recently started XML element
     * @return this StaxxasStreamWriter in order to allow method chaining
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying StAX library 
     * throws an XMLStreamException 
     */    
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
     * Sets the mappings of prefixes to URIs on the XMLStreamWriter.
     * Should only be called from startDoc() after calling
     * {@link XMLStreamWriter#writeStartDocument()} (or one of the
     * other <code>startDocument</code> methods).
     * 
     * @throws StaxxasStreamWriterException (RuntimeException) if the underlying
     * StAX library throws an XMLStreamException 
     */    private void setPrefixes() {
    	try {
    		for (String p: m.keySet()) {
    			w.setPrefix(p, m.get(p));
    		}
    	} catch (XMLStreamException e) {
			throw new StaxxasStreamWriterException("startDocument", 
					"setPrefixes", e);    					    		
    	}
    }
}
