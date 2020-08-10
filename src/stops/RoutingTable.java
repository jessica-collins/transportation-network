package stops;

import java.util.*;
import java.lang.Integer;
import exceptions.DuplicateStopException;
import java.util.HashMap;
import java.util.Map;

/**
 * The class should map destination stops to RoutingEntry objects.
 *
 * The table is able to redirect passengers from their current stop to the next
 * intermediate stop which they should go to in order to reach their final
 * destination.
 */
public class RoutingTable {
    // the stop stored in this routing table
    private Stop initial;

    // the destination stops for the stop stored in this table and the
    // routing entries associated with them
    private Map<Stop, RoutingEntry> entries;

    // the routing entry for the stop stored in this routing table
    private RoutingEntry initialEntry;

    /**
     * Creates a new RoutingTable for the given stop.
     *
     * <p>The routing table should be created with an entry for its initial
     * stop (i.e. a mapping from the stop to a RoutingEntry.RoutingEntry() for
     * that stop.
     *
     * @param initialStop The stop for which this table will handle routing.
     */
    public RoutingTable(Stop initialStop) {
        this.initial = initialStop;
        this.entries = new HashMap<>();
        this.initialEntry = new RoutingEntry(initialStop, 0);
        this.entries.put(initialStop, initialEntry);
    }

    /**
     * Adds the given stop as a neighbour of the stop stored in this table.
     *
     * <p>A neighbouring stop should be added as a destination in this table,
     * with the cost to reach that destination simply being the Manhattan distance
     * between this table's stop and the given neighbour stop.
     *
     * <p>If the given neighbour already exists in the table, it should be
     * updated (as defined in addOrUpdateEntry(Stop, int, Stop)).
     *
     * <p>The 'intermediate'/'next' stop between this table's stop and the new
     * neighbour stop should simply be the neighbour stop itself.
     *
     * <p>Once the new neighbour has been added as an entry, this table should
     * be synchronised with the rest of the network using the synchronise() method.
     *
     * @param neighbour The stop to be added as a neighbour.
     */
    public void addNeighbour(Stop neighbour) {
        int distance = this.initial.distanceTo(neighbour);
        // create a routing entry for the neighbour stop
        RoutingEntry neighbourEntry = new RoutingEntry(neighbour, distance);

        // if the table does not contain an entry for the neighbour
        if (!this.entries.containsKey(neighbour)) {
            this.entries.put(neighbour, neighbourEntry);
            this.initial.addNeighbouringStop(neighbour);
        // if the table does contain an entry for the neighbour
        } else {
            // if the newCost to the neighbour is less than the current cost
            if (this.entries.get(neighbour).getCost() > distance) {
                this.entries.put(neighbour, neighbourEntry);
                this.initial.addNeighbouringStop(neighbour);
            // if the current cost is less than the newCost
            } else {
                this.initial.addNeighbouringStop(neighbour);
            }
        }

        this.synchronise();
    }

    /**
     * Returns whether a new entry was added to the routing table, an
     * existing one was updated, or if the table remained unchanged.
     *
     * <p>If there is currently no entry for the destination in the table, a
     * new entry for the given destination should be added, with a RoutingEntry
     * for the given cost and next (intermediate) stop.
     *
     * <p>If there is already an entry for the given destination, and the
     * newCost is lower than the current cost associated with the destination,
     * then the entry should be updated to have the given newCost and next
     * (intermediate) stop.
     *
     * <p>If there is already an entry for the given destination, but the
     * newCost is greater than or equal to the current cost associated with the
     * destination, then the entry should remain unchanged.
     *
     * @param destination The destination stop to add/update the entry.
     * @param newCost The new cost to associate with the new/updated entry.
     * @param intermediate The new intermediate/next stop to associate with the
     *                     new/updated entry.
     *
     * @return True if a new entry was added, or an existing one was updated, or
     * false if the table remained unchanged.
     */
    public boolean addOrUpdateEntry(Stop destination, int newCost,
                                    Stop intermediate) {
        RoutingEntry destinationEntry = new RoutingEntry(intermediate, newCost);
        boolean changed = false;

        // if the entry does not exist in this table or if the new cost is
        // less than the current cost
        if (!this.entries.containsKey(destination) ||
                newCost < this.entries.get(destination).getCost()) {
            this.entries.put(destination, destinationEntry);

            changed = true;
        }

        return changed;
    }

    /**
     * Returns the cost associated with getting to the given stop.
     *
     * @param stop The stop to get the cost.
     *
     * @return The cost to the given stop, or Integer.MAX_VALUE if the stop is
     * not currently in this routing table.
     */
    public int costTo(Stop stop) {
        // if the stop does not exist in this routing table
        if (!this.entries.containsKey(stop)) {
            return Integer.MAX_VALUE;
        } else {
            return this.entries.get(stop).getCost();
        }
    }

    /**
     * Maps each destination stop in this table to the cost associated with
     * getting to that destination.
     *
     * @return A mapping from destination stops to the costs associated with
     * getting to those stops.
     */
    public Map<Stop, Integer> getCosts() {
        // a map of the destinations and the cost associated with them
        Map<Stop, Integer> costs = new HashMap<>();
        List<Stop> destinations = new ArrayList<>(this.entries.keySet());

        for (Stop destination : destinations) {
            costs.put(destination, this.entries.get(destination).getCost());
        }
        return costs;
    }

    /**
     * Returns the stop for which this table will handle routing.
     *
     * @return The stop for which this table will handle routing.
     */
    public Stop getStop() {
        return this.initial;
    }

    /**
     * Returns the next intermediate stop which passengers should be routed to
     * in order to reach the given destination.
     *
     * <p>If the given stop is null or not in the table, then return null.
     *
     * @param destination The destination which the passengers are being routed.
     *
     * @return The best stop to route the passengers to in order to reach the
     * given destination.
     */
    public Stop nextStop(Stop destination) {
        List<Stop> destinations = new ArrayList<>(this.entries.keySet());

        // if the destination is null or does not exist in this table
        if (destination == null || !destinations.contains(destination)) {
            return null;
        } else {
            return this.entries.get(destination).getNext();
        }
    }

    /**
     * Synchronises this routing table with the other tables in the network.
     *
     * <p>In each iteration, every stop in the network which is reachable by
     * this table's stop (as returned by traverseNetwork()) must be considered.
     *
     * <p>For each stop in the network, each of its neighbours must be
     * visited, and the entries from the stop must be transferred to each
     * neighbour (using the transferEntries(Stop) method).
     *
     * <p>If any of these transfers results in a change to the table that the
     * entries are being transferred, then the entire process must be repeated
     * again. These iterations should continue happening until no changes
     * occur to any of the tables in the network.
     *
     * <p>This process is designed to handle changes which need to be
     * propagated throughout the entire network, which could take more than one
     * iteration.
     */
    public void synchronise() {
        List<Stop> destinations = new ArrayList<>(this.entries.keySet());

        for (Stop stop : destinations) {
            RoutingTable oldStopTable = stop.getRoutingTable();
            RoutingTable newStopTable = stop.getRoutingTable();
            for (Stop neighbour : newStopTable.traverseNetwork()) {
                while (!newStopTable.equals(oldStopTable)) {
                    // transfer entries to the neighbour until the table is
                    // not the same as it started
                    this.transferEntries(neighbour);
                }
            }
        }
    }

    /**
     * Updates the entries in the routing table of the given other stop, with the
     * entries from this routing table.
     *
     * <p>If this routing table has entries which the other stop's table
     * doesn't, then the entries should be added to the other table (as defined
     * in addOrUpdateEntry(Stop, int, Stop)) with the cost being updated to
     * include the distance.
     *
     * <p>If this routing table has entries which the other stop's table does
     * have, and the new cost would be lower than that associated with its
     * existing entry, then its entry should be updated (as defined in
     * addOrUpdateEntry(Stop, int, Stop)).
     *
     * <p>If this routing table has entries which the other stop's table does
     * have, but the new cost would be greater than or equal to that associated
     * with its existing entry, then its entry should remain unchanged.
     *
     * @param other The stop whose routing table this table's entries should
     *              be transferred.
     * @return True if any new entries were added to the other stop's table, or
     * if any of its existing entries were updated, or false if the other stop's
     * table remains unchanged.
     *
     * @require this.getStop().getNeighbours().contains(other) == true.
     */
    public boolean transferEntries (Stop other) {
        // distance from this stop to the other stop
        int distance = this.initial.distanceTo(other);
        RoutingTable otherTable = other.getRoutingTable();
        List<Stop> destinations = new ArrayList<>(this.entries.keySet());
        List<Stop> otherDestinations =
                new ArrayList<>(otherTable.entries.keySet());
        boolean changed = false;

        for (Stop destination : destinations) {
            // the cost associated with getting from this stop to the
            // destination
            int destinationCost = this.entries.get(destination).getCost();

            if (!otherDestinations.contains(destination)) {
                RoutingEntry destinationEntry = new RoutingEntry(this.initial
                        , distance + destinationCost);
                otherTable.entries.put(destination, destinationEntry);
                changed = otherTable.addOrUpdateEntry(destination,
                        destinationCost + distance, this.initial);
            } else {
                changed = otherTable.addOrUpdateEntry(destination,
                        destinationCost + distance, this.initial);
            }
        }

        return changed;
    }

    /**
     * Performs a traversal of all the stops in the network, and returns a list
     * of every stop which is reachable from the stop stored in this table.
     *
     * <p>Firstly create an empty list of Stops and an empty Stack of Stops, push
     * the RoutingTable's Stop on to the stack, while the stack is not empty,
     * pop the top Stop (current) from the stack, for each of that stop's
     * neighbours, if they are not in the list, add them to the stack, then add
     * the current Stop to the list.
     *
     * @return All of the stops in the network which are reachable by the stop
     * stored in this table.
     */
    public java.util.List<Stop> traverseNetwork() {
        List<Stop> traversedStops = new ArrayList<>();
        Stack<Stop> orderedStops = new Stack<>();

        orderedStops.push(this.initial);

        while (!orderedStops.isEmpty()) {
            Stop currentStop = orderedStops.pop();
            for (Stop neighbour : currentStop.getNeighbours()) {
                if (!traversedStops.contains(neighbour)) {
                    orderedStops.add(neighbour);
                }
            }
            traversedStops.add(currentStop);
        }
        return orderedStops;
    }
}
