package core;

import java.util.ArrayList;

public class Player {

	public int position = 0; //gives the position of a player on the board
	public int number; //player number, which determines order
	public boolean inJail = false;
	public int jailTime;
	//allows "get out of jail free" card to be directly accessed by player in order to perform actions more quickly
	public ArrayList<Card> getOutOfJailFree = new ArrayList<>();
	
	public int money; //how much money a player has in cash
	//elements of this array list will be linked to pre-existing properties in Board Class
	public ArrayList<Property> property = new ArrayList<Property>();
	/* describes how much money a player has including value of houses and property 
	 * (not counting value of properties that might be sold to other players)
	 */
	public int trueWealth; 
	

	public Player(int number){
		this.number = number;
		money = 1500;
		trueWealth = 1500;
	}

	public void takeTurn(){
		move();
		Board.tile[position].count++;
	}

	public void move(){ //tell player to move by rolling dice (i.e. a normal move)
		boolean doubles;
		int speeding = 0;

		if (inJail && jailTime == 0){ //checks if sentence is complete
			inJail = false;
		}
		if (inJail){
			jailTime--;
			decideJail();
		}

		else {
			int roll1, roll2;

			do {
				if (inJail) break; //checks for case in which player landed on "go to jail" with doubles

				doubles = false;
				roll1 = Board.rollDie();
				roll2 = Board.rollDie();

				if (roll1 == roll2) { //checks for doubles
					speeding++;
					if (speeding == 3) { //checks for speeding
						goToJail();
						break;
					}
					doubles = true;
				}

				if (inJail) break; //checks for case in which player was speeding

				position += roll1 + roll2;
				if (position >= 40) position -= 40; //collect $200 

				performTurn(); //based on what the player landed on, an action will occur
			} while(doubles);
		}
	}
	public void move(int tiles){ //tell player to move a certain amount (e.g. they drew a chance card)
		/* the reason why this method is preferable to telling the player to move to a tile by giving the tile id is because this will
		 * more easily detect whether the player has passed go
		 */
		position += tiles;
		if (position >= 40) position -= 40; //collect $200
		else if (position < 0) position += 40; //do not collect $200
	}

	public void goToJail(){ //quite similar to a "move" method, but specific to going to jail
		position = 10; //rather than moving by a normal method, this guarantees the player will not pass "go" and collect $200
		/* since the end of sentence must be checked BEFORE the player makes a decision about jail-leaving strategy 
		 * (and therefore before sentence time is reduced), jailTime must be set to 2 instead of 3
		 */
		jailTime = 2; 
		inJail = true;
	}
	public void decideJail(){ //player makes a decision about whether to leave jail
		if (getOutOfJailFree.size() > 0) useGetOutOfJailFree();
		else breakOut();
	}
	public void useGetOutOfJailFree(){ //player uses up a "get out of jail free" card to leave jail
		getOutOfJailFree.get(0).inUse = false;
		getOutOfJailFree.remove(0);
		jailTime = 0;
		inJail = false;
		move();
	}
	public void breakOut(){ //player tries to roll doubles to break out of jail
		int roll1 = Board.rollDie(), roll2 = Board.rollDie();
		if (roll1 == roll2) {
			jailTime = 0;
			inJail = false;
			move(roll1 + roll2);
		}
	}

	public void exchangeMoney(int amount){ //this occurs when the quantity of the deal is unknown (so there is an implied debt check)
		money += amount;
		trueWealth += amount;
		if (money < 0) handleDebt();
	}
	public void handleDebt(){
		int round = 0;
		/* round 0: attempt to mortgage property, not in a monopoly
		 * round 1: attempt to sell houses and hotels (if there are many remaining houses)
		 * round 2: attempt to mortgage property, regardless of monopoly
		 * round 3: attempt to sell hotels (even if there are not enough remaining houses)
		 * round >3: sell anything
		 */
		double lowestValue;
		int propertySelected;
		int[] monopoly;

		while (money < 0) {
			if(trueWealth <= 0) declareBankruptcy(); //if trueWealth > 0, there must be some un-mortgaged properties;

			lowestValue = property.get(0).value[number]; //defaults to first property in list
			propertySelected = 0;

			for (int i = 1; i < property.size(); i++) { // skips 0th iteration (see previous line)
				if (!monopoly.equals(null) && round == 0) // monopoly is not null in round 0 if some properties have been checked
					if (property.get(i).color == property.get(monopoly[0]).color) continue; //skips redundant check (sometimes)

				if (property.get(i).value[number] < lowestValue){
					// no money can be extracted from an already mortgaged property
					if (property.get(i).mortgaged) continue;
					// properties in monopolies will not be sold on the first round
					if (property.get(i).monopoly && round == 0){ //UNLESS the monopoly has no houses
						// (monopoly = getMonopoly(i)) to skip properties of the same color
						 
					}
					// if a property has houses, they must be sold first
					if ((property.get(i).houses > 0 || property.get(i).hotels > 0) && round != 1 && round != 3) continue;
					// if a property has no houses but is in a monopoly, skip round two

					propertySelected = i;
					lowestValue = property.get(i).value[number];
				}
			}

			// if no properties were selected, the only possible selected property is 0
			if (property.get(0).mortgaged){
				round++;
				continue;
			}
			if (property.get(0).monopoly && round == 0)
				if (checkDevelopment(getMonopoly(0))){
					round++;
					continue;
				}
			if ((property.get(0).houses > 0 || property.get(0).hotels > 0) && round != 1 && round != 3){
				round++;
				continue;
			}

			if (round == 0 || round == 2 || round > 3){ // mortgage property
				property.get(propertySelected).mortgaged = true;
				money += property.get(propertySelected).mortgageValue;
			}
			else { // sell houses
				monopoly = getMonopoly(propertySelected);
				
			}
		}
	}
	public int[] decisionSellHouse(int[] monopoly){ //ai decides how many houses to sell from each property in the specified monopoly
		int[] houses = new int[monopoly.length], sell = new int[monopoly.length];
		int hotels = 0; //counts total number of hotels
		int houseSellValue = property.get(monopoly[0]).houseCost / 2;
		int housesRequired = Math.abs(money) / houseSellValue;
		//will under-shoot by 1 unless houseSellValue divides into |money| evenly
		if (housesRequired * houseSellValue < Math.abs(money)) housesRequired++;

		// round will comprise the last element of the returned array, detailing the round in which the decision arrived at should be made
		int round = 1; // by default, the round will be set to 1, but will be set to 3 if no appropriate round 1 decision can be made

		for(int i = 0; i < monopoly.length; i++){
			houses[i] = property.get(monopoly[i]).houses;
			hotels += property.get(monopoly[i]).hotels;
		}

		for (int i = 0; i < hotels; i++) { // ignores for loop if hotels = 0
			if (i <= housesRequired)
				// checks if replacing hotels with houses will return too few houses
				if (4 * i <= Board.bankHouses)
		}
				
		
	}
	public boolean checkDevelopment(int[] monopoly){
		boolean developed = false;

		for (int i = 0; i < monopoly.length; i++)
			if (property.get(monopoly[i]).houses > 0 || property.get(monopoly[i]).hotels > 0) developed = true;

		return developed;
	}

	public int[] getMonopoly(int propertySelected){ //given a property, find all the properties in the given property's color group
		ArrayList<Integer> monopolyList = new ArrayList<Integer>();

		for (int i = 0; i < property.size(); i++)
			if (property.get(propertySelected).color == property.get(i).color) monopolyList.add(i);

		int[] monopoly = new int[monopolyList.size()];
		for (int i = 0; i < monopolyList.size(); i++) monopoly[i] = monopolyList.get(i);

		return monopoly;
	}

	public void performTurn(){
		if (Board.tile[position].type == 28) Board.drawChance();
		else if (Board.tile[position].type == 29) Board.drawComChest();
		else if (Board.tile[position].type == 33){
			goToJail();
		}
	}
}
