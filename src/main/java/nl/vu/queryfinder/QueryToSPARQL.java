package nl.vu.queryfinder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import nl.erdf.util.Converter;
import nl.vu.queryfinder.model.Quad;
import nl.vu.queryfinder.model.Query;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFParseException;

public class QueryToSPARQL {

	/**
	 * @param exitCode
	 */
	public static void printHelpAndExit(Options options, int exitCode) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(ServiceExec.class.getName(), options);
		System.exit(exitCode);
	}

	/**
	 * @param args
	 * @throws ParseException
	 * @throws IOException
	 * @throws RepositoryException
	 * @throws RDFParseException
	 */
	public static void main(String[] args) throws ParseException,
			RDFParseException, RepositoryException, IOException {
		// Compose the options
		Options options = new Options();
		options.addOption("i", "input", true, "query input file (ttl)");
		options.addOption("o", "output", true, "result");
		options.addOption("h", "help", false, "print help message");

		// Parse the command line
		CommandLineParser parser = new PosixParser();
		CommandLine line = parser.parse(options, args);

		// Handle request for help
		if (line.hasOption("h"))
			printHelpAndExit(options, 0);

		// Handle miss-use
		if (!line.hasOption("i") || !line.hasOption("o"))
			printHelpAndExit(options, -1);

		// Handle using a non existing file as an input
		String inputQueryName = line.getOptionValue("i");
		if (!(new File(inputQueryName)).canRead()) {
			System.out.println("Can not open " + inputQueryName);
			printHelpAndExit(options, -1);
		}

		// Load the input query
		Query inputQuery = new Query();
		inputQuery.loadFrom(inputQueryName);

		process(inputQuery.getQuads(), new File(line.getOptionValue("o")));
		System.exit(0);
	}

	/**
	 * @param quads
	 * @param optionValue
	 * @throws IOException
	 */
	private static void process(List<Quad> quads, File outputFile)
			throws IOException {
		Map<String, TreeSet<String>> queries = new HashMap<String, TreeSet<String>>();

		// Group the patterns in different query sets
		for (Quad quad : quads) {
			// Parse the bindings of the solution
			String bindingsRaw = quad.getContext().toString().split("  ")[1];
			Map<String, String> map = new HashMap<String, String>();
			for (String binding : bindingsRaw.substring(1,
					bindingsRaw.length() - 2).split(",")) {
				String[] b = binding.split("=");
				map.put(b[1], "?" + b[0]);
			}

			// Is this quad associated to a new solution ?
			TreeSet<String> query = null;
			if (!queries.containsKey(quad.getContext().toString())) {
				query = new TreeSet<String>();
				queries.put(quad.getContext().toString(), query);
			} else {
				query = queries.get(quad.getContext().toString());
			}

			// Compose the triple pattern from the solution
			Value s = quad.getSubject();
			Value p = quad.getPredicate();
			Value o = quad.getObject();
			StringBuffer buffer = new StringBuffer();
			buffer.append(
					map.containsKey(s.toString()) ? map.get(s.toString())
							: Converter.toN3(s)).append(" ");
			buffer.append(
					map.containsKey(p.toString()) ? map.get(p.toString())
							: Converter.toN3(p)).append(" ");
			buffer.append(
					map.containsKey(o.toString()) ? map.get(o.toString())
							: Converter.toN3(o)).append(" .");

			// Add it to the query
			query.add(buffer.toString());
		}

		// Group and count
		Set<TreeSet<String>> uniqQueries = new HashSet<TreeSet<String>>();
		uniqQueries.addAll(queries.values());

		// Prepare output
		outputFile.delete();
		BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));

		// Compose and display the Queries
		for (TreeSet<String> patterns : uniqQueries) {
			// Get the number of instances for that query
			int c = 0;
			for (TreeSet<String> p : queries.values())
				if (patterns.equals(p))
					c++;

			// Compose the query
			StringBuffer buffer = new StringBuffer("SELECT ");
			Set<String> vars = new HashSet<String>();
			for (String t : patterns)
				for (String part : t.split(" "))
					if (part.startsWith("?"))
						vars.add(part);
			buffer.append(StringUtils.join(vars, " "));
			buffer.append(" WHERE {\n");
			for (String t : patterns)
				buffer.append("\t").append(t).append("\n");
			buffer.append("}\n");

			// Save the results
			out.write("# Number of matching instances: " + c + "\n");
			out.write(buffer.append("\n").toString());
		}
		
		// Close output
		out.close();
		
	}

}
