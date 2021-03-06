package elevator.rmi.impl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import elevator.ElevatorIO;
import elevator.ElevatorGUI;
import elevator.rmi.Scale;
import elevator.rmi.IllegalParamException;

/**
 * Title:        Green Elevator
 * Description:  Green Elevator, 2G1915
 * Copyright:    Copyright (c) 2001
 * Company:      IMIT/KTH
 * @author Vlad Vlassov
 * @version 1.0
 */
/**
 * The class <code>ScaleImpl</code> implements the
 * <code>elevator.rmi.Scale</code> remote interface.
 * An object with this class represents a scale (level indicator) of one elevator
 * and is used to set/get a value to/from the scale of the elevator.
 * The object servers as an RMI proxy to the scale of the elevator.
 * <p>An object with this class is created
 * when a thread invokes the <code>getScale</code> static method on the
 * <code>elevator.rmi.MakeAll</code> class with the elevator number (1, 2, ...)
 * as a parameter. The calling thread gets a RMI stub of the
 * <code>ScaleImpl</code> object with the <code>Scale</code> interface used to
 * control the door of the given elevator. For example:
 * <p><blockquote><pre>
 *     Elevator e3 = MakeAll.getElevator(3);
 *     double where = e3.whereIs();
 *     System.out.println(where);
 *     e3.up();
 *     do {
 *       e3.setScalePosition((int)where);
 *       sleep(100);
 *     } while ((where = e3.whereIs()) < 3.999);
 *     e3.stop();
 *     e3.setScalePosition(4);
 *     e3.open();
 *     sleep(3000);
 *     e3.close();
 * </pre></blockquote>
 * @author Vlad Vlassov, IMIT/KTH, Stockholm, Sweden
 * @version 1.0
 * @see     elevator.rmi.MakeAll
 * @see     elevator.rmi.Motor
 */
public class ScaleImpl extends UnicastRemoteObject implements Scale {
  private int number;
  private ElevatorIO io = ElevatorGUI.io;
    /**
     * Allocates a new <code>ScaleImpl</code> object that represents
     * a scale of an elevator with the given elevator number.
     *
     * @param   elevatorNumber   the number of the elevator whose
     *                           scale is represented by this object.
     * @exception  RemoteException  if the scale object cannot be created
     */
  public ScaleImpl(int elevatorNumber) throws RemoteException {
    super();
    number = elevatorNumber;
  }
  /**
   * Get a scale position (a level number) from this scale.
   *
   * @return an integer level number set to the scale
   * @exception RemoteException if failed to execute
   * @see     #setScalePosition(int) setScalePosition
   */
  public int getScalePosition() throws RemoteException {
    return io.getScalePosition(number);
  }
  /**
   * Set a scale position (a level number) to this scale.
   *
   * @param level the integer value, i.e. a floor number, to be set to this scale
   * @exception IllegalParamException if <code>level</code> is not a
   *            legal floor number
   * @exception RemoteException if failed to execute
   * @see     #getScalePosition() getScalePosition
   */
  public void setScalePosition(int level) throws RemoteException, IllegalParamException {
    io.scaleRMI(number, level);
  }
}
