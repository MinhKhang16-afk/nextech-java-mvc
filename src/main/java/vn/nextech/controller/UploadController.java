package vn.nextech.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import vn.nextech.service.ImageService;

@Controller
public class UploadController {
    private final ImageService images;
    public UploadController(ImageService images){this.images=images;}
    @GetMapping("/uploads/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> uploaded(@PathVariable String filename){return images.serve(filename);}
}
