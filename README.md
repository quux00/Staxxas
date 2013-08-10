## Staxxas StAX XML Document Writer

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
Create it only on the filesystem:
`mvn javadoc:javadoc`

Create it on the fileystem and package it up as a jar:
`mvn javadoc:jar`

<hr/>

### Rationale and How-to

The JAXP XMLStreamWriter is a somewhat clunky to use XML writing library. This **Staxxas** library provides a facade that wraps the JAXP XMLStreamWriter. All the writing to the underlying output source or stream is still done by the XMLStreamWriter object that is passed into the constructor of this class.

The Staxxas API simplifies the XMLStreamWriter API and wraps methods so that the API user does not have to handle the checked XMLStreamException that is thrown by the XMLStreamWriter "write" methods.

In particular, this facade provides three primary improvements over the native XMLStreamWriter API:

1. The "write" method names are less verbose, you can do method call chaining and in some cases call fewer methods to get the document written
2. Only unchecked exceptions are thrown, which wrap any checked exceptions thrown by the underlying library
3. Namespace handling is simplified


##### Less verbosity, method call chaining, fewer methods calls.

To help decrease the verbosity of the XMLStreamWriter API, any method that starts with "write", such as writeEmptyElement, has been shortened by removing the "write" prefix, such as emptyElement. Those name-shortened methods are just wrappers around the longer methods of the XMLStreamWriter object.

Another aspect of the verbosity of the StAX API is having to pass in the full URI of the namespace whenever you need to associate a namespace with an XML element. This requirement has been removed - see the Simplified namespace handling section below.

In the case of finishing off the XML doc writing, the XMLStreamWriter API writer requires you to call three methods: XMLStreamWriter.writeEndDocument(), followed by XMLStreamWriter.flush(), followed by XMLStreamWriter.close(). The Staxxas API only requires that endDoc() be called and it calls flush() and close() on the underlying writer.

##### Only unchecked exceptions

In Effective Java, 2nd edition, Josh Bloch argues that checked exceptions are only appropriate when two conditions hold:

1. The exception cannot be prevented by proper use of the API
2. The user of the API can take some useful action to recover when the exception occurs.

All of the "write" methods of XMLStreamWriter throw a checked exception. Those exceptions can arise even if the API is used properly, so Bloch's first condition applies, but in that case it likely to be a problem with the file system or stream to which the XML is being written, which a Java programmer has little control over. Thus, the second condition does not apply and the exceptions thrown here should be unchecked.

In the tradition of the Spring framework, all these checked exceptions have been wrapped and are rethrown as an unchecked exception of type StaxxasStreamWriterException.

##### Simplified namespace handling

Lastly, the namepace handling in the XMLStreamWriter is rather ghastly and unnecessarily verbose. In the Staxxas API, you basically only have to map all your namespace prefixes to their corresponding namespace APIs at the beginning and then call setCurrentNamespace(prefix) passing in the prefix that is current. All subsequent startElement() calls will use that namespace until setCurrentNamespace(prefix) is called with a different current namespace.

A default namespace can also be specified and any unprefixed XML elements will be associated with that default namespace. A unprefixed XML element can be set by calling setCurrentNamespace(null) or startElement("myEltName", null).

<hr/>

#### Full-fledged Example

Goal: create the following XML file.

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


**The XMLStreamWriter implementation to write the above XML document is shown below.**  Note the general verbosity, how the whole thing must be wrapped in a try/catch block and how the full namespace must be passed in to use prefixes to the elements.  Also, note that you must call flush and close on _both_ the `XMLStreamWriter` and the `java.io.Writer` objects.

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


**Here is Staxxas implementation by contrast:**

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
    
    stxs.startElement("item", null);  // unset currNamespace only for this node
    stxs.startElement("sku").characters("DEF456").endElement();
    stxs.startElement("description").characters("Kindle Fire").endElement();
    stxs.startElement("quantity").characters("22,200").endElement();
    
    stxs.endElement("item");  // use self documenting feature to label close tag 
    stxs.endElement("items");
    
    stxs.endElement("inventory");
    stxs.endDoc();


<hr/>

### License

Staxxas is open source released under the MIT License: http://opensource.org/licenses/MIT

