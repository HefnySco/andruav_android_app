package com.andruav.andruavUnit;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AndruavLocation extends Location {



    protected  double m_alt_abs;

    public AndruavLocation(@NonNull Location location) {
        super(location);
    }

    public AndruavLocation(@Nullable String provider) {
        super(provider);
    }


    public double getAltitudeAbsolute() { return m_alt_abs; }

    public void setAltitudeAbsolute(double alt_abs) { m_alt_abs = alt_abs; }

}
