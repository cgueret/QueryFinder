/**
 * 
 */
package nl.vu.queryfinder.services.impl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import nl.erdf.datalayer.sparql.orig.SPARQLDataLayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// FIXME : In the patterns set, every valid set of X patterns is equivalent.
// There is no consideration of the different branches of the graph the
// different pattern represent
public class BruteSolver {
	static final Logger logger = LoggerFactory.getLogger(BruteSolver.class);
	static final String SPARQL_ENDPOINT = "http://lod.openlinksw.com/sparql";

	/**
	 * @author Christophe Guéret <christophe.gueret@gmail.com>
	 * 
	 */
	public class Block {
		private final Set<ElementGroup> elements = new HashSet<ElementGroup>();
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		BruteSolver solver = new BruteSolver();
		List<Block> startingBlocks = solver.readFrom("data/patterns2.list");
		solver.combineBlocks(startingBlocks, 0);

	}

	/**
	 * @param blocks
	 */
	private void combineBlocks(List<Block> blocks, int queries) {
		if (blocks.size() == 0) {
			logger.warn("No solution found !");
			return;
		}
		
		if (blocks.size() == 1) {
			logger.info("Solution found with " + queries + " queries");
			for (ElementGroup element : blocks.get(0).elements)
				logger.info(element.toString());
			return;
		}
		
		List<Block> newBlocks = new ArrayList<Block>();

		for (int index = 0; index < blocks.size() - 1; index++) {
			// Get the two blocks
			Block firstBlock = blocks.get(index);
			Block secondBlock = blocks.get(index + 1);
			Block newBlock = new Block();

			// Try to combine their content
			for (ElementGroup firstGroup : firstBlock.elements) {
				for (ElementGroup secondGroup : secondBlock.elements) {
					// Prepare the query
					ElementGroup elg = new ElementGroup();
					elg.addElement(firstGroup);
					elg.addElement(secondGroup);
					Query query = QueryFactory.make();
					query.setQueryAskType();
					query.setQueryPattern(elg);
					QueryExecution queryExec = QueryExecutionFactory.sparqlService(SPARQL_ENDPOINT, query);
					boolean valid = queryExec.execAsk();
					if (valid)
						newBlock.elements.add(elg);
					queries++;
				}
			}

			if (newBlock.elements.size() > 0)
				newBlocks.add(newBlock);
		}
		
		combineBlocks(newBlocks, queries);
	}

	/**
	 * @param string
	 * @return
	 * @throws IOException
	 */
	private List<Block> readFrom(String fileName) throws IOException {
		List<Block> blocks = new ArrayList<Block>();

		// Read the patterns
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		Block currentBlock = null;
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			// Ignore comments and blank lines
			if (line.equals("") || line.startsWith("#"))
				continue;

			// Detect blocks
			if (line.startsWith("=")) {
				if (currentBlock != null)
					blocks.add(currentBlock);
				currentBlock = new Block();
				continue;
			}

			// Read the triple
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

			// Store it to the current block
			if (currentBlock != null) {
				Triple triple = Triple.create(spoNodes[0], spoNodes[1], spoNodes[2]);
				ElementGroup elementGroup = new ElementGroup();
				elementGroup.addTriplePattern(triple);
				currentBlock.elements.add(elementGroup);
			}
		}

		// Add the final block
		if (currentBlock != null)
			blocks.add(currentBlock);

		return blocks;
	}
}
