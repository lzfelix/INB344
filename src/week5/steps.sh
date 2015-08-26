# maps the collection. This will create the following files:
#   collection mapping: etc/collection.spec
#   indexing files: var/index (flags only -- I guess)
# to change these, use --Dcollection.spec=<path> and -Dterrier.index.path=<path>
./bin/trec_setup.sh corpus/TREC

# This replaces the configuration file on etc/terrier.properties by
# a default file. Clobs it an replaces by customised file to perform
# batch TREC search. To change these configs, edit terrier.customproperties
rm -f etc/terrier.properties
cp etc/terrier.custom etc/terrier.properties

# clears previous indexing. Without this Terrier won't carry on indexing
rm -f var/index/*

# creates new indexing using inverted index
./bin/trec_terrier.sh -i

echo run retriaval with .bin/trec_terrier -r
