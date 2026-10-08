package vn.nextech.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CartItemTest {
    @Test
    void subtotalUsesServerPrice() {
        Product p=new Product(1,"TEST","Củ sạc 20W",3,"Củ sạc","", "", "assets/img/products/default.svg",199000,10,false,true);
        CartItem item=new CartItem(p,3);
        assertEquals(597000L,item.subtotal());
    }
}
