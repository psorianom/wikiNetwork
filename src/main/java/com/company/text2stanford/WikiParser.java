package com.company.text2stanford;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.commons.cli.*;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.company.text2stanford.Utils.listFiles;

public class WikiParser {
    private static int nThreads;
    //    private static int imput
    private static String inputFolderPath;
    private static String outputFolderPath;

    private static String pathFolder;

    WikiParser(String pathFolder, int nThreads) {
        WikiParser.nThreads = nThreads;
        WikiParser.pathFolder = pathFolder;
    }

    public static void main(String[] args) throws InterruptedException {

        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption("i", "input", true, "Input folder of extracted Wikipedia files");
//        options.addOption("o", "output", true, "Output folder for parsed Wikipedia files");
        String inputPath;

        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("i")) {
                inputFolderPath = line.getOptionValue("i");
            } else
            //We cant continue if this is not set
            {
                System.out.println("Please give an input folder");
                return;
            }
        } catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            return;

        }
        System.out.println("Remember to use the appropriate parser, depending on the data that you are trying to parse.\n");
//        String data = "/media/stuff/Pavel/Documents/Eclipse/workspace/data/these_graph/oanc/corpus";
        WikiParser myWiki = new WikiParser(inputFolderPath, 12);
        myWiki.run();


    }

    public void run() throws InterruptedException {
        long start = System.nanoTime();
        StanfordCoreNLP nlpPipe = createCoreNLPObject();
        ArrayList<String> listPaths = listFiles(inputFolderPath);
//        listPaths.add("/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data/AA/wiki_00");
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(nThreads, listPaths.size()));
        for (String path : listPaths) {
//            System.out.println(path);
            Runnable worker = new ParserThread(path, nlpPipe);
            //worker will execute its "run()" function
            executor.execute(worker);
        }
        // This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        // Wait until all threads are finish
        executor.awaitTermination(10, TimeUnit.DAYS);
        long time = System.nanoTime() - start;
        System.out.println("Finished all threads");
        System.out.printf("Tasks took %.3f m to run%n", time / (60 * 1e9));
    }


    public StanfordCoreNLP createCoreNLPObject() {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, parse");
        props.put("parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
        return new StanfordCoreNLP(props);
    }
} 
