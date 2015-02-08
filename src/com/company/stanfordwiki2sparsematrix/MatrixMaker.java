package com.company.stanfordwiki2sparsematrix;

import java.io.IOException;
import java.util.*;

import static com.company.stanfordwiki2sparsematrix.Utils.*;

public class MatrixMaker {
    private static int nThreads;
    private static String pathFolder;
    private static int column_j = 0;
    private int row_i = 0; ///> The i index for the matrix
    //All this should not be part of the class... it should just have a MatrixContainer object
    private MatrixContainer matrix;


    //Constructor
    MatrixMaker(String pathFolder, int nThreads) {
        MatrixMaker.nThreads = nThreads;
        MatrixMaker.pathFolder = pathFolder;
        row_i = 0; ///> The i index for the matrix
        column_j = 0; ///> The j index for the matrix
        matrix = new MatrixContainer();
    }

    public static void main(String[] args) throws InterruptedException, IOException, InvalidLengthsException {
        MatrixMaker myMaker = new MatrixMaker("/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data", 15);
        myMaker.runSingle();


    }

    private Map<String, ArrayList<Integer>> clauseToIndices(ArrayList<String> clauseNames) {
        Map<String, ArrayList<Integer>> columnsIndices = new LinkedHashMap<>();
        String lastSeen = clauseNames.get(0);

        for (String clause : clauseNames) {
            if (!clause.equals(lastSeen))
                column_j++;
            if (columnsIndices.containsKey(clause) && !columnsIndices.get(clause).contains(column_j))
                columnsIndices.get(clause).add(column_j);
            else
                columnsIndices.put(clause, new ArrayList(Arrays.asList(column_j)));
            lastSeen = clause;
            matrix.cCols.add(column_j);
        }

        return columnsIndices;
    }


//    public MatrixContainer makeMatrix(String pathFile, MatrixContainer buildingMatrix) throws IOException {
//
//        String[] splittedLine;
//        String token;
//        String lemma;
//        String posTag;
//        String constituency;
//        String dependencyHead;
//        String dependency;
//        int level = 1 + 1;
////        HashMap<String,Integer> rowTokens = buildingMatrix.cMapTokenRow;
//        HashMap<String, ArrayList<Integer>> posTokens = new HashMap<>();
//        LinkedHashMap<String, ArrayList<String>> mapClausePOStags = new LinkedHashMap<>(); ///> Contains a hashmap of NP_12:[DT,JJ,NN] or whatever this NP_12 was made of
//        ArrayList<Integer> rows = buildingMatrix.rows;///> Contains row indices
//        ArrayList<Integer> cCols = buildingMatrix.cCols; ///> Contains columns indices
//        ArrayList<Integer> data = new ArrayList<>(); ///> Contains the data
//        HashSet<String> clausesSeen = new HashSet<>(); ///> Contains the type of clause found in the file. Only local, no need to return it.
//        HashMap<String,HashMap<ArrayList<String>,ArrayList<Integer>>> cClauseSubClauseColumns = buildingMatrix.cClauseSubClauseColumns;
//        ArrayList<String> lines = (ArrayList)Files.readAllLines(Paths.get(pathFile));
//        HashMap<String, ArrayList<Integer>> mapSubClausesColumns = buildingMatrix.cSubClausesColumns;
//        HashMap<String, ArrayList<Integer>> mapClausesColumns = buildingMatrix.cSubClausesColumns;
//        lines.remove(0); ///> Remove the FILENAME header
//        lines.remove(0); ///> Remove the columns headers
//        for (String line:lines)
//        {
//            // it is a token. we deal with it.
//            if (!line.contains("%%#"))
//            {
//                // We get the values from the csv line
//                splittedLine = line.split("\t");
//                token = splittedLine[0];
//                lemma = splittedLine[1];
//                posTag = splittedLine[2];
//                constituency = splittedLine[3];
//                dependencyHead = splittedLine[4];
//                dependency = splittedLine[5];
//                if (dependency.equals( "PUNCT"))
//                    continue;
//                /// Lets add the token to dictionary of row-index : token
//                if (rowTokens.containsKey(lemma)) {
//                    /// Lets add the token to the rows list
//                    row_i = rowTokens.get(lemma);
//                    rows.add(row_i);
//
//                }
//                else {
//                    rows.add(row_i);
//                    //TODO: I should really get a defaultdict thingy over here...
//                    rowTokens.put(lemma, row_i);
//                }
//
//                //lets do the Constituency parsing
//
//                // second, we get the clause type (NP,VP, PRP, etc) we are interested in according to the level
//                ArrayList<String> clauses = new ArrayList( Arrays.asList( constituency.split(",") ) );
//
//                if (clauses.size() < level)
//                    level = clauses.size();
//                String targetClause = clauses.get(clauses.size() - level);
//                String[] clauseSplitted = targetClause.split("_");
//                String clauseInitials = clauseSplitted[0];
//                String clauseID = clauseSplitted[1];
//                clausesSeen.add(clauseInitials);
//
//                // We get wh
//                if (posTokens.containsKey(posTag))
//                    posTokens.get(posTag).add(row_i);
//                else
//                    posTokens.put(posTag, new ArrayList(Arrays.asList(row_i)));
//
//
//                if (mapClausePOStags.containsKey(targetClause))
//                    mapClausePOStags.get(targetClause).add(posTag);
//                else
//                        mapClausePOStags.put(targetClause, new ArrayList(Arrays.asList(posTag)));
//
//                row_i = Collections.max(rows);
//                row_i++;
//            }
//            else
//            {
//
//                if (line.contains("SEN")) {
//                    //we have the sentence id
//                    String lineNb = line.split(" ")[1];
//                    System.out.println("\t\tline: " +  lineNb);
//                }
//                else //line contains "PAGE"
//                {
//                    //we have the page name
//                    System.out.println("PAGE: " +  line.split(" ")[1]);
//                }
//
//
//                if (row_i < 1)
//                    continue;
//                // We are done with the sentence, we actually assign values to the matrix
//                //I get the relation between seen clause types (NP, VP,...) and the columns index
//                Map<String, Integer> tuplesClauseColumn = clauseToIndices(new ArrayList<>(mapClausePOStags.keySet()));
////                cCols = tuplesClauseColumn
//                // We prepare a hashmap with the type of clause and the columns that correspond to each of them
//                //  {NP_:[0,3,4], VP:[2,1]}
//
//                for (Map.Entry<String, Integer> entry : tuplesClauseColumn.entrySet()) {
//                    String key = entry.getKey();
//                    Integer value = entry.getValue();
//                    if (mapSubClausesColumns.containsKey(key))
//                        mapSubClausesColumns.get(key).add(value);
//                    else
//                        mapSubClausesColumns.put(key, new ArrayList(Arrays.asList(value)));
//                }
//
//                for (String cl : clausesSeen)
//                {
//                    ArrayList<Integer> tempClauseColumn = new ArrayList<>();
//                    for (Map.Entry<String, ArrayList<String>> entry : mapClausePOStags.entrySet())
//                    {
//                        String key = entry.getKey();
//                        ArrayList<String> value = entry.getValue();
//                        if (key.contains(cl+ "_"))
//                        {
//                            tempClauseColumn.addAll(mapSubClausesColumns.get(key));
//                            if (cClauseSubClauseColumns.containsKey(cl))
//                                if (cClauseSubClauseColumns.get(cl).containsKey(value))
//                                    // We have the 'NP' first key, we have a second [DT,NN] key, now
//                                    // we add the index of the columns that have this last two infos
//                                    cClauseSubClauseColumns.get(cl).get(value).addAll(mapSubClausesColumns.get(key));
//                                else
//                                    cClauseSubClauseColumns.get(cl).put(value, new ArrayList(Arrays.asList(mapSubClausesColumns.get(key))));
//                            else
//                            {
//                                HashMap<ArrayList<String>, ArrayList<Integer>> tempMap = new HashMap<>();
//                                tempMap.put(value, new ArrayList(Arrays.asList(mapSubClausesColumns.get(key))));
//                                // TODO: No se que estoy metiendo aqui. Esta mal.
//                                cClauseSubClauseColumns.put(cl, tempMap);
//                            }
//
//                            //This is another way to initialize a list inside a dict if there is no key. Like a defaultdict
//                            // putIfabsent returns null if it does exist. otherwise it puts the key and value you indicate.
//                            if (matrix.clauseColumns.containsKey(cl))
//                                matrix.clauseColumns.get(cl).addAll(mapSubClausesColumns.get(key));
//                            else
//                                matrix.clauseColumns.put(cl,  new ArrayList(Arrays.asList(mapSubClausesColumns.get(key))));
//
//
//
////                            ArrayList<Integer> listTemp = matrix.clauseColumns.putIfAbsent(cl, tempClauseColumn);
////                            if (listTemp != null)
////                                listTemp.add(tuplesClauseColumn.get(key));
//                        }
//                    }
//                }
//                mapClausePOStags.clear();
//                clausesSeen.clear();
//            }
//        }
//
//        matrix.rows = rows;
//        matrix.cCols = cCols;
//
//        matrix.mapTokenRow = rowTokens;
//
//        matrix.cSubClausesColumns = mapSubClausesColumns;
//
//        matrix.cClauseSubClauseColumns = cClauseSubClauseColumns;
//        return matrix;
//
//    }

    private void makeMatrix(String pathFile) throws IOException, InvalidLengthsException {
        String file = readFile(pathFile, true);

        ///DECLARATIONS
        int level = 1 + 1;
        Map<String, Integer> lMapTokenRow = new HashMap<>();
        Map<String, String> lpreClause;
        ArrayList<String> pages = new ArrayList(Arrays.asList(file.split("%%#PAGE ")));
        if (pages.get(0).equals(""))
            pages.remove(0);

        for (String p : pages) {
            ArrayList<String> sentences = new ArrayList(Arrays.asList(p.split("%%#SEN ")));
            String pageTitle = sentences.get(0).trim();
            System.out.println("page: " + pageTitle);
            sentences.remove(0);

            for (String s : sentences) {
                ArrayList<String> lines = new ArrayList(Arrays.asList(s.split("\n")));
                String sentenceID = lines.get(0).trim();
                System.out.println("\t\tsentence: " + sentenceID);
                lines.remove(0);
                ArrayList<String> lClausesSeen = new ArrayList<>();
                ArrayList<String> lSubClausesSeen = new ArrayList<>();
                lpreClause = new HashMap<>();
                for (String l : lines) {
                    String[] splittedLine = l.split("\t");
                    String token = splittedLine[0];
                    String lemma = splittedLine[1];
                    String posTag = splittedLine[2];
                    String constituency = splittedLine[3];
                    String dependencyHead = splittedLine[4];
                    String dependency = splittedLine[5];
                    String token_pos = lemma + "_" + posTag;
                    if (dependency.equals("PUNCT"))
                        continue;
                    /***
                     * 1. Get the token and store it in a dictionary string:int, with its row index as value:
                     *      {"the_DT":0, "car_NN":1, ...}
                     *  1.a Add to the row list the current row i. rows[0,0,1,1...] for the i vector of the ijv sparse matrix
                     */
                    if (matrix.cMapTokenRow.containsKey(token_pos))
                        matrix.cRows.add(matrix.cMapTokenRow.get(token_pos));
                    else {
                        matrix.cMapTokenRow.put(token_pos, row_i);
                        matrix.cRows.add(row_i);
                        row_i++;
                    }

                    /**
                     * 2. Using the constituency results, we determine the clause level we are interested in (Level 1, 2,..).
                     *      The level is taken from the constituency string, starting from left to right.
                     *      The desired clause tag is stored in targetClause
                     */
                    ArrayList<String> clauses = new ArrayList(Arrays.asList(constituency.split(",")));
                    if (level > clauses.size())
                        level = clauses.size();
                    String targetClause = clauses.get(clauses.size() - level);
                    String clauseInitials = targetClause.split("_")[0];
                    lClausesSeen.add(clauseInitials);
                    lSubClausesSeen.add(targetClause);

                    /**
                     * 2.1 We get the tags of what constitutes the targetClause. Such that:
                     * a dictionary str:str with clauseTarget as key and a key describing its components as values:
                     * {"NP_18":"DET_NN_JJ', 'VP_70':'VBZ_NP',...}
                     *
                     *
                     */
                    //TODO: I could store this preClause structure for later use.
                    String tempPreClause;
                    if (clauses.size() >= level + 1) ///> There is actually a preClause identificator (e.g., a PRP for a NP)
                        tempPreClause = clauses.get(clauses.size() - (level + 1));
                    else
                        tempPreClause = posTag; ///> There is no preClause. We take the POS tag.

                    if (lpreClause.containsKey(targetClause)) {
                        if (!lpreClause.get(targetClause).contains(tempPreClause))
                            lpreClause.put(targetClause, lpreClause.get(targetClause) + ":" + tempPreClause);
                    } else
                        lpreClause.put(targetClause, tempPreClause);


                }// end of current line. Sentence is completely read.
                /**
                 * 3. Once the complete pass over the sentence is done, we get what represent each column.
                 * 3.1 We get the columns for each different type of clause. In a dict str:int. Keys are
                 * the clauses, columns are the values: {"NP_18": [1,3,5,7], "VP_70":[2,4], ...}
                 */
                if (lClausesSeen.isEmpty())
                    continue;
                Map<String, ArrayList<Integer>> lSubClausesColumns = clauseToIndices(lSubClausesSeen);
                for (Map.Entry<String, ArrayList<Integer>> entry : lSubClausesColumns.entrySet()) {
                    String clause = entry.getKey();
                    ArrayList value_j = entry.getValue();
                    matrix.cSubClausesColumns.get(clause).addAll(value_j);
                }

                if (lSubClausesColumns.size() != lpreClause.size())
                    throw new Utils.InvalidLengthsException("These lengths should be equal!!");

                /**
                 * 3.2 We get a dict str:dict<str:list<int>> to map for desired clause columns.
                 * Such as this: {"NP":{"DET_NN_JJ":[1,3,5,7]}}
                 */
                Set<String> lSetClausesSeen = new HashSet<>(lClausesSeen);
                for (String clause : lSetClausesSeen) {
                    for (Map.Entry<String, String> entry : lpreClause.entrySet()) {
                        String subClause = entry.getKey();
                        String subClauseComponents = entry.getValue();
                        if (subClause.contains(clause + "_"))
                            matrix.cClauseSubClauseColumns.get(clause).get(subClauseComponents).addAll(lSubClausesColumns.get(subClause));
                    }
                }
                column_j++;

            }
        }
        System.out.println();


    }
//    public  void run() throws InterruptedException {
//        long start = System.nanoTime();
//        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
//        ArrayList<String> listPaths = listFiles(pathFolder, "txt");
////        listPaths.add("/media/stuff/Pavel/Documents/Eclipse/workspace/javahello/data/AA/wiki_00");
//        for (String path : listPaths) {
//            System.out.println("still working...");
//            Runnable worker = new MatrixThread(path);
//            //worker will execute its "run" function
//            executor.execute(worker);
//        }
//        // This will make the executor accept no new threads
//        // and finish all existing threads in the queue
//        executor.shutdown();
//        // Wait until all threads are finish
//        executor.awaitTermination(10, TimeUnit.DAYS);
//        long time = System.nanoTime() - start;
//        System.out.println("Finished all threads");
//        System.out.printf("Tasks took %.3f m to run%n", time/(60*1e9));
//    }

    /**
     * This function runs the stanfordwiki2sparsematrix conversion in a single thread
     */
    public void runSingle() throws IOException, InvalidLengthsException {
        long start = System.nanoTime();
        ArrayList<String> listPaths = listFiles(pathFolder, "txt");
        /* big loop with for each file of the wiki stanford-parsed files*/
        int idx = 1;

        ///> This loops goes file by file
        for (String path : listPaths) {
            System.out.println(Integer.toString(idx) + ": " + path);
            //necesito aqui una funcion que me regrese  un objeto con todas las estructuras necesarias para armar la matriz
//            buildingMatrix = makeMatrix(path,buildingMatrix);
            this.makeMatrix(path);
            idx++;
        }


        long time = System.nanoTime() - start;
        System.out.println("Finished all threads");
        System.out.printf("Tasks took %.3f m to run%n", time / (60 * 1e9));

    }
}