package com.andruav.andruavUnit;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AndruavLocation extends Location {



    protected  double m_alt_rel;

    public AndruavLocation(@NonNull Location location) {
        super(location);
    }

    public AndruavLocation(@Nullable String provider) {
        super(provider);
    }


    public double getAltitudeRelative() { return m_alt_rel; }

    public void setAltitudeRelative(double alt_abs) { m_alt_rel = alt_abs; }

}
