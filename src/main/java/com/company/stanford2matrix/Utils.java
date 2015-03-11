package com.company.stanford2matrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by pavel on 16/12/14.
 */
public class Utils {

    /**
     * Read contents of a file into a string.
     *
     * @param file
     * @return file contents.
     */
    public static String readFile(String fileName, boolean skipHeader) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            StringBuffer sb = new StringBuffer();
            String line = null;
            if (skipHeader) {
                br.readLine();
                br.readLine();
            }

            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static ArrayList<String> listFiles(String pathFolder, String ext) {
        ArrayList<String> filesPaths = new ArrayList<>();
        final File folder = new File(pathFolder);

        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                filesPaths.addAll(listFiles(fileEntry.getPath(), ext));
            } else {
                filesPaths.add(fileEntry.getPath());
//                System.out.println(fileEntry.getName());

            }
        }
        ArrayList<String> finalFilesPaths = new ArrayList<>();
        for (final String path : filesPaths) {
            if (path.contains("." + ext))
                finalFilesPaths.add(path);
        }
        return finalFilesPaths;
    }

    public static void main(String[] args) {
        listFiles("/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data", "txt");
    }


    public static class DefaultDict<K, V> extends HashMap<K, V> {

        Class<V> klass;

        public DefaultDict() {
            this.klass = (Class<V>) ArrayList.class;
        }

        public DefaultDict(Class klass) {
            this.klass = klass;
        }

        @Override
        public V get(Object key) {
            V returnValue = super.get(key);
            if (returnValue == null) {
                try {
                    returnValue = klass.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                this.put((K) key, returnValue);
            }
            return returnValue;
        }
    }


    public static class InvalidLengthsException extends Exception {

        public InvalidLengthsException(String message) {
            super(message);
        }

    }
}


