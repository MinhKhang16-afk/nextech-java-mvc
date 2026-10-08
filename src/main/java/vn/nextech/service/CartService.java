package vn.nextech.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import vn.nextech.model.CartItem;
import vn.nextech.model.Product;
import vn.nextech.repository.ShopRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartService {
    private final ShopRepository repository;
    public CartService(ShopRepository repository){this.repository=repository;}

    @SuppressWarnings("unchecked")
    public Map<Integer,Integer> values(HttpSession session){
        Object old=session.getAttribute("CART");
        if(old instanceof Map<?,?>) return (Map<Integer,Integer>)old;
        Map<Integer,Integer> cart=new LinkedHashMap<>(); session.setAttribute("CART",cart); return cart;
    }
    public void add(HttpSession session, int id, int amount){
        Product p=repository.activeById(id).orElseThrow(()->new IllegalArgumentException("Sản phẩm không tồn tại"));
        if(amount<1 || amount>50 || p.stock()<1)throw new IllegalArgumentException("Số lượng không hợp lệ hoặc hết hàng");
        Map<Integer,Integer> cart=values(session);
        int next=Math.addExact(cart.getOrDefault(id,0),amount);
        if(next>p.stock())throw new IllegalArgumentException("Số lượng vượt quá hàng còn trong kho");
        cart.put(id,next);
    }
    public void change(HttpSession session,int id,int amount){
        Map<Integer,Integer> cart=values(session);
        if(amount<=0){cart.remove(id);return;}
        Product p=repository.activeById(id).orElseThrow(()->new IllegalArgumentException("Sản phẩm không còn kinh doanh"));
        if(amount>p.stock())throw new IllegalArgumentException("Số lượng vượt quá tồn kho");
        cart.put(id,amount);
    }
    public List<CartItem> items(HttpSession session){
        List<CartItem> result=new ArrayList<>();
        values(session).forEach((id,qty)->repository.activeById(id).ifPresent(p->result.add(new CartItem(p,qty))));
        return result;
    }
    public long total(HttpSession session){
        long sum=0; for(CartItem item:items(session))sum=Math.addExact(sum,item.subtotal()); return sum;
    }
    public int count(HttpSession session){return values(session).values().stream().mapToInt(Integer::intValue).sum();}
    public void clear(HttpSession session){values(session).clear();}
}
