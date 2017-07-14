package elevator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Controller{
	
	protected MasterThread master;
	protected Map<Integer, ElevWorker> elevatorsList;
	
	protected Socket socket;
	public double elev_speed;
	public int num_elevators;
	public int num_floors;
	public static OutputThread output;
	public BufferedReader input;
	
	public static void main(String args[]){
		output = new OutputThread();
		output.start();
            Controller controller = new Controller();
	}
	
	public Controller() {
            try {
               elev_speed = 0;
                
                //stablish connection over TCP socket and get socket input stream
                socket = new Socket("localhost", 4712);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                elevatorsList = new HashMap<>();
                
                //send info request for number of elevetors and number of floors
                output.putOnCommandQue("i");
                
                // read info from input stream
                String[] info = null;
                info = input.readLine().split(" ");

                num_elevators = Integer.parseInt(info[1]);
                num_floors = Integer.parseInt(info[2]);
                System.out.println("INFO:\n\t number of elevators: " + num_elevators + "\n\t number of floors: " + num_floors);
                //start the master thread
                master = new MasterThread();
                master.start();
                
                //strat the elevators threads. one thread per elevator
                for(int i = 0; i < num_elevators; i++){
                    elevatorsList.put(i, new ElevWorker(i+1));
                }
            } catch (UnknownHostException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        /*
         * this thread has following responsibilities:
         * 1. To find the best elevator to give service when a floor button has been pressed.
         * It aims to reduce both  waiting time by finding the closest elevator and energy consumption by not awaking idle elevator unnecessarily.
         * 2. It put the recieved command on the queue of the corresponding elevator to be performed.
         */
	public class MasterThread extends Thread{
            
            @Override
            public void run() {
                String in;
                String[] commArray;
                char comm;
                int elevatorNo;
                int floorNo;
                double value2;
                
                    while (true) {
                    try {
                        // read commands from input stream
                        in = input.readLine();
                        commArray = in.split(" ");
                        comm = commArray[0].toCharArray()[0];
                        // command indicating the speed 
                        if(comm == 'v'){
                            value2 = Double.parseDouble(commArray[1]);
                            elev_speed = value2;
                            continue;
                        }
                        
                        // command indicating which floor's button is pressed and the intended moving direction
                        if(comm  == 'b'){
                            floorNo = Integer.parseInt(commArray[1]);
                            int direction = Integer.parseInt(commArray[2]);
                            //Find the best elevator to give service.
                            elevatorNo = callElevator(floorNo,direction);
                            if(elevatorNo < 1) elevatorNo = 1;
                        }
                        //for all other command just forward the command to the corresponding elevator by putting the command in their command queue
                        else{
                            elevatorNo = Integer.parseInt(commArray[1]);
                        }
                        elevatorsList.get(elevatorNo-1).commandQue.put(in);
                    } catch (IOException | InterruptedException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    }
            }
		
		/**
		 * it gets the floor number and intended direction as argument and returns best option (elevator), which can give service
                 * here the aim is to save the energy, prevent unnecessary moves and reduce waiting time as much as possible.
		 */
		public int callElevator(int callingFloor, int direction){
			
			boolean towardUp = false;
			double closest = Integer.MAX_VALUE;
			double atFloor = -1.0, dist = -1;
			int bestToService = -1, elevNo = -1;
			
                        //if there is only one elevator
			if(num_elevators == 1) return 1;
			
			for(int i = 0; i < num_elevators; i++){
                            //if there is an idle elevator exactly on that floor call it.
                            //otherwise check other moving elevator.
                            
				if(elevatorsList.get(i).elevState.free() && elevatorsList.get(i).movingDirection == 0){
					atFloor = elevatorsList.get(i).elevState.getCurrentPosition();
					if(atFloor == callingFloor){
						bestToService = elevatorsList.get(i).elevatorNo;
						System.out.println("Elevator number "+bestToService+" is selected to go to floor " +callingFloor);
						return bestToService;
					}
				}
			}
			
			//next option: Check for already moving elevators in the same direction to reduce waiting time.
			for(int i = 0; i < num_elevators; i++){
				
				atFloor = elevatorsList.get(i).currentFloor;
				elevNo = elevatorsList.get(i).elevatorNo;
				
				//if its not moving, discard it.
				if(elevatorsList.get(i).movingDirection == 0){
					continue;
				}
				
				if(direction == 1)towardUp = true;
				else towardUp = false;
				
                                //if moving in opposit direction discard because it will increase the waiting time
				if(elevatorsList.get(i).elevState.towardUp() != towardUp)
					continue;
				
				//Check for bad conditions. 
				//If higher and movingDirection up.
				if((atFloor > callingFloor) && (elevatorsList.get(i).movingDirection == 1))
					continue;
				//If lower and movingDirection down.
				if((atFloor < callingFloor) && (elevatorsList.get(i).movingDirection == -1))
					continue;
				//after discarding elevators that are not in good state to service, select closest one among left elevators
				dist = Math.abs(atFloor - callingFloor);
				if(dist < closest){ 
					closest = dist;
					bestToService = elevNo;
				}
			}
			if(bestToService == -1) {
				//third option: Found no good moving elevators!. Take the closest idle one
				for(int i = 0; i < num_elevators; i++){
					
					atFloor = elevatorsList.get(i).currentFloor;
					elevNo = elevatorsList.get(i).elevatorNo;

					if(elevatorsList.get(i).movingDirection != 0 || elevatorsList.get(i).elevState.free() == false){
						continue;
					}
					
					dist = Math.abs(atFloor - callingFloor);
					if(dist <= closest){ 
						closest = dist;
						bestToService = elevNo;
					}
				}
			}
			//last option: if all other options failed select one randomly
			if(bestToService == -1){
				Random r = new Random(System.currentTimeMillis());
				bestToService = r.nextInt(num_elevators);
			}
			
			System.out.println("closest to " + callingFloor +" is Elevator " + bestToService);
			if(bestToService == -1) System.out.println("WARNING: couldnot find closest elevator");
			return bestToService;
		}
	}
	
	public class ElevWorker extends Thread{
            
            //Class to plan and schedule task of the elevator thread
	    public ElevatorPlanner elevState;
            public int elevatorNo;
            //direction of the elevator: 1 up, -1 down and 0 stop.
            public int movingDirection = 0;
            //The floor in which elevator is going toward it.
            public int towardFloor = 0;
            //The current floor in which elevator is at.
	    public double currentFloor = 0;
            //list of commands forwarded from master thread
            public LinkedBlockingQueue<String> commandQue;
            
            public ElevWorker(int no){
                commandQue = new LinkedBlockingQueue<>();
                elevatorNo = no;
                elevState = new ElevatorPlanner();
                this.start();
            }
            @Override
            public void run() {
                String in = null;
                String[] commArray;
                char comm;
                int val1;
                int floorNo;
                double val2;
                while(true){
                    try {
                        //read command
                        in = commandQue.take();
                        commArray = in.split(" ");
                        comm = commArray[0].toCharArray()[0];
                        
                        if(comm == 'b'){
                            val1 = Integer.parseInt(commArray[2]);
                            floorNo = Integer.parseInt(commArray[1]);
                            doCommand(comm, floorNo, val1);
                        }
                        
                        else if(comm == 'p'){
                            val1 = Integer.parseInt(commArray[2]);
                            doCommand(comm, val1, null);
                        }
                        else if(comm == 'd'){
                            val1 = Integer.parseInt(commArray[2]);
                            doorState(val1);
                        }
                        else if(comm == 'f'){
                            val2 = Double.parseDouble(commArray[2]);
                            checkDestination(val2);
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
		
		public void doCommand(char comm, int val1, Object val2){
			int floor = 0;
			int callDirection = 0;
			boolean toUp=  false;
			boolean halt = false;
                        String direction;
			
			if(comm == 'b'){
                            floor =  val1;
                            callDirection = (Integer) val2;
                            if(callDirection == 1){
                                toUp = true;
                                direction="up";
                            } else{
                                toUp = false;
                                direction="down";
                            }
                            //if it is idle and on calling floor just open and close door.
                            //if it is in current floor and it is not idle sudden stop means hurting passengers
                            if(floor == elevState.getCurrentPosition() && elevState.free()){
                                OpenAndCloseDoors();
                            } else{
                                System.out.println("request from floor:"+ floor +", to go " +direction);
                                elevState.toService(floor, toUp);
                            }
                        }
                        //command from panel inside the elevetor
                        else if(comm == 'p'){
                            floor =  val1;
				//if stop button pressed reset elevetor state (empty task queues) and stop elevator
				if(floor == 32000){
					System.out.println("Stop! Elevator number " + elevatorNo);
					
					movingDirection = 0;
					elevState.resetState();
					
					move(0);
					halt = true;
				}else{
                                    //if command p include the floor number, check if the elevator is in that floor and open the door if elevator is idle
                                    //otherwise add the floor number to list of stops so elevator will stop on its way when arriving that floor
					if(floor == elevState.getCurrentPosition() && elevState.free()){
						OpenAndCloseDoors();
					}
					else{
						System.out.println("passenger wants to go to floor " + floor);
						elevState.goTo(floor);
					}
				}
			}
                        
                        //now get the next floor that elevator must go toward and move it.
			if(!halt){
				int nextF = elevState.nextFloor();
				
				if(towardFloor != nextF && nextF!=-1){
					towardFloor = nextF;
					goToFloor(towardFloor);
				}
			}
		}
		//this method is called each time recieving the f command from the elevatorIO to set exact position of the elevator
                // it is responsible to check when the elevator must be stopped.
		public void checkDestination(double position) {
                    
                    // elevetor will stop when it is exactly at correct location for opening the door
                    // as the f command interval is 0.04, when difference of aimed floor and current exact position is dif<=0.045 we can stop it.  
                    int inFloor = (int) (position+0.5);
			currentFloor = position;
			
			double dif = Math.abs((double)towardFloor - position);
			
			if(dif <= 0.045){
				movingDirection = 0;
                                //remove the floor from the stop list and rearrenge the stop list. refer to method comment for more info.
				elevState.rearrangeStopList(inFloor);
				//send command m with value 0 to ElevatorIO to stop
				move(0);
				
				OpenAndCloseDoors();
				
                                //get next floor in the queue and move toward it
				towardFloor =  elevState.nextFloor();
				if(towardFloor != -1){
					goToFloor(towardFloor);
				}
				System.out.println("Elevator number "+elevatorNo+" stopped at "+ inFloor +", next stop is " + towardFloor);
				if(!elevState.free())elevState.info();
			}
			else{
				elevState.setCurrentPosition(inFloor);
			}
		}

		private void OpenAndCloseDoors() {
                    open();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    close();
                }
		/* considering current position and the floorNo that elevetor must go toward,
                 * this method finds the direction of the move to send command to the ElevatorIO
                 */
		public void goToFloor(int floorNo){
			if(floorNo < 0 || floorNo > num_floors){
				System.out.println("Elevator "+elevatorNo+" says there is no floor  " +floorNo);
			}
			int direction = 0;
			if(currentFloor > floorNo) direction = -1;
			else if(currentFloor < floorNo) direction = 1;
			else direction = 0;
			
			move(direction);
		}
		
		//send move/stop command to the ElevatorIO
		public void move(int direction){
                    String out = "m " + elevatorNo + " " + direction;
                    movingDirection = direction;
                    Controller.output.putOnCommandQue(out);
		}
		
		//control the door state open/close
		public void doorState(int state){
                    if(state == 1)
                        open();
                    else if(state == -1)
                        close();
		}
		
		//send command to open the door
		public void open(){
			output.putOnCommandQue("d " + elevatorNo + " 1");
		}
		
		//send command to close the door
		public void close(){
			output.putOnCommandQue("d " + elevatorNo + " -1");
		}
	}
}
