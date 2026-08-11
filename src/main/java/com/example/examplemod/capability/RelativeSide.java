package com.example.examplemod.capability;

import net.minecraft.core.Direction;

/**
 * Relative face from block FACING.
 */
public enum RelativeSide {
    FRONT,
    BACK,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM;

    public static RelativeSide from(Direction facing, Direction absolute) {
        if (absolute == Direction.UP) {
            return TOP;
        }
        if (absolute == Direction.DOWN) {
            return BOTTOM;
        }
        if (absolute == facing) {
            return FRONT;
        }
        if (absolute == facing.getOpposite()) {
            return BACK;
        }
        if (absolute == facing.getClockWise()) {
            return RIGHT;
        }
        if (absolute == facing.getCounterClockWise()) {
            return LEFT;
        }
        return BACK;
    }
}
