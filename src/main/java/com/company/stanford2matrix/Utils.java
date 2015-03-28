package com.company.stanford2matrix;

import com.google.gson.Gson;

import java.io.*;
import java.util.*;

/**
 * Created by pavel on 16/12/14.
 */
public class Utils {

    /**
     * Read contents of a file into a string.
     *
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

    public static Map<Integer, String> invertMapOfSets(DefaultDict<String, HashSet<Integer>> map) {

        Map<Integer, String> inv = new HashMap<>();

        for (Map.Entry<String, HashSet<Integer>> entry : map.entrySet()) {
            for (int listValue : entry.getValue()) {
                String oldValue = inv.put(listValue, entry.getKey());
                if (oldValue != null)
                    throw new IllegalArgumentException("Map values must be unique");
            }


        }

        return inv;
    }

    public static <V, K> Map<V, K> invertMap(Map<K, V> map) {

        Map<V, K> inv = new HashMap<V, K>();

        for (Map.Entry<K, V> entry : map.entrySet()) {
            K oldValue = inv.put(entry.getValue(), entry.getKey());
            if (oldValue != null)
                throw new IllegalArgumentException("Map values must be unique");
        }
        return inv;
    }
    public static void saveMetaData(String pathMetaData, MatrixContainer matrix) throws IOException {
        System.out.print("Saving matrix metadata as JSON...");

        Gson gson = new Gson();

//        String cNPWordsColumn = gson.toJson(matrix.cNPwordsColumn);
//        saveTextFile(pathMetaData + "cNPWordsColumn", cNPWordsColumn, ".json");

        String cSubClausesColumnsJSON = gson.toJson(matrix.cSubClausesColumns);
        saveTextFile(pathMetaData + "cSubClausesColumns", cSubClausesColumnsJSON, ".json");

        String cTokenRowJSON = gson.toJson(matrix.cTokenRow);
        saveTextFile(pathMetaData + "cTokenRow", cTokenRowJSON, ".json");

        String cPOSTokenJSON = gson.toJson(matrix.cPOSToken);
        saveTextFile(pathMetaData + "cPOSToken", cPOSTokenJSON, ".json");

//        String cNPwordsColumnJSON = gson.toJson(matrix.cNPwordsColumn);
//        saveTextFile(pathMetaData + "cNPwordsColumn", cNPwordsColumnJSON, ".json");

        String cClauseSubClauseColumnsJSON = gson.toJson(matrix.cClauseSubClauseColumns);
        saveTextFile(pathMetaData + "cClauseSubClauseColumns", cClauseSubClauseColumnsJSON, ".json");

        String cNgramColumnJSON = gson.toJson(matrix.cNgramColumn);
        saveTextFile(pathMetaData + "cNgramColumn", cNgramColumnJSON, ".json");

        String cDependencyColumn = gson.toJson(matrix.cDependencyColumn);
        saveTextFile(pathMetaData + "cDependencyColumn", cDependencyColumn, ".json");

        /// Inverted index-es

        String cRowToken = gson.toJson(matrix.cRowToken);
        saveTextFile(pathMetaData + "cRowToken", cRowToken, ".json");

        String cColumnSubClause = gson.toJson(matrix.cColumnSubClause);
        saveTextFile(pathMetaData + "cColumnSubClause", cColumnSubClause, ".json");

        String cColumnDependency = gson.toJson(matrix.cColumnDependency);
        saveTextFile(pathMetaData + "cColumnDependency", cColumnDependency, ".json");


        System.out.println("Done");
    }

    public final static void saveTextFile(String pathFile, String toSave, String extension) throws IOException {
        File file = new File(pathFile + extension);
        FileWriter writer = new FileWriter(file);
        writer.write(toSave);
        writer.close();

    }

    public static boolean saveMatrixMarketFormat(String pathMMatrix, MatrixContainer matrix) throws IOException {

        File file = new File(pathMMatrix + ".mtx");
        file.createNewFile();
//        System.out.println(pathMMatrix);
        FileWriter writer = new FileWriter(file);
        System.out.print("\nWriting Matrix Market Format matrix... ");
        writer.write("%%MatrixMarket matrix coordinate real general\n%\n");
        writer.write(String.format("%d\t%d\t%d\n", matrix.getNumberRows(), matrix.getNumberColumns(), matrix.getNumberNonZeroElements()));
        for (int v = 0; v < matrix.getNumberNonZeroElements(); v++)
            writer.write(String.format("%d\t%d\t%d\n", matrix.cRows.get(v), matrix.cCols.get(v), matrix.cData.get(v)));

        writer.close();
        System.out.print("Done\n");
        return true;
    }


    public static DefaultDict<String, HashSet<Integer>> updateSubClausesColumns(DefaultDict<String, HashSet<Integer>> oldSubClausesColumns,
                                                                                DefaultDict<String, HashSet<Integer>> newSubClausesColumns) {
        for (Map.Entry e : newSubClausesColumns.entrySet())
            oldSubClausesColumns.get(e.getKey()).addAll(newSubClausesColumns.get(e.getKey()));

        return oldSubClausesColumns;
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

        //Filter files with extension ext and avoid hidden files (starting with .)
        for (final String path : filesPaths) {
            String[] pathLevels = path.split("/");
            String fileName = pathLevels[pathLevels.length - 1];
            String firstChar = fileName.substring(0, 1);
            /// If filepath is not metadata json file, and if it has the required extensions and if it is not hidden file
            if (!path.contains("metadata") && path.contains("." + ext) && !firstChar.contains("."))
                finalFilesPaths.add(path);
        }
        return finalFilesPaths;
    }

    public static void main(String[] args) {
        listFiles("/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data", "txt");
    }


    public static class OrderedDefaultDict<K, V> extends LinkedHashMap<K, V> {

        Class<V> klass;

        public OrderedDefaultDict() {
            this.klass = (Class<V>) ArrayList.class;
        }

        public OrderedDefaultDict(Class klass) {
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


