package com.example.timphongtro.Models;
 
public class ExtensionRoom_class {
    private String name;
    private String img;

    private String id;

    public ExtensionRoom_class() {
    }

    public ExtensionRoom_class(String id,String name, String img) {
        this.id = id;
        this.name = name;
        this.img = img;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name; 
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }


}
