"""
Utility script used to find some statistics about the queries such as average
maximum and minimum query length.
Luiz Felix (2015 - QUT 2nd Semester - Search Engines Technology)
"""

queries = open("clef_queries.txt", "r")
size = 0
amount = 0
biggest = 0
smallest = 999   # this is enough

for line in queries:
    query_len = len(line.split(' ')) - 1
    size += query_len
    amount += 1

    if query_len > biggest:
        biggest = query_len
    if query_len < smallest:
        smallest = query_len


print("Biggest query = %d" % biggest)
print("Smallest query = %d" % smallest)
print("Average query size = %.3f" % (size / amount))
print("%d queries" % amount)
