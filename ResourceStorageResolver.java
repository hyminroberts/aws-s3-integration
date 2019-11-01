/*
 * Copyright (c) 2019 XXX, Inc - All Rights Reserved.
 * This file is part of 'YYY' project, unauthorized copying of this file, via any medium is strictly prohibited.
 */

package com.xxx.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.util.List;

/**
 * Abstraction for miscellaneous storage resolutions, like storage on CDN, AWS or local server.
 * No matter where the resources will be saved, implementors of this interface deserve the following common behaviors.
 * @since Nov 16th, 2018
 * @author Zhonghui Luo
 */
public interface ResourceStorageResolver {

    public abstract Boolean exists(String resource);

    public abstract Boolean exists(long resourceId, String resource);

    public abstract InputStream getResource(String resource);

    public abstract InputStream getResource(long resourcesId, String resource);

    public abstract List<FileItem> getResourcesSummary(ContentFileType contentType, String directory);

    public abstract List<FileItem> getSummarizedResources(String filePath);

    public abstract void storeResource(String resource);

    public abstract void storeResource(String resource, byte[] bytes, String mime);

    public abstract void storeResource(String resource, String mime, InputStream stream) throws FileAlreadyExistsException;

    public abstract void storeResource(long resourceId, String resource, String mime, String data) throws FileAlreadyExistsException;

    public abstract void storeResource(long resourceId, String resource, String mime, InputStream stream) throws FileAlreadyExistsException;

    public abstract boolean storeResourceDirectly(ContentFileType contentType, MultipartParams params, String subDir, String... resources) throws IOException;

    public abstract void deleteResource(long resourceId, String resource);

    public abstract void deleteResource(String resource);

    public abstract void deleteResourcesInDirectory(String fileKeyPrefix);

    public abstract void copyResource(ContentFileType contentType, String src, String dest);

    public abstract String constructRelativePath(ContentFileType contentType, String path);

    public abstract String constructRelativePathWithFileName(ContentFileType contentType, String resource, String subDir);

    public abstract String getResourceUrl(String resource);

}
