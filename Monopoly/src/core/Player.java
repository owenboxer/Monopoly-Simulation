package core;

import java.util.*;

public class Player {

	public int position = 0; //gives the position of a player on the board
	public int playerNumber; //player number, which determines order
	public boolean inJail = false;
	public int jailTime;
	//allows "get out of jail free" card to be directly accessed by player in order to perform actions more quickly:
	public ArrayList<Card> getOutOfJailFree = new ArrayList<>();
	
	public int money; //how much money a player has in cash
	//elements of this array list will be linked to pre-existing properties in Board Class:
	public ArrayList<Property> property = new ArrayList<Property>();
	public ArrayList<Integer> propertyRank = new ArrayList<Integer>();
	/* describes how much money a player has including value of houses and property 
	 * (not counting value of properties that might be sold to other players):
	 */
	public int trueWealth; 
	public boolean bankrupt = false;

	//risk quantizes how much money you will make/lose each turn, value quantizes how much you stand to gain by taking turns
	public int risk, value;

	public Player(int number){
		this.playerNumber = number;
		money = 1500;
		trueWealth = 1500;

		//value properties:
		for (int p = 0; p < 28; p++){
			Board.property[p].value[playerNumber] = Board.property[p].cost; 
			//default value must be at least cost, so that all properties can be bought at the beginning of the game
		}
	}

	public void takeTurn(){
		risk = assessRisk(); //many actions that may (and most often do) occur during a turn require assessing risk
		value = assessValue(); // 										" 									   value
		move();

		//Try to buy houses
		
	}

	public void move(){ //tell player to move by rolling dice (i.e. a normal move)
		boolean doubles;
		int speeding = 0;

		if (inJail && jailTime == 0){ //checks if sentence is complete
			exchangeMoney(-50); //pay $50 dollar fine to leave jail
			inJail = false;
		}
		if (inJail){
			jailTime--;
			decideJail();
		}

		else {
			int roll1, roll2;

			do {
				if (inJail) break; //checks for case in which player landed on "go to jail" but had gotten doubles and loop continued
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
				if (position >= 40) {
					position -= 40;
					exchangeMoney(200); //collect $200
				}

				performTurn(); //based on what the player landed on, an action will occur
			} while(doubles);
		}
	}
	public void move(int tiles){ //tell player to move a certain amount (e.g. they drew a chance card)
		/* the reason why this method is preferable to telling the player to move to a tile by giving the tile id is because this will
		 * more easily detect whether the player has passed go.
		 */
		position += tiles;
		if (position >= 40) {
			position -= 40;
			exchangeMoney(200); //collect $200
		}
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
		boolean stay = false;
		if (risk > 0 && value < 50) stay = true;

		if (!stay) {
			if (getOutOfJailFree.size() > 0) useGetOutOfJailFree();
			else if (value > 100) {
				exchangeMoney(-50); // pay $50 bail
				jailTime = 0;
				inJail = false;
				move();
			}
		}
		else breakOut(); // even if you don't want to stay in jail, you have to attempt to break out (according to the rules of monopoly)
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
	public void declareBankruptcy(){
		bankrupt = true;
		for (int p = 0; p < property.size(); p++)
			Board.auction(property.get(p)); // auction 
	}
	public void handleDebt(){
		int round = 0;
		/* round 0: attempt to mortgage property, not in a monopoly (unless the monopoly is not developed)
		 * round 1: attempt to sell houses, and maybe hotels if there are enough remaining houses in the bank
		 * round 2: attempt to mortgage property, regardless of monopoly
		 * round 3: attempt to sell hotels even if there are not enough remaining houses
		 * round >3: sell anything
		 */
		double lowestValue; //used to compare values with other properties
		int propertySelected; //used to note which property will be manipulated
		int[] monopoly; //stores values associated with all the properties in a monopoly
		returnedValues sell; //stores values for selling houses

		if(trueWealth <= 0) declareBankruptcy(); //if trueWealth > 0, there is still hope
		
		else {
			while (money < 0) { // continues loop until money is positive

				lowestValue = property.get(0).value[playerNumber]; //defaults to first property in list
				propertySelected = 0;

				for (int i = 1; i < property.size(); i++) { // skips 0th iteration (see previous line)
					if (property.get(i).value[playerNumber] < lowestValue){
						// no money can be extracted from an already mortgaged property
						if (property.get(i).mortgaged) continue;
						// properties in monopolies will not be sold on the first round
						if (property.get(i).monopoly && round == 0){ //UNLESS the monopoly has no houses
							if (checkDevelopment(getMonopoly(i))) continue;
						}
						// if a property has houses, they must be sold first
						if ((property.get(i).houses > 0 || property.get(i).hotels > 0) && round != 1 && round != 3) continue;
						// if a property has no houses but is in a monopoly, skip round two

						
						propertySelected = i;
						lowestValue = property.get(i).value[playerNumber];
					}
				}

				// if no properties were selected, the only possible selected property is 0, which must also be checked
				if (property.get(0).mortgaged){
					round++;
					continue; // if the default property fails to meet conditions of the current round, no properties do
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

				//round is not increased at the end of the loop
				if (round == 0 || round == 2 || round > 3){ // mortgage property
					property.get(propertySelected).mortgaged = true;
					money += property.get(propertySelected).mortgageValue;
				}
				else { // sell houses
					sell = decisionSellHouse(monopoly = getMonopoly(propertySelected));
					if (sell.round >= round) {
						for (int i = 0; i < monopoly.length; i++){
							if (sell.sell[i] > 0) {
								if (property.get(monopoly[i]).hotels > 0) {
									property.get(monopoly[i]).hotels = 0;
									property.get(monopoly[i]).houses = 4;
									sell.sell[i]--;
									money += property.get(monopoly[i]).houseCost / 2;
									Board.bankHotels++;
									property.get(monopoly[i]).setRent();
								}
								if (sell.sell[i] > 0) {
									property.get(monopoly[i]).houses -= sell.sell[i];
									money += sell.sell[i] * (property.get(monopoly[i]).houseCost / 2);
								}
							}
						}
						Board.bankHouses -= sell.housesFromBank; //selling houses is much more complex than selling hotels
					}
				}
			}
		}
	}
	public returnedValues decisionSellHouse(int[] monopoly){ //ai decides how many houses to sell from each property in the specified monopoly
		int[] valueRank = getHousePriority(monopoly, false); //high is more valuable
		/* houses keeps track of how many houses currently exist on each property
		 * sell keeps track of how many houses will be sold from each property and is a value being returned
		 * housesLeft keeps track on how many houses will exist on each property after being sold
		 * hotels keeps track of how many hotels currently exist on each property
		 */
		int[] houses = new int[monopoly.length], sell = new int[monopoly.length], housesLeft = new int[monopoly.length],
				hotels = new int[monopoly.length];
		/* hotels keeps track of total number of hotels
		 * hotelsRequired keeps track of how many hotels must be sold, which helps in case of not having to sell all hotels
		 * housesFromBank keeps track of how many houses will need to be taken from the bank in order to replace sold hotels
		 * housesSold keeps track of how many houses will have been sold, so they can replace housesFromBank
		 */
		int totalHotels = 0, hotelsRequired = 0, housesFromBank = 0;
		int houseSellValue = property.get(monopoly[0]).houseCost / 2;
		int housesRequired = Math.abs(money) / houseSellValue;
		// will under-shoot by 1 unless houseSellValue divides into |money| evenly:
		if (housesRequired * houseSellValue < Math.abs(money)) housesRequired++;

		// notes the round in which the decision arrived at should be made
		int round = 1; // by default, the round will be set to 1, but will be set to 3 if no appropriate round 1 decision can be made
		int p = 0;

		returnedValues returnValue; //object which will store values that are being returned

		for(p = 0; p < monopoly.length; p++){
			houses[p] = property.get(monopoly[p]).houses;
			hotels[p] = property.get(monopoly[p]).hotels;
			totalHotels += property.get(monopoly[p]).hotels;
		}

		housesRequired -= totalHotels; //hotels will be sold no matter what
		if (housesRequired < 0) {
			hotelsRequired = totalHotels + housesRequired; //sets hotels equal to original number of houses required
			housesRequired = 0;
		}
		else hotelsRequired = totalHotels;

		//the current value of hotels is the number of hotels that must be sold
		if (totalHotels > 0)
			for (p = 0; p < hotelsRequired; p++) { // limits hotels sold to number of hotels required to be sold
				 housesLeft[valueRank[p]] = 4; // converts hotels to houses based on their ranking 
				 housesFromBank += 4;
			}
		for (p = 0; p < monopoly.length; p++) // accounts for houses that may have already existed if hotel was not there
			if (housesLeft[p] == 0) {
				//*line 228 is useful for calculating sell value (line 254), and implies that no houses need to be sold from this property
				if (hotels[p] > 0) housesLeft[p] = 5;
				housesLeft[p] = houses[p];
			}
		for (p = 0; p < housesRequired; p++) { // sells houses until housesRequired are met or there are no more houses
			housesLeft[valueRank[p % monopoly.length]]--;
			if (housesLeft[valueRank[p % monopoly.length]] < 0) { //no more houses
				housesLeft[valueRank[p % monopoly.length]]++;
				break;
			}
			housesFromBank--; 
			/* even if houses are being sold from a property that never had a hotel (and therefore is not borrowing houses from the 
			 * bank), the houses sold from it can be used to replace those being taken from the bank
			 */
		}
		if (housesFromBank >= Board.bankHouses) { // not enough houses in bank to replace hotels
			round = 3;
			p = 0;
			do {
				// it is impossible to run out of houses on properties, because borrowed houses can't exceed 
				housesLeft[valueRank[p % monopoly.length]]--;
				housesFromBank--; //*see above explanation (line 238 - 240)
			} while (housesFromBank >= Board.bankHouses);
		}

		// at this point, the ideal house selling strategy should be obtained
		for (p = 0; p < monopoly.length; p++){
			if (hotels[p] > 0) sell[p] = 5 - housesLeft[p];
			else sell[p] = houses[p] - housesLeft[p];
		}

		//compiling values into returnValue
		returnValue = new returnedValues(sell, housesFromBank, round);
		return returnValue;
	}
	public boolean checkDevelopment(int[] monopoly){
		boolean developed = false;

		for (int i = 0; i < monopoly.length; i++)
			if (property.get(monopoly[i]).houses > 0 || property.get(monopoly[i]).hotels > 0) developed = true;

		return developed;
	}
	public int[] decisionBuyHouses(int propertySelected){
		int monopoly = getMonopoly(propertySelected), priority = getHousePriority(monopoly, true);
		int availableMoney = money;
		if ((0.5 * value) + risk > availibleMoney) availableMoney = (0.5 * value) + risk;

		int[] houses, buy;
		int totalHouses = 0, totalHotels = 0;
		int houseCost = property.get(propertySelected).houseCost;
		int p = 0

		for (p = 0; p < monopoly.length; p++) {
			houses[p] = property.get(monopoly[p]).houses;
			if (property.get(monopoly[p]).hotels > 0) houses[p] = 5;
		}

		p = 0;
		while (totalHouses < bankHouses  - 1 && totalHotels < bankHotels - 1) {
			houses[priority[p % 3]]++;
			if ((totalHouses * houseCost) + (totalHotels * houseCost) > availableMoney) {
				houses[priority[p % 3]]--;
				break;
			}
			if (houses[priority[p % 3]] == 5) {
				totalHouses -= 4;
				totalHotels++;
				continue;
			}
			else if (houses[priority[p % 3]] > 5) {
				houses[priority[p % 3]]--;
				break;
			}
			totalHouses++;
			p++;
		}

		for (p = 0; p < monopoly.length; p++) {
			if (property.get(monopoly[p]).hotels > 0) buy[p] = 0;
			else buy[p] = houses[p] - property.get(monopoly[p]).houses;
		}
		return buy;
	}
	public int[] getHousePriority(int[] monopoly, boolean buying){
		/* Determines in which order houses should be bought/sold on properties based on the number of houses on those properties and the
		 * value of those properties.
		 * Earlier elements in the array have higher values if buying, and later elements have higher values if not buying.
		 * In either case, the returned array should be iterated through from beginning to end, and not vice versa.
		 */
		ArrayList<Integer> priorityList = new ArrayList<Integer>();
		int[] houses = new int[monopoly.length];
		double[] income = new double[monopoly.length];
		for (int i = 0; i < monopoly.length; i++){
			if (property.get(monopoly[i]).hotels > 0) houses[i] = 5;
			else houses[i] = property.get(monopoly[i]).houses;
			income[i] = property.get(monopoly[i]).income;
		}

		for (int i = 0; i < monopoly.length; i++)
			for (int j = 0; j < priorityList.size(); j++) { // increasing in priority, so defaults to order when "not buying"
				// add element i at position j (immediately before greater element)
				if (houses[i] < houses[priorityList.get(j)]) priorityList.add(j, i); // compare number of houses
				else if (houses[i] == houses[priorityList.get(j)]) {
					//compare values
					if (income[i] >= income[priorityList.get(j)]) priorityList.add(j + 1, i); //compare values
					else priorityList.add(j, i);
				}
			}

		//convert ArrayList to array
		int[] priority = new int[priorityList.size()];
		for (int i = 0; i < priorityList.size(); i++) {
			if (!buying) priority[i] = priorityList.get(i);
			else priority[priority.length - i - 1] = priorityList.get(i); // reverses order for buying
		}

		return priority;
	}

	public void decisionBuyProperty(Property property) {
		int[] monopoly;

		if (property.value[playerNumber] >= property.cost) { //value has to be greater than its cost
			//value has to be greater twice as great as cost if you don't have enough money to buy it
			if (property.cost <= money || (property.value[playerNumber] * 0.5 > property.cost && risk < 0)) 
				if (property.cost <= trueWealth) { //cost must be less than true wealth
					exchangeMoney(-1 * property.cost);
					property.owner = playerNumber;
					monopoly = Board.getMonopoly(property);
					for (int p = 0; p < monopoly.length; p++)
						Board.property[monopoly[p]].value[playerNumber] *= 1.25;
					Board.checkMonopoly(monopoly);
					if (Board.checkMonopoly(monopoly))
						for (int p = 0; p < monopoly.length; p++){
							Board.property[monopoly[p]].monopoly = true;
							Board.property[monopoly[p]].value[rank.get(rank.size() - 1)] *= 1.5;
						}
				}
		}
		else Board.auction(property);
	}
	public int decisionAuction(Property property) {
		if (property.value[playerNumber] >= property.cost) { //value has to be greater than its cost
			//value has to be greater twice as great as cost if you don't have enough money to buy it
			if (property.cost <= money || (property.value[playerNumber] * 0.5 > property.cost && risk < 0)) 
				if (property.cost <= trueWealth) { //cost must be less than true wealth
					return (int) property.value[playerNumber];
				}
		}
		return 0;
	}
	public void payRent(Property property) {
		if (trueWealth < property.rent)
			Board.player[property.owner].exchangeMoney(trueWealth);
		else Board.player[property.owner].exchangeMoney(property.rent);

		exchangeMoney(-1 * property.rent);
	}

	public int assessRisk() {
		/* The risk of the board is assessed by calculating the average money earned/lost after an infinite amount of time of 
		 * progressing through the board in its current state. This includes paying rent, passing go, being paid, landing on chance and 
		 * community chest, etc., but does not include jail penalty.
		 * 
		 * risk = -(mean income per turn)
		 */
		double risk = 0;

		for (int t = 0; t < 40; t++) { //iterates through tiles 
			if (Board.tile[t].type < 28) {
				if (Board.tile[t].property.owner != playerNumber)
					risk += Board.tile[t].property.income; //adds rent paid
				else if (Board.tile[t].property.owner == playerNumber)
					risk -= Board.tile[t].property.income; //subtracts rent earned
			}
			else if (Board.tile[t].type == 28)
				for (int c = 0; c < 16; c++) {
					if (Board.chanceRef[c].effect == 1)
						//divide by 16 because chance of draw is 1/16
						risk -= Board.chanceRef[c].getQuantity() * 0.0625 * Board.tile[t].probability;
				}
			else if (Board.tile[t].type == 29)
				for (int c = 0; c < 16; c++) {
					if (Board.comChestRef[c].effect == 1)
						risk -= Board.comChestRef[c].getQuantity() * 0.0625 * Board.tile[t].probability;
				}
		}

		//since "Go" only needs to be passed to earn money, it has a special probability P(passing Go) instead of P(landing on Go)
		risk -= 200 * Board.tile[0].probability;

		return (int) risk;
	}
	public int assessValue() { // entire board
		/* The value of the board is determined by adding up the values of un-owned properties.
		 * Value is used to make decisions about buying property and leaving jail.
		 */

		double value = 0;

		for (int p = 0; p < 28; p++)
			if (Board.property[p].owner == -1) value += Board.property[p].value[playerNumber];

		return (int) value;
	}
	public int[] getMonopoly(int propertySelected){ //given a property, find all the properties in the given property's color group
		ArrayList<Integer> monopolyList = new ArrayList<Integer>();

		for (int p = 0; p < property.size(); p++)
			if (property.get(propertySelected).color == property.get(p).color) monopolyList.add(p);

		int[] monopoly = new int[monopolyList.size()];
		for (int p = 0; p < monopolyList.size(); p++) monopoly[p] = monopolyList.get(p);

		return monopoly;
	}

	public void performTurn(){
		if (Board.tile[position].type < 28) {
			if (Board.tile[position].property.owner == -1) decisionBuyProperty(Board.tile[position].property);
			else if (Board.tile[position].property.owner != playerNumber) payRent(Board.tile[position].property);
		}
		if (Board.tile[position].type == 28) Board.drawChance();
		else if (Board.tile[position].type == 29) Board.drawComChest();
		else if (Board.tile[position].type == 33) goToJail();
	}
}

class returnedValues { // wrapper class for returning values from certain methods
	int[] sell;
	int housesFromBank;
	int round;

	returnedValues(int[] sell, int housesFromBank, int round){ //returning from decisionSellHouse()
		this.sell = sell;
		this.housesFromBank = housesFromBank;
		this.round = round;
	}
}
