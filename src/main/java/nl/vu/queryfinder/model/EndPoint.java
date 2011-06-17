/**
 * 
 */
package nl.vu.queryfinder.model;


/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class EndPoint {
	public enum EndPointType {
		OWLIM, VIRTUOSO
	}
	private final String defaultGraph;
	private final EndPointType type;

	private final String uri;

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
	 * @return the type
	 */
	public EndPointType getType() {
		return type;
	}

	/**
	 * @return
	 */
	public String getURI() {
		return uri;
	}

}
