#!/bin/bash

# testing script usage
if [ $1 == "-d" ]; then
    echo "Using default parameters."

    terrier_path="/Users/luiz/Desktop/SET/terrier-4.0/bin/"
    queries_file="/Users/luiz/Desktop/SET/terrier-4.0/queries/clef_queries.txt"
    index_path="/Users/luiz/Desktop/SET/terrier-4.0/corpus/clef/"
    result_file="/Users/luiz/Desktop/wow/result.txt"
    treceval_path="/Users/luiz/Desktop/SET/trec_eval.9.0/trec_eval"
    qrels_file="/Users/luiz/Desktop/SET/terrier-4.0/queries/clef_relevance.qrels"
    report_path="/Users/luiz/Desktop/wow/report.txt"

elif ["$#" -eq 7]; then
    terrier_path=$1
    queries_file=$2
    index_path=$3
    result_file=$4
    treceval_path=$5
    qrels_file=$6
    report_path=$7

elif [ "$#" -ne 7 ]; then
    echo "Utility script for retrieving with Terrier."
    echo "Parameters: <terrier-bin_path> <queries_file> <index_path>
        <result_file_output_path> <trec-eval_path> <qrels_file> <report_path>"
    echo "Flags:"
    echo "  -d use default parameters"
    echo "  -r to perform retrieval with Terrier (can only be used with -d)"

    exit 1
fi

# Step 1 - Running queries (optional)
if [ $2 == "-r" ]; then
    $terrier_path/trec_terrier.sh -r -Dterrier.index.path=$index_path \
    -Dterrier.index.prefix=data \
    -Dtermpipelines=Stopwords,PorterStemmer \
    -Dtrec.model=DirichletLM \
    -Dtrec.results.file=$result_file \
    -Dtrec.topics=$queries_file \
    -Dtrec.topics.parser=SingleLineTRECQuery \
    -Dtrec.querying.outputformat.docno.meta.key=filename
else
    echo "xxx"

fi

# Step 2 - Removing extra tags from output file
sed -i.bak 's/\/Volumes\/ext\/data\/team1\///g' $result_file
sed -i.bak 's/.html//g' $result_file

# Step 3 - Evaluation
$treceval_path $qrels_file $result_file > $report_path

more $report_path
echo "This report was saved at $report_path"
