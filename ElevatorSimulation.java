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
                            dir = Direction.IDLE;
                            // wait for new requests
                            wait();
                            if (shutdown) {
                                break;
                            }
                        }
                    }

                    // Process up stops first if any, then down stops
                    if (!upStops.isEmpty()) {
                        dir = Direction.UP;
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
                        dir = Direction.DOWN;
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






    }
}