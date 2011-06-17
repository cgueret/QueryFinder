/**
 * 
 */
package nl.vu.queryfinder.model;


/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class EndPoint {
	private final String uri;
	private final String defaultGraph;
	private final EndPointType type;
	public enum EndPointType {
		VIRTUOSO, OWLIM
	}
	
	/**
	 * @param uri
	 * @param defaultGraph
	 */
	public EndPoint(String uri, String defaultGraph, EndPointType type) {
		this.uri = uri;
		this.defaultGraph = defaultGraph;
		this.type = type;
	}

	/**
	 * @return
	 */
	public String getDefaultGraph() {
		return defaultGraph;
	}

	/**
	 * @return
	 */
	public String getURI() {
		return uri;
	}

	/**
	 * @return the type
	 */
	public EndPointType getType() {
		return type;
	}

}
