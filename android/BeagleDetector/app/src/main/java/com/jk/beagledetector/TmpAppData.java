package com.jk.beagledetector;

public class TmpAppData {
    private static TmpAppData instance = new TmpAppData();

    private TmpAppData()
    {

    }

    public static TmpAppData getInstance()
    {
        return instance;
    }

    public boolean debug_mode = false;
    public int model = 0;
    public float threshlod = 0.5f;
}
