package vn.nextech.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ImageService {
    private final Path root;
    public ImageService(@Value("${shop.upload-dir}") String root){this.root=Path.of(root).toAbsolutePath().normalize();}
    public String save(MultipartFile file) throws IOException {
        if(file==null||file.isEmpty())return null;
        if(file.getSize()>4_000_000)throw new IllegalArgumentException("Ảnh tối đa 4 MB");
        if(!MediaType.IMAGE_JPEG_VALUE.equals(file.getContentType()) && !MediaType.IMAGE_PNG_VALUE.equals(file.getContentType()))
            throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPEG/PNG");
        BufferedImage img;
        try(var input=file.getInputStream()){ img=ImageIO.read(input); }
        if(img==null|| (long)img.getWidth()*img.getHeight()>12_000_000L)
            throw new IllegalArgumentException("File ảnh không hợp lệ hoặc độ phân giải quá lớn");
        Files.createDirectories(root);
        String filename=UUID.randomUUID()+".png";
        Path destination=root.resolve(filename);
        if(!ImageIO.write(img,"png",destination.toFile()))throw new IOException("Không ghi được ảnh PNG");
        return "uploads/"+filename;
    }
    public ResponseEntity<Resource> serve(String file){
        if(!file.matches("[a-f0-9-]{36}\\.png"))return ResponseEntity.notFound().build();
        Path selected=root.resolve(file).normalize();
        if(!selected.startsWith(root)||!Files.isRegularFile(selected))return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
            .cacheControl(CacheControl.maxAge(1,TimeUnit.DAYS))
            .body(new FileSystemResource(selected));
    }
}
