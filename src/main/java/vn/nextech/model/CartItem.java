package vn.nextech.model;

public record CartItem(Product product, int quantity) {
    public long subtotal() { return Math.multiplyExact(product.price(), quantity); }
}
