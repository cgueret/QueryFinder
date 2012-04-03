package nl.vu.queryfinder;

import java.io.FileNotFoundException;
import java.io.IOException;

import nl.vu.queryfinder.model.Query;
import nl.vu.queryfinder.services.Service;
import nl.vu.queryfinder.services.impl.Copy;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;

public class ServiceExec {

	/**
	 * @param input
	 * @param component
	 * @return
	 * @throws IOException
	 * @throws RepositoryException
	 * @throws RDFParseException
	 */
	public Query process(String input, String component) throws RDFParseException, RepositoryException, IOException {
		Query inputQuery = new Query();
		inputQuery.loadFrom(input);

		Service service = new Copy();
		return service.process(inputQuery);
	}

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
	 * @throws FileNotFoundException
	 * @throws RDFHandlerException
	 * @throws RDFParseException
	 * @throws RepositoryException
	 */
	public static void main(String[] args) throws ParseException, RDFParseException, RDFHandlerException,
			FileNotFoundException, IOException, RepositoryException {
		// Compose the options
		Options options = new Options();
		options.addOption("c", "component", true, "name of the processing component");
		options.addOption("i", "input", true, "query input file (ttl)");
		options.addOption("o", "output", true, "query output file (ttl)");
		options.addOption("p", "parameters", true, "component parameters");
		options.addOption("h", "help", false, "print help message");

		// Parse the command line
		CommandLineParser parser = new PosixParser();
		CommandLine line = parser.parse(options, args);

		// Handle request for help
		if (line.hasOption("h"))
			printHelpAndExit(options, 0);

		// Handle miss-use
		if (!line.hasOption("c") || !line.hasOption("o") || !line.hasOption("i"))
			printHelpAndExit(options, -1);

		// Create an instance
		ServiceExec instance = new ServiceExec();

		// Process the input query and get the new one
		Query newQuery = instance.process(line.getOptionValue("i"), line.getOptionValue("c"));

		// Save the new query to disk
		newQuery.saveTo(line.getOptionValue("o"));
	}
}
