package com.immon.truckorbit;

import android.app.Application;
import android.content.Context;

import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.ref.WeakReference;

public class TruckOrbit extends Application {

    private static TruckOrbit instance;
    private static WeakReference<Context> contextReference;
    private static WeakReference<FirebaseFirestore> firestoreReference;

    public void onCreate() {
        super.onCreate();
        instance = this;
        contextReference = new WeakReference<>(getApplicationContext());
    }

    public static Context getAppContext() {
        if (contextReference == null || contextReference.get() == null) {
            contextReference = new WeakReference<>(TruckOrbit.getInstance().getApplicationContext());
        }
        return contextReference.get();
    }

    private static TruckOrbit getInstance() {
        if (instance == null) {
            instance = new TruckOrbit();
        }
        return instance;
    }
}