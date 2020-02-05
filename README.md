## Overview

This small projects makes a list of the most visually similar character for a given character. This is done by comparing the character decompositions (such as radicals) of two characters. For instance, 汪, 拄, and 柱 could be considered visually similar to 注. 

This decomposition is obtained by parsing the CJK decomposition [[1]](#1) recursively until a radical from a specified list is hit. There are other decompositions available which could also be tried such as the Wikimedia ones.

Specifically, the current heuristic in the main branch checks how many identical subcomponents there are, with the position within the character as an additional factor. For example, the characters 陪 and 部 have exactly the same characters but the components are transposed horizontally, so this is deducted from their similarity rating. I experimented with weighing in how rare a subcomponent was (e.g. 單 is probably less common than 月 so if it is contained in both characters it should have a higher weight) but preliminary results were not promising. Additionally, pairs where one is a simplification of the other one receive near-perfect scores, like 难 and 難, based on a mapping table [[2]](#2).

A potential use case would be a learning app where similar characters known to the user are displayed. I wrote a small plugin for Anki for myself, for example.


## Usage

This is a Java codebase with the following command-line options:

* `-m` Should be `create` if you want to make a new file with similar characters
* `-d` CJK decomposition file, e.g. `src/main/resources/cjk-decomp.txt`
* `-r` Radicals to stop the decomposition, e.g. `src/main/resources/chinese-radicals.csv`
* `-o` Output filename
* `-c` How many similar characters will be included
* `-t` Number of threads
* `-j` Simplified <-> traditional mapping, e.g. `src/main/resources/kanji-mapping-table.txt`
* `--restrict` Only include characters found in this document

The computation may take a while for all CJK characters, about 8 minutes on my machine using 8 threads. Using the `restrict` parameter results in faster runtimes, obviously.

The 50 most similar characters for each CJK character and the 100 most similar for some more common characters are precomputed for your convenience (located in `output`).

## Possible Improvements

There is a small file with testcases to evaluate the rankings included already (`src/main/resources/similarity-testcases.csv`), but having more well thought out and comprehensive evaluation testcases would be desirable (even though this is highly subjective).

## References

* <a id="1">[1]</a> 
[CJK Decomposition](https://github.com/amake/cjk-decomp) which splits about 80,000 Chinese and Japanese characters into subcombonents

* <a id="2">[2]</a> 
Chenhui Chu, Toshiaki Nakazawa and Sadao Kurohashi:
Chinese Characters Mapping Table of Japanese, Traditional Chinese and Simplified Chinese.
In Proceedings of the Eighth Conference on International Language Resources and Evaluation 
(LREC2012), pp.2149-2152, Istanbul, Turkey, (2012.5).
