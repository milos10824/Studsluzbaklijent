package org.raflab.studsluzba.service;

import java.util.List;

/**
 * Minimalna reprezentacija Spring Data Page JSON odgovora.
 * Dovoljno za prikaz sadržaja i paginaciju na klijentu.
 */
public class PageResponse<T> {
    private List<T> content;
    private int totalPages;
    private long totalElements;
    private int number; // current page index (0-based)
    private int size;

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
