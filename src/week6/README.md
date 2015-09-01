# Week 6 Activities

To generate the indexes and evaluate the queries result, the general configurations 
(such as type of document and query parser) were added to the terrier.properties file and all
other parameters were coded on the shellscript `index_generator.sh`.

The obtained results (index and performance assessment) are on the zip file `results.zip` and
divided according to the pipeline used on indexing. Files ending with `_retrieved` contain the
list of files retrieved using the privded queries and files ended with `_evaluation` corresponds
to the trec\_eval report.
