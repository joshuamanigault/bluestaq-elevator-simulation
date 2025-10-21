import java.util.*;

public class ElevatorSimulation {

    enum Direction { UP, DOWN, IDLE};

    static class Request {
        final int floor;
        final Direction direction;
        final Boolean internal;

        Request(int floor, Direction direction, Boolean internal) {
            this.floor = floor;
            this.direction = direction;
            this.internal = internal;
        }

        @Override
        public String toString() {
            return "Request[floor=" + floor + ", direction=" + direction + ", internal=" + internal + "]";
        }
    }

    static class Elevator implements Runnable {
        private final int id;
        private int currentFloor = 1;  // Assuming ground floor is 1
        private Direction direction = Direction.IDLE;
        
        // Up stops and down stops
        private final TreeSet<Integer> upStops = new TreeSet<>();
        private final TreeSet<Integer> downStops = new TreeSet<>(Collections.reverseOrder());

        private volatile boolean shutdown = false;

        Elevator(int id, int startFloor) {
            this.id = id;
            this.currentFloor = startFloor;
        }

        synchronized void addRequest(int floor) {
            if (floor == currentFloor) {
                System.out.println("Elevator " + id + " already at floor " + floor + ", opening doors.");
                openDoors();
                return;
            }

            if (floor > currentFloor) {
                upStops.add(floor);
            } else {
                downStops.add(floor);
            }
            notifyAll();
        }

        synchronized boolean hasPendingRequests() {
            return !upStops.isEmpty() || !downStops.isEmpty();
        }

        private void openDoors() {
            System.out.println("[Elevator " + id + "] Doors opening at floor " + currentFloor);

            // Simulating door delay
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {

            }
            System.out.println("[Elevator " + id + "] Doors closing at floor " + currentFloor);
        }

        private void stepUp() {
            currentFloor++;
            System.out.println("[Elevator " + id + "] moved up to floor " + currentFloor);
        }

        private void stepDown() {
            currentFloor--;
            System.out.println("[Elevator " + id + "] moved down to floor " + currentFloor);
        }

        @Override
        public void run() {
            while (!shutdown) {
                try {
                    synchronized (this) {
                        if (!hasPendingRequests()) {
                            direction = Direction.IDLE;
                            // wait for new requests
                            wait();
                            if (shutdown) {
                                break;
                            }
                        }
                    }

                    // Process up stops first if any, then down stops
                    if (!upStops.isEmpty()) {
                        direction = Direction.UP;
                        Integer nextStop = upStops.first();

                        while (currentFloor < nextStop) {
                            Thread.sleep(400); // travel time between the floors
                            stepUp();
                        }

                        synchronized (this) {
                            upStops.remove(nextStop);
                        }
                        openDoors();
                    } else if (!downStops.isEmpty()) {
                        direction = Direction.DOWN;
                        Integer nextStop = downStops.first();

                        while (currentFloor > nextStop) {
                            Thread.sleep(400);
                                stepDown();
                            }
                        synchronized(this) {
                            downStops.remove(nextStop);
                        }
                        openDoors();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.println("[Elevator " + id + "] shutting down.");
        }

        synchronized void shutdown() {
            this.shutdown = true;
            notifyAll();
        }

        synchronized int getCurrentFloor() {
            return currentFloor;
        }

        synchronized Direction getDirection() {
            return direction;
        }

        public String toString() {
            return "Elevator[" + id + ", floor=" + currentFloor + ", direction=" + direction + ", upStops=" + upStops + ", downStops=" + downStops + "]";
        }
    }

    // Assigns requests to the best elevator
    static class ElevatorController {
        private final List<Elevator> elevators = new ArrayList<>();
        private final List<Thread> elevatorThreads = new ArrayList<>();

        ElevatorController(int n, int startFloor) {
            for (int i = 0; i < n; i++) {
                Elevator e = new Elevator(i + 1, startFloor);
                elevators.add(e);
                Thread t = new Thread(e, "Elevator-" + (i + 1));
                t.setDaemon(true);
                elevatorThreads.add(t);
                t.start();
            }
        }

        void externalRequest(int floor, Direction direction) {
            Elevator best = null;
            int bestDist = Integer.MAX_VALUE;
            for (Elevator e : elevators) {
                int dist = Math.abs(e.getCurrentFloor() - floor);
                if (!e.hasPendingRequests() && dist < bestDist) {
                    best = e;
                    bestDist = dist;
                }
            }
           // If there is no idle elevator, pick the one with the shortest distance away
           if (best == null) {
                for (Elevator e : elevators) {
                    int dist = Math.abs(e.getCurrentFloor() - floor);
                    if (dist < bestDist) {
                        best = e;
                        bestDist = dist;
                    }
                }
           }

           System.out.println("[Controller] assigning external request at floor " + floor + " to elevator " + best.id);
           if (best != null) {
                best.addRequest(floor);
           }
        }


        void internalRequest(int elevatorId, int floor) {
            Elevator e =  elevators.get(elevatorId - 1);
            System.out.println("[Controller] internal request: elevator " + elevatorId + " to floor " + floor);
            e.addRequest(floor);
        }

        void shutdown() {
            for (Elevator e: elevators) {
                e.shutdown();
            }
        }

        void printStatus() {
            for (Elevator e: elevators) {
                System.out.println(e);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting Elevator Simulation");
        ElevatorController controller = new ElevatorController(3, 1);

        controller.externalRequest(5, Direction.UP);
        Thread.sleep(1000);
        controller.externalRequest(3, Direction.DOWN);
        Thread.sleep(500);
        controller.internalRequest(1, 7);
        Thread.sleep(2000);
        controller.externalRequest(2, Direction.UP);
        Thread.sleep(1000);
        controller.internalRequest(2, 1);

        // Let the simulation run for a while
        Thread.sleep(15000);

        controller.printStatus();
        controller.shutdown();
    }
}

/*
Assumptions:
-Elevators start at floor 1.
-Elevator travel time between floors is simulated with Thread.sleep(400) and is consistent.
-Simple assignment strategy: idle elevators are prioritized, otherwise the closest elevator is chosen.

Features Implemented: 
-Multiple elevators operating concurrently.
-Per elevator request queues for up and down stops.
-Console logging of elevator movements and door operations.
-Internal and external request handling.
-Shutdown mechanism for elevators

Limitations/Not Implemented:
-No advanced grouping of requests or optimization strategies.
-No handling of edge cases like invalid floor requests.
-No handling of capacity, maintenance mode, or emergency situations.
-No handling of weight limits
-No GUI
-There is no scheduling fairness or peak time handling.
-Floor bounds are not enforced.
-External requests do not specify direction beyond UP/DOWN.
*/
