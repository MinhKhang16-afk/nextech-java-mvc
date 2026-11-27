package vn.nextech.model;

public record AdminCategoryRow(int id, String code, String name, String description,
                               int sortOrder, boolean active, long productCount) {}
