# Terrier variables: http://terrier.org/docs/v3.6/properties.html
#run this script on the root folder of Terrier
#(01-09-2015) Search Engine Technology @ QUT 2nd Semester

#||==========================||
#|| Variables                ||
#||==========================||

#the location of the file containing the queries on the format [id query\n]
queryFile="/Users/luiz/Desktop/SET/terrier-4.0/queries/queries.txt"

#the folder that will be created containing index and query results
outputFolder="/Users/luiz/Desktop/SET/terrier-4.0/processing"

#path to the corpus folder
corpusFolder="/Users/luiz/Desktop/SET/terrier-4.0/corpus/TREC"

trecEvalBinary="/Users/luiz/Desktop/SET/trec_eval.9.0/trec_eval"

relevanceFile="/Users/luiz/Desktop/SET/terrier-4.0/queries/relevance.qrels"

#used to create the zipfile to be posted on GitHub
tempFolderName="output"

#||==========================||
#|| Collection mapping       ||
#||==========================||

# removes the .DS_Store file from the collection folder, so it won't be indexed
rm $corpusFolder/.DS_Store

# maps the collection. This will create the following files:
#   - collection mapping: etc/collection.spec
#   - indexing files: var/index (flags only -- I guess)
# to change these, use --Dcollection.spec=<path> and -Dterrier.index.path=<path>
./bin/trec_setup.sh corpus/TREC

# Replaces the standard terrier.properties (that isn't configured for TREC
# collection on my case) to a modified file prepared for TREC.
rm -f etc/terrier.properties
cp etc/terrier.custom etc/terrier.properties

#||==========================||
#|| Index generation         ||
#||==========================||

# creates the processing folder
mkdir $outputFolder

# creates one folder for each pipeline option and builds the index

modes=('PorterStemmer' 'Stopwords' 'PorterStemmer,Stopwords' '')
folders=('porter' 'stop' 'stopNporter' 'nofilter')
len=${#folders[@]}      # this finds the size of the previous arrays
#
# for ((i=0; i<$len; i++));
# do
#     mkdir -p $outputFolder/${folders[i]}/index
#     ./bin/trec_terrier.sh -i -Dterrier.index.path=$outputFolder/${folders[i]}/index -Dtermpipelines=${modes[i]}
# done


#||==========================||
#|| Querying                 ||
#||==========================||

#performs the document retrieval using TF, TF_IDF, LemurTF_IDF and BM25 weighting methods
#on each generated index

modes=('Tf' 'TF_IDF' 'LemurTF_IDF' 'BM25')
filenames=('tf' 'tf_idf' 'lemur' 'bm25')

for ((i=0; i<$len; i++));
do
    # #create the folders to store the querying results
    mkdir $outputFolder/${folders[i]}/queries

    # retrieve documents using different methods
    for ((j=0; j<$len; j++));
    do
        ./bin/trec_terrier.sh -r -Dtrec.results.file=$outputFolder/${folders[i]}/queries/${filenames[j]}.txt \
        -Dterrier.index.path=$outputFolder/${folders[i]}/index -Dtermpipelines=PorterStemmer \
        -Dterrier.index.prefix=data -Dtrec.topics=$queryFile -Dtrec.model=${modes[j]}
    done
done


#||==========================||
#|| Evaluating               ||
#||==========================||

#evaluates the results, which were stored on the previous phase

#iterating over folders
for ((i=0; i<$len; i++))
do
    path=$outputFolder/${folders[i]}/queries

    #iterating over results file using different weighting methods
    for ((j=0; j<$len; j++))
    do
        $trecEvalBinary $relevanceFile $path/${filenames[j]}'.txt' > $path/${filenames[j]}'_evaluation.txt'
    done
done


#||==========================||
#|| Packing                  ||
#||==========================||

mkdir $tempFolderName

#iterating over folders
for ((i=0; i<$len; i++))
do
    mkdir $tempFolderName/${folders[i]}

    #iterating over results file using different weighting methods
    for ((j=0; j<$len; j++))
    do
        path=$outputFolder/${folders[i]}/queries

        cp $path/${filenames[j]}'.txt' $tempFolderName/${folders[i]}/${filenames[j]}'_retrieved.txt'
        cp $path/${filenames[j]}'_evaluation.txt' $tempFolderName/${folders[i]}/${filenames[j]}'_evaluation.txt'
    done
done

#create the zipfile
zip -r -9 -m 'output.zip' $tempFolderName/

#deleting the temporary folder
rm -rf $zipFolderPath
