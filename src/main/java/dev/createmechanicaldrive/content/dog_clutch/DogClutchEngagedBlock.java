package dev.createmechanicaldrive.content.dog_clutch;

import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;

public class DogClutchEngagedBlock
        extends DogClutchBlock
        implements ICogWheel {

    public DogClutchEngagedBlock(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    public boolean isSmallCog() {
        return true;
    }

    @Override
    public boolean isLargeCog() {
        return false;
    }
}