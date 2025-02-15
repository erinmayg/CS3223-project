/**
 * prepares a random initial plan for the given SQL query
 **/

package qp.optimizer;

import qp.operators.*;
import qp.utils.*;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.*;


public class RandomInitialPlan {

    SQLQuery sqlquery;

    ArrayList<Attribute> projectlist;
    ArrayList<String> fromlist;
    ArrayList<Condition> selectionlist;   // List of select conditons
    ArrayList<Condition> joinlist;        // List of join conditions
    ArrayList<Attribute> groupbylist;
    ArrayList<Attribute> orderbylist;
    int numJoin;            // Number of joins in this query

    int numDistinctTableJoins;

    HashMap<String, Operator> tab_op_hash;  // Table name to the Operator
    Operator root;          // Root of the query plan tree

    public RandomInitialPlan(SQLQuery sqlquery) {
        this.sqlquery = sqlquery;
        projectlist = sqlquery.getProjectList();
        fromlist = sqlquery.getFromList();
        selectionlist = sqlquery.getSelectionList();
        joinlist = sqlquery.getJoinList();
        groupbylist = sqlquery.getGroupByList();
        orderbylist = sqlquery.getOrderByList();

        numJoin = joinlist.size();
    }

//    /**
//     * number of join conditions
//     **/
//    public int getNumJoins() {
//        return numJoin;
//    }

    public int getNumJoins() {
        return numDistinctTableJoins;
    }


    /**
     * prepare initial plan for the query
     **/
    public Operator prepareInitialPlan() {

        tab_op_hash = new HashMap<>();
        createScanOp();
        createSelectOp();

        if (numJoin != 0) {
            createJoinOp();
        }

        if (sqlquery.getGroupByList().size() > 0) {
            createGroupByOp();
        }

        if (sqlquery.getOrderByList().size() > 0) {
            createOrderByOp();
        }

        createProjectOp();

        if (sqlquery.isDistinct()) {
            createDistinctOp();
        }

        return root;
    }

    /**
     * Create Scan Operator for each of the table
     * * mentioned in from list
     **/
    public void createScanOp() {
        int numtab = fromlist.size();
        Scan tempop = null;

        for (String tabname : fromlist) {  // For each table in from list
            Scan op1 = new Scan(tabname, OpType.SCAN);
            tempop = op1;

            /** Read the schema of the table from tablename.md file
             ** md stands for metadata
             **/
            String filename = tabname + ".md";
            try {
                ObjectInputStream _if = new ObjectInputStream(new FileInputStream(filename));
                Schema schm = (Schema) _if.readObject();
                op1.setSchema(schm);
                _if.close();
            } catch (Exception e) {
                System.err.println("RandomInitialPlan:Error reading Schema of the table " + filename);
                System.err.println(e);
                System.exit(1);
            }
            tab_op_hash.put(tabname, op1);
        }

        if (selectionlist.size() == 0) {
            root = tempop;
            return;
        }

    }

    /**
     * Create Selection Operators for each of the
     * * selection condition mentioned in Condition list
     **/
    public void createSelectOp() {
        Select op1 = null;
        for (Condition cn : selectionlist) {
            if (cn.getOpType() == Condition.SELECT) {
                String tabname = cn.getLhs().getTabName();
                Operator tempop = (Operator) tab_op_hash.get(tabname);
                op1 = new Select(tempop, cn, OpType.SELECT);
                /** set the schema same as base relation **/
                op1.setSchema(tempop.getSchema());
                modifyHashtable(tempop, op1);
            }
        }

        /** The last selection is the root of the plan tre
         ** constructed thus far
         **/
        if (selectionlist.size() != 0)
            root = op1;
    }

    public void createGroupByOp() {
        int nOfBuffer = BufferManager.getNumberOfBuffer();

        String tabname = fromlist.get(0);
        GroupBy op = new GroupBy(root, nOfBuffer, groupbylist);
        op.setSchema(root.getSchema());
        root = op;

    }

    /**
     * create join operators that executes multiple join conditions relating to same pair of tuples
     **/
    public void createJoinOp() {

        Join jn = null;

        //Create adjacency list
        HashMap<String, ArrayList<Condition>> tableToConditionsMap = new HashMap<>();
        for (Condition cn: joinlist) {

            Condition storedCondition = cn;
            String lefttab = cn.getLhs().getTabName();
            String righttab = ((Attribute) cn.getRhs()).getTabName();

            if (lefttab.compareTo(righttab) > 0) {
                storedCondition = cn.getFlippedCondition();
                String temp = lefttab;
                lefttab =  righttab;
                righttab = temp;
            }

            String key = lefttab + "-" + righttab;
            if (!tableToConditionsMap.containsKey(key)) {
                tableToConditionsMap.put(key, new ArrayList<Condition>());
            }
            ArrayList<Condition> value = tableToConditionsMap.get(key);
            value.add(storedCondition);
        }

        int id = 0;
        //Create the joins consisting of many conditions
        for (String leftRightTable : tableToConditionsMap.keySet()) {
            String[] arrOfStr = leftRightTable.split("-", 2);
            String leftTable = arrOfStr[0];
            String rightTable = arrOfStr[1];
            Operator left = (Operator) tab_op_hash.get(leftTable);
            Operator right = (Operator) tab_op_hash.get(rightTable);
            ArrayList<Condition> conditions = tableToConditionsMap.get(leftRightTable);
            jn = new Join(left, right, conditions, OpType.JOIN);
            jn.setNodeIndex(id);
            Schema newsche = left.getSchema().joinWith(right.getSchema());
            jn.setSchema(newsche);

            /** randomly select a join type**/
            int numJMeth = JoinType.numJoinTypes();
            int joinMeth = RandNumb.randInt(0, numJMeth - 1); // default
            jn.setJoinType(joinMeth);
            modifyHashtable(left, jn);
            modifyHashtable(right, jn);
        }


        /** The last join operation is the root for the
         ** constructed till now
         **/
        if (numJoin != 0)
            root = jn;
    }



    public void createProjectOp() {
        Operator base = root;
        if (projectlist == null)
            projectlist = new ArrayList<>();
        if (!projectlist.isEmpty()) {
            root = new Project(base, projectlist, OpType.PROJECT);
            Schema newSchema = base.getSchema().subSchema(projectlist);
            root.setSchema(newSchema);
        }
    }

    public void createDistinctOp() {
        Operator op;
        String tabname = fromlist.get(0);
        Schema newSchema;
        int nOfBuffer = BufferManager.getNumberOfBuffer();

        if (projectlist == null) {
            op = new Distinct(root, nOfBuffer, tabname);
            op.setSchema(root.getSchema());
            root = op;
        }

        if (!projectlist.isEmpty()) {
            op = new Distinct(root, nOfBuffer, projectlist, tabname);
            op.setSchema(root.getSchema());
            root = op;
        }

    }

    public void createOrderByOp() {
        int nOfBuffer = BufferManager.getNumberOfBuffer();
        System.out.println(orderbylist.toString());
        String tabname = fromlist.get(0);
        OrderBy op = new OrderBy(root, nOfBuffer, orderbylist, sqlquery.isAsc());
        op.setSchema(root.getSchema());
        root = op;
    }

    private void modifyHashtable(Operator old, Operator newop) {
        for (HashMap.Entry<String, Operator> entry : tab_op_hash.entrySet()) {
            if (entry.getValue().equals(old)) {
                entry.setValue(newop);
            }
        }
    }
}
