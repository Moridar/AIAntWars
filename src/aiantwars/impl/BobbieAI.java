/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package aiantwars.impl;

import aiantwars.EAction;
import aiantwars.EAntType;
import aiantwars.IAntAI;
import aiantwars.IAntInfo;
import aiantwars.IEgg;
import aiantwars.ILocationInfo;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import jdk.nashorn.internal.runtime.arrays.ArrayLikeIterator;

/**
 *
 * @author Bobbie Apitzsch
 */
public class BobbieAI implements IAntAI {

    private int startX;
    private int startY;
    private int worldSizeX;
    private int worldSizeY;
    private final Random rnd = new Random();
    private ILocationInfo worldMap[][];
    private ArrayList<IAntInfo> aliveAnts = new ArrayList<>();
    private ArrayList<IAntInfo> enemyAnts = new ArrayList<>();
    private boolean needCarrier = true;
    private boolean needScout = true;
    private boolean needWarrior = false;
    private aStarRoute aStarRoute = new aStarRoute();

    private double calcDistance(ILocationInfo a, ILocationInfo b) {
        int x = Math.abs(a.getX() - b.getX());
        int y = Math.abs(a.getY() - b.getY());
        return Math.sqrt(x * x + y * y);
    }

    private ILocationInfo getNearestFoodLocation(ILocationInfo thisLocation) {
        if (thisLocation.getFoodCount() > 0) {
            return thisLocation;
        }
        ILocationInfo nearest = thisLocation;
        double nearestDistance = 5000.0;
        for (int x = 0; x < worldMap.length; x++) {
            for (int y = 0; y < worldMap[x].length; y++) {
                if (worldMap[x][y].getFoodCount() != 0 && calcDistance(worldMap[x][y], thisLocation) < nearestDistance) {
                    nearest = worldMap[x][y];
                    nearestDistance = calcDistance(thisLocation, nearest);
                }
            }
        }
        return nearest;
    }
    private ILocationInfo getRandomLocNearBase(int range) {
        int x = rnd.nextInt(range * 2 + 1) - range + getBaseLoc().getX();
        int y = rnd.nextInt(range * 2 + 1) - range + getBaseLoc().getY();
        if (x >= 0 && x < worldSizeX && y >= 0 && y < worldSizeY) {
            return worldMap[x][y];
        }
        if (range > 3) {
            return getRandomLocNearBase(range - 1);
        }
        return getRandomLocNearBase(range);
    }

    private ILocationInfo getBaseLoc() {
        for (IAntInfo ant : aliveAnts) {
            if (ant.getAntType() == EAntType.QUEEN) {
                return ant.getLocation();
            }
        }
        return worldMap[worldSizeX / 2][worldSizeY / 2];
    }

    private EAntType suggestNextAntType() {
        int warriors = 0;
        int scouts = 0;
        int carriers = 0;
        for (IAntInfo thisAnt : aliveAnts) {
            switch (thisAnt.getAntType()) {
                case CARRIER:
                    carriers++;
                    break;
                case WARRIOR:
                    warriors++;
                    break;
                case SCOUT:
                    scouts++;
                    break;
            }
        }
        if (needWarrior) {
            needWarrior = false;
            return EAntType.WARRIOR;
        }
        if (needCarrier) {
            needCarrier = false;
            return EAntType.CARRIER;
        }
        if (needScout) {
            needScout = false;
            return EAntType.SCOUT;
        }
        if (warriors > carriers) {
            return EAntType.CARRIER;
        }
        if (warriors > scouts * 2 && scouts == 0) {
            return EAntType.SCOUT;
        }
        return EAntType.WARRIOR;
    }

    private void needScout() {
        needScout = true;
    }

    private void needCarrier() {
        needCarrier = true;
    }

    private void worldMapAdd(ILocationInfo nLocationInfo) {
        worldMap[nLocationInfo.getX()][nLocationInfo.getY()] = nLocationInfo;
    }

    private List<ILocationInfo> getAdjacentLocations(ILocationInfo yourLocation, int repeat) {
        List<ILocationInfo> list = getAdjacentLocations(yourLocation);
        if (repeat == 0) {
            return list;
        }

        for (ILocationInfo adjacentLoc : list) {
            for (ILocationInfo newLoc : getAdjacentLocations(adjacentLoc, repeat - 1)) {
                if (!list.contains(newLoc)) {
                    list.add(newLoc);
                }
            }

        }
        return list;
    }

    private List<ILocationInfo> getAdjacentLocations(ILocationInfo yourLocation) {
        List<ILocationInfo> list = new ArrayList<ILocationInfo>();
        if (yourLocation.getX() + 1 < worldSizeX) {
            list.add(worldMap[yourLocation.getX() + 1][yourLocation.getY()]);
        }
        if (yourLocation.getX() - 1 >= 0) {
            list.add(worldMap[yourLocation.getX() - 1][yourLocation.getY()]);
        }
        if (yourLocation.getY() - 1 >= 0) {
            list.add(worldMap[yourLocation.getX()][yourLocation.getY() - 1]);
        }
        if (yourLocation.getY() + 1 < worldSizeY) {
            list.add(worldMap[yourLocation.getX()][yourLocation.getY() + 1]);
        }
        return list;
    }

    private int enemyMaxDamage(ILocationInfo myLoc) {
        int enemyDamage = 0;
        for (ILocationInfo loc : getAdjacentLocations(myLoc)) {
            if (loc.getAnt() != null && loc.getAnt().getTeamInfo().getTeamID() != myLoc.getAnt().getTeamInfo().getTeamID()) {
                enemyDamage += loc.getAnt().getAntType().getMaxAttack();
            }
        }
        return enemyDamage;
    }

    private void printMap() {
        String output = "";
        for (int y = worldMap[0].length - 1; y >= 0; y--) {
            for (int x = 0; x < worldMap.length; x++) {
                if (worldMap[x][y].getAnt() != null) {
                    output += worldMap[x][y].getAnt().getAntType().getTypeName().substring(0, 1);
                } else if (worldMap[x][y].isRock()) {
                    output += "R";
                } else if (worldMap[x][y].isFilled()) {
                    output += "S";
                } else if (worldMap[x][y].getFoodCount() < 0) {
                    output += "?";
                } else {
                    output += worldMap[x][y].getFoodCount();
                }
            }
            output += "\n";
        }
        System.out.println(output);
    }

    @Override
    public EAction chooseAction(IAntInfo thisAnt, ILocationInfo thisLocation, List<ILocationInfo> visibleLocations, List<EAction> possibleActions) {
        worldMapAdd(thisLocation);
        for (ILocationInfo locInfo : visibleLocations) {
            if (thisLocation == locInfo) {
                System.out.println("This location is in visibleLocations");
            }
            worldMapAdd(locInfo);
        }

        EAction action = null;
        if (possibleActions.contains(EAction.Attack) && visibleLocations.get(0).getAnt().getTeamInfo().getTeamID() != thisAnt.getTeamInfo().getTeamID() && visibleLocations.get(0).getAnt().getHitPoints() < thisAnt.getAntType().getMaxAttack() - (thisAnt.getAntType().getMaxAttack() - thisAnt.getAntType().getMinAttack()) / 2
                && enemyMaxDamage(thisAnt.getLocation()) - visibleLocations.get(0).getAnt().getAntType().getMaxAttack() < thisAnt.getHitPoints()) {
            action = EAction.Attack;
        } else if (possibleActions.contains(EAction.EatFood) && (thisAnt.getHitPoints() < 10 || thisAnt.getHitPoints() < enemyMaxDamage(thisLocation))) {
            action = EAction.EatFood;
        } else if (possibleActions.contains(EAction.LayEgg) && (aliveAnts.size() < 5 || thisAnt.getFoodLoad() > 9)) {
            action = EAction.LayEgg;
        } else if (possibleActions.contains(EAction.Attack) && visibleLocations.get(0).getAnt().getTeamInfo().getTeamID() != thisAnt.getTeamInfo().getTeamID()) {
            action = EAction.Attack;
        } else if (possibleActions.contains(EAction.PickUpFood) && thisAnt.getFoodLoad() < thisAnt.getAntType().getMaxFoodLoad() && (thisAnt.getAntType() == EAntType.QUEEN || thisAnt.getAntType() == EAntType.WARRIOR)) {
            action = EAction.PickUpFood;
        } else if (possibleActions.contains(EAction.PickUpFood) && thisAnt.getFoodLoad() < 2) {
            action = EAction.PickUpFood;
        } else if (thisAnt.getFoodLoad() < 2) {
            action = aStarRoute.suggestEAction(thisLocation, getNearestFoodLocation(thisLocation), thisAnt, possibleActions);
            if (action == EAction.Attack) {
                action = EAction.Pass;
            }
        } else if (possibleActions.contains(EAction.DigOut)) {
            action = EAction.DigOut;
        } else if (possibleActions.contains(EAction.DropSoil) && visibleLocations.get(0).getFoodCount() < 2 && rnd.nextBoolean()) {
            action = EAction.DropSoil;
        } else {
            if (thisAnt.getAntType() == EAntType.QUEEN) {
                action = aStarRoute.suggestEAction(thisLocation, getRandomLocNearBase(10), thisAnt, possibleActions);
            } else {
                action = aStarRoute.suggestEAction(thisLocation, getRandomLocNearBase(7), thisAnt, possibleActions);
            }
            if (action == EAction.Attack) {
                action = EAction.Pass;
            }
        }

        StringBuilder actions = new StringBuilder();
        for (EAction a : possibleActions) {
            actions.append(a.toString());
            actions.append(", ");
        }

        System.out.println(actions.toString());
        System.out.println(
                "ID: " + thisAnt.getTeamInfo().getTeamID() + "," + thisAnt.antID() + " chooseAction: " + action);
        return action;
    }

    @Override
    public void onStartTurn(IAntInfo thisAnt, int turn) {

        System.out.println("ID: " + thisAnt.getTeamInfo().getTeamID() + "," + thisAnt.antID() + " onStartTurn(" + turn + ")");
    }

    @Override
    public void onAttacked(IAntInfo thisAnt, int dir, IAntInfo attacker, int damage) {

        printMap();
        worldMapAdd(attacker.getLocation());
        worldMapAdd(thisAnt.getLocation());
        System.out.println("ID: " + thisAnt.getTeamInfo().getTeamID() + "," + thisAnt.antID() + " onAttacked: " + damage + " damage");
    }

    @Override
    public void onDeath(IAntInfo thisAnt) {
        worldMapAdd((thisAnt.getLocation()));
        aliveAnts.remove(thisAnt);
    }

    @Override
    public void onLayEgg(IAntInfo thisAnt, List<EAntType> types, IEgg egg) {
//        EAntType type = types.get(rnd.nextInt(types.size()));
        EAntType type = suggestNextAntType();
        System.out.println("ID: " + thisAnt.getTeamInfo().getTeamID() + "," + thisAnt.antID() + " onLayEgg: " + type);
        egg.set(type, this);
    }

    @Override
    public void onHatch(IAntInfo thisAnt, ILocationInfo thisLocation, int worldSizeX, int worldSizeY) {
        if (this.worldSizeX == 0 && this.worldSizeY == 0) {
            this.worldSizeX = worldSizeX;
            this.worldSizeY = worldSizeY;
            startX = thisLocation.getX();
            startY = thisLocation.getY();
            iniWorldMap();
        }
        aliveAnts.add(thisAnt);
        System.out.println("ID: " + thisAnt.getTeamInfo().getTeamID() + "," + thisAnt.antID() + " onHatch");
    }

    private void iniWorldMap() {
        if (worldMap == null) {
            worldMap = new ILocationInfo[worldSizeX][worldSizeY];
            for (int x = 0; x < worldSizeX; x++) {
                for (int y = 0; y < worldSizeY; y++) {
                    Location newLocation = new Location(x, y);
                    newLocation.setFoodCount(-1);
                    worldMap[x][y] = newLocation;

                }
            }
        }
    }

    private class aStarRoute {

        ArrayList<ILocationInfo> openList = new ArrayList<>();
        ArrayList<ILocationInfo> closedList = new ArrayList<>();
        ILocationInfo goal;
        ILocationInfo currILoc;
        HashMap<ILocationInfo, Float> HValues = new HashMap<>();
        HashMap<ILocationInfo, ILocationInfo> ParentMap = new HashMap<>();
        int direction;
        IAntInfo thisAnt;
        ArrayList<ILocationInfo> pathToGo = new ArrayList<>();

        private int findDirection(ILocationInfo startLoc, ILocationInfo endLoc) {
            if (startLoc.getX() < endLoc.getX()) {
                return 1;
            }
            if (startLoc.getY() < endLoc.getY()) {
                return 2;
            }
            if (startLoc.getX() > endLoc.getX()) {
                return 3;
            }
            return 0;
        }

        public EAction suggestEAction(ILocationInfo startLoc, ILocationInfo endLoc, IAntInfo thisAnt, List<EAction> possibleActions) {
            if (possibleActions.size() == 1) {
                return possibleActions.get(0);
            }
            openList.clear();
            closedList.clear();
            HValues.clear();
            ParentMap.clear();
            this.thisAnt = thisAnt;
            goal = endLoc;
            currILoc = startLoc;
            boolean countine = true;
            while (countine) {
                if (goal == currILoc || getGValue(currILoc) > 50) {
                    if (ParentMap.get(currILoc) == null) {
                    } else {
                        while (ParentMap.get(ParentMap.get(currILoc)) != null) {
                            currILoc = ParentMap.get(currILoc);
                        }
                    }
                    switch (findDirection(startLoc, currILoc) - thisAnt.getDirection()) {
                        case -3:
                            if (possibleActions.contains(EAction.TurnRight)) {
                                return EAction.TurnRight;
                            }
                            break;
                        case -2:
                            if (possibleActions.contains(EAction.MoveBackward)) {
                                return EAction.MoveBackward;
                            }
                            break;
                        case -1:
                            return EAction.TurnLeft;
                        case 0:
                            if (possibleActions.contains(EAction.MoveForward)) {
                                return EAction.MoveForward;
                            }
                            break;
                        case 1:
                            return EAction.TurnRight;
                        case 2:
                            if (possibleActions.contains(EAction.MoveBackward)) {
                                return EAction.MoveBackward;
                            }
                            break;
                        case 3:
                            return EAction.TurnLeft;

                    }
                    System.out.println("WARNING! Random turning!");
                    return possibleActions.get(rnd.nextInt(possibleActions.size()));
                } else {
                    ILocationInfo tmp = currILoc;
                    if (!openList.isEmpty()) {
                        currILoc = openList.get(0);
                    }
                    for (ILocationInfo t : openList) { //Search for tile with lowest F value
                        if (getFValue(currILoc) >= getFValue(t)) {
                            currILoc = t;
                        }
                    }
                    if (closedList.size() > 30) {
                        goal = currILoc;
                    }
                    closedList.add(currILoc);
                    openList.remove(currILoc);
                }
                if (currILoc != goal) {
                    for (ILocationInfo t : getAdjacentLocations(currILoc)) {
                        if (isSamePosition(currILoc, startLoc)) {
                            if (!t.isRock() && !t.isFilled() && t.getAnt() == null) {
                                boolean contain = false;
                                for (ILocationInfo closedInfo : closedList) {
                                    if (isSamePosition(closedInfo, t)) {
                                        contain = true;
                                    }
                                }
                                if (!contain) {
                                    boolean openContain = false;
                                    for (ILocationInfo openInfo : openList) {
                                        if (isSamePosition(openInfo, t)) {
                                            openContain = true;
                                        }
                                    }
                                    if (!openContain) {
                                        openList.add(calcILocationInfo(currILoc, t));
                                    } else {
                                        reCalcILocationInfo(currILoc, t);
                                    }
                                }
                            }
                        } else if ((!t.isRock() && !t.isFilled()) || (t == goal)) {
                            boolean contain = false;
                            for (ILocationInfo closedInfo : closedList) {
                                if (isSamePosition(closedInfo, t)) {
                                    contain = true;
                                }
                            }
                            if (!contain) {
                                boolean openContain = false;
                                for (ILocationInfo openInfo : openList) {
                                    if (isSamePosition(openInfo, t)) {
                                        openContain = true;
                                    }
                                }
                                if (!openContain) {
                                    openList.add(calcILocationInfo(currILoc, t));
                                } else {
                                    reCalcILocationInfo(currILoc, t);
                                }
                            }
                        }
                    }
                }
            }
            return possibleActions.get(rnd.nextInt(possibleActions.size()));
        }

        private boolean isSamePosition(ILocationInfo t, ILocationInfo t1) {
            if (t.getX() == t1.getX() && t.getY() == t1.getY()) {
                return true;
            }
            return false;
        }

        private Float getFValue(ILocationInfo t) {
            return getGValue(t) + HValues.get(t);
        }

        private int getGValue(ILocationInfo t) {
            if (ParentMap.containsKey(t)) {
                if (ParentMap.containsKey(ParentMap.get(t))) {
                    if (!isGrandparentStraight(t, ParentMap.get(t), ParentMap.get(ParentMap.get(t)))) {
                        return 3 + 2 + getGValue(ParentMap.get(t));
                    }
                }
                return 3 + getGValue(ParentMap.get(t));
            }
            return 0;
        }

        private boolean isGrandparentStraight(ILocationInfo child, ILocationInfo parent, ILocationInfo grandParent) {
            if (child.getX() + 1 == parent.getX() && child.getX() + 2 == grandParent.getX()) {
                return true;
            } else if (child.getX() - 1 == parent.getX() && child.getX() - 2 == grandParent.getX()) {
                return true;
            } else if (child.getY() + 1 == parent.getY() && child.getY() + 2 == grandParent.getY()) {
                return true;
            } else if (child.getY() - 1 == parent.getY() && child.getY() - 2 == grandParent.getY()) {
                return true;
            }

            return false;
        }

        private void reCalcILocationInfo(ILocationInfo cameFrom, ILocationInfo t) {
            ILocationInfo tmp = ParentMap.get(t);
            int oldG = getGValue(t);
            ParentMap.put(t, cameFrom);
            if (oldG <= getGValue(t)) {
                ParentMap.put(t, tmp);
            }
        }

        private ILocationInfo calcILocationInfo(ILocationInfo cameFrom, ILocationInfo t) {
            ParentMap.put(t, cameFrom);
            HValues.put(t, (float) Math.sqrt(Math.abs(t.getX() - goal.getX()) * Math.abs(t.getX() - goal.getX()) + Math.abs(t.getY() - goal.getY()) * Math.abs(t.getY() - goal.getY())));
            return t;
        }
    }
}
