package nl.vu.queryfinder.util;

import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class PaginatedQueryExec {
	private final static int PAGE_SIZE = 100;

	/**
	 * @param service
	 * @param query
	 * @param var
	 * @return
	 */
	public static Set<Node> process(String service, Query query, Node var) {
		query.setLimit(PAGE_SIZE);
		query.setOffset(0);

		Set<Node> results = new HashSet<Node>();
		boolean morePages = true;
		while (morePages) {
			QueryExecution queryExec = QueryExecutionFactory.sparqlService(service, query);
			ResultSet bindings = queryExec.execSelect();

			long count = 0;
			if (bindings.hasNext())
				for (QuerySolution binding = bindings.next(); bindings.hasNext(); binding = bindings.next(), count++)
					results.add(binding.get(var.getName()).asNode());

			morePages = (count == PAGE_SIZE - 1);
			query.setOffset(query.getOffset() + PAGE_SIZE);
		}

		return results;
	}
}
