package elevator;

import java.util.Stack;
import java.util.Vector;

public class ElevatorPlanner {
	private boolean towardUp = true;
	private Vector<Integer> toStopUp;
	private Stack<Integer> waitingListUp;
	private Vector<Integer> toStopsDown;
	private Stack<Integer> waitingListDown;
	private int atFloor = 0;
	private int moveState = 0;//1 means up,0 means idle,-1 means down

	public ElevatorPlanner(){
            //list of stops when moving up
            toStopUp = new Vector<>();
            //list of stops when moving down
            toStopsDown = new Vector<>();
            //next floors to get the service. e.g if going up and request to go down resived it will be put on waitingListDown
            waitingListUp = new Stack<>();
            waitingListDown = new Stack<>();
	}
	/**
	 * when passenger press button from inside panel the floor number will be added to the stops list to stop on that floor
	 */
	public void goTo(int goToFloor){
		if(goToFloor == atFloor){
                    //do nothing
		}
		else{
			if(goToFloor < atFloor){
				if(free()){
					towardUp = false;
				}
				addToDownStops(goToFloor);
			}else{
				if(free()){
					towardUp = true;
				}
				addToUpStops(goToFloor);
			}
		}
		
	}
	/**
	 * when stopping at a floor it is responsible to remove that floor from the list
         * and if there is no stop left switch to waiting list in opposite direction
         * consider for example when moving up and after passing a floor a request to go up issued since the floor is passed it will not be put in toStopUP list
         * it will be added to waitingListUp instead. when toStopUp list is empty we will switch the direction and adding all floor numbers in waitingListDown to toDownStop list.
         * in this way we can ensure fairness and preventing to give service only in one direction
	 */
	public void rearrangeStopList(Integer servicedFloor){
		atFloor =servicedFloor;
                
                if(toStopUp.contains(servicedFloor))
                    toStopUp.remove(servicedFloor);
                
                if(waitingListDown.contains(servicedFloor))
                    waitingListDown.remove(servicedFloor);
                
                if(toStopsDown.contains(servicedFloor))
                    toStopsDown.remove(servicedFloor);
                
                if(waitingListUp.contains(servicedFloor))
                    waitingListUp.remove(servicedFloor);
                
                if(towardUp){
                    if(toStopUp.isEmpty()){
                        towardUp = false;
                        while(!waitingListDown.isEmpty()){
                            addToDownStops(waitingListDown.pop());
                        }
                        if(toStopsDown.isEmpty()){
                            towardUp = true;
                            while(!waitingListUp.isEmpty())
                                addToUpStops(waitingListUp.pop());
                        }
                    }
                }else{
                    if(toStopsDown.isEmpty()){
                        towardUp = true;
                        while(!waitingListUp.isEmpty())
                            addToUpStops(waitingListUp.pop());
                        if(toStopUp.isEmpty())
                            towardUp = false;
                        while(!waitingListDown.isEmpty())
                            addToDownStops(waitingListDown.pop());
                    }
                }
        }
	/**
	 * when the out side button down/up pressed
         *
	 */
	public void toService(int floorNo, boolean goingUp){
            /* this method considers the fairness
             * if elevator has nothing to do the solution is straight forward
             * but if it is busy we need to schedule it carefully as discribed in else section.
             */
            if(free()){
                if(goingUp){
                    toStopUp.add(floorNo);
                    towardUp = true;
                }else{
                    toStopsDown.add(floorNo);
                    towardUp = false;
                }
                if(floorNo < atFloor)
                    moveState = -1;
                else
                    moveState = 1;
		}else{
                // if request to go up
                if(goingUp){
                    //if elevator also moving up and passed the calling floor. do not back immediately by adding it to toStopUp list to ensure faireness
                    if(moveState == 1 && atFloor >= floorNo){
                        if(!waitingListUp.contains(floorNo))
                            waitingListUp.add(floorNo);
                    }
                    //if elevator also moving down and passed the calling floor. do not back immediately by adding it to toStopUp list to ensure faireness
                    else if(moveState==-1&&atFloor<=floorNo){
                        if(!waitingListUp.contains(floorNo))
                            waitingListUp.add(floorNo);
                    }
                    else{ //otherwise it can be safely to be added in the list of stops on the way up
                        if(!toStopUp.contains(floorNo))
                            addToUpStops(floorNo);
                    }
                    
                }else{ //if request to go down
                    //if elevator also moving down and passed the calling floor. do not back immediately by adding it to toStopDown list to ensure faireness
                    if(moveState == -1&&atFloor<=floorNo){
                        if(!waitingListDown.contains(floorNo))
                            waitingListDown.add(floorNo);
                    }
                    //if elevator also moving up and passed the calling floor. do not back immediately by adding it to toStopDown list to ensure faireness
                    else if(moveState == 1&&atFloor>=floorNo){
                        if(!waitingListDown.contains(floorNo))
                            waitingListDown.add(floorNo);
                    }
                    else { //otherwise it can be safely to be added in the list of stops on the way down
                        if(!toStopsDown.contains(floorNo))
                            addToDownStops(floorNo);
                    }
                }
            }
        }
	/**
	 * adds specified floor to the list of stops on the way down. Also the list will be sorted
         * Because the next stop would be the first element. without sorted list we will have lots of unnecessary up and down
	 */
	private void addToDownStops(int floorNo){
		int size = toStopsDown.size();
                //to sort
		if(!toStopsDown.contains(floorNo)){
                    for(int i = 0;i<size;i++){
                        if(toStopsDown.elementAt(i)>floorNo){
                            continue;
                        }
                        toStopsDown.insertElementAt(floorNo, i);
                        break;
                    }
                    if(size == toStopsDown.size()){
                        toStopsDown.add(floorNo);
                    }
                }
	}
	/**
	 * adds specified floor to the list of stops on the way up. Also the list will be sorted
         * Because the next stop would be the first element. without sorted list we will have lots of unnecessary up and down
	 */
	private void addToUpStops(int floorNo){
            int size = toStopUp.size();
            //sort
            if(!toStopUp.contains(floorNo)){
                for(int i = 0;i<size;i++){
                    if(toStopUp.elementAt(i)<floorNo){
                        continue;
                    }
                    toStopUp.insertElementAt(floorNo, i);
                    break;
                }
                if(size == toStopUp.size()){
                    toStopUp.add(floorNo);
                }
            }
        }
	/**
	 * returns the next stop
	 */
	public int nextFloor(){
            
            if(toStopUp.isEmpty()&&toStopsDown.isEmpty()){
                return -1;
            }
            if(towardUp){
                if(toStopUp.isEmpty()){
                    System.out.println("no stop toward up left");
                    return -2;
                }
                if(atFloor<toStopUp.firstElement())
                    moveState = 1;
                else
                    moveState = -1;
                return toStopUp.firstElement();
            }else{
                if(toStopsDown.isEmpty()){
                    System.out.println("no stop toward down left");
                    return -2;
                }
                if(atFloor<toStopsDown.firstElement())
                    moveState = 1;
                else
                    moveState = -1;
                return toStopsDown.firstElement();
            }
        }
	/**
	 * return true if the elevator has nothing to do
	 */
	public boolean free(){
            if(toStopUp.isEmpty()&&toStopsDown.isEmpty())
                return true;
            return false;
        }
	/**
         * true if going up
	 */
	public boolean towardUp(){
		return towardUp;
	}
	/**
	 * sets the current position of the elevator
	 */
	public void setCurrentPosition(int floorNo){
		atFloor = floorNo;
		if(atFloor<nextFloor())
                    moveState = 1;
		else
                    moveState = -1;
        }
	
	/**
	 * gets the current position of the elevator.
	 */
	public int getCurrentPosition(){
		return atFloor;
	}
	//reset state when stop button pressed. removes all tasks
	public void resetState(){
            toStopUp.clear();
            toStopsDown.clear();
            waitingListDown.clear();
            waitingListUp.clear();
            moveState = 0;
	}
	
        //just printing some intresting information
	public void info(){
            System.out.println("	At floor" +atFloor + ", towardUp is " +towardUp);
            if(toStopUp.size() > 0){
                System.out.print("	Stops on the way up:");
                for(int i: toStopUp){
                    System.out.print(i+",");
                }
                System.out.println();
            }
            
            if(toStopsDown.size() > 0){
                System.out.print("	Stops on the way down:");
                for(int i: toStopsDown){
                    System.out.print(i+",");
                }
                System.out.println();
            }
            
            if(waitingListUp.size() > 0){
                System.out.print("	waiting List to go up:");
                for(int i: waitingListUp){
                    System.out.print(i+",");
                }
                System.out.println();
            }
            
            if(waitingListDown.size() > 0){
                System.out.print("	waiting List to go down:");
                for(int i: waitingListDown){
                    System.out.print(i+",");
                }
                System.out.println();
            }
        }
}
