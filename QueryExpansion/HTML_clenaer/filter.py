from bs4 import BeautifulSoup as bs
from os import listdir, getcwd
from os.path import join, isfile

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

    title = soup.title.string

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
    # I/O paths. Change here
    path = getcwd()
    out_path = "/Users/luiz/Desktop/soupOut"

    # counting files on the folder for progress measure
    total = len([name for name in listdir('.') if isfile(name)])

    count = 0
    for file in listdir(path):
        count += 1

        # This is a >WEAK< way to check extension, since this is a controlled environment,
        # I can ensure that it's going to work without resorting to external libs
        extension = file.split(".", 1)[1]
        if extension != "html":
            print("Skipped file %d -> it was .%s" % (count, extension))
            continue

        print "Cleaning file %d of %d" % (count, total)

        # Doing the dirty job...
        input_path = join(path, file)
        output_path = join(out_path, file)
        clean(input_path, output_path)
