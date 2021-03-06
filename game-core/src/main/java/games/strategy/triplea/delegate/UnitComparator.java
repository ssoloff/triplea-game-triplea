package games.strategy.triplea.delegate;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.util.TransportUtils;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A collection of comparators for sorting units based on various heuristics. */
public final class UnitComparator {
  private UnitComparator() {}

  static Comparator<Unit> getLowestToHighestMovementComparator() {
    return Comparator.comparing(
        TripleAUnit::get, Comparator.comparing(TripleAUnit::getMovementLeft));
  }

  public static Comparator<Unit> getHighestToLowestMovementComparator() {
    return getLowestToHighestMovementComparator().reversed();
  }

  /**
   * A Helper method that returns a Comparator comparing Units based on the total transporting cost
   * of transportable Units in the same territory as the Unit.
   *
   * <p>Because figuring out this cost is a relatively expensive operation The returned Comparator
   * stores lazily calculated costs for its lifetime.
   *
   * @return A {@code Comparator<Unit>} that compares Units by transport capacity
   */
  public static Comparator<Unit> getIncreasingCapacityComparator() {
    final Map<Unit, Integer> cache = new HashMap<>();
    return Comparator.comparingInt(
        u ->
            cache.computeIfAbsent(
                u, k -> TransportUtils.getTransportCost(TripleAUnit.get(u).getTransporting())));
  }

  private static Comparator<Unit> getDecreasingCapacityComparator() {
    return getIncreasingCapacityComparator().reversed();
  }

  /** Return a Comparator that will order the specified transports in preferred load order. */
  public static Comparator<Unit> getLoadableTransportsComparator(
      final Route route, final PlayerId player) {
    return Comparator.comparing(Matches.transportCannotUnload(route.getEnd())::test)
        .thenComparing(Unit::getOwner, Comparator.comparing(player::equals).reversed())
        .thenComparing(getDecreasingCapacityComparator())
        .thenComparing(
            TripleAUnit::get, Comparator.comparing(TripleAUnit::getMovementLeft).reversed())
        .thenComparingInt(Object::hashCode);
  }

  /** Return a Comparator that will order the specified transports in preferred unload order. */
  public static Comparator<Unit> getUnloadableTransportsComparator(
      final Route route, final PlayerId player, final boolean noTies) {
    return Comparator.comparing(Matches.transportCannotUnload(route.getEnd())::test)
        .thenComparing(Unit::getOwner, Comparator.comparing(player::equals))
        .thenComparing(getDecreasingCapacityComparator())
        .thenComparing(TripleAUnit::get, Comparator.comparing(TripleAUnit::getMovementLeft))
        .thenComparingInt(t -> noTies ? t.hashCode() : 0);
  }

  /** Return a Comparator that will order the specified units in preferred move order. */
  public static Comparator<Unit> getMovableUnitsComparator(
      final List<Unit> units, final Route route) {
    final Comparator<Unit> decreasingCapacityComparator = getDecreasingCapacityComparator();
    return (u1, u2) -> {
      // Ensure units have enough movement
      final BigDecimal left1 = TripleAUnit.get(u1).getMovementLeft();
      final BigDecimal left2 = TripleAUnit.get(u2).getMovementLeft();
      if (route != null) {
        if (left1.compareTo(route.getMovementCost(u1)) >= 0
            && left2.compareTo(route.getMovementCost(u2)) < 0) {
          return -1;
        }
        if (left1.compareTo(route.getMovementCost(u1)) < 0
            && left2.compareTo(route.getMovementCost(u2)) >= 0) {
          return 1;
        }
      }

      // Prefer transports for which dependents are also selected
      final Collection<Unit> transporting1 = TripleAUnit.get(u1).getTransporting();
      final Collection<Unit> transporting2 = TripleAUnit.get(u2).getTransporting();
      final int hasDepends1 = units.containsAll(transporting1) ? 1 : 0;
      final int hasDepends2 = units.containsAll(transporting2) ? 1 : 0;
      if (hasDepends1 != hasDepends2) {
        return hasDepends1 - hasDepends2;
      }

      // Sort by decreasing transport capacity (only valid for transports)
      final int compareCapacity = decreasingCapacityComparator.compare(u1, u2);
      if (compareCapacity != 0) {
        return compareCapacity;
      }

      // Sort by increasing movement normally, but by decreasing movement during loading
      if (left1.compareTo(left2) != 0) {
        return (route != null && route.isLoad()) ? left2.compareTo(left1) : left1.compareTo(left2);
      }

      return Integer.compare(u1.hashCode(), u2.hashCode());
    };
  }

  /**
   * Return a Comparator that will order the specified units in preferred unload order. If needed it
   * may also inspect the transport holding the units.
   */
  public static Comparator<Unit> getUnloadableUnitsComparator(
      final List<Unit> units, final Route route, final PlayerId player) {
    return Comparator.comparing(
            TripleAUnit::get,
            Comparator.comparing(
                TripleAUnit::getTransportedBy,
                Comparator.nullsLast(getUnloadableTransportsComparator(route, player, false))))
        .thenComparing(getMovableUnitsComparator(units, route));
  }

  static Comparator<Unit> getDecreasingBombardComparator() {
    return Comparator.comparing(
        Unit::getType,
        Comparator.comparing(
            UnitAttachment::get, Comparator.comparingInt(UnitAttachment::getBombard).reversed()));
  }
}
