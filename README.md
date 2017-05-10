# DETROID Framework [![Build Status](https://travis-ci.org/ViktorC/DETROID.svg?branch=master)](https://travis-ci.org/ViktorC/DETROID)
A chess engine framework that handles the UCI protocol, provides a chess GUI, and serves as an engine parameter optimization framework. An engine only needs to implement the interface [*TunableEngine*](http://viktorc.github.io/DETROID/net/viktorc/detroid/framework/tuning/TunableEngine.html) to be usable as a UCI chess engine, the search engine of the GUI, and to be tunable. As the framework includes the [*Detroid*](http://viktorc.github.io/DETROID/net/viktorc/detroid/framework/engine/Detroid.html) chess engine, which is by default used as the controller engine for the GUI and for tuning, other engines using the framework do not have to implement the [*ControllerEngine*](http://viktorc.github.io/DETROID/net/viktorc/detroid/framework/validation/ControllerEngine.html) interface. The [*ApplicationFramework*](http://viktorc.github.io/DETROID/net/viktorc/detroid/framework/ApplicationFramework.html) itself is a [*Runnable*](https://docs.oracle.com/javase/8/docs/api/java/lang/Runnable.html) class whith a single two-parameter constructor. The first parameter is an [*EngineFactory*](http://viktorc.github.io/DETROID/net/viktorc/detroid/framework/EngineFactory.html) instance which is responsible for creating instances of a *TunableEngine* and a *ControllerEngine*. This is where *Detroid* is used as the *ControllerEngine* by default to avoid having to implement the interface to tune the engine, use it in the GUI, or use it as a UCI engine. The second parameter is an array of strings for program arguments which define the behaviour of the framework. To launch the framework, an instance of *ApplicationFramework* needs to be constructed and run in the main method of the application. The following sections describe the features of the framework and present the usage of the program arguments. The complete Javadoc of the framework can be found [here](http://viktorc.github.io/DETROID/).

### GUI ###
If the framework is run without program arguments, it defaults to a JavaFX GUI mode. The GUI uses a *ControllerEngine* instance to keep the game state and check proposed moves for legality, and it uses a [*UCIEngine*](http://viktorc.github.io/DETROID/net/viktorc/detroid/framework/uci/UCIEngine.html) instance to search the positions and propose moves. Both instances are created by the provided *EngineFactory*. The GUI offers several useful functionalities for the testing and debugging of the engine used for searching. It has a table for real time search statistics, a chart for the search result scores returned over the course of the game, a debug console, a dialog for the UCI options provided, and a demo mode which has the engine play against itself. Furthermore, it supports pondering, time control settings, [FEN](https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation) and [PGN](https://en.wikipedia.org/wiki/Portable_Game_Notation) parsing and extraction, changing sides, and undoing moves.

### UCI ###
The UCI mode handles the [Universal Chess Interface](http://wbec-ridderkerk.nl/html/UCIProtocol.html) protocol for the *UCIEngine* (*TunableEngine* extends *UCIEngine*) instance created by the provided *EngineFactory*.  
**Usage:** `-u`

### Tuning ###
Perhaps the most important feature of the framework is its parameter tuning support. Chess engines using the framework are expected to implement the *TunableEngine* interface. This interface requires them to use a subclass of [*EngineParameters*](http://viktorc.github.io/DETROID/net/viktorc/detroid/framework/tuning/EngineParameters.html) to define the paremeters to tune by annotating the fields with the [*Parameter*](http://viktorc.github.io/DETROID/net/viktorc/detroid/framework/tuning/Parameter.html) annotation. Only primitives are allowed to be marked as parameters. The parameters are not allowed to take on values below 0, thus the most significant bit of all signed integers and floating point types are ignored. The *Parameter* annotation takes two optional arguments, the [*ParameterType*](http://viktorc.github.io/DETROID/net/viktorc/detroid/framework/tuning/ParameterType.html) and a byte value, binaryLengthLimit, that limits the number of bits considered when tuning. The type is used to specify whether a parameter is a static evaluation parameter, a search control parameter, or an engine management parameter; the significance of this will be explained in the next paragraphs. The default type is static evaluation. The binaryLengthLimit can be used to restrict the number of values to consider when tuning if the maximum value the parameter can or should take on is known and it is smaller than the maximum value of its primitive type. This can speed up the tuning process.

Two different parameter optimization methods are supported by the framework. The first one is a [Population-based Incremental Learning](http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.61.8554) algorithm with a self-play based fitness function inspired by Thomas Petzke's [work](http://macechess.blogspot.co.at/2013/03/population-based-incremental-learning.html) on his chess engine [ICE](http://www.fam-petzke.de/cp_ice_en.shtml). It can be used to tune static evaluation parameters, search control parameters, engine management parameters, different combinations of these, or all. Its mandatory parameters are the population size, the number of games the engine should play against itself, and the time control for the games in milliseconds. The optional parameters are the type of parameters to tune ('eval', 'control', 'management', 'eval+control', 'control+management', or 'all') which defaults to 'all'; the time incerement to use for time control in milliseconds, 0 by default; the validation factor which determines the factor of the original number of games played to play in addition in case a parameter set is found to be the fittest of its generation, by default 0; a flag, by deafault false, denoting whether the OwnBook parameter of the engine, if it exists, should be set to true; the number of MBs the hash size of the engine should be set to if it supports the corresponding UCI option; the initial probability vector which can be set to continue the tuning process from a certain generation by taking the probability vector logged for that generation; the log file path, by default "log.txt"; and the number of processors to use, by defualt 1 and maximum the number of available logical cores divided by two or 1 if only 1 core is available. Using a high level of concurrency can be detrimental to the quality of the optimization results; it is not recommended to use a value higher than the number of available physical cores divided by two.  
**Usage:** `-t selfplay -population 100 -games 100 -tc 2000 --paramtype control --inc 10 --validfactor 0.5 --trybook true --tryhash 8 --initprobvector "0.9, 0.121, 0.4" --log my_log.txt --concurrency 2`

The other optimization method is a stochastic gradient descent algorithm with [Nesterov-accelerated Adaptive Moment Estimation](http://cs229.stanford.edu/proj2015/054_report.pdf) using the [Texel](https://chessprogramming.wikispaces.com/Texel's+Tuning+Method) cost function. It can only be applied to static evaluation parameter optimization, but it is a lot more efficient at that than the evolutionary algorithm based method. However, this requires a so called FEN-file which contains labelled data entries consisting of a position in FEN and the result of the game the position occurred in as the label. This tuning method's only mandatory parameter is the sample size which determines the number of data entries to use for training per epoch. The optional parameters are K, a constant used in the cost function calibrated to achieve the lowest costs, if it is not set, it is calibrated before the tuning begins; the base learning rate which determines the initial step size of the gradient descent and by default is 1; the path to the FEN-file, by default "fens.txt"; the log file path, by default "log.txt"; and the number of processors to use, by defualt 1. In the case of this optimization method, parallelism cannot have an effect on the quality of the results, thus it is recommended to use the number of available physical cores as the value of concurrency.  
**Usage:** `-t texel -samplesize 16000 --k 0.54 --learningrate 2 --fensfile my_fens.txt --log my_log.txt --concurrency 4`

The framework provides two different ways to generate a FEN-file for static evaluation tuning. The first way is self-play. With the exception of one, all parameters and their descriptions can be found in the paragraph describing the self-play based optimization method. The only novel parameter is the target path for the FEN-file which defaults to "fens.txt". Positions arising from book moves or positions in which the engines found a mate are not logged to the FEN-file. For short time controls (below 2s), concurrency is not recommended to have a value greater than the number of available physical cores divided by two.  
**Usage:** `-g byselfplay -games 60000 -tc 2000 --inc 10 --trybook true --tryhash 8 --destfile my_fens.txt --concurrency 2`

The other way is PGN conversion. This requires a PGN file of chess games which can then be converted to a FEN-file. The only mandatory parameter is the file path to the PGN file. The optional parameters are the maximum number of games from the PGN file to convert, and the file path of the generated FEN-file.  
**Usage:** `-g bypgnconversion -sourcefile my_pgn.pgn --maxgames 50000 --destfile my_fens.txt`

The generated FEN-files can also be filtered to possibly improve the optimization results. For example, all the positions and their labels from drawn games can be removed from the FEN-file. The file path to the source FEN-file is a mandatory parameter, while the destination file path is optional and defaults to "fens.txt".  
**Usage:** `-f draws -sourcefile old_fen.txt --destfile new_fen.txt`

Another way to filter FEN-files is removing the first *X* positions from each game. The only new parameter defines the value of *X*; the others are the same as above.  
**Usage:** `-f openings -sourcefile old_fen.txt --firstxmoves 8 --destfile new_fen.txt`

Last but not least, the outputs of the two optimization methods logged in their log files can be converted into XML files containing the optimized values of the parameters. The PBIL algorithm logs the probability vector of each generation. This can be converted into an XML file by specifying the value argument using the probability vector from the log file. The other two optional parameters are the type of the parameters to convert which defaults to 'all' and the destination path for the XML file which defaults to "params.xml". The type should be the same as what was used for optimization.  
**Usage:** `-c probvector -value "0.9, 0.121, 0.4" --paramtype control --paramsfile my_params.xml`

The static evaluation parameter optimization method using the Texel cost function logs the values of the optimized parameters as an array of decimals called 'features'. It can be converted to an XML file almost exactly as described above, but using the features instead to specify the value argument.  
**Usage:** `-c features -value "124.12357, 5.02345, 2.98875" --paramsfile my_params.xml`

# DETROID Chess Engine
A chess engine developed using the **DETROID Framework**. It uses a magic bitboard-based position representation and [Zobrist](https://en.wikipedia.org/wiki/Zobrist_hashing) hashing. Furthermore, it employs a PVS algorithm with quiescence search within an iterative deepening framework with aspiration windows and lazy SMP; a transposition table, an evaluation hash table, <del>and a pawn table</del>; fractional search extensions for checks, pushed pawns, <del>mate-threats</del>, single-replies, and recaptures; IID; adaptive null-move pruning, adaptive late move reductions, futility pruning, razoring; staged move generation, MVV-LVA, SEE, killer heuristics, and relative history heuristics for move ordering; and finally, measurements of score-fluctuation levels and the time of the last root move change relative to the total time spent on the search to determine if a search time extension is worthwhile. The evaluation 'function' relies on piece-square tables; tapered evaluation; the pawn-king structure; king-pawn tropism determined by the average Manhattan distance of the king to enemy and friendly pawns relative to their numbers and weighted by whether they are passed, backward, or 'normal' pawns; bishop pair advantage; <del>knight advantage as a function of how crowded the board is</del>; tempo advantage; the number of pinned pieces weighted by their type; pawn-piece defense and attack; piece mobility; and <del>piece-king</del> queen-king tropism determined by the average Chebyshev distance. All the chess heuristics employed are based on articles on [chessprogramming.wikispaces.com](https://chessprogramming.wikispaces.com). The engine also supports [Polyglot](http://wbec-ridderkerk.nl/html/details1/PolyGlot.html) chess opening books.

*Detroid* implements both the *TunableEngine* (and thus implicitly the *UCIEngine*) and the *ControllerEngine* interfaces. All the parameters that affect the engine's playing strength are defined as engine parameters. Its 540+ static evaluation parameters have been optimized for 2 days using an unfiltered FEN-file of about 8,000,000 positions from roughly 60,000 games of self-play. Its search and engine control parameters have been tuned in two turns. First all the search control and time management parameters, which made up 158 bits, had been optimized for 4 weeks on an 8-core [Google Compute Engine](https://cloud.google.com/compute) instance, then the hash size distribution and hash entry lifecycle parameters, another 15 bits, were tuned for another 5 days with longer time controls. All the struck through features in the paragraph above have been removed or disabled as tuning and/or testing have proven them counterproductive.

The engine offers the following UCI options:
* **Hash [spin]**: The hash size allocated for the transposition and evaluation tables in MB.
* **ClearHash [button]**: Clears the hash.
* **Ponder [check]**: Whether pondering is allowed by the engine.
* **OwnBook [check]**: Whether the engine should use its opening book.
* **PrimaryBookPath [string]**: The path to the primary Polyglot opening book.
* **SecondaryBookPath [string]**: The path to the secondary Polyglot opening book; if the position is not found in the primary book, the engine will look for it in the secondary book.
* **SearchThreads [spin]**: The number of parallel threads to use for searching.
* **ParametersPath [string]**: The path to the XML file containing the values for all parameters.
* **UCI_Opponent [string]**: The name of the opponent.

On an i7-4710HQ @2.50GHz CPU, perft to a depth of 6 in the starting position takes the engine 4.9s with staged move generation and the Zobrist hashing of each position, it solves 277 out of the 300 positions of [Win at Chess](https://chessprogramming.wikispaces.com/Win+at+Chess) in 1s per position, and the average search speed of the engine when searching the starting position to a depth of 17 using one thread is around 650 knps.
