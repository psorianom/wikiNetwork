package com.company;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by pavel on 16/12/14.
 */


public class ParserThread implements Runnable {


    public String pathFile;
    public StanfordCoreNLP coreParser;

    //    Constructor
    ParserThread(String pathFile, StanfordCoreNLP coreParser) {
        this.coreParser = coreParser;
        this.pathFile = pathFile;
    }

    public HashMap<String, String> extractInfo() {
        File input = new File(pathFile);

        return new HashMap<>();
    }

    @Override
    public void run() {
        try {
            File input = new File(pathFile);

            String parsedFilePath = input.getCanonicalPath() + ".parsed.txt";
            FileWriter parsedOutput = new FileWriter(parsedFilePath);
            BufferedWriter bufferedOut = new BufferedWriter(parsedOutput);

            Document doc;
            doc = Jsoup.parse(input, "UTF-8", "");

            // Foreach doc in the file
            Elements docs = doc.getElementsByTag("doc");
            for (Element c : docs) {
                HashMap<String, String> anchorMap = new HashMap<>();

                System.out.println(c.attr("title"));
                bufferedOut.write(c.attr("title") + "\n");

                Elements anchors = c.getElementsByTag("a");

                // Foreach link in the doc
                for (Element a : anchors) {
                    String href = a.attr("href");

                    String hrefWord = a.text();
                    bufferedOut.write("\t" + hrefWord + " :" + href + "\n");
                    System.out.println("\t" + hrefWord + ": " + href);
                    anchorMap.put(href, hrefWord);

                }
//            System.out.println(c.text());


            }
            bufferedOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}