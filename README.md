# DETROID Framework
...

# DETROID Chess Engine
A chess engine developed using the **DETROID Framework**. It uses a magic bitboard-based position representation and Zobrist hashing. Furthermore, it employs a PVS algorithm with quiescence search within an iterative deepening framework with aspiration windows; a transposition table, an evaluation hash table, <del>and a pawn table</del>; fractional search extensions for checks, <del>mate-threats</del>, single-replies, and recaptures; <del>IID</del>; null-move pruning, late move reductions, futility pruning, extended futility pruning, <del>razoring, and deep razoting</del>; MVV-LVA, SEE, killer heuristics, and relative history heuristics for move ordering; and finally, measurements of score-fluctuation levels, <del>the number of root move changes per depth</del>, and the time of the last root move change relative to the total time spent on the search to determine if a search time extension is worthwhile. The evaluation 'function' relies on piece-square tables; tapered evaluation; the pawn-king structure; king-pawn tropism determined by the average Manhattan distance of the king to enemy and friendly pawns relative to their numbers and weighted by whether they are passed, backward, or 'normal' pawns; bishop pair advantage; <del>knight advantage as a function of how crowded the board is</del>; tempo advantage; the number of pinned pieces weighted by their type; pawn-piece defense and attack; piece mobility; and <del>piece-</del> queen-king tropism determined by the average Chebyshev distance. The engine also supports Polyglot chess opening books.

It implements both the TunableEngine (and thus implicitly the UCIEngine) and the ControllerEngine interfaces. All the parameters that affect the engine's playing strength are defined as engine parameters. Its 540+ static evaluation parameters have been tuned for about 40 hours using an unfiltered FEN-file of about 8,000,000 positions from 60,000-70,000 games of self-play. Its search and engine control parameters have been tuned in two turns. First all the search and time management parameters, which made up 158 bits, had been tuned for about 4 weeks, then the hash size distribution and hash entry lifecycle parameters were tuned for another 1 week. All the struck through features in the paragraph above have been removed or disabled as tuning and/or testing have proved them to be counterproductive.

The engine offers the following UCI options:
* Hash [spin]: The hash size allocated for the transposition and evaluation tables in MB.
* ClearHash [button]: Clears the hash.
* Ponder [check]: Whether pondering is allowed by the engine.
* OwnBook [check]: Whether the engine should use its opening book.
* PrimaryBookPath [string]: The path to the primary Polyglot opening book.
* SecondaryBookPath [string]: The path to the secondary Polyglot opening book; if the position is not found in the primary book, the engine will look for it in the secondary book.
* ParametersPath [string]: The path to the XML file containing the values for all parameters.
* UCI_Opponent [string]: The name of the opponent.
