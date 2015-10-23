# CLEF Health 2015 - Tools

This folder contains queries, some utility scripts discussed on the main readme, the results obtained after running the software using the configurations described on the report, *trec_eval* itself and a python statistics utility script.

## Queries

| File name | Details |
|-----------|---------|
| clef_queries | The original queries used on the CLEF task |
| clef_chv_queries | The original queries expanded using only the CHV vocabulary |
| clef_expanded_queries | Queries expanded using *short* policy |
| clef_expanded_queries2 | Queries expaned using *long* policy |
| clef_lgqueries | Expansion performed using Log-policy |
| clef_relevance | The relevance assessment file, used with *trec_eval* |

## Results

The obtained results, described on the report are stored on the ```results.zip```.

## Utility scripts

The shell scripts are used on post-retrieval to perform evaluation, for more information refer to the main readme. On the other hand, ```stats.py``` is used to obtain simple statistics about the query files. You'll need Python 2.x to run it.

## Trec eval

It's bundled with the repository for convenience. To download the latest version, visit http://trec.nist.gov/trec_eval/.
