package nl.vu.queryfinder;

import java.io.File;
import java.io.IOException;

import nl.vu.queryfinder.model.Quad;
import nl.vu.queryfinder.model.Query;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFParseException;

public class DumpQuery {

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
	public static void main(String[] args) throws ParseException, RDFParseException, RepositoryException, IOException {
		// Compose the options
		Options options = new Options();
		options.addOption("i", "input", true, "query input file (ttl)");
		options.addOption("h", "help", false, "print help message");

		// Parse the command line
		CommandLineParser parser = new PosixParser();
		CommandLine line = parser.parse(options, args);

		// Handle request for help
		if (line.hasOption("h"))
			printHelpAndExit(options, 0);

		// Handle miss-use
		if (!line.hasOption("i"))
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

		for (Quad quad: inputQuery.getQuads())
			System.out.println(quad.getSubject() + "\t" + quad.getPredicate() + "\t" + quad.getObject());
		
		System.exit(0);
	}

}
