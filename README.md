<<<<<<< HEAD
# inb344
Repository for the Search Engine Technology unit.
More details will be added as soon as the repository is expanded.

For now you can check the Javadoc documentation [here](http://lzfelix.github.io/inb344/).
=======
# CLEF Health 2015

*This folder contains all the code developed while working on the assignment. The report is on ```report/```, while all the developed code is on  ```QueryExpansion/```. This readme just discusses how to install and use the project, it doesn't discusses technical aspects related to IR. For this kind of information, please refer to the report.*

## Organisation

The files on ```QueryExpansion/``` are divided as follows:

* **CHV:** The code used to perform Query Expansion based on the Consumer Health Vocabulary (available on http://consumerhealthvocab.org/)
* **HTML_cleaner:** Python script that uses BeautifulSoup and LXML to parse the original HTML files from the corpus. Further instructions on this folder's ```README.md```.
* **src:** Constains the Java code developed atop of Terrier. It contains the Dirichlet-JM Language Model, EMIM and CHV Query Expansions and Mu-Tunning. You can find more information about the code on ```doc/``` or on the report.
* **doc:** Javadoc for the files on ```src/```.
* **bin:** Binaries generated by Eclipse when compiling the core Java code.
* **libs:** Contains all external dependencies used on this code, including Terrier jars.
* **tools:** This folder contains the original queries, the relevance assessment file and three utilities scripts, which are discussed on upcoming sections.

## Getting started

The basic workflow consists in removing the HTML tags from the original corpus, generating a new collection. Following, this collection is indexed using Terrier.

On the other hand, expansion is performed on the original queries using the developed code, then retrieval can be performed either by using the developed code, or Terrier. If the first approach is chosen, then use ```tools/retrieve_terrier.sh``` to remove the extra meta tags appended when querying (this is needed in order to evaluate using the provided *.qrels* file). This script automatically evaluates the retrieval result.

If querying with Terrier is chosen, then use the same script, but with the flags ```-d -r```. For a further discussion comparing these decisions, please refer to the report on ```report/```.

### Removing the HTML tags from the corpus

To do so, run ```HTML_cleaner/filter.py```. In order to do so, you'll need Python 2.x, BeautifulSoup and lxml. These dependencies can be resolved though pip using ```pip install beautifulsoup4``` and ```pip install lxml```.

This script takes two parameters, the corpus collection path and the cleaned collection destination. You can set the origin path as destination, this will cause Python to replace the HTML files on the fly, although this is not recommended.

If a HTML file is too long, causing BeautifulSoup to throw a ```tooDeepStack``` exception, its parsing will fail. On this case, the script will keep this file ID (its sequential number) and resume the parsing from the next one. At the end of the operation, a list of problematic files is displayed.

### Indexing with Terrier

To index run the command on ```tools/index_terrier.sh```. You'll need to setup the parameters according to your environment propertly. No bash script was written to do so because this step is completed just once and most of the command onsists on parameters that depends on your configurations.

### Retrieving from the index

You can index either by using the script ```tools/retrieve_terrier.sh``` or using the Java code (though the class ```useTerrier.java```). For details, plese see the report.

**TO DO:** improve this.
>>>>>>> 6e7c4ccc135408a3ae6f04221b179a49a1d7a6e6
