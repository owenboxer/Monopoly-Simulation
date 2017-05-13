package core;

import java.util.*;

public class Board {

	static Scanner in = new Scanner(System.in);

	public static Tile tile[] = new Tile[40]; //there are 40 tiles (spaces)
	public static Property property[] = new Property[28]; //there are 28 properties

	public static int playerQuantity = 1, currentPlayer; //shows other classes which player is currently going
	public static Player player[] = new Player[playerQuantity];
	public static int turn = 0;

	public static Card[] comChestRef = new Card[16], chanceRef = new Card[16]; //creates unchanging references to each type of card
	public static HashMap<Integer, Card> comChestShuffled, chanceShuffled; //maps each card to a shuffled position
	public static int comChestDraw = 0, chanceDraw = 0; //tracks where in deck next draw will be

	public static int bankHouses = 32, bankHotels = 12; //game starts with 32 houses and 12 hotels in the bank

	public static void main(String[] args) {
		setup();
		startGame();
	}

	public static void setup(){
		String[] packedData;

		packedData = util.FileHandler.readFile("res/property.txt"); //create properties
		for (int line = 0; line < 28; line++)
			property[line] = new Property(packedData[line + 1]);

		packedData = util.FileHandler.readFile("res/tile.txt"); //create tiles
		for (int line = 0; line < 40; line++)
			tile[line] = new Tile(packedData[line + 1]);

		packedData = util.FileHandler.readFile("res/communityChest.txt"); //create community chest cards
		for (int line = 0; line < 16; line++)
			comChestRef[line] = new Card(packedData[line + 1]);

		packedData = util.FileHandler.readFile("res/chance.txt"); //create chance cards
		for (int line = 0; line < 16; line++)
			chanceRef[line] = new Card(packedData[line + 1]);

		comChestShuffled = shuffle(comChestRef); //shuffle cards
		chanceShuffled = shuffle(chanceRef);

		//create players
		for (int playerNumber = 0; playerNumber < playerQuantity; playerNumber++) player[playerNumber] = new Player(playerNumber);
	}

	public static void startGame(){
		for (int turnCounter = 0; turnCounter < 1000000; turnCounter++) takeTurn();
	}
	public static void takeTurn(){
		for (currentPlayer = 0; currentPlayer < playerQuantity; currentPlayer++) player[currentPlayer].takeTurn();
	}


	public static void drawComChest(){
		//checks for case in which "get out of jail free" card is in use by a player, meaning a draw must be skipped
		if (comChestShuffled.containsKey(comChestDraw)) comChestShuffled.get(comChestDraw).performFunction();
		else {
			comChestDraw = increaseDraw(comChestDraw, 1);
			comChestShuffled.get(comChestDraw).performFunction();
		}

		comChestDraw = increaseDraw(comChestDraw, 1);
	}
	public static void drawChance(){
		if (chanceShuffled.containsKey(chanceDraw)) chanceShuffled.get(chanceDraw).performFunction();
		else {
			chanceDraw = increaseDraw(chanceDraw, 0);
			chanceShuffled.get(chanceDraw).performFunction();
		}

		chanceDraw = increaseDraw(chanceDraw, 0);
	}
	public static int increaseDraw(int draw, int deckID){ 
		/* increases draw number, resetting and re-shuffling when draw reaches 16.
		 * deckID - 0: chance, 1: community chest
		 */
		draw++;
		if (draw >= 16){
			draw = 0;
			if (deckID == 0) chanceShuffled = shuffle(chanceRef);
			else comChestShuffled = shuffle(comChestRef);
		}

		return draw;
	}

	//RNG methods
	public static int rollDie(){
		Random rnum = new Random();
		return (rnum.nextInt(6) + 1);
	}
	public static HashMap<Integer, Card> shuffle(Card[] card){
		ArrayList<Integer> remainingPosition = new ArrayList<Integer>();
		for (int i = 0; i < 16; i++) remainingPosition.add(i);

		HashMap<Integer, Card> shuffledDeck = new HashMap<>();
		Random rnum = new Random();
		int position = 0;

		for (int i = 0; i < 16; i++){
			if(card[i].inUse) continue; //skips "get out of jail free" cards that are in use by a player
			position = rnum.nextInt(remainingPosition.size()); //generates a position
			shuffledDeck.put(remainingPosition.get(position), card[i]); //associates next card with that position
			remainingPosition.remove(position); //removes that position from remaining positions
		}

		return shuffledDeck;
	}
}
