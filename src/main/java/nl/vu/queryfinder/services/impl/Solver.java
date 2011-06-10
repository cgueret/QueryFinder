/**
 * 
 */
package nl.vu.queryfinder.services.impl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import nl.erdf.constraints.TripleBlockConstraint;
import nl.erdf.constraints.TripleConstraint;
import nl.erdf.datalayer.sparql.SPARQLRequest;
import nl.erdf.datalayer.sparql.orig.Directory;
import nl.erdf.datalayer.sparql.orig.SPARQLDataLayer;
import nl.erdf.model.Solution;
import nl.erdf.optimizer.Optimizer;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// FIXME : In the patterns set, every valid set of X patterns is equivalent.
// There is no consideration of the different branches of the graph the
// different pattern represent
public class Solver implements Observer {
	static final Logger logger = LoggerFactory.getLogger(Solver.class);
	private final Optimizer optimizer;
	private final SPARQLRequest request;
	private final Directory directory;

	public Solver() throws FileNotFoundException, IOException {
		// Create a directory
		directory = new Directory();
		directory.add("DBPedia", "http://lod.openlinksw.com/sparql");

		// Create a data layer
		SPARQLDataLayer datalayer = new SPARQLDataLayer(directory);

		// Create the request
		request = new SPARQLRequest(datalayer);
		BufferedReader reader = new BufferedReader(new FileReader("data/patterns-cleaned.list"));
		TripleBlockConstraint currentBlock = null;
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			// Ignore comments and blank lines
			if (line.equals("") || line.startsWith("#"))
				continue;

			// Detect groups
			if (line.startsWith("=")) {
				if (currentBlock != null)
					request.addConstraint(currentBlock);
				currentBlock = new TripleBlockConstraint();
				continue;
			}

			String[] spoStrings = line.split(" ");
			Node[] spoNodes = new Node[3];
			for (int i = 0; i < 3; i++) {
				if (spoStrings[i].startsWith("?"))
					spoNodes[i] = Node.createVariable(spoStrings[i].substring(1));
				else if (spoStrings[i].startsWith("<http://"))
					spoNodes[i] = Node.createURI(spoStrings[i].substring(1, spoStrings[i].length() - 1));
				else if (spoStrings[i].startsWith("\""))
					spoNodes[i] = Node.createLiteral(spoStrings[i].substring(1, spoStrings[i].length() - 1));
			}
			if (currentBlock != null) {
				TripleConstraint constraint = new TripleConstraint(Triple.create(spoNodes[0], spoNodes[1], spoNodes[2]));
				currentBlock.add(constraint);
				request.addResourceProvider(constraint);
			}
		}
		// Add the final block
		if (currentBlock != null)
			request.addConstraint(currentBlock);

		// Create the optimiser
		optimizer = new Optimizer(datalayer, request, null);
		optimizer.addObserver(this);
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Solver solver = new Solver();
		solver.run();
	}

	/**
	 * 
	 */
	private void run() {
		optimizer.run();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	public void update(Observable source, Object arg) {
		// Check source
		if (!(source instanceof Optimizer))
			return;

		// Get the best solution
		@SuppressWarnings("unchecked")
		Collection<Solution> solutions = (Collection<Solution>) arg;
		boolean stop = false;
		for (Solution s : solutions) {
			if (s.isOptimal()) {
				logger.info("Found optimal solution :");
				Model model = ModelFactory.createDefaultModel();
				for (Triple triple : request.getTripleSet(s))
					logger.info(model.asStatement(triple).toString());
				if (s.isOptimal())
					stop = true;
			}
		}

		// If we should stop, do it
		if (stop) {
			optimizer.terminate();
			directory.close();
		}
	}
}
