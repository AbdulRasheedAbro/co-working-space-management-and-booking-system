package com.time.coworking;

import com.time.coworking.repository.SchemaInitializer;

public class Launcher {
    public static void main(String[] args) {
        SchemaInitializer.initialize();
        Main.main(args);
    }
}