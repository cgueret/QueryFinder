/**
 * 
 */
package nl.vu.queryfinder.model;

import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class StructuredQuery extends HashSet<QueryPattern> {
	static final Logger logger = LoggerFactory.getLogger(StructuredQuery.class);
	/** For serialisation */
	private static final long serialVersionUID = -718582275488380974L;
	private String title;

	/**
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return
	 */
	public String getTitle() {
		return title;
	}
}
