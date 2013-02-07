/**
 * 
 */
package inat.util;

import inat.graph.FileUtils;

import java.io.File;

import javax.swing.JOptionPane;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cytoscape.Cytoscape;

/**
 * An XML configuration file.
 * 
 * @author B. Wanders
 */
public class XmlConfiguration {
	/**
	 * The configuration key for the verifyta path property.
	 */
	public static final String VERIFY_KEY = "/ANIMO/UppaalInvoker/verifyta";

	/**
	 * The configuration key for the verifyta path property (SMC version).
	 */
	public static final String VERIFY_SMC_KEY = "/ANIMO/UppaalInvoker/verifytaSMC";

	/**
	 * The configuration key for the tracer path property.
	 */
	public static final String TRACER_KEY = "/ANIMO/UppaalInvoker/tracer";
	
	/**
	 * Are we the developer? If so, enable more options and more debugging
	 */
	public static final String DEVELOPER_KEY = "/ANIMO/Developer";
	
	/**
	 * The document that backs this configuration.
	 */
	private final Document document;

	/**
	 * Constructor.
	 * 
	 * @param doc the configuration document
	 */
	public XmlConfiguration(Document doc) {
		this.document = doc;
	}
	
	/**
	 * Empty constructor: create the configuration file 
	 * @throws ParserConfigurationException 
	 * @throws TransformerException
	 */
	public XmlConfiguration(File configuration) throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		docBuilder = docFactory.newDocumentBuilder();
		document = docBuilder.newDocument();

		Element rootElement = document.createElement("ANIMO");
		document.appendChild(rootElement);
		 
		Element uppaalInvoker = document.createElement("UppaalInvoker");
		rootElement.appendChild(uppaalInvoker);
		 
		/*Element tracerLocation = document.createElement("tracer");
		JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Please, find and select the \"tracer\" tool.", "Tracer", JOptionPane.QUESTION_MESSAGE);
		File tracerLocationFile = new File(FileUtils.open(null, "Tracer Executable", Cytoscape.getDesktop()));
		if (tracerLocationFile != null) {
			tracerLocation.appendChild(document.createTextNode(tracerLocationFile.getAbsolutePath()));
		} else {
			tracerLocation.appendChild(document.createTextNode("\\uppaal-4.1.4\\bin-Win32\\tracer.exe"));
		}
		uppaalInvoker.appendChild(tracerLocation);*/
		
		Element verifytaLocation = document.createElement("verifyta");
		Element verifytaSMCLocation = document.createElement("verifytaSMC");
		JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Please, find and select the \"verifyta\" tool.\nIt is usually located in the \"bin\" directory of UPPAAL.", "Verifyta", JOptionPane.QUESTION_MESSAGE);
		File verifytaLocationFile = new File(FileUtils.open(null, "Verifyta Executable", Cytoscape.getDesktop()));
		if (verifytaLocationFile != null) {
			verifytaLocation.appendChild(document.createTextNode(verifytaLocationFile.getAbsolutePath()));
			verifytaSMCLocation.appendChild(document.createTextNode(verifytaLocationFile.getAbsolutePath()));
		} else {
			verifytaLocation.appendChild(document.createTextNode("\\uppaal-4.1.4\\bin-Win32\\verifyta.exe"));
			verifytaSMCLocation.appendChild(document.createTextNode("\\uppaal-4.1.4\\bin-Win32\\verifyta.exe"));
			
		}
		uppaalInvoker.appendChild(verifytaLocation);
		uppaalInvoker.appendChild(verifytaSMCLocation);
		
		Element developerNode = document.createElement("Developer");
		developerNode.appendChild(document.createTextNode(Boolean.FALSE.toString()));
		rootElement.appendChild(developerNode);
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		DOMSource source = new DOMSource(document);
		StreamResult result = new StreamResult(configuration);
		
		transformer.transform(source, result);
	}

	/**
	 * Evaluates the given XPath expression in the context of this document.
	 * 
	 * @param expression the expression to evaluate
	 * @param resultType the result type
	 * @return an object or {@code null}
	 */
	private Object evaluate(String expression, QName resultType) {
		try {
			AXPathExpression xpath = XmlEnvironment.hardcodedXPath(expression);
			return xpath.evaluate(this.document, resultType);
		} catch (XPathExpressionException e) {
			return null;
		}
	}

	/**
	 * Returns a node from this document.
	 * 
	 * @param xpath the selection expression
	 * @return a node
	 */
	public Node getNode(String xpath) {
		return (Node) this.evaluate(xpath, XPathConstants.NODE);
	}

	/**
	 * Returns a set of nodes from this document.
	 * 
	 * @param xpath the selection expression
	 * @return a set of nodes
	 */
	public ANodeList getNodes(String xpath) {
		return new ANodeList((NodeList) this.evaluate(xpath, XPathConstants.NODESET));
	}

	/**
	 * Returns a string from this document.
	 * 
	 * @param xpath the selection expression
	 * @return the string, or {@code null}
	 */
	public String get(String xpath) {
		return (String) this.evaluate(xpath, XPathConstants.STRING);
	}

	/**
	 * Returns a string from this document, or the default value if the string
	 * is not present.
	 * 
	 * @param xpath the selection expression
	 * @param defaultValue the default value
	 * @return the string from the document or the default value
	 */
	public String get(String xpath, String defaultValue) {
		if (this.has(xpath)) {
			return this.get(xpath);
		} else {
			return defaultValue;
		}
	}

	/**
	 * Checks to see whether this document matches the given expression.
	 * 
	 * @param xpath the expression to test
	 * @return {@code true} if the document matches, {@code false} otherwise
	 */
	public boolean has(String xpath) {
		return (Boolean) this.evaluate(xpath, XPathConstants.BOOLEAN);
	}
}
