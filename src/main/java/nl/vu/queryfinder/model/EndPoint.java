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

	/**
	 * @param uri
	 * @param defaultGraph
	 */
	public EndPoint(String uri, String defaultGraph) {
		this.uri = uri;
		this.defaultGraph = defaultGraph;
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

}
