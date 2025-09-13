package com.ipachi.pos.service;


import com.ipachi.pos.model.FileAsset;
import com.ipachi.pos.repo.FileAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileStorageDbService {

    private final FileAssetRepository repo;

    public FileAsset save(MultipartFile file) throws java.io.IOException {
        if (file == null || file.isEmpty()) return null;
        var asset = FileAsset.builder()
                .filename(safeName(file.getOriginalFilename()))
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .size(file.getSize())
                .data(file.getBytes())
                .build();
        return repo.save(asset);
    }

    public FileAsset get(String id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("File not found"));
    }

    private static String safeName(String n) { return n == null ? "file" : n.replaceAll("[\\r\\n]", "_"); }
}
