package com.company;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by pavel on 16/12/14.
 */
public class Utils {
    public static ArrayList<String> listFiles(String pathFolder) {
        ArrayList<String> filesPaths = new ArrayList<>();
        final File folder = new File(pathFolder);

        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                filesPaths.addAll(listFiles(fileEntry.getPath()));
            } else {
                filesPaths.add(fileEntry.getPath());
//                System.out.println(fileEntry.getName());

            }
        }
        return filesPaths;
    }

    public static void main(String[] args) {
        listFiles("/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data");
    }

}
