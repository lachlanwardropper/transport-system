package stops;

import java.util.*;

/**
 * The class should map destination stops to RoutingEntry objects.
 *
 * The table is able to redirect passengers from their current stop
 * to the next intermediate stop which they should go to in
 * order to reach their final destination.
 */
public class RoutingTable {

    private Stop initialStop;
    private Map<Stop, RoutingEntry> routingTable = new LinkedHashMap<>();

    /**
     * Creates a new RoutingTable for the given stop.
     * The routing table should be created with an entry for
     * its initial stop (i.e. a mapping from the stop to a
     * RoutingEntry.RoutingEntry() for that stop.
     * @param initialStop The stop for which this table will handle routing.
     */
    public RoutingTable(Stop initialStop) {
        this.initialStop = initialStop;
        routingTable.put(initialStop,
                new RoutingEntry(initialStop, 0));
    }

    /**
     * Adds the given stop as a neighbour of the stop stored in this table.
     * A neighbouring stop should be added as a destination in this
     * table, with the cost to reach that destination simply being the
     * Manhattan distance between this table's stop and the given neighbour stop.
     *
     * If the given neighbour already exists in the table, it should be updated
     * (as defined in addOrUpdateEntry(Stop, int, Stop)).
     *
     * The 'intermediate'/'next' stop between this table's stop
     * and the new neighbour stop should simply be the neighbour stop itself.
     *
     * Once the new neighbour has been added as an entry, this table should be
     * synchronised with the rest of the network using the synchronise() method.
     * @param neighbour The stop to be added as a neighbour.
     */
    public void addNeighbour(Stop neighbour) {
        int cost = initialStop.distanceTo(neighbour);
        RoutingEntry entry = new RoutingEntry(neighbour, cost);

        if (routingTable.containsKey(neighbour)) {
            addOrUpdateEntry(neighbour, cost, neighbour);
        } else {
            routingTable.put(neighbour, entry);
        }
        this.synchronise();
    }

    /**
     * If there is currently no entry for the destination in
     * the table, a new entry for the given destination
     * should be added, with a RoutingEntry for the given
     * cost and next (intermediate) stop.
     * If there is already an entry for the given destination,
     * and the newCost is lower than the current cost associated with
     * the destination, then the entry should be updated to have the given
     * newCost and next (intermediate) stop.
     *
     * If there is already an entry for the given destination, but
     * the newCost is greater than or equal to the current cost associated
     * with the destination, then the entry should remain unchanged.
     * @param destination The destination stop to add/update the entry.
     * @param newCost The new cost to associate with the new/updated entry
     * @param intermediate The new intermediate/next stop
     *                     to associate with the new/updated entry
     * @return True if a new entry was added, or an existing one was updated,
     * or false if the table remained unchanged.
     */
    public boolean addOrUpdateEntry(Stop destination, int newCost,
                                    Stop intermediate) {
        // check destination is in routingTable
        if (routingTable.get(destination) == null) {
            routingTable.put(destination,
                    new RoutingEntry(intermediate, newCost));
        } else {
            // check newCost is less than previous cost
            if (newCost < routingTable.get(destination).getCost()) {
                routingTable.put(destination,
                        new RoutingEntry(intermediate, newCost));
            }
            else {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the cost associated with getting to the given stop.
     * @param stop The stop to get the cost.
     * @return The cost to the given stop, or Integer.MAX_VALUE if the
     * stop is not currently in this routing table.
     */
    public int costTo(Stop stop) {
        if (routingTable.get(stop) == null) {
            return Integer.MAX_VALUE;
        }

        return routingTable.get(stop).getCost();
    }

    /**
     * Maps each destination stop in this table to the cost associated
     * with getting to that destination.
     * @return A mapping from destination stops to the costs
     * associated with getting to those stops.
     */
    public Map<Stop, Integer> getCosts() {
        Map<Stop, Integer> routingCosts = new LinkedHashMap<>();

        for (Stop key : routingTable.keySet()) {
            routingCosts.put(key, routingTable.get(key).getCost());
        }
        return routingCosts;
    }

    /**
     * Return the stop for which this table will handle routing.
     * @return the stop for which this table will handle routing.
     */
    public Stop getStop() {
        return initialStop;
    }

    /**
     * Returns the next intermediate stop which passengers should be
     * routed to in order to reach the given destination. If the
     * given stop is null or not in the table, then return null
     * @param destination The destination which the passengers
     *                    are being routed.
     * @return The best stop to route the passengers to in order
     * to reach the given destination.
     */
    public Stop nextStop (Stop destination) {
        return routingTable.get(destination).getNext();
    }

    /**
     * Synchronises this routing table with the other tables
     * in the network.
     * In each iteration, every stop in the network which is reachable
     * by this table's stop (as returned by traverseNetwork()) must be
     * considered. For each stop x in the network, each of its
     * neighbours must be visited, and the entries from x must be
     * transferred to each neighbour (using the transferEntries(Stop) method).
     *
     * If any of these transfers results in a change to the table that the
     * entries are being transferred, then the entire process must be
     * repeated again. These iterations should continue happening until no
     * changes occur to any of the tables in the network.
     *
     * This process is designed to handle changes which need to be
     * propagated throughout the entire network, which could take
     * more than one iteration.
     */
    public void synchronise() {
        // a boolean value used to check whether routingTable is updated.
        boolean isChanged = true;

        // a boolean value that determines whether to continue while loop
        boolean toCheck = true;

        while (toCheck) {
            toCheck = false;
            for (Stop stop : this.traverseNetwork()) {
                List<Stop> neighbours = stop.getNeighbours();
                for (Stop neighbour : neighbours) {
                    // assign new boolean value to isChanged
                    isChanged = stop.getRoutingTable().transferEntries(
                            neighbour);
                    // if statement that defines toCheck to be false
                    // if and only if isChanged is false for all iterations
                    if (isChanged) {
                        toCheck = true;
                    }
                }
            }
        }
    }

    /**
     * Updates the entries in the routing table of the given other stop,
     * with the entries from this routing table.
     * If this routing table has entries which the other stop's table
     * doesn't, then the entries should be added to the other table
     * (as defined in addOrUpdateEntry(Stop, int, Stop)) with the cost
     * being updated to include the distance.
     *
     * If this routing table has entries which the other stop's table
     * does have, and the new cost would be lower than that associated
     * with its existing entry, then its entry should be updated (as
     * defined in addOrUpdateEntry(Stop, int, Stop)).
     *
     * If this routing table has entries which the other stop's table
     * does have, but the new cost would be greater than or equal to
     * that associated with its existing entry, then its entry should
     * remain unchanged.
     * @param other The stop whose routing table this table's entries
     *              should be transferred.
     * @return True if any new entries were added to the other stop's
     * table, or if any of its existing entries were updated, or
     * false if the other stop's table remains unchanged.
     *
     * requires: this.getStop().getNeighbours().contains(other) == true
     */
    public boolean transferEntries(Stop other) {
        // a pre-condition of this method
        assert this.getStop().getNeighbours().contains(other);

        // determine whether other.getRoutingTable() has been modified
        boolean isChanged = false;

        for (Stop key : routingTable.keySet()) {
            // returns true if entry has been updated
            boolean check = other.getRoutingTable().addOrUpdateEntry
                    (key, this.costTo(key) +
                            this.costTo(other), this.getStop());
            // isChanged is false if and only if all iterations of
            // routingTable are false
            if (check) {
                isChanged = true;
            }
        }
        return isChanged;
    }

    /**
     * Performs a traversal of all the stops in the network, and returns a
     * list of every stop which is reachable from the stop stored in this table.
     * 1)   Firstly create an empty list of Stops and an empty Stack of Stops.
     *      Push the RoutingTable's Stop on to the stack.
     * 2)   While the stack is not empty,
     *      1)  pop the top Stop (current) from the stack.
     *      2)  For each of that stop's neighbours,
     *          1)  if they are not in the list, add them to the stack.
     *      3)  Then add the current Stop to the list.
     * 3)   Return the list of seen stops.
     * @return All of the stops in the network which are reachable by the
     * stop stored in this table.
     */
    public List<Stop> traverseNetwork() {
        List<Stop> networkStops = new ArrayList<>();
        Stack<Stop> routingStops = new Stack<>();

        routingStops.push(initialStop);

        while (!routingStops.isEmpty()) {
            // remove top item from the stack
            Stop stopToCheck = routingStops.pop();
            for (Stop neighbour : stopToCheck.getNeighbours()) {
                // check whether this stop is already a neighbour
                if (!networkStops.contains(neighbour)
                        && !routingStops.contains(neighbour)) {
                    // add neighbour to stack
                    routingStops.push(neighbour);
                }
            }
            networkStops.add(stopToCheck);
        }

        return networkStops;
    }
}
