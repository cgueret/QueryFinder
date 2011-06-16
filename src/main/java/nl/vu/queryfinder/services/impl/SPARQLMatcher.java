package nl.vu.queryfinder.services.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

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
	final EndPoint endPoint;

	/**
	 * @param endPoint
	 */
	public SPARQLMatcher(EndPoint endPoint) {
		this.endPoint = endPoint;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.queryfinder.services.PropertyMatcher#getProperties(java.lang.String
	 * )
	 */
	public Set<Node> getProperties(String keyword) {
		logger.debug(String.format("Get properties for \"%s\"", keyword));
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
			group.addTriplePattern(new Triple(var, RDFS.label.asNode(), label));
			if (endPoint.getType().equals(EndPointType.VIRTUOSO))
				group.addTriplePattern(new Triple(label, Node.createURI("bif:contains"), Node.createLiteral(keyword)));
			// Restrict the range
			// group.addElementFilter(new ElementFilter(new E_IsURI(new
			// ExprVar(o))));
			query.setQueryPattern(group);
			results.addAll(PaginatedQueryExec.process(endPoint, query, var));
		}

		return results;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.queryfinder.services.ResourceMatcher#getResources(java.lang.String)
	 */
	public Set<Node> getResources(String keyword) {
		logger.debug(String.format("Get resources for \"%s\"", keyword));
		Node var = Node.createVariable("r");
		Node label = Node.createVariable("l");
		Query query = QueryFactory.create();
		query.setQuerySelectType();
		query.setDistinct(true);
		query.addResultVar(var);
		ElementGroup group = new ElementGroup();
		group.addTriplePattern(new Triple(var, RDF.type.asNode(), Node.createAnon()));
		group.addTriplePattern(new Triple(var, RDFS.label.asNode(), label));
		if (endPoint.getType().equals(EndPointType.VIRTUOSO))
			group.addTriplePattern(new Triple(label, Node.createURI("bif:contains"), Node.createLiteral(keyword)));
		query.setQueryPattern(group);

		Set<Node> results = PaginatedQueryExec.process(endPoint, query, var);
		return results;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.queryfinder.services.ClassMatcher#getClasses(java.lang.String)
	 */
	public Set<Node> getClasses(String keyword) {
		logger.debug(String.format("Get classes for \"%s\"", keyword));
		Node var = Node.createVariable("r");
		Node label = Node.createVariable("l");
		Query query = QueryFactory.create();
		query.setQuerySelectType();
		query.setDistinct(true);
		query.addResultVar(var);
		ElementGroup group = new ElementGroup();
		group.addTriplePattern(new Triple(Node.createAnon(), RDF.type.asNode(), var));
		group.addTriplePattern(new Triple(var, RDF.type.asNode(), OWL.Class.asNode()));
		group.addTriplePattern(new Triple(var, RDFS.label.asNode(), label));
		if (endPoint.getType().equals(EndPointType.VIRTUOSO))
			group.addTriplePattern(new Triple(label, Node.createURI("bif:contains"), Node.createLiteral(keyword)));
		query.setQueryPattern(group);

		Set<Node> results = PaginatedQueryExec.process(endPoint, query, var);
		return results;
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		EndPoint endPoint = new EndPoint("http://dbpedia.org/sparql", "http://dbpedia.org", EndPointType.VIRTUOSO);
		SPARQLMatcher me = new SPARQLMatcher(endPoint);
		logger.info("arsist      : " + me.getClasses("artist").size());
		logger.info("field       : " + me.getProperties("field").size());
		logger.info("birth       : " + me.getProperties("birth").size());
		logger.info("amsterdam   : " + me.getResources("amsterdam").size());
		logger.info("Netherlands : " + me.getResources("Netherlands").size());
	}

}
