package net.thornydev.staxxas;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class StaxxasStreamWriterTest {

	StringWriter writer;
	StaxxasStreamWriter sx;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		writer = new StringWriter(); 
		XMLOutputFactory xof = XMLOutputFactory.newFactory();
	    sx = new StaxxasStreamWriter( xof.createXMLStreamWriter(writer) );
	}

	@After
	public void tearDown() throws Exception {
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
	public void testStartAndEndElementCompound() {
		sx.startDoc();
		sx.startElement("foo");
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
	
		String reBody = "\\s*<foo>\\s*<bar>\\s*<baz>\\s*</baz>\\s*<quux>\\s*</quux>\\s*</bar>\\s*</foo>$";
		assertTrue(out, Pattern.compile(reBody).matcher(out).find());	
	}

//	<beans xmlns="http://www.springframework.org/schema/beans"
//		    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
//		    xmlns:context="http://www.springframework.org/schema/context"
//		    xsi:schemaLocation="http://www.springframework.org/schema/beans
//		              http://www.springframework.org/schema/beans/spring-beans-2.5.xsd
//		              http://www.springframework.org/schema/context
//		              http://www.springframework.org/schema/context/spring-context-2.5.xsd">

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
	public void testStartAndEndElementCompoundWithPrimaryNamespaces() {
		String defaultNsUri = "http://www.quux.org/quux";
		sx.mapNamespaceToUri("xsi", "http://www.w3.org/2001/XMLSchema-instance");
	    sx.setDefaultNamespace(defaultNsUri);
	    sx.startDoc();
	    sx.startRootElement("foo");
	    sx.writeAttribute("xsi:schemaLocation", "http://www.springframework.org/schema/beans");
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
