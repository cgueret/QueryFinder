package nl.vu.queryfinder.util;

import java.util.HashSet;
import java.util.Set;

import nl.vu.queryfinder.model.EndPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;

public class PaginatedQueryExec {
	static final Logger logger = LoggerFactory.getLogger(PaginatedQueryExec.class);
	private final static int PAGE_SIZE = 100;

	/**
	 * @param service
	 * @param query
	 * @param var
	 * @return
	 */
	public static Set<Node> process(EndPoint endPoint, Query query, Node var) {
		query.setLimit(PAGE_SIZE);
		query.setOffset(0);

		Set<Node> results = new HashSet<Node>();
		boolean morePages = true;
		while (morePages) {
			long count = 0;
			try {
				QueryEngineHTTPClient queryExec = new QueryEngineHTTPClient(endPoint.getURI(), query);
				queryExec.addDefaultGraph(endPoint.getDefaultGraph());
				// queryExec.addParam("timeout", "2000");
				ResultSet bindings = queryExec.execSelect();
				if (bindings != null)
					while (bindings.hasNext())
						results.add(bindings.next().get(var.getName()).asNode());
				// for (QuerySolution binding = bindings.next();
				// bindings.hasNext(); binding = bindings.next(), count++)
				queryExec.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			morePages = (count == PAGE_SIZE - 1);
			query.setOffset(query.getOffset() + PAGE_SIZE);
		}

		//logger.info(results.size() + " results for ");
		//logger.info(query.serialize());
		return results;
	}
}
