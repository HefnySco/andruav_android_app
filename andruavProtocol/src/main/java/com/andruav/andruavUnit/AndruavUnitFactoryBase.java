package com.andruav.andruavUnit;

import com.andruav.interfaces.IAndruavWe7daMasna3;

/**
 * Created by M.Hefny on 14-Feb-15.
 */
public class AndruavUnitFactoryBase implements IAndruavWe7daMasna3 {

    @Override
    public AndruavUnitBase createAndruavUnitClass(final String groupName, final String senderName, final boolean isGCS) {
        return new AndruavUnitBase(groupName,senderName, isGCS);
    }
}
