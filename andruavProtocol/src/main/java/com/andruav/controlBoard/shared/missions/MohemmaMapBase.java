package com.andruav.controlBoard.shared.missions;

import androidx.collection.SimpleArrayMap;


/**
 * Created by M.Hefny on 16-Apr-15.
 */
public class MohemmaMapBase extends SimpleArrayMap<String, MissionBase> {

    public MissionBase Put(String key, MissionBase value) {
        synchronized (this) {
            super.put(key, value);
        }

        return value;
    }

    @Override
    public MissionBase valueAt(final int index) {
        return super.valueAt(index);
    }




    public MissionBase getWayPointByHash (final double hash)
    {
        final int len = this.size();

        MissionBase wayPointStep;
        for (int i=0;i<len;i=i+1)
        {

            wayPointStep = this.valueAt(i);
            if (wayPointStep.getHash() == hash)
            {
                return wayPointStep;
            }
        }

        return null;
    }

}