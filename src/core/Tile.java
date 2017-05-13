package core;

public class Tile {

	//gameplay:
	public int type;
	/*0-27: properties, 28: chance, 29: community chest, 30: go, 31: jail, 32: free parking, 33: go to jail, 34: income tax, 35: luxury tax
	 * (these don't reach 40 because there are multiple chance and community chest spots)
	 */
	public int number; //0: Go, 1: Mediterranean Ave., etc.

	//statistics:
	public double probability; //proportion of turns that ended on a certain tile (cumulative)
	public int count; //count of turns that ended on a certain tile (each game)

	public Tile(String packedTiles){
		String subString = "";

		for (int clause = 0; clause < 2; clause++){
			subString = packedTiles.substring(0, packedTiles.indexOf('\t'));
			subString.trim();

			switch(clause) {
			case 0:
				number = Integer.parseInt(subString);
				break;
			case 1:
				type = Integer.parseInt(subString);
				break;
			}

			packedTiles = packedTiles.substring(packedTiles.indexOf('\t') + 1);
		}
	}

}
