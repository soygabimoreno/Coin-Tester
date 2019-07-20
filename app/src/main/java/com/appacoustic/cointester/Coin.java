package com.appacoustic.cointester;

public class Coin {

    private String name;
    private String place;
    private int[] tones;
    private int head;
    private int tail;
    private float diameter;
    private float weight;
    private float purity;
    private float pmContent;
    private float thickness;

    public Coin(String name, String place, int head) {
        this.name = name;
        this.place = place;
        this.head = head;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public int[] getTones() {
        return tones;
    }

    public void setTones(int[] tones) {
        this.tones = tones;
    }

    public int getHead() {
        return head;
    }

    public void setHead(int head) {
        this.head = head;
    }

    public int getTail() {
        return tail;
    }

    public void setTail(int tail) {
        this.tail = tail;
    }

    public float getDiameter() {
        return diameter;
    }

    public void setDiameter(float diameter) {
        this.diameter = diameter;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public float getPurity() {
        return purity;
    }

    public void setPurity(float purity) {
        this.purity = purity;
    }

    public float getPmContent() {
        return pmContent;
    }

    public void setPmContent(float pmContent) {
        this.pmContent = pmContent;
    }

    public float getThickness() {
        return thickness;
    }

    public void setThickness(float thickness) {
        this.thickness = thickness;
    }
}