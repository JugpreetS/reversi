# reversi

Best move predictor in Reversi.

Starting with a board position, this program builds a search tree of all the possible board positions that can be played
between a player and the opponent and suggests the best possible move based on heuristics. It uses Min-Max game playing 
algorithm to make this decision, coupled with Alpha-Beta pruning to limit the search space to relevant search branches.

