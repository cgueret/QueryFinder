package nl.vu.queryfinder.util;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import nl.erdf.model.EndPoint;
import nl.erdf.model.EndPoint.EndPointType;

import org.openrdf.model.Value;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sparql.SPARQLRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaginatedQueryExec {
	protected static final Logger logger = LoggerFactory.getLogger(PaginatedQueryExec.class);
	private final static int PAGE_SIZE = 500;
	private final static int HARD_LIMIT = 8;
	SPARQLRepository repository;

	/**
	 * @param endPoint
	 * @throws RepositoryException
	 */
	public PaginatedQueryExec(EndPoint endPoint) throws RepositoryException {
		repository = new SPARQLRepository(endPoint.getURI().toString());
		repository.initialize();
	}

	/**
	 * @param service
	 * @param query
	 * @param varName
	 * @return
	 * @throws RepositoryException
	 */
	public List<Value> process(String query, String varName) {
		List<Value> results = new ArrayList<Value>();

		try {

			boolean morePages = true;
			int limit = PAGE_SIZE;
			int offset = 0;

			RepositoryConnection conn = repository.getConnection();
			while (morePages && results.size() < HARD_LIMIT) {
				long count = 0;
				String queryPage = query;
				queryPage += " LIMIT " + limit + " OFFSET " + offset;

				TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryPage);
				TupleQueryResult res = tupleQuery.evaluate();
				while (res.hasNext() && results.size() < HARD_LIMIT) {
					results.add(res.next().getValue(varName));
					count++;
				}

				morePages = (count == PAGE_SIZE);
				offset += PAGE_SIZE;
			}
			conn.commit();
			conn.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return results;
	}

	/**
	 * @param args
	 * @throws URISyntaxException
	 * @throws RepositoryException
	 */
	public static void main(String[] args) throws URISyntaxException, RepositoryException {
		String query = "Select distinct ?o where {<http://dbpedia.org/resource/Amsterdam> ?p ?o}";
		EndPoint endPoint = new EndPoint("http://dbpedia.org/sparql", null, EndPointType.VIRTUOSO);
		PaginatedQueryExec exec = new PaginatedQueryExec(endPoint);
		List<Value> r = exec.process(query, "o");
		logger.info(r.toString());
		exec.shutDown();
		logger.info("ok");
		exec = null;
	}

	/**
	 * 
	 */
	private void shutDown() {
		try {
			repository.shutDown();
		} catch (RepositoryException e) {
			e.printStackTrace();
		}

	}
}
