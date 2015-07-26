import string
import matplotlib.pyplot as plt

f = open("../../../stats.txt")
frequency = []
probability = []

# get just the words frequency
for line in f:
    data = string.split(line,  " ")

    # ignore the last character (\n)
    frequency.append((int)(data[1]))
    probability.append((float)(data[2][:-1]))

# y = f(x) -> prob = f(freq)
plt.plot(frequency, probability, linestyle='---', marker='o', color='r')
# plt.scatter(frequency, probability)
plt.gca().invert_xaxis()
plt.ylabel("Probability")
plt.xlabel("Frequency")

# plt.show()
plt.savefig('graph.png', dpi=200)
