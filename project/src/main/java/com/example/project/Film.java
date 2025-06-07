package com.example.project;

public class Film {
    private int id;
    private String title;
    private String genre;
    private int year;
    private boolean watched;

    public Film(int id, String title, String genre, int year, boolean watched) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.year = year;
        this.watched = watched;
    }
    public Film() {}

    // Геттеры и сеттеры
    public int getId() {return id;}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public boolean isWatched() { return watched; }
    public void setWatched(boolean watched) { this.watched = watched; }
}
