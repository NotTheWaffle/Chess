
public class TranspositionTable {
	public static final byte EXACT = 0;
	public static final byte LOWER_BOUND = 1;
	public static final byte UPPER_BOUND = 2;

	private static final int TABLE_SIZE = 1 << 20; // ~1M entries
	private final SearchState[] table;

	public TranspositionTable(){
		table = new SearchState[TABLE_SIZE];
	}
	public SearchState probe(long hash){
		SearchState state = table[(int)(hash & (TABLE_SIZE - 1))];
		if (state != null && state.hash == hash) return state;
		return null;
	}
	public void store(long hash, int evaluation, int depth, Move bestMove, byte boundType){
		int index = (int)(hash & (TABLE_SIZE - 1));
		SearchState existing = table[index];
		if (existing == null || existing.hash != hash || existing.depth <= depth) {
			table[index] = new SearchState(hash, evaluation, depth, bestMove, boundType);
		}
	}
	public void clear(){
		java.util.Arrays.fill(table, null);
	}

	public static class SearchState{
		public final long hash;
		public final int evaluation;
		public final int depth;
		public final int bestMove;
		public final boolean hasBestMove;
		public final byte boundType;
		public SearchState(long hash, int evaluation, int depth, Move bestMove, byte boundType){
			this.hash = hash;
			this.evaluation = evaluation;
			this.depth = depth;
			this.boundType = boundType;
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
