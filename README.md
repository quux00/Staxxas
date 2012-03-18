## Staxxas StAX XML Document Writer

More details to come later ...



### Dependencies

* JDK 1.6
* JUnit 4
* Maven 2 or 3 if you want to build it using maven


### Build 

I built this as a Maven project, so all the standard commands apply.  For those unfamiliar with Maven, here's a quick how-to.

First, clone this repo:
`git clone git@github.com:midpeter444/Staxxas.git`. Then cd into the Staxxas directory and run one or more of the following maven commands.  If you haven't run them before, be aware that you need to be online and maven downloads _a lot_ of jars to do this.  These are maven dependencies, not Staxxas dependencies (except for JUnit 4).

#### Compile
`mvn compile`

#### Run unit tests
`mvn test`

#### Package into a jar
`mvn package`

#### Install the jar into your local maven repository
`mvn install`

BTW, `mvn install` does all of the above.

#### Create the javadoc
**Create it only on the filesystem:**:
`mvn javadoc:javadoc`

**Create it on the fileystem and package it up as a jar:**:
`mvn javadoc:jar`




### Rationale and How-to

<?xml version="1.0" ?>
<inventory xmlns="http://www.example.com/quux" 
           xmlns:foo="http://www.example.com/foo" 
           xmlns:bar="http://www.example.com/bar">
  <foo:site foo:isWarehouse="yes">Oklahoma City facility</foo:site>
  <bar:capacity foo:units="sq.ft.">200,000</bar:capacity>
  <items>
    <item>
      <foo:sku>ABC123</foo:sku>
      <foo:description>iPad3</foo:description>
      <foo:quantity>38,500</foo:quantity>
    </item>
    <item>
      <foo:sku>DEF456</foo:sku>
      <foo:description>Kindle Fire</foo:description>
      <foo:quantity>22,200</foo:quantity>
    </item>
  </items>
</inventory>

XMLStreamWriter implementation:

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


Staxxas implementation:

    StaxxasStreamWriter stxs = null;
    XMLOutputFactory xof = XMLOutputFactory.newFactory();
    FileWriter fw = null;
    try {
        fw = new FileWriter("staxxas-out.xml");
        XMLStreamWriter xmlsw = xof.createXMLStreamWriter(fw);
        stxs = new StaxxasStreamWriter(xmlsw);
        
    } catch (XMLStreamException e) {
        e.printStackTrace();
        throw e;
    } catch (IOException e) {
        e.printStackTrace();
        throw e;
    }

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
    
    stxs.setCurrentNamespace(null);
    stxs.startElement("item");      
    stxs.setCurrentNamespace("foo");
    stxs.startElement("sku").characters("DEF456").endElement();
    stxs.startElement("description").characters("Kindle Fire").endElement();
    stxs.startElement("quantity").characters("22,200").endElement();
    
    stxs.endElement("item");  // use self documenting feature to label close tag 
    stxs.endElement("items");
    
    stxs.endElement("inventory");
    stxs.endDoc();
    fw.close();
