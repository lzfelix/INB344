#!/bin/bash
cd /Users/luiz/Desktop/SET/terrier-4.0
terrier_dir="/Users/luiz/Desktop/SET/terrier-4.0"

for mu in 100 500 1000 1500 2000 2500 3000 3500 4000 5000 6000 7000 8000 9000 10000 11000 12000 13000 14000 15000
do
    echo "Computing LM baseline for mu = $mu"
        bin/trec_terrier.sh -r -Dterrier.index.path=$terrier_dir/var/index -Dterrier.index.prefix=data \
     -Dtermpipelines=Stopwords -Dtrec.model=DirichletLM -Dtrec.results.file=../results/terrier_ap8889_trec123_dir_mu_${mu}.txt \
     -Dtrec.topics=queries/queries.txt \
     -Dtrec.topics.parser=SingleLineTRECQuery -c $mu &
done
