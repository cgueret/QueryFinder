package nl.vu.queryfinder.util;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import nl.vu.queryfinder.model.EndPoint;
import nl.vu.queryfinder.model.EndPoint.EndPointType;

import org.openrdf.model.Value;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sparql.SPARQLRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AskQueryExecutor {
	protected static final Logger logger = LoggerFactory.getLogger(SelectQueryExecutor.class);
	SPARQLRepository repository;

	/**
	 * @param endPoint
	 * @throws RepositoryException
	 */
	public AskQueryExecutor(EndPoint endPoint) throws RepositoryException {
		repository = new SPARQLRepository(endPoint.getURI().toString());
		repository.initialize();
	}

	/**
	 * @param pattern
	 * @return
	 */
	public boolean process(String pattern) {
		boolean result = false;
		
		try {
			RepositoryConnection conn = repository.getConnection();
			String query = "ASK {" + pattern + "}";
			BooleanQuery booleanQuery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, query);
			result = booleanQuery.evaluate();
			conn.commit();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * @param args
	 * @throws URISyntaxException
	 * @throws RepositoryException
	 */
	public static void main(String[] args) throws URISyntaxException, RepositoryException {
		String query = "<http://dbpedia.org/resource/Amsterdam> ?p ?o";
		EndPoint endPoint = new EndPoint("http://dbpedia.org/sparql", "http://dbpedia.org", EndPointType.VIRTUOSO);
		AskQueryExecutor exec = new AskQueryExecutor(endPoint);
		boolean r = exec.process(query);
		logger.info(""+r);
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
