package nl.vu.queryfinder.services.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import nl.vu.queryfinder.model.EndPoint;
import nl.vu.queryfinder.model.EndPoint.EndPointType;
import nl.vu.queryfinder.services.ClassMatcher;
import nl.vu.queryfinder.services.PropertyMatcher;
import nl.vu.queryfinder.services.ResourceMatcher;
import nl.vu.queryfinder.util.PaginatedQueryExec;

public class SPARQLMatcher implements ClassMatcher, ResourceMatcher, PropertyMatcher {
	static final Logger logger = LoggerFactory.getLogger(SPARQLMatcher.class);
	// static final Node[] propertyTypes = { RDF.Property.asNode(),
	// OWL.DatatypeProperty.asNode(),
	// OWL.ObjectProperty.asNode() };
	static final Node[] propertyTypes = { OWL.ObjectProperty.asNode() };
	// The end point to query
	final EndPoint endPoint;

	public static final Node VAR = Node.createVariable("r");

	/**
	 * @param endPoint
	 */
	public SPARQLMatcher(EndPoint endPoint) {
		this.endPoint = endPoint;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.queryfinder.services.ClassMatcher#getClasses(java.lang.String)
	 */
	public Set<Node> getClasses(String keyword) {
		Node label = Node.createVariable("l");
		Query query = QueryFactory.create();
		query.setQuerySelectType();
		query.setDistinct(true);
		query.addResultVar(VAR);
		ElementGroup group = new ElementGroup();
		group.addTriplePattern(new Triple(Node.createAnon(), RDF.type.asNode(), VAR));
		group.addTriplePattern(new Triple(VAR, RDF.type.asNode(), OWL.Class.asNode()));
		if (endPoint.getType().equals(EndPointType.VIRTUOSO)) {
			//String text = StringUtils.join(keyword.split(" "), " and ");
			String text = "'" + keyword + "'";
			group.addTriplePattern(new Triple(VAR, RDFS.label.asNode(), label));
			group.addTriplePattern(new Triple(label, Node.createURI("bif:contains"), Node.createLiteral(text)));
		} else if (endPoint.getType().equals(EndPointType.OWLIM))
			group.addTriplePattern(new Triple(VAR, Node.createURI("http://www.ontotext.com/owlim/lucene#"), Node
					.createLiteral(keyword)));

		query.setQueryPattern(group);

		Set<Node> results = PaginatedQueryExec.process(endPoint, query, VAR);
		logger.info(String.format("[class] \"%s\" -> %d", keyword, results.size()));
		return results;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.queryfinder.services.PropertyMatcher#getProperties(java.lang.String
	 * )
	 */
	public Set<Node> getProperties(String keyword) {
		Node var = Node.createVariable("r");
		Node label = Node.createVariable("l");
		// Node o = Node.createVariable("o");
		Query query = QueryFactory.create();
		query.setQuerySelectType();
		query.setDistinct(true);
		query.addResultVar(var);

		Set<Node> results = new HashSet<Node>();
		for (Node propertyType : propertyTypes) {
			ElementGroup group = new ElementGroup();
			// group.addTriplePattern(new Triple(Node.createAnon(), var, o));
			group.addTriplePattern(new Triple(var, RDF.type.asNode(), propertyType));
			if (endPoint.getType().equals(EndPointType.VIRTUOSO)) {
				//String text = StringUtils.join(keyword.split(" "), " and ");
				String text = "'" + keyword + "'";
				group.addTriplePattern(new Triple(var, RDFS.label.asNode(), label));
				group.addTriplePattern(new Triple(label, Node.createURI("bif:contains"), Node.createLiteral(text)));
			} else if (endPoint.getType().equals(EndPointType.OWLIM))
				group.addTriplePattern(new Triple(var, Node.createURI("http://www.ontotext.com/owlim/lucene#"), Node
						.createLiteral(keyword)));
			// Restrict the range
			// group.addElementFilter(new ElementFilter(new E_IsURI(new
			// ExprVar(o))));
			query.setQueryPattern(group);
			results.addAll(PaginatedQueryExec.process(endPoint, query, var));
		}

		logger.info(String.format("[property] \"%s\" -> %d", keyword, results.size()));
		return results;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.queryfinder.services.ResourceMatcher#getResources(java.lang.String,
	 * nl.vu.queryfinder.model.context.Context)
	 */
	public Set<Node> getResources(String keyword, Triple context) {
		Node var = Node.createVariable("r");
		Node label = Node.createVariable("l");
		Query query = QueryFactory.create();
		query.setQuerySelectType();
		query.setDistinct(true);
		query.addResultVar(var);
		ElementGroup group = new ElementGroup();
		group.addTriplePattern(new Triple(var, RDF.type.asNode(), Node.createAnon()));
		if (context != null)
			group.addTriplePattern(context);
		if (endPoint.getType().equals(EndPointType.VIRTUOSO)) {
			//String text = StringUtils.join(keyword.split(" "), " and ");
			String text = "'" + keyword + "'";
			group.addTriplePattern(new Triple(var, RDFS.label.asNode(), label));
			group.addTriplePattern(new Triple(label, Node.createURI("bif:contains"), Node.createLiteral(text)));
		} else if (endPoint.getType().equals(EndPointType.OWLIM))
			group.addTriplePattern(new Triple(var, Node.createURI("http://www.ontotext.com/owlim/lucene#"), Node
					.createLiteral(keyword)));
		query.setQueryPattern(group);

		logger.info(query.serialize());
		
		Set<Node> results = PaginatedQueryExec.process(endPoint, query, var);
		logger.info(String.format("[resource] \"%s\" -> %d", keyword, results.size()));
		return results;
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		EndPoint endPoint = new EndPoint("http://factforge.net/sparql", null, EndPointType.OWLIM);
		// EndPoint endPoint = new EndPoint("http://dbpedia.org/sparql",
		// "http://dbpedia.org", EndPointType.VIRTUOSO);

		SPARQLMatcher me = new SPARQLMatcher(endPoint);
		logger.info("artist      : " + me.getClasses("artist").size());
		logger.info("field       : " + me.getProperties("field").size());
		logger.info("birth       : " + me.getProperties("birth").size());
		logger.info("amsterdam   : " + me.getResources("amsterdam", null).size());
		logger.info("Netherlands : " + me.getResources("Netherlands", null).size());
	}

	public Node getVariable() {
		return VAR;
	}

}
