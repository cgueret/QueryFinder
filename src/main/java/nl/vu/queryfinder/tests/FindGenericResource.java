package nl.vu.queryfinder.tests;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

import javax.swing.plaf.basic.BasicGraphicsUtils;

import nl.vu.queryfinder.model.EndPoint;
import nl.vu.queryfinder.model.EndPoint.EndPointType;
import nl.vu.queryfinder.services.impl.SPARQLMatcher;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.Template;

public class FindGenericResource {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		FindGenericResource test = new FindGenericResource();
		test.go("http://dbpedia.org/ontology/genre", "hip hop");
		//test.go("http://dbpedia.org/ontology/birthPlace", "New York");
	}

	/**
	 * @param keyword
	 * @throws IOException
	 */
	private void go(String rel, String keyword) throws IOException {
		EndPoint endPoint = new EndPoint("http://lod.openlinksw.com/sparql", "http://dbpedia.org", EndPointType.VIRTUOSO);
		SPARQLMatcher matcher = new SPARQLMatcher(endPoint);
		Triple context = Triple.create(Node.createAnon(), Node.createURI(rel), matcher.getVariable());
		Set<Node> resources = matcher.getResources(keyword, context);
		Model model = ModelFactory.createDefaultModel();
		for (Node s : resources) {
			System.out.println(s + " " + model.size());
			OutputStream out = new FileOutputStream("relations.n3");
			model.write(out, "N3");
			out.close();
			for (Node o : resources) {
				if (s.equals(o))
					continue;

				Node v = Node.createVariable("v");
				Triple t = Triple.create(s, v, o);
				Query query = QueryFactory.create();
				query.setQueryConstructType();
				BasicPattern bgp = new BasicPattern();
				bgp.add(t);
				query.setConstructTemplate(new Template(bgp));
				ElementGroup group = new ElementGroup();
				group.addTriplePattern(t);
				query.setQueryPattern(group);

				QueryEngineHTTP qExec = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(endPoint.getURI(), query);
				qExec.addDefaultGraph(endPoint.getDefaultGraph());
				qExec.execConstruct(model);
			}
		}
	}
}
