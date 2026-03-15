
import java.util.HashMap;
import java.util.Map;

public class TranspositionTable {
	private final Map<Long, SearchState> table;
	public TranspositionTable(){
		table = new HashMap<>();
	}
	public SearchState getData(GameState gameState){
		SearchState state = table.get(gameState.generateZobristHash());
		return state;
	}
	public void setData(GameState gameState, int evaluation, int depth, Move bestMove){
		table.put(gameState.generateZobristHash(), new SearchState(evaluation, depth, bestMove));
	}
	public void updatedata(GameState gameState, int evaluation, int depth, Move bestMove){
		long hash = gameState.generateZobristHash();
		if (!table.containsKey(hash)){
			table.put(hash, new SearchState(evaluation, depth, bestMove));
			return;
		}
		SearchState entry = table.get(hash);
		if (entry.depth < depth){
			table.put(hash, new SearchState(evaluation, depth, bestMove));
		}
		if (!entry.hasBestMove){
			table.put(hash, new SearchState(evaluation, depth, bestMove));
		}
	}
	public boolean contains(GameState gameState, int depth){
		SearchState state = table.get(gameState.generateZobristHash());
		if (state == null) return false;
		return state.depth >= depth;
	}
	public int size(){
		return table.size();
	}
	public static class SearchState{
		public final int evaluation;
		public final int depth;
		public final int bestMove;
		public final boolean hasBestMove;
		public SearchState(int evaluation, int depth, Move bestMove){
			this.evaluation = evaluation;
			this.depth = depth;
			if (bestMove == null){
				hasBestMove = false;
				this.bestMove = 0;
			} else {
				hasBestMove = true;
				this.bestMove = bestMove.data;
			}
		}
	}
}
