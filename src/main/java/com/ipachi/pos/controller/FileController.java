package com.ipachi.pos.controller;


import com.ipachi.pos.service.FileStorageDbService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageDbService storage;

    @GetMapping("/{id}")
    public ResponseEntity<ByteArrayResource> download(@PathVariable String id,
                                                      @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        var asset = storage.get(id);
        String etag = "\"" + asset.getId() + "-" + asset.getSize() + "\"";
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        var body = new ByteArrayResource(asset.getData());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.getContentType()))
                .contentLength(asset.getSize())
                .eTag(etag)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(asset.getFilename()).build().toString())
                .cacheControl(CacheControl.noCache()) // tweak to taste
                .body(body);
    }
}
