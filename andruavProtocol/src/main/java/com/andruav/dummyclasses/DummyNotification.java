package com.andruav.dummyclasses;

import android.text.Spanned;

import com.andruav.interfaces.INotification;

/**
 * Created by M.Hefny on 08-Feb-15.
 */
public class DummyNotification implements INotification {

    @Override
    public void displayNotification(int smallLogo, Spanned title, Spanned text, boolean Sound, int Id, boolean isPresistant) {

    }

    @Override
    public void displayNotification(int smallLogo, String title, String text, boolean Sound, int Id, boolean isPresistant)
    {

    }

    @Override
    public void displayNotification(String title, String text, boolean Sound, int Id, boolean isPresistant)
    {

    }


    @Override
    public void Cancel(int Id)
    {

    }

    @Override
    public void Speak(String message) {

    }


}
