package com.company.stanford2matrix;

import java.util.*;

/**
 * Created by pavel on 19/05/15.
 */
public class JSONGraphContainer {
    /**
     * Contains a lighter version of MatrixContainer. Represents a matrix as a list of dicts, where each
     * element will be a vertex of the graph, and each attribute will be a field of said node.
     * It contains another structure which is a  list of dictionaries that contain the edges and the
     * weight of each one of them
     */
    public ArrayList<Map<String, String>> vertices; ///> Contains row indices
    public ArrayList<Map<String, Integer>> edges; ///> Contains columns indices


    public JSONGraphContainer(MatrixContainer matrix) {
        /**
         * Create the JSON representation of the vertice and edge Dato dataframe (two lists of maps)
         */
        vertices = new ArrayList<>();
        edges = new ArrayList<>();
        Set<Integer> uniqueRows = new HashSet<>(matrix.cRows);
        Set<Integer> uniqueCols = new HashSet<>(matrix.cCols);
        int numRows = matrix.cRows.size();
        Map<String, String> vertexTemporalMap = new HashMap<>();
        Map<String, Integer> edgeTemporalMap = new HashMap<>();
        int maxRows = Collections.max(matrix.cRows);

        /// Get the required maps to build the vertices dataframe
        Map<Integer, ArrayList<String>> columnSubclause = columnToNode(matrix.cColumnSubClause, maxRows, "phrase");
        Map<Integer, ArrayList<String>> columnDependency = columnToNode(matrix.cColumnDependency, maxRows, "dependency");
        Map<Integer, ArrayList<String>> columnSentence = columnToNode(matrix.cColumnSentence, maxRows, "sentence");

        Map<Integer, String> rowToken = matrix.cRowToken;
        Map<Integer, String> rowPOS = matrix.cTokenPOS;

        /// Join the maps containing column (hyperedges) info
        Map<Integer, ArrayList<String>> columnsData = new HashMap<>();
        columnsData.putAll(columnSubclause);
        columnsData.putAll(columnDependency);
        columnsData.putAll(columnSentence);

        for (int i : uniqueRows) {
            vertexTemporalMap.put("id", Integer.toString(i));
            vertexTemporalMap.put("type", "token");
            vertexTemporalMap.put("POS_tag", rowPOS.get(i));
            vertexTemporalMap.put("data", rowToken.get(i));
            /// Add them to the list
            vertices.add(new HashMap<>(vertexTemporalMap));


        }
        int new_j;
        for (int j : uniqueCols) {
            new_j = j + maxRows;
            vertexTemporalMap.put("id", Integer.toString(new_j));
            vertexTemporalMap.put("type", columnsData.get(new_j).get(1));
            vertexTemporalMap.put("POS_tag", "NA");
            vertexTemporalMap.put("data", columnsData.get(new_j).get(0));
            /// Add them to the list
            vertices.add(new HashMap<>(vertexTemporalMap));

        }

        /// Now we create the edges dataframe
        for (int e = 0; e < numRows; e++) {
            edgeTemporalMap.put("__src_id", matrix.cRows.get(e));
            edgeTemporalMap.put("__dst_id", matrix.cCols.get(e) + maxRows);
            edgeTemporalMap.put("weight", matrix.cData.get(e));

            edges.add(new HashMap<>(edgeTemporalMap));
        }

    }

    public Map<Integer, ArrayList<String>> columnToNode(Map<Integer, String> columnMap, int offset, String type) {
        Map<Integer, ArrayList<String>> newColumnMap = new HashMap<>();
        for (Map.Entry<Integer, String> entry : columnMap.entrySet()) {
            Integer key = entry.getKey();
            String value = entry.getValue();
            newColumnMap.put(key + offset, new ArrayList<>(Arrays.asList(value, type)));
        }

        return newColumnMap;
    }
}

