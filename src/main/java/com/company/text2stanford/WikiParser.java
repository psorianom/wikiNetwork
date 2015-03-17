package com.company.text2stanford;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.company.text2stanford.Utils.listFiles;

public class WikiParser {
    private static int nThreads;

    private static String pathFolder;

    WikiParser(String pathFolder, int nThreads) {
        WikiParser.nThreads = nThreads;
        WikiParser.pathFolder = pathFolder;
        //"/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data"
    }

    public static void main(String[] args) throws InterruptedException {
//        String data = "/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/wikidata";
//        String data = "/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data/sentencedata";
        String data = "/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data/oanc/";
        WikiParser myWiki = new WikiParser(data, 8);
        myWiki.run();


    }

    public void run() throws InterruptedException {
        long start = System.nanoTime();
        StanfordCoreNLP nlpPipe = createCoreNLPObject();
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        ArrayList<String> listPaths = listFiles(pathFolder);
//        listPaths.add("/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data/AA/wiki_00");
        for (String path : listPaths) {
            System.out.println("still working...");
            Runnable worker = new ParserThread(path, nlpPipe);
            //worker will execute its "run" functionl
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