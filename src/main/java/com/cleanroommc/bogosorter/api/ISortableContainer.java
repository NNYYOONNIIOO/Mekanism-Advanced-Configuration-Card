// STUB: This is a minimal API stub for compile-time only.
// If Inventory Bogo Sorter is present at runtime, its real classes will take precedence.
// If the real API changes method signatures, this mod will crash with NoSuchMethodError.
// Consider migrating to reflection-based registration (BogoSortAPI.addCompat) in a future release.
package com.cleanroommc.bogosorter.api;

public interface ISortableContainer {
    void buildSortingContext(ISortingContextBuilder builder);
}
