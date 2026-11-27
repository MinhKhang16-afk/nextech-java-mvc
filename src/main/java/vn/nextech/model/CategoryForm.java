package vn.nextech.model;

public class CategoryForm {
    private String code;
    private String name;
    private String description;
    private int sortOrder;
    private boolean active = true;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public static CategoryForm from(AdminCategoryRow category) {
        CategoryForm form = new CategoryForm();
        form.setCode(category.code());
        form.setName(category.name());
        form.setDescription(category.description());
        form.setSortOrder(category.sortOrder());
        form.setActive(category.active());
        return form;
    }
}
