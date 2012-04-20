package nl.vu.queryfinder.services.impl;

import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.erdf.model.Directory;
import nl.erdf.model.EndPoint;
import nl.vu.queryfinder.model.Query;
import nl.vu.queryfinder.model.Triple;
import nl.vu.queryfinder.services.Service;
import nl.vu.queryfinder.util.SelectQueryExecutor;

public class AskFilter extends Service {
	// Logger
	private static final Logger logger = LoggerFactory.getLogger(AskFilter.class);

	// The end points to query
	private final Map<EndPoint, SelectQueryExecutor> executors = new HashMap<EndPoint, SelectQueryExecutor>();

	private final ValueFactory factory = new ValueFactoryImpl();

	/**
	 * @param directory
	 * @throws RepositoryException
	 */
	public AskFilter(Directory directory) throws RepositoryException {
		for (EndPoint endPoint : directory) {
			SelectQueryExecutor exec = new SelectQueryExecutor(endPoint);
			executors.put(endPoint, exec);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.queryfinder.services.Service#process(nl.vu.queryfinder.model.Query)
	 */
	@Override
	public Query process(Query inputQuery) {
		// Create the query
		Query outputQuery = new Query();

		// Copy the value of the description
		outputQuery.setDescription(inputQuery.getDescription());

		// Iterate over the triples
		for (Triple triple : inputQuery.getTriples()) {
			logger.info(triple.toString());
			if (isValid(triple))
				outputQuery.addTriple(triple);
		}
		return outputQuery;
	}

	/**
	 * @param triple
	 * @return
	 */
	private boolean isValid(Triple triple) {
		// Compose a triple
		Resource s = null;
		URI p = null;
		Value o = null;
		if (triple.getSubject().stringValue().startsWith("?"))
			s = factory.createBNode();
		else if (triple.getSubject() instanceof Resource)
			s = (Resource) triple.getSubject();
		if (triple.getPredicate().stringValue().startsWith("?"))
			p = (URI) factory.createBNode();
		else if (triple.getPredicate() instanceof Resource)
			p = (URI) triple.getPredicate();
		if (triple.getObject().stringValue().startsWith("?"))
			o = factory.createBNode();
		else
			o = triple.getObject();

		Statement stmt = new StatementImpl(s, p, o);
		logger.info(stmt.toString());

		// Easy case
		if (s == null || p == null || o == null)
			return false;

		return false;
	}
}
