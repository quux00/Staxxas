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
	    sx = new StaxxasStreamWriter(writer);
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
	public void testConstructorTakesXMLStreamWriter() throws Exception {
		XMLOutputFactory xof = XMLOutputFactory.newFactory();
	    sx = new StaxxasStreamWriter( xof.createXMLStreamWriter(writer) );
	
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
	public void testMixPrefixedAndDefaultNamespaces() throws Exception {
		Map<String,String> nsToUri = new HashMap<String,String>();
		nsToUri.put("foo", "http://www.example.com/foo");
		nsToUri.put("bar", "http://www.example.com/bar");
		
		sx.mapNamespaceToUri(nsToUri);
		String defaultNsUri = "http://www.example.com/quux";
		sx.setDefaultNamespace(defaultNsUri);

		sx.startDoc();
		sx.startRootElement("inventory");
		
		sx.setCurrentNamespace("foo");
		sx.startElement("site").
			prefixedAttribute("foo", "isWarehouse", "yes").
			characters("Oklahoma City facility").
			endElement();

		// set prefix for this one element only in the startElement call
		sx.startElement("capacity", "bar").
			prefixedAttribute("foo", "units", "sq.ft.").
			characters("200,000").
			endElement();

		sx.setCurrentNamespace(null);  // next element(s) in default namespace
		sx.startElement("items");
		
		sx.startElement("item");		
		sx.setCurrentNamespace("foo");
		sx.startElement("sku").characters("ABC123").endElement();
		sx.startElement("description").characters("iPad3").endElement();
		sx.startElement("quantity").characters("38,500").endElement();
		sx.endElement("item");
		
		sx.startElement("item", null);  // unset currName only for this one element
		sx.setCurrentNamespace("foo");
		sx.startElement("sku").characters("DEF456").endElement();
		sx.startElement("description").characters("Kindle Fire").endElement();
		sx.startElement("quantity").characters("22,200").endElement();
		
		sx.endElement("item");  // use self documenting feature to label close tag 
		sx.endElement("items");
		
		sx.endElement("inventory");
		sx.endDoc();
		
		String out = writer.toString();
		Document doc = createDOM(out);
		nsToUri.put("def", defaultNsUri);  // add it here for the XPath NamespaceContext
		XPath xp = createXPath(nsToUri);
		
		NodeList nl = null;
		Node     n = null;
		String   s = null;
		
		nl = (NodeList) xp.evaluate("//*", doc, XPathConstants.NODESET);
		assertNotNull(nl);
		assertEquals(12, nl.getLength());

		nl = (NodeList) xp.evaluate("/def:inventory", doc, XPathConstants.NODESET);
		assertNotNull(nl);
		assertEquals(1, nl.getLength());

		nl = (NodeList) xp.evaluate("//def:item", doc, XPathConstants.NODESET);
		assertNotNull(nl);
		assertEquals(2, nl.getLength());
	
		nl = (NodeList) xp.evaluate("/def:inventory/def:items/def:item/foo:sku", 
				doc, XPathConstants.NODESET);
		assertNotNull(nl);
		assertEquals(2, nl.getLength());
	
		nl = (NodeList) xp.evaluate("//def:item/foo:quantity", 
				doc, XPathConstants.NODESET);
		assertNotNull(nl);
		assertEquals(2, nl.getLength());
	
		s = (String) xp.evaluate("//bar:capacity/text()", doc, XPathConstants.STRING);
		assertEquals("200,000", s);

		n = (Node) xp.evaluate("//bar:capacity/@foo:units", doc, XPathConstants.NODE);
		assertNotNull(n);
		assertEquals("sq.ft.", n.getNodeValue());
		
		s = (String) xp.evaluate("//def:item[2]/foo:description/text()", doc, 
				XPathConstants.STRING);
		assertEquals("Kindle Fire", s);
	}
	
	@Test
	public void testDeleteMeLater3() throws Exception {
		FileWriter fw = null;
		try {
			fw = new FileWriter("staxxas-out.xml");
			
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}

		StaxxasStreamWriter stxs = new StaxxasStreamWriter(fw);
		stxs.setDefaultNamespace("http://www.example.com/quux");
		stxs.mapNamespaceToUri("foo", "http://www.example.com/foo");
		stxs.mapNamespaceToUri("bar", "http://www.example.com/bar");

		stxs.startDoc();
		stxs.startRootElement("inventory");
		
		stxs.setCurrentNamespace("foo");
		stxs.startElement("site").
			prefixedAttribute("foo", "isWarehouse", "yes").
			characters("Oklahoma City facility").
			endElement();
		
		stxs.setCurrentNamespace("bar");
		stxs.startElement("capacity").
			prefixedAttribute("foo", "units", "sq.ft.").
			characters("200,000").
			endElement();

		stxs.setCurrentNamespace(null);  // next element(s) in default namespace
		stxs.startElement("items");
		
		stxs.startElement("item");		
		stxs.setCurrentNamespace("foo");
		stxs.startElement("sku").characters("ABC123").endElement();
		stxs.startElement("description").characters("iPad3").endElement();
		stxs.startElement("quantity").characters("38,500").endElement();
		stxs.endElement("item");
		
		stxs.startElement("item", null);  // unset currNamespace only for this one element
		stxs.startElement("sku").characters("DEF456").endElement();
		stxs.startElement("description").characters("Kindle Fire").endElement();
		stxs.startElement("quantity").characters("22,200").endElement();
		
		stxs.endElement("item");  // use self documenting feature to label close tag 
		stxs.endElement("items");
		
		stxs.endElement("inventory");
		stxs.endDoc();
	}

	@Test
	public void testDeleteMeLater2() {
		try {
			FileWriter fw = new FileWriter("jaxp-stax-out.xml");
			XMLOutputFactory xmlof = XMLOutputFactory.newFactory();
			XMLStreamWriter xmlsw = xmlof.createXMLStreamWriter(fw);
			xmlsw.writeStartDocument();
			
			// have to set up prefixes like this first
			xmlsw.setDefaultNamespace("http://www.example.com/quux");
			xmlsw.setPrefix("foo", "http://www.example.com/foo");
			xmlsw.setPrefix("bar", "http://www.example.com/bar");

			xmlsw.writeStartElement("inventory");
			xmlsw.writeDefaultNamespace("http://www.example.com/quux");
			xmlsw.writeNamespace("foo", "http://www.example.com/foo");
			xmlsw.writeNamespace("bar", "http://www.example.com/bar");		

			xmlsw.writeStartElement("http://www.example.com/foo", "site");
			xmlsw.writeAttribute("http://www.example.com/foo", "isWarehouse", "yes");
			xmlsw.writeCharacters("Oklahoma City facility");
			xmlsw.writeEndElement();
			
			xmlsw.writeStartElement("http://www.example.com/bar", "capacity");
			xmlsw.writeAttribute("http://www.example.com/foo", "units", "sq.ft.");
			xmlsw.writeCharacters("200,000");
			xmlsw.writeEndElement();
			
			xmlsw.writeStartElement("items");
			
			xmlsw.writeStartElement("item");
			xmlsw.writeStartElement("http://www.example.com/foo", "sku");
			xmlsw.writeCharacters("ABC123");
			xmlsw.writeEndElement();
			xmlsw.writeStartElement("http://www.example.com/foo", "description");
			xmlsw.writeCharacters("iPad3");
			xmlsw.writeEndElement();
			xmlsw.writeStartElement("http://www.example.com/foo", "quantity");
			xmlsw.writeCharacters("38,500");
			xmlsw.writeEndElement();
			xmlsw.writeEndElement();

			xmlsw.writeStartElement("item");
			xmlsw.writeStartElement("http://www.example.com/foo", "sku");
			xmlsw.writeCharacters("DEF456");
			xmlsw.writeEndElement();
			xmlsw.writeStartElement("http://www.example.com/foo", "description");
			xmlsw.writeCharacters("Kindle Fire");
			xmlsw.writeEndElement();
			xmlsw.writeStartElement("http://www.example.com/foo", "quantity");
			xmlsw.writeCharacters("22,200");
			xmlsw.writeEndElement();
			xmlsw.writeEndElement();

			xmlsw.writeEndElement();
			
			xmlsw.writeEndElement();
			xmlsw.writeEndDocument();
			xmlsw.flush();
			xmlsw.close();
			fw.close();

		} catch (FactoryConfigurationError e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//@Test
	public void testDeleteMeLater() {
		try {
			XMLOutputFactory xmlof = XMLOutputFactory.newFactory();
			XMLStreamWriter xmlsw = xmlof.createXMLStreamWriter(new FileWriter("jaxp-stax-out.xml"));
			xmlsw.writeStartDocument();
			xmlsw.setPrefix("foo", "http://www.example.com/foo");
			xmlsw.writeStartElement("http://www.example.com/foo", "html");
			xmlsw.writeNamespace("foo", "http://www.example.com/foo");
			xmlsw.writeNamespace("bar", "http://www.example.com/bar");
			xmlsw.writeDefaultNamespace("http://www.example.com/default");
			xmlsw.writeStartElement("http://www.example.com/foo", "head");
			xmlsw.writeStartElement("http://www.example.com/foo", "title");
			xmlsw.writeCharacters("Recipe");
			xmlsw.writeEndElement();
			xmlsw.writeEndElement();
			xmlsw.writeStartElement("http://www.example.com/foo", "body");
			xmlsw.setPrefix("bar", "http://www.example.com/bar");
			xmlsw.writeStartElement("http://www.example.com/bar", "recipe");
			xmlsw.writeStartElement("http://www.example.com/bar", "title");
			xmlsw.writeCharacters("Grilled Cheese Sandwich");
			xmlsw.writeEndElement();
			xmlsw.writeStartElement("http://www.example.com/bar", "ingredients");
			xmlsw.setPrefix("h", "http://www.example.com/foo");
			xmlsw.writeStartElement("http://www.example.com/foo", "ul");
			xmlsw.writeStartElement("http://www.example.com/foo", "li");
			xmlsw.setPrefix("r", "http://www.example.com/bar");
			xmlsw.writeStartElement("http://www.example.com/bar", "ingredient");
			xmlsw.writeAttribute("qty", "2");
			xmlsw.writeCharacters("bread slice");
			xmlsw.writeEndElement();
			xmlsw.setPrefix("h", "http://www.example.com/foo");
			xmlsw.writeEndElement();
			xmlsw.writeEndElement();
			xmlsw.setPrefix("r", "http://www.example.com/bar");
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
