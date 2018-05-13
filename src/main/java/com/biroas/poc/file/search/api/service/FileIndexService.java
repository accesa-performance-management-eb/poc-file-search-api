package com.biroas.poc.file.search.api.service;

import com.biroas.poc.file.search.api.model.file.File;
import com.biroas.poc.file.search.api.model.result.IndexResult;
import com.biroas.poc.file.search.api.repository.FileRepository;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

@Service
public class FileIndexService {

    private FileRepository fileRepository;

    @Inject
    public FileIndexService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public File indexFile(File file) {
        return fileRepository.save(file);
    }

    public IndexResult indexDirectory(String path, boolean recursive) throws IOException {
        IndexResult indexResult = new IndexResult();

        java.io.File folder = new java.io.File(path);
        java.io.File[] listOfFiles = folder.listFiles();
        long count = 0;

        if (listOfFiles == null) {
            return indexResult;
        }

        for (java.io.File diskFile : listOfFiles) {
            File file = new File();
            file.setFileName(diskFile.getName());
            file.setParentDirectory(diskFile.getParentFile().getAbsolutePath());
            file.setDirectory(diskFile.isDirectory());

            if (recursive && file.isDirectory()) {
                count += indexDirectory(file.getParentDirectory() + java.io.File.separator + file.getFileName(), true)
                        .getIndexedDocuments();
            }

            if (!file.isDirectory()) {

                BasicFileAttributes fileAttributes = Files.readAttributes(diskFile.toPath(), BasicFileAttributes.class);
                file.getFileAttributes().setCreationDate(new Date(fileAttributes.creationTime().toMillis()));
                file.getFileAttributes().setLastModifiedDate(new Date(fileAttributes.lastModifiedTime().toMillis()));
                file.getFileAttributes().setLastAccessDate(new Date(fileAttributes.lastAccessTime().toMillis()));
                file.getFileAttributes().setSize(fileAttributes.size());

            }

            this.indexFile(file);
            count++;
        }

        indexResult.setIndexedDocuments(count);
        return indexResult;
    }

    public IndexResult deleteAllFiles() {
        IndexResult indexResult = new IndexResult();
        indexResult.setDeletedDocuments(fileRepository.count());
        fileRepository.deleteAll();
        return indexResult;
    }
}
