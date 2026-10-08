package vn.nextech.model;

public record Product(int id, String sku, String name, int categoryId, String categoryName,
                      String description, String specifications, String image,
                      long price, int stock, boolean featured, boolean active) {}
