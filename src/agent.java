import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class agent {
	
	Long startTime;
	int maxTime;
	int taskType=0;
	int cutOffDepth=0;
	char player = '-';
	char[][] board = new char[8][8];
	int[][] evaluationBoard = 
		{
			{99,-8,8,6,6,8,-8,99},
			{-8,-24,-4,-3,-3,-4,-24,-8},
			{8,-4,7,4,4,7,-4,8},
			{6,-3,4,0,0,4,-3,6},
			{6,-3,4,0,0,4,-3,6},
			{8,-4,7,4,4,7,-4,8},
			{-8,-24,-4,-3,-3,-4,-24,-8},
			{99,-8,8,6,6,8,-8,99}
		};
	int POS_INFINITY = 9999999;
	int NEG_INFINITY = -9999999;
	char[] rowsChars = {'a','b','c','d','e','f','g','h'};
	
	List<String> outputLog = new ArrayList<String>();
	
	public int calculatePlayerValueOnBoard(char[][] board, char player){
		int value = 0;
		for(int i=0; i<8;i++){
			for(int j=0; j<8; j++){
				if(board[i][j]==player){
					value+=evaluationBoard[i][j];
				}
			}
		}
		
		return value;
	}
	
	//returns the score difference between player 1 and player 2
	//can be used to populate the evaluation function.
	public int evaluateScore(char[][] board, char player1){
		int score=0;
		int scoreP1 = calculatePlayerValueOnBoard(board, player1);
		int scoreP2 = calculatePlayerValueOnBoard(board,this.opponent(player1));
		score = scoreP1-scoreP2;
		return score;
	}
	
	public void GreedyAlg(){
		List<Move> moves = FindValidMoves(this.board, this.player);
		
		if(!moves.isEmpty()){
			//boards available at a level
			List<char[][]> boards = new ArrayList<char[][]>();
			for(Move move : moves){
				boards.add(makeMoveToPosition(this.board, move.xCoordinate, move.yCoordinate, this.player));
			}
			
			//List that stores the score of each board.
			List<Integer> scoresBoard = new ArrayList<Integer>();
			//find the score of each board
			for(int k=0; k<boards.size();k++){
				scoresBoard.add(this.evaluateScore(boards.get(k), this.player));
			}
			
			//find the maximum score
			int maxScore=findMaxScore(scoresBoard);
			
			//at a particular level find the board that has the max score.
			int index = 0;
			for(int i=0;i<scoresBoard.size();i++){
				if(maxScore == scoresBoard.get(i)){
					index = i;
					break;
				}
			}
			//best choice
			this.printBoardGreed(boards.get(index));
			//this.printBoard(boards.get(index));
			//System.out.println(maxScore);
		}
		//there are no moves available for the player
		else{
			//the player doesn't have any valid move, print the initial board as it is.
			this.printBoardGreed(this.board);
		}
	}
	
	public void MiniMaxAlg(){
		//Long startTime = System.nanoTime();
		this.outputLog.add("Node,Depth,Value");
		String node = "root";
		int depth = 0;
		this.prepareOutputForMiniMax(node, depth, NEG_INFINITY);
		
		//scores board at a particular level these contain the value of the board
		List<Integer> scoresBoard = new ArrayList<Integer>();
		//The available moves that player has at the beginning of the game
		//for board.
		List<Move> moves = FindValidMoves(this.board, this.player);
		if(!moves.isEmpty()){
			
			//boards at the first level
			List<char[][]> boards = new ArrayList<char[][]>();
			for(Move move : moves){
				char[][] tempBoard = copyBoard(this.board);
				
				char[][] boardAfterMove = this.makeMoveToPosition(tempBoard, move.xCoordinate, move.yCoordinate, this.player);
				boards.add(boardAfterMove);
				
				//feed this new obtained board to the algorithm and move and current depth and value.
				int boardValue = this.MiniMax_Value(boardAfterMove, move, depth+1, POS_INFINITY, this.player);
				scoresBoard.add(boardValue);
				
				//right the output for the root once it considers one child
				int val = this.findMaxScore(scoresBoard);
				this.prepareOutputForMiniMax(node, depth, val);
			}
			
			//select the max score
			int seletedValue = this.findMaxScore(scoresBoard);//select the move with the highest boardValue
			int index = 0;
			//find the board with the max score
			for(int i=0;i<scoresBoard.size();i++){
				if(seletedValue == scoresBoard.get(i)){
					index = i;
					break;
				}
			}
			//write the board and log to the output file
			printBoardMM(boards.get(index), this.outputLog);
			
			//print the best board in console
			//char[][] b = boards.get(index);
			//for(int i=0; i<8;i++){
			//	for(int j=0;j<8;j++){
			//		System.out.print(b[i][j]);
			//	}
			//	System.out.println();
			//}
			//print the output log in console
			//this.printArray(this.outputLog);
		}
		else{
			//the player doesn't have any valid move from the initial configuration.
			int numPlayerTiles = this.calculateNumOfTiles(this.board, this.player);
			//check if the player has tiles on the table
			if(numPlayerTiles!=0){
				List<Move> movesOpponent = FindValidMoves(this.board, this.opponent(player));
				//the opponent has valid moves pass the play
				if(movesOpponent.size()!=0){
					int boardValue = this.MiniMax_Value(this.board, null, depth+1, POS_INFINITY, this.player);
					scoresBoard.add(boardValue);
					int seletedValue = this.findMaxScore(scoresBoard);
					
					this.prepareOutputForMiniMax(node, 0, seletedValue);
					this.printBoardMM(this.board, this.outputLog);
				}
				//the opponent doesn't have a any valid moves
				else{
					int numOpponentTiles = this.calculateNumOfTiles(this.board, this.opponent(player));
					this.outputLog.clear();
					this.outputLog.add("Node,Depth,Value");
					int val = this.evaluateScore(this.board, this.player);
					//check if the opponent has some tiles on the board
					if(numOpponentTiles!=0){
						if(this.cutOffDepth>=2){
							this.prepareOutputForMiniMax(node, 0, NEG_INFINITY);
							this.prepareOutputForMiniMax("pass", 1, POS_INFINITY);
							this.prepareOutputForMiniMax("pass", 2, val);
							this.prepareOutputForMiniMax("pass", 1, val);
							this.prepareOutputForMiniMax(node, 0, val);
						}
						else{
							this.prepareOutputForMiniMax(node, 0, NEG_INFINITY);
							this.prepareOutputForMiniMax("pass", 1, val);
							this.prepareOutputForMiniMax(node, 0, val);
						}
					}
					//the opponent doesn't have any tiles, log score with root
					else{
						this.prepareOutputForMiniMax(node, 0, val);
					}
					this.printBoardMM(this.board, this.outputLog);
				}
			}
			//the initial player neither has moves nor has any tiles
			//log the current board score with root
			else{
				int val = this.evaluateScore(this.board, this.player);
				this.outputLog.clear();
				this.outputLog.add("Node,Depth,Value");
				this.prepareOutputForMiniMax(node, 0, val);
				this.printBoardMM(this.board, this.outputLog);
			}
		}
	}
	
	//recursively called Minimax method
	public int MiniMax_Value(char[][] board, Move move, int depth, int value, char player){
		String node="";
		if(move==null){
			node = "pass";
			this.prepareOutputForMiniMax(node, depth, value);
		}
		else{
			node = this.rowsChars[move.yCoordinate]+Integer.toString(move.xCoordinate+1);
			//if we have reached the cutoff depth or if none of the players have any valid moves, consider this as the
			//last layer and return from here.
			if(depth==this.cutOffDepth || this.GameOver(board)){
				//this might need to be changed if the evaluation function is dependent on 
				//whose going to make the current move. (this.opponent(player))
				int val = this.evaluateScore(board, this.player);
				this.prepareOutputForMiniMax(node, depth, val);
				return val;
			}
			this.prepareOutputForMiniMax(node, depth, value);
		}
		
		int bestValue;
		char[][] tempBoard = copyBoard(board);
		
		//scoresboard at a particular level contain the scores of boards
		List<Integer> scoresBoard = new ArrayList<Integer>();
		char currentPlayer = this.opponent(player);
		if(currentPlayer==this.player)
			bestValue = POS_INFINITY;
		else
			bestValue = NEG_INFINITY;
		
		//The available moves that the opponent to the current player has for the board.
		List<Move> moves = FindValidMoves(tempBoard, currentPlayer);
		if(!moves.isEmpty()){
			//available boards at a particular level
			List<char[][]> boards = new ArrayList<char[][]>();
			for(Move m : moves){
				char[][] tempBoard2 = copyBoard(tempBoard);
				
				char[][] boardAfterMove = this.makeMoveToPosition(tempBoard2, m.xCoordinate, m.yCoordinate, currentPlayer);
				boards.add(boardAfterMove);
				
				//feed this new obtained board to the algorithm and move and current depth and value.
				int boardValue = this.MiniMax_Value(boardAfterMove, m, depth+1, bestValue, currentPlayer);
				//add the value of this board to the scores board
				scoresBoard.add(boardValue);
				int val;
				if(currentPlayer==this.player)
					val = this.findMaxScore(scoresBoard);//see if the current obtained value or the max value needs to be selected
				else
					val = this.findMinScore(scoresBoard);
				
				this.prepareOutputForMiniMax(node, depth, val);
			}
			//select the highest/lowest score from the scores of board depending on the current player
			if(currentPlayer==this.player)
				return this.findMaxScore(scoresBoard);
			else
				return this.findMinScore(scoresBoard);
		}
		else{
			//the current player doesn't have any valid move from this configuration.
			//the other player makes the move now.
			if(currentPlayer==this.player)
				bestValue = POS_INFINITY;
			else
				bestValue = NEG_INFINITY;
			//might need to revisit this.
			// when the play is passed what should be the value passed
			//this call may be cleared and in case a player doesn't have a move in a particular branch,that,
			//should be considered as the cut-off condition and the heuristic value is returned.
			//return MiniMax_Value(board, null, depth+1, bestValue, currentPlayer);
			int boardValue = this.MiniMax_Value(board, null, depth+1, bestValue, currentPlayer);
			scoresBoard.add(boardValue);
			int val;
			if(currentPlayer==this.player)
				val = this.findMaxScore(scoresBoard);//see if the current obtained value or the max value needs to be selected
			else
				val = this.findMinScore(scoresBoard);
			
			this.prepareOutputForMiniMax(node, depth, val);
			
			if(currentPlayer==this.player)
				return this.findMaxScore(scoresBoard);
			else
				return this.findMinScore(scoresBoard);
		}
	}
	
	public void AlphaBetaAlg(){
		this.outputLog.add("Node,Depth,Value,Alpha,Beta");
		String node = "root";
		int depth = 0;
		int alpha = NEG_INFINITY;
		int beta = POS_INFINITY;
		this.preapreOutputForAlphaBeta(node, depth, NEG_INFINITY, NEG_INFINITY, POS_INFINITY);
		
		//scores board at a particular level these contain the value of the board
		List<Integer> scoresBoard = new ArrayList<Integer>();
		//The available moves that player has at the beginning of the game
		//for board.
		List<Move> moves = FindValidMoves(this.board, this.player);
		if(!moves.isEmpty()){
			//boards at the first level
			List<char[][]> boards = new ArrayList<char[][]>();
			for(Move move : moves){
				char[][] tempBoard = copyBoard(this.board);
				
				char[][] boardAfterMove = this.makeMoveToPosition(tempBoard, move.xCoordinate, move.yCoordinate, this.player);
				boards.add(boardAfterMove);
				
				//feed this new obtained board to the algorithm and move and current depth and value.
				int boardValue = this.AlphBetaMaxValue(boardAfterMove, move, depth+1, POS_INFINITY, this.player, alpha, beta);
				scoresBoard.add(boardValue);
				
				//right the output for the root once it considers one child
				int val = this.findMaxScore(scoresBoard);
				if(val > alpha){
					alpha = val;
				}
				this.preapreOutputForAlphaBeta(node, depth, val, alpha, beta);
				if(alpha >= beta)
					break;
			}
			
			//select the max score
			int seletedValue = this.findMaxScore(scoresBoard);//select the move with the highest boardValue
			int index = 0;
			//find the board with the max score
			for(int i=0;i<scoresBoard.size();i++){
				if(seletedValue == scoresBoard.get(i)){
					index = i;
					break;
				}
			}
			//print the board with the max score
			printBoardMM(boards.get(index), this.outputLog);
			
			//char[][] b = boards.get(index);
			//for(int i=0; i<8;i++){
			//	for(int j=0;j<8;j++){
			//		System.out.print(b[i][j]);
			//	}
			//	System.out.println();
			//}
			//this.printArray(this.outputLog);
		}
		else{
			//the player doesn't have any valid move from the initial configuration.
			//print the initial log of root and the initial board configuration
			//check if the other player too has no moves, if not then print the original board and calculate the first player's score and print
			int numPlayerTiles = this.calculateNumOfTiles(this.board, this.player);
			if(numPlayerTiles!=0){
				List<Move> movesOpponent = FindValidMoves(this.board, this.opponent(player));
				if(movesOpponent.size()!=0){
					int boardValue = this.AlphBetaMaxValue(this.board, null, depth+1, POS_INFINITY, this.player, alpha, beta);
					scoresBoard.add(boardValue);
					int seletedValue = this.findMaxScore(scoresBoard);
					this.preapreOutputForAlphaBeta(node, 0, seletedValue, seletedValue, POS_INFINITY);
					this.printBoardMM(this.board, this.outputLog);
				}
				else{
					int numOpponentTiles = this.calculateNumOfTiles(this.board, this.opponent(this.player));
					this.outputLog.clear();
					this.outputLog.add("Node,Depth,Value,Alpha,Beta");
					int val = this.evaluateScore(this.board, this.player);
					if(numOpponentTiles!=0){
						if(this.cutOffDepth>=2){
							this.preapreOutputForAlphaBeta(node, 0, NEG_INFINITY, NEG_INFINITY, POS_INFINITY);
							this.preapreOutputForAlphaBeta("pass", 1, POS_INFINITY, NEG_INFINITY, POS_INFINITY);
							this.preapreOutputForAlphaBeta("pass", 2, val, NEG_INFINITY, POS_INFINITY);
							this.preapreOutputForAlphaBeta("pass", 1, val, NEG_INFINITY, val);
							this.preapreOutputForAlphaBeta(node, 0, val, val, POS_INFINITY);
						}
						else{
							this.preapreOutputForAlphaBeta(node, 0, NEG_INFINITY, NEG_INFINITY, POS_INFINITY);
							this.preapreOutputForAlphaBeta("pass", 1, val, NEG_INFINITY, POS_INFINITY);
							this.preapreOutputForAlphaBeta(node, 0, val, val, POS_INFINITY);
						}
					}
					else{
						this.preapreOutputForAlphaBeta(node, 0, val, NEG_INFINITY, POS_INFINITY);
					}
					this.printBoardMM(this.board, this.outputLog);
				}
			}
			else{
				int val = this.evaluateScore(this.board, this.player);
				this.outputLog.clear();
				this.outputLog.add("Node,Depth,Value,Alpha,Beta");
				this.preapreOutputForAlphaBeta(node, 0, val, NEG_INFINITY, POS_INFINITY);
				this.printBoardMM(this.board, this.outputLog);
			}
		}
	}
	
	//recursively called alphbeta method
	public int AlphBetaMaxValue(char[][] board, Move move, int depth, int value, char player, int alpha, int beta){
		String node="";
		if(move==null){
			node = "pass";
			this.preapreOutputForAlphaBeta(node, depth, value, alpha, beta);
			
		}
		else{
			node = this.rowsChars[move.yCoordinate]+Integer.toString(move.xCoordinate+1);
			//if we have reached the cutoff depth or if none of the players have any valid moves, consider this as the
			//last layer and return from here.
			if(depth==this.cutOffDepth || this.GameOver(board)){
				//this might need to be changed if the evaluation function is dependent on 
				//who's going to make the current move. (this.opponent(player))
				int val = this.evaluateScore(board, this.player);
				this.preapreOutputForAlphaBeta(node, depth, val, alpha, beta);
				return val;
			}
			
			this.preapreOutputForAlphaBeta(node, depth, value, alpha, beta);
		}
		int bestValue;
		int alphaInner = alpha;
		int betaInner = beta;
		char[][] tempBoard = copyBoard(board);
		
		//scoresboard at a particular level contain the scores of boards
		List<Integer> scoresBoard = new ArrayList<Integer>();
		char currentPlayer = this.opponent(player);
		if(currentPlayer==this.player)
			bestValue = POS_INFINITY;
		else
			bestValue = NEG_INFINITY;
		
		//The available moves that the opponent to the current player has for the board.
		List<Move> moves = FindValidMoves(tempBoard, currentPlayer);
		if(!moves.isEmpty()){
			
			//available boards at a particular level
			List<char[][]> boards = new ArrayList<char[][]>();
			for(Move m : moves){
				char[][] tempBoard2 = copyBoard(tempBoard);
				
				char[][] boardAfterMove = this.makeMoveToPosition(tempBoard2, m.xCoordinate, m.yCoordinate, currentPlayer);
				boards.add(boardAfterMove);
				
				//feed this new obtained board to the algorithm and move and current depth and value.
				int boardValue = this.AlphBetaMaxValue(boardAfterMove, m, depth+1, bestValue, currentPlayer, alphaInner, betaInner);
				//add the value of this board to the scores board
				scoresBoard.add(boardValue);
				int val;
				if(currentPlayer==this.player){
					val = this.findMaxScore(scoresBoard);
					
					if(val>=betaInner){
						this.preapreOutputForAlphaBeta(node, depth, val, alphaInner, betaInner);
						break;
					}
					if(val > alphaInner){
						alphaInner = val;
					}
					this.preapreOutputForAlphaBeta(node, depth, val, alphaInner, betaInner);
					if(alphaInner>=betaInner)
						break;
				}
				else{
					val = this.findMinScore(scoresBoard);
					if( val <= alphaInner){
						this.preapreOutputForAlphaBeta(node, depth, val, alphaInner, betaInner);
						break;
					}
					
					if(val < betaInner){
						betaInner = val;
					}
					this.preapreOutputForAlphaBeta(node, depth, val, alphaInner, betaInner);
					if(alphaInner>= betaInner)
						break;
				}
			}
			//select the highest/lowest score from the scores of board depending on the current player
			if(currentPlayer==this.player)
				return this.findMaxScore(scoresBoard);
			else
				return this.findMinScore(scoresBoard);
		}
		else{
			int boardValue = this.AlphBetaMaxValue(board, null, depth+1, bestValue, currentPlayer, alphaInner, betaInner);
			scoresBoard.add(boardValue);
			int val;
			if(currentPlayer==this.player){
				val = this.findMaxScore(scoresBoard);
				if(val>=betaInner){
					this.preapreOutputForAlphaBeta(node, depth, val, alphaInner, betaInner);
				}
				else{
					if(val > alphaInner){
						alphaInner = val;
					}
					this.preapreOutputForAlphaBeta(node, depth, val, alphaInner, betaInner);
				}
				
			}
			else{
				val = this.findMinScore(scoresBoard);
				if( val <= alphaInner){
					this.preapreOutputForAlphaBeta(node, depth, val, alphaInner, betaInner);
				}
				else{
					if(val < betaInner){
						betaInner = val;
					}
					this.preapreOutputForAlphaBeta(node, depth, val, alphaInner, betaInner);
				}
			}
		}
		//select the highest/lowest score from the scores of board depending on the current player
		if(currentPlayer==this.player)
			return this.findMaxScore(scoresBoard);
		else
			return this.findMinScore(scoresBoard);
	}

	
	//list of moves that player can make.
	public List<Move> FindValidMoves(char[][] board, char player){
		//iterate over all positions to see
		//which positions are suitable to move to
		List<Move> moves = new ArrayList<Move>();
		for(int x=0; x<8; x++){
			for(int y=0; y<8; y++){
				if(canMoveToPositionXY(board, x, y, player)){
					moves.add(new Move(x,y));
				}
			}
		}
		return moves;	
	}
	
	//find opponent.
	public char opponent(char player){
		if(player=='X')
			return 'O';
		else
			return 'X';
	}
	
	//check if player can move to position x,y on board.
	public boolean canMoveToPositionXY(char[][]board, int x, int y, char player){
		
		if(board[x][y]!='*')
			return false;
		
		int xDir, yDir, xTemp, yTemp, distance, value=0;
		char opponent = opponent(player);
		
		for(xDir=-1; xDir<2; xDir++){
			for(yDir=-1; yDir<2; yDir++){
				if(xDir!=0 || yDir!=0){
					distance=1;
					xTemp = x + xDir;
					yTemp = y + yDir;
					while(isPositionXYOnBoard(xTemp, yTemp) && board[xTemp][yTemp]==opponent){
						distance++;
						xTemp+=xDir;
						yTemp+=yDir;
					}
					if(distance>1 && isPositionXYOnBoard(xTemp, yTemp) && board[xTemp][yTemp]==player){
						value+=distance-1;
					}
				}
			}
		}
		if(value>0)
			return true;
		
		return false;
	}
	
	//check is position x,y is one board or not.
	public boolean isPositionXYOnBoard(int x, int y){
		return (x>=0 && x<8 && y>=0 && y<8);
	}
	
	//return board after assigning player to position x,y.
	public char[][] movePlayerToPositionXY(char[][]board, int x, int y, char player){
		char[][] tempBoard = copyBoard(board);
		tempBoard[x][y] = player;
		return tempBoard;
	}
	
	//return board after player makes a move to x,y.
	//this step also takes care of flip-ing opponent positions if that's the case.
	public char[][] makeMoveToPosition(char[][] board, int x, int y, char player){
		char[][] tempBoard = copyBoard(board);
		tempBoard = movePlayerToPositionXY(tempBoard, x, y, player);
		
		int xDir, yDir, xTemp, yTemp, distance;
		char opponent = opponent(player);
		
		for(xDir=-1; xDir<2; xDir++){
			for(yDir=-1; yDir<2; yDir++){
				if(xDir!=0 || yDir!=0){
					distance=1;
					xTemp = x + xDir;
					yTemp = y + yDir;
					while(isPositionXYOnBoard(xTemp, yTemp) && board[xTemp][yTemp]==opponent){
						distance++;
						xTemp+=xDir;
						yTemp+=yDir;
					}
					if(distance>1 && isPositionXYOnBoard(xTemp, yTemp) && board[xTemp][yTemp]==player){					
						do
						{
							tempBoard = movePlayerToPositionXY(tempBoard, xTemp, yTemp, player);
							distance--;
							xTemp-=xDir;
							yTemp-=yDir;
						}while(distance>0);
					}
				}
			}
		}
		
		return tempBoard;
	}
	
	//make a copy of the board.
	public char[][] copyBoard(char[][] board){
		char[][] tempBoard = new char[8][8];
		for(int i=0;i<8;i++){
			for(int j=0;j<8;j++){
				tempBoard[i][j]=board[i][j];
			}
		}
		return tempBoard;
	}
 	
	public static void main(String[] args){
		List<String> inDocument = new ArrayList<String>();
		agent obj = new agent();
		try {
            Scanner sc = new Scanner(new File("input.txt"));
            while(sc.hasNextLine()) 
            {
               String next = sc.nextLine();
               inDocument.add(next);
            }
            sc.close();
        } 
        catch (FileNotFoundException ex) 
        {
        	System.out.println("make sure the name of the input file is input.txt and it is placed in the same directory as the .java and makefile files are");
        	return;
        }
		//obj.printArray(inDocument);
		
		//System.out.println("-------------------------------------------");
		
		obj.taskType =Integer.parseInt(inDocument.get(0));
		obj.player = inDocument.get(1).charAt(0);
		
		obj.board=populateInitialBoard(inDocument);
		
		switch(obj.taskType){
			case 1: 
				obj.cutOffDepth = Integer.parseInt(inDocument.get(2));
				obj.GreedyAlg();
			break;
			case 2: 
				obj.cutOffDepth = Integer.parseInt(inDocument.get(2));
				obj.MiniMaxAlg();
			break;
			case 3:
				obj.cutOffDepth = Integer.parseInt(inDocument.get(2));
				obj.AlphaBetaAlg();
			break;
			case 4: 
				//initial time=200s
				float timeRemaining = Float.parseFloat(inDocument.get(2));
				//based on some logic decide the depth  according to the time remaining and or the number of
				//pieces that the player and the opponent has.
				obj.startTime = System.currentTimeMillis();
				int numPlayerTiles = obj.calculateNumOfTiles(obj.board, obj.player);
				int numOpponentTiles = obj.calculateNumOfTiles(obj.board, obj.opponent(obj.player));
				List<Move> moves = obj.FindValidMoves(obj.board, obj.player);
				int remainingTiles = 64 - (numPlayerTiles+numOpponentTiles);
				int movesCount = moves.size();
				obj.cutOffDepth = 9;
				obj.maxTime = 5;
				if(timeRemaining >= 180.0){
					obj.cutOffDepth = 5;
					obj.maxTime = 4;
				}
				else if(timeRemaining >= 130.0){
					obj.cutOffDepth = 8;
					obj.maxTime = 5;
				}
				else if(timeRemaining >= 95.0){
					obj.cutOffDepth = 7;
					obj.maxTime = 5;
				}
				else if(timeRemaining >= 65.0){
					obj.cutOffDepth = 8;
					obj.maxTime = 4;
				}
				else if(timeRemaining >= 30.0){
					obj.cutOffDepth = 7;
					obj.maxTime = 3;
				}
				else{
					obj.cutOffDepth = 5;
					obj.maxTime = 2;
				}
				obj.AlphaBetaCompAlg();
				break;
		}
	}
	
	int [][] heuristicValues = 
		{
			{25, -4, 12, 8, 8, 12, -4, 25},
			{-4, -7, -4, 1, 1, -4, -7, -4},
			{12, -4, 2, 2, 2, 2, -4, 12},
			{8, 1, 2, -3, -3, 2, 1, 8},
			{8, 1, 2, -3, -3, 2, 1, 8},
			{12, -4, 2, 2, 2, 2, -4, 12},
			{-4, -7, -4, 1, 1, -4, -7, -4},
			{25, -4, 12, 8, 8, 12, -4, 25}
		};
	public int calculateNumOfTiles(char[][] board, char player){
		int tileCount=0;
		for(int i=0; i<8;i++){
			for(int j=0; j<8; j++){
				if(board[i][j]==player){
					tileCount++;
				}
			}
		}
		return tileCount;
	}
	
	//stronger evaluation function
	public double strongEvaluation(char[][] board, char player){
		int numPlayerTiles, numOpponentTiles;
		int numPlayerValidMoves, numOpponentValidMoves;
		double evaluatedScore = 0.0;
		//score form the heuristic board
		int evaluationBoardScore = this.evaluateScore(board, player);
		
		numPlayerTiles = this.calculateNumOfTiles(board, player);
		numOpponentTiles = this.calculateNumOfTiles(board, this.opponent(player));
		//score from the number of tiles of player on board, higher weight is preferred
		double numTilesValue = this.numTilesWeight(numPlayerTiles, numOpponentTiles);
		//score from the number of tiles on the corner, higher weight is preferred
		double cornerTilesValue = this.cornerTilesWeight(board, player);
		//score from number of tiles close to corner, not preferred
		double nearCornerTilesValue = this.nearCornerTilesWeight(board, player);
		
		numPlayerValidMoves = this.FindValidMoves(board, player).size();
		numOpponentValidMoves = this.FindValidMoves(board, this.opponent(player)).size();
		//score from the number of available valid moves
		double validMovesValue = this.numValidMovesWeight(numPlayerValidMoves,numOpponentValidMoves);
		
		evaluatedScore = 300*nearCornerTilesValue + 15*evaluationBoardScore + 15*numTilesValue + 110*validMovesValue+
				+ 900*cornerTilesValue;
		return evaluatedScore;
	}
	
	//number if tiles in the board
	public double numTilesWeight(int numPlayerTiles, int numOpponentTiles){
		double numTilesValue = 0.0;
		if(numPlayerTiles > numOpponentTiles)
			numTilesValue = (100*numPlayerTiles)/(numPlayerTiles+numOpponentTiles);
		else if(numPlayerTiles < numOpponentTiles)
			numTilesValue = -(100*numOpponentTiles)/(numPlayerTiles+numOpponentTiles);
		else
			numTilesValue = 0.0;
			
		return numTilesValue;
	}
	
	//corner positions are safe, very high points
	public double cornerTilesWeight(char[][] board, char player){
		int numPlayerTiles=0;
		double cornerTileValue=0.0;
		if(board[0][0]==player)
			numPlayerTiles++;
		else if(board[0][0]==this.opponent(player))
			numPlayerTiles--;
		if(board[0][7]==player)
			numPlayerTiles++;
		else if(board[0][7]==this.opponent(player))
			numPlayerTiles--;
		if(board[7][0]==player)
			numPlayerTiles++;
		else if(board[7][0]==this.opponent(player))
			numPlayerTiles--;
		if(board[7][7]==player)
			numPlayerTiles++;
		else if(board[7][7]==this.opponent(player))
			numPlayerTiles--;
		
		cornerTileValue = 30*numPlayerTiles;
		return cornerTileValue;
	}
	
	//count of how many tiles are are near the corners, this is a negative weight value as the opponent may
	//take advantage of this
	public double nearCornerTilesWeight(char[][]bard, char player){
		int numPlayerTiles = 0;
		double nearCornerTileValue = 0.0;
		if(board[0][0]=='*'){
			if(board[1][0]==player)
				numPlayerTiles++;
			else if(board[1][0]==this.opponent(player))
				numPlayerTiles--;
			if(board[0][1]==player)
				numPlayerTiles++;
			else if(board[0][1]==this.opponent(player))
				numPlayerTiles--;
			if(board[1][1]==player)
				numPlayerTiles++;
			else if(board[1][1]==this.opponent(player))
				numPlayerTiles--;
		}
		if(board[0][7]=='*'){
			if(board[1][6]==player)
				numPlayerTiles++;
			else if(board[1][6]==this.opponent(player))
				numPlayerTiles--;
			if(board[1][7]==player)
				numPlayerTiles++;
			else if(board[1][7]==this.opponent(player))
				numPlayerTiles--;
			if(board[0][6]==player)
				numPlayerTiles++;
			else if(board[0][6]==this.opponent(player))
				numPlayerTiles--;
		}
		if(board[7][0]=='*'){
			if(board[6][0]==player)
				numPlayerTiles++;
			else if(board[6][0]==this.opponent(player))
				numPlayerTiles--;
			if(board[6][1]==player)
				numPlayerTiles++;
			else if(board[6][1]==this.opponent(player))
				numPlayerTiles--;
			if(board[7][1]==player)
				numPlayerTiles++;
			else if(board[7][1]==this.opponent(player))
				numPlayerTiles--;
		}
		if(board[7][7]=='*'){
			if(board[6][6]==player)
				numPlayerTiles++;
			else if(board[6][6]==this.opponent(player))
				numPlayerTiles--;
			if(board[7][6]==player)
				numPlayerTiles++;
			else if(board[7][6]==this.opponent(player))
				numPlayerTiles--;
			if(board[6][7]==player)
				numPlayerTiles++;
			else if(board[6][7]==this.opponent(player))
				numPlayerTiles--;
		}
		nearCornerTileValue = -15*numPlayerTiles;
		return nearCornerTileValue;
	}
	
	//number of available moves compared to the opponent, desired value
	public double numValidMovesWeight(int numPlayerValidMoves,int numOpponentValidMoves){
		double validMovesValue = 0.0;
		if(numPlayerValidMoves>numOpponentValidMoves)
			validMovesValue = 100*numPlayerValidMoves/(numPlayerValidMoves-numOpponentValidMoves);
		else if(numPlayerValidMoves<numOpponentValidMoves)
			validMovesValue = 100*numOpponentValidMoves/(numPlayerValidMoves-numOpponentValidMoves);
		else
			validMovesValue = 0.0;
		return validMovesValue;
	}
	
 	public void AlphaBetaCompAlg(){
		Long startTime = System.nanoTime();
		int depth = 0;
		int alpha = NEG_INFINITY;
		int beta = POS_INFINITY;
		
		//scores board at a particular level these contain the value of the board
		List<Integer> scoresBoard = new ArrayList<Integer>();
		//The available moves that player has at the beginning of the game
		//for board.
		List<Move> moves = FindValidMoves(this.board, this.player);
		if(!moves.isEmpty()){
			//boards at the first level
			List<char[][]> boards = new ArrayList<char[][]>();
			for(Move move : moves){
				char[][] tempBoard = copyBoard(this.board);
				
				char[][] boardAfterMove = this.makeMoveToPosition(tempBoard, move.xCoordinate, move.yCoordinate, this.player);
				boards.add(boardAfterMove);
				
				//feed this new obtained board to the algorithm and move and current depth and value.
				int boardValue = this.AlphBetaCompMaxValue(boardAfterMove, move, depth+1, POS_INFINITY, this.player, alpha, beta);
				scoresBoard.add(boardValue);
				
				int val = this.findMaxScore(scoresBoard);
				if(val > alpha){
					alpha = val;
				}
				if(alpha >= beta)
					break;
			}
			
			//select the max score
			int seletedValue = this.findMaxScore(scoresBoard);//select the move with the highest boardValue
			int index = 0;
			//find the board with the max score
			for(int i=0;i<scoresBoard.size();i++){
				if(seletedValue == scoresBoard.get(i)){
					index = i;
					break;
				}
			}
			//output the best move here
			Move m = moves.get(index);
			String node = this.rowsChars[m.yCoordinate]+Integer.toString(m.xCoordinate+1);
			//System.out.println(node);
			//print the best move to the output file.
			this.printBoardComp(node);
			//char[][] b = boards.get(index);
			
			//for(int i=0; i<8;i++){
			//	for(int j=0;j<8;j++){
			//		System.out.print(b[i][j]);
			//	}
			//	System.out.println();
			//}
			
		}
		else{
			//there would always be a first move, this code should never be reached in the competition
		}
		Long endTIme = System.nanoTime();
		System.out.println((endTIme-startTime)/1000000000);
	}
	
	public int AlphBetaCompMaxValue(char[][] board, Move move, int depth, int value, char player, int alpha, int beta){
		//if we have reached the cutoff depth or if none of the players have any valid moves, consider this as the
		//last layer and return from here.
		//taking too much time get out of here
		
		if(depth==this.cutOffDepth || this.GameOver(board) || (System.currentTimeMillis() - this.startTime)/1000 > this.maxTime){
			int val = (int)this.strongEvaluation(board, this.player);
			return val;
		}
		int bestValue;
		int alphaInner = alpha;
		int betaInner = beta;
		char[][] tempBoard = copyBoard(board);
		
		//scoresboard at a particular level contain the scores of boards
		List<Integer> scoresBoard = new ArrayList<Integer>();
		char currentPlayer = this.opponent(player);
		if(currentPlayer==this.player)
			bestValue = POS_INFINITY;
		else
			bestValue = NEG_INFINITY;
		
		//The available moves that the opponent to the current player has for the board.
		List<Move> moves = FindValidMoves(tempBoard, currentPlayer);
		if(!moves.isEmpty()){
			
			//available boards at a particular level
			List<char[][]> boards = new ArrayList<char[][]>();
			for(Move m : moves){
				char[][] tempBoard2 = copyBoard(tempBoard);
				
				char[][] boardAfterMove = this.makeMoveToPosition(tempBoard2, m.xCoordinate, m.yCoordinate, currentPlayer);
				boards.add(boardAfterMove);
				
				//feed this new obtained board to the algorithm and move and current depth and value.
				int boardValue = this.AlphBetaCompMaxValue(boardAfterMove, m, depth+1, bestValue, currentPlayer, alphaInner, betaInner);
				//add the value of this board to the scores board
				scoresBoard.add(boardValue);
				if(currentPlayer==this.player){
					if(boardValue > alphaInner){
						alphaInner = boardValue;
					}
					if(alphaInner>=betaInner)
						break;
				}
				else{
					if(boardValue < betaInner){
						betaInner = boardValue;
					}
					if(alphaInner>= betaInner)
						break;
				}
			}
			//select the highest/lowest score from the scores of board depending on the current player
			if(currentPlayer==this.player)
				return this.findMaxScore(scoresBoard);
			else
				return this.findMinScore(scoresBoard);
		}
		else{
			int boardValue = this.AlphBetaCompMaxValue(board, null, depth+1, bestValue, currentPlayer, alphaInner, betaInner);
			scoresBoard.add(boardValue);
			//select the highest/lowest score from the scores of board depending on the current player
			if(currentPlayer==this.player)
				return this.findMaxScore(scoresBoard);
			else
				return this.findMinScore(scoresBoard);
		}
	}
	
	public static char[][] populateInitialBoard(List<String> initialData){
		char[][] board = new char[8][8];
		for(int i=0; i<8;i++){
			for(int j=0; j<8 ;j++){
				board[i][j] = initialData.get(i+3).charAt(j);
			}
		}
		return board;
	}
	
	//redundant
	//public void printArray(List<String> arrayList){
	//	
	//	for(int i=0; i < arrayList.size(); i++){
	//		System.out.println(arrayList.get(i));
	//	}
	//}

	public void printBoardGreed(char[][] board){
		try{
			File file = new File("output.txt");
			if(!file.exists()){
				file.createNewFile();
			}
			FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			
			for(int i=0; i<8; i++){
				for(int j=0;j<8;j++){
					bufferedWriter.write(board[i][j]);
				}
				bufferedWriter.newLine();
			}
			bufferedWriter.close();
		}catch(IOException e){
			System.out.println("this is embarrasing");
		}
	}
	
	//public void printBoard(char[][] board){
	//	for(int i=0; i<8; i++){
	//		for(int j=0;j<8;j++){
	//			System.out.print((board[i][j]));
	//		}
	//		System.out.println();
	//}
	//}
	
	//write the board and log to the output file
	public void printBoardMM(char[][] board, List<String> outputLog){
		try{
			File file = new File("output.txt");
			if(!file.exists()){
				file.createNewFile();
			}
			FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
						
			for(int i=0; i<8; i++){
				for(int j=0;j<8;j++){
					bufferedWriter.write(board[i][j]);
				}
				bufferedWriter.newLine();
			}
			
			for(String row : outputLog){
				bufferedWriter.write(row);
				bufferedWriter.newLine();
			}
			
			bufferedWriter.close();
		}catch(IOException e){
			System.out.println("this is embarrasing");
		}
	}
	
	//print best move for the competition.
	public void printBoardComp(String node){
		try{
			File file = new File("output.txt");
			if(!file.exists()){
				file.createNewFile();
			}
			FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.write(node);
			bufferedWriter.close();
		}catch(IOException e){
			System.out.println("this is embarrasing");
		}
	}
	
	//find the max score out of the available board scores
	public int findMaxScore(List<Integer> boardScores){
		
		int maxScore = NEG_INFINITY;
		for(int i=0; i<boardScores.size();i++){
			if(boardScores.get(i)>maxScore){
				maxScore = boardScores.get(i);
			}
		}		
		return maxScore;
	}
	
	//find the min score out of the available board scores
	public int findMinScore(List<Integer> boardScores){
		
		int minScore = POS_INFINITY;
		for(int i=0; i<boardScores.size();i++){
			if(boardScores.get(i)<minScore){
				minScore = boardScores.get(i);
			}
		}		
		return minScore;
	}
	
	//prepare the output log for minimax.	
	public void prepareOutputForMiniMax(String node, int depth, int value){
		String extValues = "";
		if(value == NEG_INFINITY)
			extValues = "-Infinity";
		else if(value == POS_INFINITY)
			extValues = "Infinity";
		else
			extValues = Integer.toString(value);
		
		String outputRow = node+","+depth+","+extValues;
		//System.out.println(outputRow);
		this.outputLog.add(outputRow);
	}
	
	//prepare output log for alpha beta.
	public void preapreOutputForAlphaBeta(String node, int depth, int value, int alpha, int beta){
		String extValues = "";
		if(value == NEG_INFINITY)
			extValues = "-Infinity";
		else if(value == POS_INFINITY)
			extValues = "Infinity";
		else
			extValues = Integer.toString(value);
		
		String extAlpha = "";
		if(alpha == NEG_INFINITY)
			extAlpha = "-Infinity";
		else if(alpha == POS_INFINITY)
			extAlpha = "Infinity";
		else
			extAlpha = Integer.toString(alpha);
		
		String extBeta = "";
		if(beta == NEG_INFINITY)
			extBeta = "-Infinity";
		else if(beta == POS_INFINITY)
			extBeta = "Infinity";
		else
			extBeta = Integer.toString(beta);
		
		String outputRow = node+","+depth+","+extValues+","+extAlpha+","+extBeta;
		//System.out.println(outputRow);
		this.outputLog.add(outputRow);
		
	}
	public boolean hasMoves(char[][] board, char player){
		 List<Move> moves = FindValidMoves(board, player);
		 if(!moves.isEmpty())
			 return true;
		 return false;
	}
	 
	public boolean GameOver(char[][] board){
		 return (!(this.hasMoves(board, this.player) || this.hasMoves(board, this.opponent(this.player))));
	}
	
	public int findMax(int a, int b){
		return a>b?a:b;
	}
	
	public int findMin(int a, int b){
		return a<b?a:b;
	}
}
class Move{
	public int xCoordinate;
	public int yCoordinate;
	
	public Move(int x, int y){
		this.xCoordinate = x;
		this.yCoordinate = y;
	}

}