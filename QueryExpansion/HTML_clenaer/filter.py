# Dependencies:
# Python 2.x
# beautifulsoup4
# lxlm
# (All available though pip)

from bs4 import BeautifulSoup as bs
from os import listdir, getcwd
from os.path import join, isfile

import sys

def clean(input_file, output_file):
    """
    Given an HTML file at <input_file>. This function removes the head (but
    keeps the title), script, noscript, meta, table, li, ul, link, form, style,
    img tags and all its contents.

    Following, the informatino that is not displayed to the user when the page is
    redered is discarded. The output then consists in a single line cleaned text.
    """

    # tags that are removed
    targets = ["head", "script", "noscript", "meta", "table", "li", "ul", "link", "form", "style", "img"]

    file = open(input_file, "r")
    soup = bs(file, "lxml")
    file.close()

    try:
        title = soup.title.string.encode("UTF-8")
    except:
        title = ""


    # removing all target tags and its contents
    [tag.decompose() for tag in soup(targets)]

    # Bypassing issue.
    # For some reason after the previous operation, the Beautiful Soup object
    # isn't able to render the text when using .stripped_strings anymore. It
    # doesn't make sense, as it is still possible to print the file normally
    # (although generating a big number of line breaks). To fix this, store the
    # obtained data so far as plain text and then reinitialises the BS object.
    plain_html = soup.encode("utf-8")
    soup = bs(plain_html, "lxml")

    # Appending all the lines of text in a list, removing the Unicode encoding
    digest = []
    for text in soup.stripped_strings:
        digest.append(text.encode("UTF-8"))

    # join the lists on a single line. Each list is separated by a space (may result
    # in multiple spaces if a list element already ends with space)
    out = open(output_file, "w")
    out.write(title + "\n")
    out.write(' '.join(digest))


if __name__ == "__main__":

    if len(sys.argv) == 1:
        print 'Usage: filter.py <corpus_collection_path> <cleaned_collection_destination_path>'
        sys.exit(2)
    # I/O paths. Change here
    path = sys.argv[1]
    out_path = sys.argv[2]

    # counting files on the folder for progress measure
    total = len([name for name in listdir(path) if isfile(name)])

    for file in listdir(path):
        count += 1

        # This is a >WEAK< way to check extension, since this is a controlled environment,
        # I can ensure that it's going to work without resorting to external libs
        extension_separator = file.rfind('.')
        splitted_extension = [file[:extension_separator - 1], file[extension_separator + 1:]]

        if len(splitted_extension) < 2:
            print("Skipped file %d -> it had no extension (A folder maybe?)." % (count))
            continue

        extension = splitted_extension[1]
        if extension != "html":
            print("Skipped file %d -> it was .%s" % (count, extension))
            continue

        # print "Cleaning file %d of %d" % (count, total)
        #

        if count % 100 == 0
            print('Cleaned files %d to %d' % (count - 100, count))

        # Doing the dirty job...
        input_path = join(path, file)
        output_path = join(out_path, file)
        clean(input_path, output_path)
