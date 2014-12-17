package com.company;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.HashMap;

public class Main {


    public static String pathfile;


    public static void main(String argv[]) throws Exception {

        File input = new File("/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data/AA/wiki_00");

        Document doc = Jsoup.parse(input, "UTF-8", "");


        Elements docs = doc.getElementsByTag("doc");
        for (Element c : docs) {
            HashMap<String, String> anchorMap = new HashMap<>();
            Elements anchors = c.getElementsByTag("a");
            for (Element a : anchors) {
                String href = a.attr("href");
                String hrefWord = a.text();
                System.out.println("\t" + hrefWord + ":" + href);
                anchorMap.put(href, hrefWord);

            }
//            System.out.println(c.text());
            System.out.println(c.attr("id"));

        }


    }
}
