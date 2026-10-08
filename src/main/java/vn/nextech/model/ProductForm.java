package vn.nextech.model;

public class ProductForm {
    private String sku, name, description, specifications;
    private int categoryId, stock;
    private long price;
    private boolean featured, active;
    public String getSku(){return sku;} public void setSku(String v){sku=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getSpecifications(){return specifications;} public void setSpecifications(String v){specifications=v;}
    public int getCategoryId(){return categoryId;} public void setCategoryId(int v){categoryId=v;}
    public int getStock(){return stock;} public void setStock(int v){stock=v;}
    public long getPrice(){return price;} public void setPrice(long v){price=v;}
    public boolean isFeatured(){return featured;} public void setFeatured(boolean v){featured=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public static ProductForm from(Product p) {
        ProductForm f=new ProductForm(); f.setSku(p.sku()); f.setName(p.name());
        f.setDescription(p.description()); f.setSpecifications(p.specifications());
        f.setCategoryId(p.categoryId()); f.setPrice(p.price()); f.setStock(p.stock());
        f.setFeatured(p.featured()); f.setActive(p.active()); return f;
    }
}
