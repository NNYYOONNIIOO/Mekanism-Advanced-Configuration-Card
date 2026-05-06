package com.cleanroommc.bogosorter.api;

public interface ISortingContextBuilder {
    void addSlotGroup(int startIndex, int endIndex, int rowSize);
}
