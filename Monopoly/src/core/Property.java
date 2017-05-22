package core;

public class Property {

	public String name;
	public int cost, mortgageValue; //mortgageValue is 0.5 * cost
	public int rentPresets[] = new int[6], houses = 0, hotels = 0, houseCost; //rent[0]: default rent, rent[n]: n houses, rent[5]: 1 hotel
	public int rent
	public double income;
	public int owner = -1; //a value of -1 signifies being unowned, while any other value will correspond to a player
	public int color; //color refers to the monopoly which the property is in
	public boolean monopoly = false, mortgaged = false;
	public boolean railroad, utility;
	public double interest = 1.0; //interest increases the cost of buying back a mortgaged property
	Tile tile;

	public double[] value; // each player will have a value for each property in order to make decisions

	public Property(String packedProperty){
		String subString = "";

		for (int clause = 0; clause < 10; clause++){ // a "clause" is a part of packed data, each of which is separated by a tab
			if (clause == 9) subString = packedProperty; // on last iteration there is no tab
			else subString = packedProperty.substring(0, packedProperty.indexOf('\t')); //cuts off substrings at each tab
			subString.trim();

			switch(clause) {
			case 0:
				name = subString;
				break;
			case 1:
				color = Integer.parseInt(subString);
				break;
			case 2:
				cost = Integer.parseInt(subString);
				mortgageValue = cost / 2;
				break;
			case 3:
				if (subString.startsWith("n")) break;
				houseCost = Integer.parseInt(subString);
				break;
			case 4:
				if (subString.startsWith("n")) {
					utility = true;
					break;
				}
				rentPresets[0] = Integer.parseInt(subString);
				break;
			case 5:
				rentPresets[1] = Integer.parseInt(subString);
				break;
			case 6:
				rentPresets[2] = Integer.parseInt(subString);
				break;
			case 7: 
				rentPresets[3] = Integer.parseInt(subString);
				break;
			case 8:
				if (subString.startsWith("n")) {
					railroad = true;
					break;
				}
				rentPresets[4] = Integer.parseInt(subString);
				break;
			case 9:
				rentPresets[5] = Integer.parseInt(subString);
				break;
			}

			if (railroad || utility) break;

			if (clause != 9) packedProperty = packedProperty.substring(packedProperty.indexOf('\t') + 1); //removes previous clause
		}

		value = new double[Board.playerQuantity];
	}

	public void establishValue(){
		value = new double[Board.playerQuantity];
	}
	public void setRent(){
		int houses = this.houses;
		if (hotels > 0) houses++;

		rent = rentPresets[houses];
		income = rent * (Board.playerQuantity - 1) * tile.probability;
	}
}