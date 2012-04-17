/**
 * 
 */
package nl.vu.queryfinder.vocabulary;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class QF {
	/** http://example.org# */
	public static final String NAMESPACE = "http://example.org#";

	/** http://example.org#Query */
	public final static URI QUERY;

	/** http://example.org#statement */
	public final static URI STATEMENT;

	static {
		ValueFactory factory = ValueFactoryImpl.getInstance();
		QUERY = factory.createURI(QF.NAMESPACE, "Query");
		STATEMENT = factory.createURI(QF.NAMESPACE, "statement");
	}
}
