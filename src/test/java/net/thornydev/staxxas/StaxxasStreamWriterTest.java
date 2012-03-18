package net.thornydev.staxxas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class StaxxasStreamWriterTest {

	StringWriter writer;
	StaxxasStreamWriter sx;
	
	@Before
	public void setUp() throws Exception {
		writer = new StringWriter(); 
		XMLOutputFactory xof = XMLOutputFactory.newFactory();
	    sx = new StaxxasStreamWriter( xof.createXMLStreamWriter(writer) );
	}

	@Test
	public void testStartAndEndDoc() {
		sx.startDoc();
		sx.endDoc();
		assertNotNull(writer.toString());
		
		String out = writer.toString().trim();
		String re = "^<\\?\\s*xml\\s+version=\"1.0\"\\s*\\?>";
		Matcher m = Pattern.compile(re).matcher(out);
		assertTrue(out, m.find());
		assertTrue(out, m.matches());
	}

	@Test
	public void testProcessingInstruction() {
		sx.startDoc();
		sx.processingInstruction("quux");
		sx.processingInstruction("xml-stylesheet", "type=\"text/css\" href=\"style.css\"");
		sx.emptyElement("foo");
		sx.endDoc();
		assertNotNull(writer.toString());


		String out = writer.toString().trim();
		// <?xml version="1.0" ?><?quux?><?xml-stylesheet type="text/css" href="style.css"?><foo/>
		String re = "^<\\?\\s*xml\\s+version=\"1.0\"\\s*\\?><\\?\\s*quux\\s*\\?>"
				+"<\\?xml-stylesheet\\s+type=\"text/css\"\\s+href=\"style.css\"\\s*\\?>\\s*<foo/>\\s*$";
		Matcher m = Pattern.compile(re).matcher(out);
		assertTrue(out, m.matches());
	}

	@Test
	public void testEntityRef() {
		//apos
		sx.startDoc();
		sx.startElement("foo");
		sx.entityRef("apos");
		sx.emptyElement("bar");
		sx.entityRef("frankenstein");
		sx.endElement();
		sx.endDoc();
		assertNotNull(writer.toString());

		String out = writer.toString().trim();
		String re = "^<\\?\\s*xml\\s+version=\"1.0\"\\s*\\?>\\s*<foo>\\s*&apos;\\s*<bar/>\\s*&frankenstein;\\s*</foo>\\s*$";
		assertTrue(Pattern.compile(re).matcher(out).matches());
	}
	
	@Test
	public void testStartAndEndDocWithEncodingAndVersion() {
		sx.startDoc("UTF-8", "1.1");
		sx.endDoc();
		assertNotNull(writer.toString());
		
		String out = writer.toString().trim();
		String re = "^<\\?\\s*xml\\s+version=\"1.1\"\\s*encoding=\"UTF-8\"\\s*\\?>";
		Matcher m = Pattern.compile(re).matcher(out);
		assertTrue(out, m.find());
		assertTrue(out, m.matches());
	}

	@Test
	public void testStartAndEndElementSimple() {
		sx.startDoc();
		sx.startElement("foo");
		sx.endElement();
		sx.endDoc();
		assertNotNull(writer.toString());
		
		String out = writer.toString().trim();
		String reDecl = "^<\\?\\s*xml\\s+version=\"1.0\"\\s*\\?>";
		assertTrue(out, Pattern.compile(reDecl).matcher(out).find());
	
		String reBody = "<foo>\\s*</foo>$";
		assertTrue(out, Pattern.compile(reBody).matcher(out).find());
	}

	@Test
	public void testWriteCharacters() throws Exception {
		String barText = "I am the text in bar.\nLine 2.";
		char[] bazText = "1234567. All good children go to heaven.".toCharArray();
		
		sx.startDoc();
		sx.startElement("foo");
		sx.startElement("bar").characters(barText).endElement();
		sx.startElement("baz").characters(bazText, 13, 13).endElement();
		sx.endElement();
		sx.endDoc();
		assertNotNull(writer.toString());
		
		String out = writer.toString().trim();
		String reDecl = "^<\\?\\s*xml\\s+version=\"1.0\"\\s*\\?>";
		assertTrue(out, Pattern.compile(reDecl).matcher(out).find());
	
		Document doc = createDOM(out);
		XPath xp = XPathFactory.newInstance().newXPath();
		
		String s;
		s = (String) xp.evaluate("//bar/text()", doc, XPathConstants.STRING);
		assertEquals(barText, s);

		s = (String) xp.evaluate("//baz/text()", doc, XPathConstants.STRING);
		assertEquals("good children", s);
	}

	@Test
	public void testWriteComment() throws Exception {
		String comment1 = "I am the comment.";
		String comment2 = "I am the other comment.";
		
		sx.startDoc();
		sx.startElement("foo");
		sx.comment(comment1);
		sx.startElement("bar").comment(comment2).endElement();
		sx.emptyElement("baz");
		sx.endElement("foo");
		sx.endDoc();
		assertNotNull(writer.toString());
		
		String out = writer.toString().trim();
		String reDecl = "^<\\?\\s*xml\\s+version=\"1.0\"\\s*\\?>";
		assertTrue(out, Pattern.compile(reDecl).matcher(out).find());
	
		Document doc = createDOM(out);
		XPath xp = XPathFactory.newInstance().newXPath();
		Node n;
		n = (Node) xp.evaluate("/foo/comment()", doc, XPathConstants.NODE);
		assertNotNull(n);
		assertEquals(comment1, n.getNodeValue());
		n = (Node) xp.evaluate("/foo/bar/comment()", doc, XPathConstants.NODE);
		assertNotNull(n);
		assertEquals(comment2, n.getNodeValue());
	}
	
	@Test
	public void testStartAndEndElementCompound() {
		sx.startDoc();
		sx.startElement("foo");
		sx.startElement("bar");
		sx.startElement("baz").endElement();
		sx.startElement("quux").endElement();
		sx.emptyElement("wibble");
		sx.endElement("bar");
		sx.endElement("foo");
		sx.endDoc();
		assertNotNull(writer.toString());
		
		String out = writer.toString().trim();
		String reDecl = "^<\\?\\s*xml\\s+version=\"1.0\"\\s*\\?>";
		assertTrue(out, Pattern.compile(reDecl).matcher(out).find());
	
		String reBody = "\\s*<foo>\\s*<bar>\\s*<baz>\\s*</baz>\\s*<quux>\\s*</quux>\\s*<wibble/>\\s*</bar>\\s*</foo>$";
		assertTrue(out, Pattern.compile(reBody).matcher(out).find());	
	}

	@Test
	public void testStartAndEndElementSimpleWithNamespaces() {
	    sx.mapNamespaceToUri("quux", "http://www.quux.org/quux");
	    sx.startDoc();
	    sx.setCurrentNamespace("quux");
	    sx.startRootElement("foo");
	    sx.endElement();
	    sx.endDoc();

	    assertNotNull(writer.toString());
		
		String out = writer.toString().trim();
		String reDecl = "^<\\?\\s*xml\\s+version=\"1.0\"\\s*\\?>";
		assertTrue(out, Pattern.compile(reDecl).matcher(out).find());
	
		String reBody = "<quux:foo\\s+xmlns:quux=\"http://www.quux.org/quux\"\\s*>\\s*</quux:foo>$";
		assertTrue(out, Pattern.compile(reBody).matcher(out).find());	
	}

	@Test
	public void testEmptyElementWithPrimaryNamespace() throws Exception {
		String defaultNsUri = "http://www.quux.org/quux";
		Map<String,String> nsToUri = new HashMap<String,String>();
		nsToUri.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		sx.mapNamespaceToUri(nsToUri);
		sx.setDefaultNamespace(defaultNsUri);
		sx.startDoc().
		  startRootElement("foo").
		  attribute("xsi:schemaLocation", "http://www.springframework.org/schema/beans");
		sx.emptyElement("bar");
		sx.emptyElement("baz");
		sx.endElement("foo");
		sx.endDoc();
		
		assertNotNull(writer.toString());
		
		String out = writer.toString().trim();
		String reDecl = "^<\\?\\s*xml\\s+version=\"1.0\"\\s*\\?>";
		assertTrue(out, Pattern.compile(reDecl).matcher(out).find());

		String reBody = "<bar\\s*/>\\s*<baz\\s*/>\\s*</foo>";
		assertTrue(out, Pattern.compile(reBody).matcher(out).find());
				
		/* ---[ test with XPath expressions ]--- */
		Document doc = createDOM(out);
		// even though it is the default ns and does not require prefixing in the XML doc
		// namespace aware DOM/XPath parsing requires you to refer to it via a prefix,
		// so we add the "q" prefix for XPath
		nsToUri.put("q", defaultNsUri);
		XPath xp = createXPath(nsToUri);
        
        // refs returned from XPath queries
        NodeList nl;
        Node n;
        
        n = (Node) xp.evaluate("/q:foo", doc, XPathConstants.NODE);
        assertNotNull(n);
        assertEquals("foo", n.getLocalName());

        nl = (NodeList) xp.evaluate("/q:foo/q:bar", doc, XPathConstants.NODESET);
        assertNotNull(nl);
        assertEquals(1, nl.getLength());
        
        nl = (NodeList) xp.evaluate("/q:foo/q:baz", doc, XPathConstants.NODESET);
        assertNotNull(nl);
        assertEquals(1, nl.getLength());
	}	
	
	@Test
	public void testStartAndEndElementCompoundWithPrimaryNamespaces() {
		String defaultNsUri = "http://www.quux.org/quux";
		sx.mapNamespaceToUri("xsi", "http://www.w3.org/2001/XMLSchema-instance");
	    sx.setDefaultNamespace(defaultNsUri);
	    sx.startDoc();
	    sx.startRootElement("foo");
	    sx.attribute("xsi:schemaLocation", "http://www.springframework.org/schema/beans");
	    sx.startElement("bar");
		sx.startElement("baz");
		sx.endElement();
		sx.startElement("quux");
		sx.endElement();
		sx.endElement();
		sx.endElement();
	    sx.endDoc();

	    assertNotNull(writer.toString());
		
		String out = writer.toString().trim();
		String reDecl = "^<\\?\\s*xml\\s+version=\"1.0\"\\s*\\?>";
		assertTrue(out, Pattern.compile(reDecl).matcher(out).find());
	
		String reRoot1 = "\\s*<foo[^>]+xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";
		assertTrue(out, Pattern.compile(reRoot1).matcher(out).find());	
		String reRoot2 = "\\s*<foo[^>]+xsi:schemaLocation=\"http://www.springframework.org/schema/beans\"";
		assertTrue(out, Pattern.compile(reRoot2).matcher(out).find());	
		String reRoot3 = String.format("\\s*<foo[^>]+xmlns=\"%s\"", defaultNsUri);
		assertTrue(out, Pattern.compile(reRoot3).matcher(out).find());	
		
		String reRest = "\\s*<bar>\\s*<baz>\\s*</baz>\\s*<quux>\\s*</quux>\\s*</bar>\\s*</foo>$";
		assertTrue(out, Pattern.compile(reRest).matcher(out).find());	
	}
	
	@Test
	public void testWriteAttributesNoPrefixes() throws Exception {
		sx.startDoc();
		sx.startRootElement("foo").
			attribute("bar", "baz").
			attribute("abc", "123");
		sx.startElement("quux").
			attribute("wibble", "wobble").
			endElement();
		sx.endElement("foo");
		sx.endDoc();

		String out = writer.toString().trim();
		String reDecl = "^<\\?\\s*xml\\s+version=\"1.0\"\\s*\\?>";
		assertTrue(out, Pattern.compile(reDecl).matcher(out).find());

		/* ---[ test with XPath expressions ]--- */
		Document doc = createDOM(out);
        XPath xp = XPathFactory.newInstance().newXPath();
        
        // refs returned from XPath queries
        NodeList nl;
        Node n;
        
        n = (Node) xp.evaluate("/foo", doc, XPathConstants.NODE);
        assertNotNull(n);
        assertEquals("foo", n.getLocalName());

        nl = (NodeList) xp.evaluate("//*", doc, XPathConstants.NODESET);
        assertNotNull(nl);
        assertEquals(2, nl.getLength());

        nl = (NodeList) xp.evaluate("/foo/@*", doc, XPathConstants.NODESET);
        assertNotNull(nl);
        assertEquals(2, nl.getLength());

        n = (Node) xp.evaluate("/foo/@bar", doc, XPathConstants.NODE);
        assertNotNull(n);
        assertEquals("baz", n.getNodeValue());

        n = (Node) xp.evaluate("/foo/@abc", doc, XPathConstants.NODE);
        assertNotNull(n);
        assertEquals("123", n.getNodeValue());
        
        nl = (NodeList) xp.evaluate("/foo/quux/@*", doc, XPathConstants.NODESET);
        assertNotNull(nl);
        assertEquals(1, nl.getLength());

        n = (Node) xp.evaluate("//quux/@wibble", doc, XPathConstants.NODE);
        assertNotNull(n);
        assertEquals("wobble", n.getNodeValue());
	}

	@Test
	public void testWritePrefixedAttributes() throws Exception {
		Map<String,String> nsToUri = new HashMap<String,String>();
		nsToUri.put("aa", "http://www.example.org/aa");
		nsToUri.put("bb", "http://www.example.org/bb");
		sx.mapNamespaceToUri(nsToUri);
		
		sx.startDoc();
	    
	    sx.setCurrentNamespace("aa");
	    sx.startRootElement("foo");
	    
	    sx.startElement("quux").
    		prefixedAttribute("aa", "bar", "baz").
    		prefixedAttribute("bb", "abc", "123").
    		attribute("noway", "jose").  // this will NOT be prefixed
	    	endElement();
	    
	    sx.endElement("foo");
	    sx.endDoc();
	    
		String out = writer.toString().trim();
		String reDecl = "^<\\?\\s*xml\\s+version=\"1.0\"\\s*\\?>";
		assertTrue(out, Pattern.compile(reDecl).matcher(out).find());

		// prints
		// <?xml version="1.0" ?><aa:foo xmlns:aa="http://www.example.org/aa" xmlns:bb="http://www.example.org/bb">
		// <aa:quux aa:barr="baz" bb:abc="123"></aa:quux></aa:foo>

	    Document doc = createDOM(out);
		XPath xp = createXPath(nsToUri);

		Node n;
		NodeList nl;
		
		nl = (NodeList) xp.evaluate("/aa:foo/aa:quux/@*", doc, XPathConstants.NODESET);
		assertEquals(3, nl.getLength());
		
		n = (Node) xp.evaluate("//aa:quux/@aa:bar", doc, XPathConstants.NODE);
		assertEquals("baz", n.getNodeValue());

		n = (Node) xp.evaluate("//aa:quux/@bb:abc", doc, XPathConstants.NODE);
		assertEquals("123", n.getNodeValue());

		n = (Node) xp.evaluate("//aa:quux/@noway", doc, XPathConstants.NODE);
		assertEquals("jose", n.getNodeValue());
}	
	
	private Document createDOM(final String xml) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		Document doc = dbf.newDocumentBuilder().
				parse(new ByteArrayInputStream(xml.getBytes()));
		return doc;
	}
	
	private XPath createXPath(final Map<String,String> nsToUri) {
		XPath xp = XPathFactory.newInstance().newXPath();
		xp.setNamespaceContext(new NamespaceContext() {
			@SuppressWarnings("rawtypes")
			@Override public Iterator getPrefixes(String namespaceURI) {
				return null;
			}
			@Override public String getPrefix(String namespaceURI) {
				return null;
			}
			@Override
			public String getNamespaceURI(String prefix) {
				if (prefix == null) throw new IllegalArgumentException("null prefix");
				return nsToUri.get(prefix);
			}
		});
		return xp;
	}
	
	@Test
	public void testWriteAttributesWithNodePrefixes() throws Exception {
		Map<String,String> nsToUri = new HashMap<String,String>();
		nsToUri.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		nsToUri.put("aa", "http://www.example.org/aa");
		nsToUri.put("bb", "http://www.example.org/bb");
		sx.mapNamespaceToUri(nsToUri);

	    sx.startDoc();
	    
	    sx.setCurrentNamespace("aa");
	    sx.startRootElement("foo").
	    	attribute("bar", "baz").
	    	attribute("abc", "123");

	    sx.setCurrentNamespace("bb");	    
		sx.startElement("quux").
			attribute("wibble", "wobble").
			endElement();

		sx.endElement("foo");
		sx.endDoc();

		String out = writer.toString().trim();
		String reDecl = "^<\\?\\s*xml\\s+version=\"1.0\"\\s*\\?>";
		assertTrue(out, Pattern.compile(reDecl).matcher(out).find());

		/* ---[ test with XPath expressions ]--- */
		Document doc = createDOM(out);
		XPath xp = createXPath(nsToUri);
		
		// refs returned from XPath queries
        NodeList nl;
        Node n;
        
        n = (Node) xp.evaluate("/aa:foo", doc, XPathConstants.NODE);
        assertNotNull(n);
        assertEquals("foo", n.getLocalName());

        nl = (NodeList) xp.evaluate("/aa:foo/@*", doc, XPathConstants.NODESET);
        assertNotNull(nl);
        assertEquals(2, nl.getLength());

        n = (Node) xp.evaluate("/aa:foo/@bar", doc, XPathConstants.NODE);
        assertNotNull(n);
        assertEquals("baz", n.getNodeValue());

        n = (Node) xp.evaluate("/aa:foo/@abc", doc, XPathConstants.NODE);
        assertNotNull(n);
        assertEquals("123", n.getNodeValue());	
        
        nl = (NodeList) xp.evaluate("/aa:foo/bb:quux/@*", doc, XPathConstants.NODESET);
        assertNotNull(nl);
        assertEquals(1, nl.getLength());

        n = (Node) xp.evaluate("//bb:quux/@wibble", doc, XPathConstants.NODE);
        assertNotNull(n);
        assertEquals("wobble", n.getNodeValue());
	}
	
	@Test(expected=StaxxasStreamWriterException.class)
	public void testCallEndElementBeforeStartElementShouldThrowStaxxasException() {
		sx.startDoc();
		sx.endElement();
		fail("Shouldn't get here");
	}
	
	@Test
	public void testForgetToCloseElementIsOK() {
		sx.startDoc();
		sx.startElement("foo");
		sx.startElement("bar");
		// forgot to close both foo and bar element here
		// sx.endElement();
		sx.endDoc();
		String out = writer.toString();
	
		String re = "^<\\?\\s*xml\\s+version=\"1.0\"\\s*\\?>\\s*<foo>\\s*<bar>\\s*</bar>\\s*</foo>\\s*$";
		assertTrue(Pattern.compile(re).matcher(out).matches());		
	}
	
	
	@Test
	public void testMixedNamespacePrefixes() {
		sx.mapNamespaceToUri("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		sx.mapNamespaceToUri("aa", "http://www.example.org/aa");
		sx.mapNamespaceToUri("bb", "http://www.example.org/bb");
	    sx.startDoc();
	    sx.setCurrentNamespace("aa");
	    sx.startRootElement("foo");
	    sx.setCurrentNamespace("bb");
	    sx.startElement("bar");
		sx.startElement("baz").endElement();
	    sx.setCurrentNamespace("aa");
		sx.startElement("quux").endElement();
		sx.endElement(); // ends bar
		sx.endElement(); // ends foo
		
		assertNotNull(writer.toString());
		
		String out = writer.toString().trim();
		String reDecl = "^<\\?\\s*xml\\s+version=\"1.0\"\\s*\\?>";
		assertTrue(out, Pattern.compile(reDecl).matcher(out).find());

		// TODO: not done
		String reRoot1 = "\\s*<aa:foo[^>]+xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";
		assertTrue(out, Pattern.compile(reRoot1).matcher(out).find());			
		String reRoot2 = "\\s*<aa:foo[^>]+xmlns:aa=\"http://www.example.org/aa\"";
		assertTrue(out, Pattern.compile(reRoot2).matcher(out).find());	
		String reRoot3 = "\\s*<aa:foo[^>]+xmlns:bb=\"http://www.example.org/bb\"";
		assertTrue(out, Pattern.compile(reRoot3).matcher(out).find());	
		
		String reRest = "\\s*<bb:bar>\\s*<bb:baz>\\s*</bb:baz>\\s*<aa:quux>\\s*</aa:quux>\\s*</bb:bar>\\s*</aa:foo>$";
		assertTrue(out, Pattern.compile(reRest).matcher(out).find());	
	}
	
	@Test
	public void testDeleteMeLater() {
		try {
			XMLOutputFactory xmlof = XMLOutputFactory.newFactory();
			XMLStreamWriter xmlsw = xmlof.createXMLStreamWriter(new FileWriter("recipe.xml"));
			xmlsw.writeStartDocument();
			xmlsw.setPrefix("h", "http://www.w3.org/1999/xhtml");
			xmlsw.writeStartElement("http://www.w3.org/1999/xhtml", "html");
			xmlsw.writeNamespace("h", "http://www.w3.org/1999/xhtml");
			xmlsw.writeNamespace("r", "http://www.tutortutor.ca/");
			xmlsw.writeDefaultNamespace("http://www.weirdal.com/white-n-nerdy");
			xmlsw.writeStartElement("http://www.w3.org/1999/xhtml", "head");
			xmlsw.writeStartElement("http://www.w3.org/1999/xhtml", "title");
			xmlsw.writeCharacters("Recipe");
			xmlsw.writeEndElement();
			xmlsw.writeEndElement();
			xmlsw.writeStartElement("http://www.w3.org/1999/xhtml", "body");
			xmlsw.setPrefix("r", "http://www.tutortutor.ca/");
			xmlsw.writeStartElement("http://www.tutortutor.ca/", "recipe");
			xmlsw.writeStartElement("http://www.tutortutor.ca/", "title");
			xmlsw.writeCharacters("Grilled Cheese Sandwich");
			xmlsw.writeEndElement();
			xmlsw.writeStartElement("http://www.tutortutor.ca/", "ingredients");
			xmlsw.setPrefix("h", "http://www.w3.org/1999/xhtml");
			xmlsw.writeStartElement("http://www.w3.org/1999/xhtml", "ul");
			xmlsw.writeStartElement("http://www.w3.org/1999/xhtml", "li");
			xmlsw.setPrefix("r", "http://www.tutortutor.ca/");
			xmlsw.writeStartElement("http://www.tutortutor.ca/", "ingredient");
			xmlsw.writeAttribute("qty", "2");
			xmlsw.writeCharacters("bread slice");
			xmlsw.writeEndElement();
			xmlsw.setPrefix("h", "http://www.w3.org/1999/xhtml");
			xmlsw.writeEndElement();
			xmlsw.writeEndElement();
			xmlsw.setPrefix("r", "http://www.tutortutor.ca/");
			xmlsw.writeEndElement();
			xmlsw.writeEndDocument();
			xmlsw.flush();
			xmlsw.close();
		} catch (FactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
