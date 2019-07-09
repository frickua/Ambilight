package com.frickua.ambilight;

public class Resolution {
        int x;
        int y;

        public Resolution(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return x + "x" + y;
        }
    }