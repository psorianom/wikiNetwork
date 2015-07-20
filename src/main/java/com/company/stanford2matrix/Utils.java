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

    public static ArrayList<String> removeFolders(ArrayList<String> listStrings, String pickup, String limit) {
        int i;
        ArrayList<String> newList = listStrings;
        if (!pickup.isEmpty()) {
            for (i = 0; i < listStrings.size(); i++)
                if (listStrings.get(i).contains(pickup)) {
                    newList = new ArrayList<>(listStrings.subList(i, listStrings.size()));
                    break;
                }
        }

        if (!limit.isEmpty()) {
            for (i = 0; i < newList.size(); i++)
                if (newList.get(i).contains(limit)) {
                    newList = new ArrayList<>(newList.subList(0, i));
                    break;
                }
        }
        return newList;
    }

    public static Integer sumValues(Map<String, Integer> mapToSum) {
        int sum = 0;
        for (int f : mapToSum.values())
            sum += f;
        return sum;
    }
    public static double average(List<Integer> list) {
        // 'average' is undefined if there are no elements in the list.
        if (list == null || list.isEmpty())
            return 0.0;
        // Calculate the summation of the elements in the list
        long sum = 0;
        int n = list.size();
        // Iterating manually is faster than using an enhanced for loop.
        for (int i = 0; i < n; i++)
            sum += list.get(i);
        // We don't want to perform an integer division, so the cast is mandatory.
        return ((double) sum) / n;
    }

    public static Map<Integer, String> invertMapOfLists(DefaultDict<String, ArrayList<Integer>> map) {

        Map<Integer, String> inv = new HashMap<>();

        for (Map.Entry<String, ArrayList<Integer>> entry : map.entrySet()) {
            for (int listValue : entry.getValue()) {
                String oldValue = inv.put(listValue, entry.getKey());
                if (oldValue != null)
                    throw new IllegalArgumentException("Map values must be unique");
            }


        }

        return inv;
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


        /// Inverted maps

        String cRowToken = gson.toJson(matrix.cRowToken);
        saveTextFile(pathMetaData + "cRowToken", cRowToken, ".json");


        String cTokenPOS = gson.toJson(matrix.cTokenPOS);
        saveTextFile(pathMetaData + "cTokenPOS", cTokenPOS, ".json");

        String cColumnSubClause = gson.toJson(matrix.cColumnSubClause);
        saveTextFile(pathMetaData + "cColumnSubClause", cColumnSubClause, ".json");

        String cColumnDependency = gson.toJson(matrix.cColumnDependency);
        saveTextFile(pathMetaData + "cColumnDependency", cColumnDependency, ".json");

        String cColumnSentence = gson.toJson(matrix.cColumnSentence);
        saveTextFile(pathMetaData + "cColumnSentence", cColumnSentence, ".json");


        System.out.println("Done");
    }

    public final static void saveTextFile(String pathFile, String toSave, String extension) throws IOException {
        File file = new File(pathFile + extension);
        FileWriter writer = new FileWriter(file);
        writer.write(toSave);
        writer.close();

    }

    public static StringBuilder saveMatrixMarketFormat(String pathMMatrix, MatrixContainer matrix, boolean writeToDisk) throws IOException {
        System.out.println(pathMMatrix);
        /// Assign StringBuilder with roughly this number of chars: (Number of lines * avg_size_of_a_line) + chars_in_header
        StringBuilder matrixData = new StringBuilder((matrix.getNumberNonZeroElements() * 15) + 72);
        System.out.print("\nWriting Matrix Market Format matrix... ");
//        writer.write("%%MatrixMarket matrix coordinate real general\n%\n");
//        writer.write(String.format("%d\t%d\t%d\n", matrix.getNumberRows(), matrix.getNumberColumns(), matrix.getNumberNonZeroElements()));
        matrixData.append("%%MatrixMarket matrix coordinate real general\n%\n");
        matrixData.append(String.format("%d\t%d\t%d\n", matrix.getNumberRows(), matrix.getNumberColumns(), matrix.getNumberNonZeroElements()));
        for (int v = 0; v < matrix.getNumberNonZeroElements(); v++) {
//            writer.write(String.format("%d\t%d\t%d\n", matrix.cRows.get(v), matrix.cCols.get(v), matrix.cData.get(v)));
            matrixData.append(String.format("%d\t%d\t%d\n", matrix.cRows.get(v), matrix.cCols.get(v), matrix.cData.get(v)));
        }

        if (writeToDisk) {
            File file = new File(pathMMatrix + ".mtx");
            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            writer.write(matrixData.toString());
            writer.close();
        }
        System.out.print("Done\n");
        return matrixData;
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
        ArrayList<String> finalFilesPaths = new ArrayList<>();

        if (folder.isFile()) {
            finalFilesPaths.add(pathFolder);
            return finalFilesPaths;
        }

        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                filesPaths.addAll(listFiles(fileEntry.getPath(), ext));
            } else {
                filesPaths.add(fileEntry.getPath());
//                System.out.println(fileEntry.getName());

            }
        }

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


