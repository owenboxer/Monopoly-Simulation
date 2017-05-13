package core;

public class Card {
	public int effect, quantity; 
	/* Effects - 1: Earn/Lose Money, 2: Move, 3: Go to Jail, 4: Get out of Jail Free
	 * Quantities (for respective effects) - 1: amount of money earned/lost, 2: designated tile number (e.g. Go: 0), 3: N/A, 4: N/A
	 */
	public int houseRepairCost, hotelRepairCost; //identifies cost of repairing houses and hotels for certain cards
	public boolean repair = false; //identifies cards that require paying money based on how many houses/hotels are owned
	public boolean moveBack = false; //identifies "move back three spaces/tiles" card
	public int playerModifier = 0; 
	/* identifies/specifies cards in which other players owe/are owed by the current player
	 *  (0: not right card type, 50: current player is owed $50 by each opponent, -50: current player owes each opponent $50)
	 */
	public boolean nearestUtility = false, nearestRailroad = false; //identifies "go to nearest rr/utility" cards
	public boolean inUse = false; //pertains to "get out of jail free" cards, will not be shuffled into deck if inUse is true

	public int count;

	public Card(String packedCard){
		String subString = "";

		for (int clause = 0; clause < 2; clause++) {
			subString = packedCard.substring(0, packedCard.indexOf('\t'));
			subString.trim();

			switch(clause){
			case 0:
				effect = Integer.parseInt(subString);
				break;
			case 1:
				if (subString.contains("n")) break;
				if (subString.contains("h")){
					repair = true;
					setRepairCost(subString);
					break;
				}
				if (subString.contains("p")){
					if (subString.contains("+")) playerModifier = 50; //$50 is always the quantity owed in player cards
					else playerModifier = -50;
					break;
				}
				if (subString.contains("t")){
					moveBack = true;
					break;
				}
				if (subString.contains("u")){
					nearestUtility = true;
					break;
				}
				if (subString.contains("r")){
					nearestRailroad = true;
					break;
				}
				quantity = Integer.parseInt(subString);
				break;
			}

			packedCard = packedCard.substring(packedCard.indexOf('\t') + 1);
		}
	}

	public void setRepairCost(String packedQuantity){
		String houseRepairCost = packedQuantity.substring(1, packedQuantity.indexOf(':')),
				hotelRepairCost = packedQuantity.substring(packedQuantity.indexOf(':') + 1);
		this.houseRepairCost = Integer.parseInt(houseRepairCost);
		this.hotelRepairCost = Integer.parseInt(hotelRepairCost);
	}

	public void performFunction(){
		//effect 1 will be coded when money is coded

		if (effect == 2) { //card which requires player to move
			int moveAmount = 0, playerPosition = Board.player[Board.currentPlayer].position;

			if (moveBack) moveAmount = -3; //move back three spaces

			else if (nearestRailroad) {
				if (playerPosition < 5) moveAmount = 5 - playerPosition; //position of Reading RR
				else if (playerPosition < 15) moveAmount = 15 - playerPosition; //position of Penn. RR
				else if (playerPosition < 25) moveAmount = 25 - playerPosition; //position of B & O RR
				else if (playerPosition < 35) moveAmount = 35 - playerPosition; //position of Short Line
				else if (playerPosition >= 35) moveAmount = 45 - playerPosition;
			}
			else if (nearestUtility) {
				if (playerPosition < 12) moveAmount = 12 - playerPosition; //position of Electric Company
				else if (playerPosition < 28) moveAmount = 28 - playerPosition; //position of Water Works
				else if (playerPosition >= 28) moveAmount = 52 - playerPosition;
			}
			else { //generic case (with single option for tile being moved to)
				if (playerPosition < quantity) moveAmount = quantity - playerPosition;
				if (playerPosition >= quantity) moveAmount = 40 + quantity - playerPosition;
			}

			Board.player[Board.currentPlayer].move(moveAmount);
		}

		else if (effect == 3){ //"go to jail" card
			Board.player[Board.currentPlayer].goToJail();
		}

		else if (effect == 4){ //"get out of jail free" card
			Board.player[Board.currentPlayer].getOutOfJailFree.add(this); //gives player "get out of jail free" card
			inUse = true; //marks card not to be shuffled back into deck
		}
	}
}
