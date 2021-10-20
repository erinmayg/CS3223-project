# CS3223 AY20/21 Sem 2 Project</br>(Database Systems Implementation)

This project is based on [SimpleDB](http://www.cs.bc.edu/~sciore/simpledb/), a multi-user transactional database server written in Java. SimpleDB is developed by Edward Sciore (Boston College), and the code is an integral part of a textbook Database Design and Implementation (Second Edition) he published with Springer. You should be able to access an e-copy from the NUS Library.

Unlike a full-fledged DBMS like MySQL or PostgreSQL, SimpleDB is intended as a teaching tool to facilitate the learning experience of a database system internal course. As such, the system implements only the basic functionalities of a “complete” database system. For example, it supports a very limited subset of SQL (and JDBC) and algorithms, offers little or no error checking, and is not designed for optimal performance/efficiency. However, the code is very well structured so that it is relatively easy to learn, use and extend SimpleDB.

## Goals

This project will focus primarily on query processing (and related topics, e.g., parser, indexing, etc) for a single user. Disk/memory management, transaction management (concurrency control and logging), failures (recovery management), multi-user setting are not considered.

### Team members :
* [Erin May Gunawan](https://github.com/erinmayg/)
* [Florencia Martina](https://github.com/florenciamartina/)
* [Quek Wei Ping](https://github.com/qweiping31415)

### Project Summary

This is an implementation of a simple SPJ (Select-Project-Join) query engine to illustrate query processing in a modern relational database management system. 
The link to our full report can be found [here](https://docs.google.com/document/d/1zTLLBD346INGFGFi3JtAiVPXt5IBuiUmO-1BVinwiK8/edit?usp=sharing).

### Implemented Features
1. [Block Nested Loop Join](https://github.com/erinmayg/CS3223-project/blob/master/src/qp/operators/BlockNestedLoopJoin.java)
2. [Sort Merge Join](https://github.com/erinmayg/CS3223-project/blob/master/src/qp/operators/SortMergeJoin.java) based on Sort
3. [Distinct](https://github.com/erinmayg/CS3223-project/blob/master/src/qp/operators/Distinct.java) based on Sort
4. [GroupBy](https://github.com/erinmayg/CS3223-project/blob/master/src/qp/operators/GroupBy.java) based on Sort 
5. [OrderBy](https://github.com/erinmayg/CS3223-project/blob/master/src/qp/operators/OrderBy.java) based on Sort
6. Identified and fixed the following bugs/limitations in the SPJ engine given:
* If the table's tuple size is bigger than the buffer size, SPJ goes to infinity loop.
* If the query does not involve join, the SPJ does not require the user to input the number of buffers.
* If a join query involves more than one join condition on two same tables, the number of tuples is higher than expected.

_Click [here](https://www.comp.nus.edu.sg/~tankl/cs3223/project.html) for more details regarding the project requirement._ 
