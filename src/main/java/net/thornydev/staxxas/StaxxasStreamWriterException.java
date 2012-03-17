package net.thornydev.staxxas;

/**
 * <p>
 * Using the criteria outlined by Josh Bloch in <i>Effective Java</i>, the JAXP
 * XMLStreamException through by an XMLStreamWriter should <strong>not</strong> be
 * a checked exception.
 * </p> 
 * <p>
 * The Staxxas library wraps all these and rethrows its own StaxxasStreamWriterException
 * which is an unchecked exception. 
 * </p> 
 * @author midpeter444
 */
public class StaxxasStreamWriterException extends RuntimeException {
	private static final long serialVersionUID = -6621587869107563570L;

	/**
	 * Reference to the StaxxasWriter method were the exception occurred
	 */
	private final String staxWriterMethod;
	/**
	 * Reference to the JAXP StAX XMLStreamWriter method were the exception occurred
	 */
    private final String xmlStreamWriterMethod;

    /**
     * This the only constructor and it requires you to specify the Staxxas wrapper method
     * and actual JAXP StAX method where the StAX occurred.
     * 
     * @param staxWriterMethod the StaxxasWriter method were the exception occurred
     * @param xmlStreamWriterMethod the JAXP StAX XMLStreamWriter method were the exception occurred
     * @param cause javax.xml.stream.XMLStreamException from the JAXP StAX codebase
     */
    public StaxxasStreamWriterException(String staxWriterMethod, 
                                     String xmlStreamWriterMethod,
                                     Throwable cause) {
      super(String.format("Exception StaxxasStreamWriter#%s when calling XMLStreamWriter#%s",
                          staxWriterMethod, xmlStreamWriterMethod), cause);
      this.staxWriterMethod = staxWriterMethod;
      this.xmlStreamWriterMethod = xmlStreamWriterMethod;
    }

    /**
     * @return the error message for this exception
     */
    @Override
    public String getMessage() {
      return String.format("Exception StaxxasStreamWriter#%s when calling XMLStreamWriter#%s",
                           staxWriterMethod, xmlStreamWriterMethod);
    }

    /**
     * @return the name of the method of the StaxxasStreamWriter when the exception 
     * occurred
     */
    public String getStaxxasWriterMethod() {
      return staxWriterMethod;
    }

    /**
     * @return the name of the method of the JAXP StAX XMLStreamWriter that was called
     * when the exception occurred
     */
    public String getXmlStreamWriterMethod() {
      return xmlStreamWriterMethod;
    }

    // TODO: do I need to override toString as well?
}
