package com.company;

import de.tudarmstadt.ukp.wikipedia.parser.Paragraph;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.FlushTemplates;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParser;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory;
import edu.jhu.nlp.wikipedia.*;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static String removeLeftMarkup(String text) {
        String cleanedText = "";
        // Remove the remaining {{...}} tags from the cleaned text
        // Remove the remaining * ... tags from the cleaned text
        // Remove the == TITLE == and leave TITLE
        cleanedText = text.replaceAll("(?s)\\{\\{.*?\\}\\}", " ");
        cleanedText = cleanedText.replaceAll("\\*.*", "");
        cleanedText = cleanedText.replaceAll("=+", "");

        return cleanedText;
    }

//    public static void testin() {
//        String cadena = "[[File:Fransisco Ferrer Guardia.jpg|left|thumb|[[Francesc Ferrer i Guàrdia]], [[Catalan people|Catalan]] anarchist pedagogue and [[Freethought|free-thinker]]]] For English anarchist [[William Godwin]] education was \"the main means by which change would be achieved.\" [http://www.infed.org/thinkers/et-good.htm \"william godwin and informal education\" by infed]  Godwin saw that the main goal of education should be the promotion of happiness.  For Godwin education had to have \"A respect for the child's autonomy which precluded any form of coercion,\" \"A pedagogy that respected this and sought to build on the child's own motivation and initiatives,\" and \"A concern about the child's capacity to resist an ideology transmitted through the school.\"  In his ''[[Political Justice]]'' he criticises state sponsored schooling \"on account of its obvious alliance with national government\".   |publisher=G.G.J. and J. Robinson |location=London, England |year=1793 |ref=harv |postscript=  }}  Early American anarchist [[Josiah Warren]] advanced alternative education experiences in the libertarian communities he established.  [[Max Stirner]] wrote in 1842 a long essay on education called ''[[The False Principle of our Education]]''. In it Stirner names his educational principle \"personalist,\" explaining that self-understanding consists in hourly self-creation. Education for him is to create \"free men, sovereign characters,\" by which he means \"eternal characters&nbsp;... who are therefore eternal because they form themselves each moment\".";
//        String cadeno = cadena.replaceAll("\\[\\[.*?:.*?\\]\\]", " ");
//        System.out.println(cadeno);
//
//    }

    public static void main(String[] args) {




        String xmlfile = "/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data/test1_wikixml.xml";
        WikiXMLParser wxsp = WikiXMLParserFactory.getSAXParser(xmlfile);
        final ArrayList<WikiPage> page_list = new ArrayList<WikiPage>();
        try {

            wxsp.setPageCallback(new PageCallbackHandler() {
                public void process(WikiPage page) {
                    MediaWikiParserFactory pf = new MediaWikiParserFactory();
                    pf.setTemplateParserClass( FlushTemplates.class );

                    MediaWikiParser parser = pf.createParser();

//                    String clearedText = removeLeftMarkup(page.getText());
//                    System.out.println(page.getText());
                    System.out.println("----------------------------------------------------------------------------------------");
//                    System.out.println(page.getWikiText());
//                    page_list.add(page);
                    ParsedPage pp = parser.parse(page.getWikiText());
                    List<Paragraph> milista = new ArrayList<Paragraph>();
                    milista = pp.getParagraphs();

                    System.out.println(page.getTitle());
                }
            });

            wxsp.parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
////        WikiPage pageon = page_list.get(900);
//        String textow = pageon.getText();
//        System.out.println(page_list.size());
//        System.out.println(pageon.getWikiText());
//        System.out.println("----------------------------------------------------------------------------------------");
//        System.out.println(pageon.getText());
//        System.out.println("----------------------------------------------------------------------------------------");
//        System.out.println(pageon);

    }
}
