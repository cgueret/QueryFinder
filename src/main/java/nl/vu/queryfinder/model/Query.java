/**
 * 
 */
package nl.vu.queryfinder.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nl.vu.queryfinder.vocabulary.QF;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.turtle.TurtleWriterFactory;
import org.openrdf.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class Query {
	// Logger
	private final Logger logger = LoggerFactory.getLogger(Query.class.getName());

	// The Sesame repository to store all the triples in memory
	private final SailRepository repository = new SailRepository(new MemoryStore());

	/**
	 * @throws RepositoryException
	 */
	public Query() {
		SailRepositoryConnection connection = null;

		try {
			repository.initialize();
			connection = repository.getConnection();

			// Add some default information
			Resource queryResource = connection.getValueFactory().createBNode();
			connection.add(queryResource, RDF.TYPE, QF.QUERY);
			connection.add(queryResource, RDFS.COMMENT, connection.getValueFactory().createLiteral("no description"));

			// Register namespaces
			connection.setNamespace("qf", QF.NAMESPACE);
			connection.setNamespace("rdf", RDF.NAMESPACE);
			connection.setNamespace("rdfs", RDFS.NAMESPACE);

			// Commit
			connection.commit();
		} catch (RepositoryException e) {
			e.printStackTrace();
		} finally {
			try {
				if (connection != null)
					connection.close();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @return
	 */
	public String getAsString() {
		StringBuffer buffer = new StringBuffer();
		SailRepositoryConnection connection = null;
		try {
			connection = repository.getConnection();
			for (Statement st : connection.getStatements(null, null, null, true).asList())
				buffer.append("\n").append(st.toString());
		} catch (RepositoryException e) {
			e.printStackTrace();
		} finally {
			try {
				if (connection != null)
					connection.close();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
		}

		return buffer.toString();
	}

	/**
	 * @param fileName
	 * @throws IOException
	 * @throws RepositoryException
	 * @throws RDFParseException
	 */
	public void loadFrom(String fileName) throws RDFParseException, RepositoryException, IOException {
		logger.info("Load from " + fileName);
		SailRepositoryConnection connection = repository.getConnection();
		connection.clear();
		connection.add(new File(fileName), "http://example.com/", RDFFormat.TURTLE);
		connection.close();
	}

	/**
	 * @param fileName
	 * @throws RDFHandlerException
	 * @throws RepositoryException
	 * @throws FileNotFoundException
	 */
	public void saveTo(String fileName) throws RepositoryException, RDFHandlerException, FileNotFoundException {
		logger.info("Save to " + fileName);
		SailRepositoryConnection connection = repository.getConnection();
		TurtleWriterFactory f = new TurtleWriterFactory();
		RDFWriter writer = f.getWriter(new FileOutputStream(fileName));
		connection.export(writer);
		connection.close();
	}

	/**
	 * @return
	 */
	public Resource getQueryResource() {
		SailRepositoryConnection connection = null;
		try {
			// Connect
			connection = repository.getConnection();

			// Get the name of the query
			String queryStr = "SELECT ?q WHERE {?q a <" + QF.QUERY + ">. }";
			TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
			TupleQueryResult result = tupleQuery.evaluate();

			// Return the first result
			return (Resource) result.next().getValue("q");
		} catch (RepositoryException e) {
			e.printStackTrace();
		} catch (MalformedQueryException e) {
			e.printStackTrace();
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
		} finally {
			try {
				if (connection != null)
					connection.close();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	/**
	 * @return
	 */
	public String getDescription() {
		// Get the name of the query
		Resource queryResource = getQueryResource();

		SailRepositoryConnection connection = null;
		try {
			connection = repository.getConnection();
			List<Statement> s = connection.getStatements(queryResource, RDFS.COMMENT, null, true).asList();
			return s.get(0).getObject().stringValue();
		} catch (RepositoryException e) {
			e.printStackTrace();
		} finally {
			try {
				if (connection != null)
					connection.close();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
		}

		return "";
	}

	/**
	 * @param description
	 */
	public void setDescription(String description) {
		// Get the name of the query
		Resource queryResource = getQueryResource();

		SailRepositoryConnection connection = null;
		try {
			// Connect
			connection = repository.getConnection();

			// Update the description
			connection.remove(queryResource, RDFS.COMMENT, null);
			connection.add(queryResource, RDFS.COMMENT, connection.getValueFactory().createLiteral(description));

			// Commit
			connection.commit();
		} catch (RepositoryException e) {
			e.printStackTrace();
		} finally {
			try {
				if (connection != null)
					connection.close();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @return
	 */
	public List<Quad> getTriples() {
		// Prepare result list
		List<Quad> triples = new ArrayList<Quad>();

		SailRepositoryConnection connection = null;
		try {
			// Connect
			connection = repository.getConnection();

			// Get the list of statements
			String queryStr = "SELECT DISTINCT ?q ?st ?s ?p ?o ?c WHERE {";
			queryStr += "?q <" + RDF.TYPE + "> <" + QF.QUERY + ">.";
			queryStr += "?q <" + QF.STATEMENT + "> ?st.";
			queryStr += "?st <" + RDF.SUBJECT + "> ?s. ";
			queryStr += "?st <" + RDF.PREDICATE + "> ?p. ";
			queryStr += "?st <" + RDF.OBJECT + "> ?o. ";
			queryStr += "?st <" + RDFS.LABEL + "> ?c. }";
			TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
			TupleQueryResult results = tupleQuery.evaluate();
			while (results.hasNext()) {
				BindingSet result = results.next();
				Quad t = new Quad(result.getValue("s"), result.getValue("p"), result.getValue("o"),
						result.getValue("c"));
				triples.add(t);
			}
		} catch (RepositoryException e) {
			e.printStackTrace();
		} catch (MalformedQueryException e) {
			e.printStackTrace();
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
		} finally {
			try {
				if (connection != null)
					connection.close();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
		}

		return triples;
	}

	/**
	 * @param quad
	 */
	public void addQuad(final Quad quad) {
		// Get the name of the query
		Resource queryResource = getQueryResource();

		SailRepositoryConnection connection = null;
		try {
			// Connect
			connection = repository.getConnection();

			// Connect the triple to the query
			Resource stmtResource = connection.getValueFactory().createBNode();
			connection.add(queryResource, QF.STATEMENT, stmtResource);

			// Reify the triple
			connection.add(stmtResource, RDF.TYPE, RDF.STATEMENT);
			connection.add(stmtResource, RDF.SUBJECT, quad.getSubject());
			connection.add(stmtResource, RDF.PREDICATE, quad.getPredicate());
			connection.add(stmtResource, RDF.OBJECT, quad.getObject());
			connection.add(stmtResource, RDFS.LABEL, quad.getContext());

			// Commit
			connection.commit();
		} catch (RepositoryException e) {
			e.printStackTrace();
		} finally {
			try {
				if (connection != null)
					connection.close();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
		}
	}
}
