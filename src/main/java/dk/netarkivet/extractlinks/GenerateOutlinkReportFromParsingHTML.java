package dk.netarkivet.extractlinks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.extractlinks.batch.ExtractLinksBatchJob;

// run local warc-batchjob on the warc files produced by Heritrix3 that 
// looks for outlink lines in WARC-metadata records

public class GenerateOutlinkReportFromParsingHTML {

	private static final Logger log = LoggerFactory.getLogger(GenerateOutlinkReportFromParsingHTML.class);
		
	public static void main(String[] args) {
		if (args.length == 0 || args.length > 1) {
			System.out.println("GenerateOutlinkReportFromParsingHTML takes only one argument (directory of warcfiles). Number of arguments was " + args.length);
			System.exit(1);
		}
		
		File warcsDir = new File(args[0]);
		if (!warcsDir.exists()) {
			System.out.println("The filedir argument '" +  warcsDir.getAbsolutePath() + "' does not exist");
			System.exit(1);
		}
		
		final String dir = System.getProperty("user.dir");
		File outputdir = new File(dir);
		if (!outputdir.canWrite()) {
			System.out.println("Unable to write to default outputdirectory (user.dir): '" +  outputdir.getAbsolutePath() + "'. Aborting outlink generation");
			System.exit(1);
		}
		File finalFile = generateReport(warcsDir, outputdir);
		System.out.println("Finished generating outlinkfile with duplicates removed '" + finalFile.getAbsolutePath() + "' with length " + finalFile.length());
		String unique = ".unique";
		File reportfileWithDuplicates = new File(outputdir, finalFile.getName().substring(0, finalFile.getName().length() - unique.length()));
		System.out.println("The outlinkfile without duplicates removed '" + reportfileWithDuplicates.getAbsolutePath() + "' with length " + reportfileWithDuplicates.length());
	}

	/**
	 * Run local warc-batchjob on the warc files produced by Heritrix3 that 
	 * looks for outlink lines in WARC-metadata records.
	 * @param warcFilesDir directory containing warc-files.
	 * @return a textfile of outlinks in WARC-metadata records.
	 */
	public static File generateReport(File warcFilesDir, File outputdir) {

		File[] warcFileArray = warcFilesDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				//boolean matches = name.matches(FileUtils.WARC_PATTERN); // FIXME this pattern fails to find : 431-35-20160317083714655-00000-sb-test-har-001.statsbiblioteket.dk.warc
				boolean matches = name.contains("warc");

				log.debug(" '" + name + "' matches: " + matches);
				return matches;	
			}
		});
		log.debug("Found {} files matching the pattern for warcfiles",  warcFileArray.length);
		String datestamp = System.currentTimeMillis() + "";
		File outlinkreportFile = new File(outputdir, "outlinkreport-" + datestamp + ".txt");
		File uniqueoutlinkreportFile = new File(outputdir, "outlinkreport-" +  datestamp + ".txt.unique");
		log.debug("Using outlinkreport file {} for outlinks written to metadata records", outlinkreportFile.getAbsolutePath());
		BatchLocalFiles batchRunner = new BatchLocalFiles(warcFileArray);
		OutputStream outlinkStream = null;
		try {
			outlinkStream = new FileOutputStream(outlinkreportFile);
			batchRunner.run(new ExtractLinksBatchJob(), outlinkStream);
			removeDuplicateOutlinks(outlinkreportFile, uniqueoutlinkreportFile);
			return uniqueoutlinkreportFile;
		} catch (FileNotFoundException e) {
			log.debug("Error while creating outlinkreportfile{}", outlinkreportFile, e);
			return null;
		} finally {
			IOUtils.closeQuietly(outlinkStream);
		}
	}
	
	private static void removeDuplicateOutlinks(File inputfile, File outputFile) {
		ArgumentNotValid.checkExistsNormalFile(inputfile, "File inputfile");
		String line;
		Set<String> links = new TreeSet<String>();
		BufferedReader bufferedReader = null;
		int linesRead=0;
        try {
	        bufferedReader = new BufferedReader( new FileReader(inputfile));
	        while ( (line = bufferedReader.readLine()) != null ){
	        	links.add(line);
	        	linesRead++;
	        }
        } catch (FileNotFoundException e) {
	        log.warn("Error ", e);
        } catch (IOException e1) {
        	log.warn("Error ", e1);
        } finally {
        	IOUtils.closeQuietly(bufferedReader);
        }
        
        log.info("Read {} lines: Found {} unique outlinks", linesRead, links.size());
        PrintWriter writer = null;
	        try {
	            writer = new PrintWriter(outputFile, "UTF-8");
	            for (String link: links) {
	            	writer.println(link);
	            }
            } catch (Throwable e) {
	            // TODO log this
            	e.printStackTrace();
            	
            } finally {
            	IOUtils.closeQuietly(writer);
            }
	}
}
