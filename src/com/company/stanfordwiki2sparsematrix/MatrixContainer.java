package com.company.stanfordwiki2sparsematrix;

import com.company.stanfordwiki2sparsematrix.Utils.DefaultDict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by pavel on 28/01/15.
 */
public class MatrixContainer {
    /**
     * This class contains the necessary arrays to hold a sparse matrix along with the information of its
     * rows and columns (what do each row and column actually means). The sparse matrix is stored in a COO-matrix,
     * that is, the data[i] is stored in (rows[i], cCols[i]), so we have three integer arrays.
     */
    //Sparse matrix container
    public ArrayList<Integer> cRows; ///> Contains row indices
    public ArrayList<Integer> cCols; ///> Contains columns indices
    public ArrayList<Integer> data; ///> Contains the data

    //Labels containers
    public HashMap<Integer, String> indicesColumns; ///> Contains the col index and the type it corresponds to.
    public DefaultDict<String, HashSet<Integer>> cSubClausesColumns; ///> Contains the rows'indices for each type of POS tag
    public HashMap<String, ArrayList<Integer>> clauseColumns; ///> Contains the rows'indices for each type of clause
    public Map<String, Integer> cMapTokenRow; ///> Contains the col index and the token it corresponds to.
    /**
     * Contains the type of column there are (NP, VP, PRP, MODIF, ...) and the subtype, if present, ("DT_JJ_NN","DT_NN",etc).
     * Then the columns indices that correspond to each case. Something like {'NP':{"DT_NN":[1,2,3]}, ...}
     */
    public DefaultDict<String, DefaultDict<String, ArrayList<Integer>>> cClauseSubClauseColumns;


    /**
     * Simple constructor
     */
    public MatrixContainer() {
        //Matrix information
        cRows = new ArrayList<>();
        data = new ArrayList<>();
        cCols = new ArrayList<>();
        //Labels information

        cMapTokenRow = new HashMap<>();
        indicesColumns = new HashMap<>();
        cClauseSubClauseColumns = new DefaultDict<>(DefaultDict.class);
        cSubClausesColumns = new DefaultDict<>(HashSet.class);
        clauseColumns = new HashMap<>();

    }


    public MatrixContainer(ArrayList<Integer> cRows, ArrayList<Integer> cCols, ArrayList<Integer> data,
                           DefaultDict<String, Integer> mapTokenRow, HashMap<Integer, String> indicesColumns,
                           DefaultDict<String, DefaultDict<String, ArrayList<Integer>>> cClauseSubClauseColumns,
                           DefaultDict<String, HashSet<Integer>> cSubClausesColumns) {
        //Matrix information
        this.cRows = cRows;
        this.data = data;
        this.cCols = cCols;
        //Labels information

        this.cMapTokenRow = mapTokenRow;
        this.indicesColumns = indicesColumns;
        this.cClauseSubClauseColumns = cClauseSubClauseColumns;
        this.cSubClausesColumns = cSubClausesColumns;
    }

    /**
     * This function converts the columns labels into a single label separated with underscores.
     *
     * @return Column label info concatenated
     */
    public String infoToText() {

        return "a";
    }

}
