package mchorse.bbs_mod.utils.resources;

import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Colors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * API for working with custom CDN service. Once I'll upload the script,
 * I'll add the URL to the script over here.
 */
public class CDNAssetSyncService
{
    private final HttpClient client;
    private final URI cdn;
    private final Path assets;
    private final Consumer<Pair<CDNStatus, IKey>> callback;

    private static String sha1OfFile(Path path) throws IOException
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            try (InputStream in = Files.newInputStream(path))
            {
                byte[] buffer = new byte[8192];
                int read;

                while ((read = in.read(buffer)) != -1)
                {
                    digest.update(buffer, 0, read);
                }
            }

            return toHex(digest.digest());
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("SHA-1 not available", e);
        }
    }

    private static String toHex(byte[] bytes)
    {
        StringBuilder builder = new StringBuilder(bytes.length * 2);

        for (byte b : bytes)
        {
            int value = b & 0xFF;

            if (value < 16) builder.append('0');

            builder.append(Integer.toHexString(value));
        }

        return builder.toString();
    }

    private static List<RemoteFile> parseRemoteFiles(String json)
    {
        List<RemoteFile> files = new ArrayList<>();
        ListType list = DataToString.listFromString(json);

        if (list == null)
        {
            return files;
        }

        for (BaseType baseType : list)
        {
            if (!baseType.isMap())
            {
                continue;
            }

            MapType map = baseType.asMap();

            files.add(new RemoteFile(map.getString("path"), map.getInt("size"), map.getString("sha1")));
        }

        return files;
    }

    private static String normalizeRelativePath(String path)
    {
        return path.replace(FileSystems.getDefault().getSeparator(), "/");
    }

    public CDNAssetSyncService(String baseUrl, Path localRootDir, Consumer<Pair<CDNStatus, IKey>> callback)
    {
        this.client = HttpClient.newBuilder().build();
        this.cdn = URI.create(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl);
        this.assets = localRootDir;
        this.callback = callback;
    }

    private void issueStatus(CDNStatus status, IKey message)
    {
        if (this.callback != null)
        {
            this.callback.accept(new Pair<>(status, message));
        }
    }

    private List<RemoteFile> fetchRemoteFiles() throws IOException, InterruptedException
    {
        HttpRequest request = HttpRequest.newBuilder(this.cdn.resolve("/files")).GET().build();
        HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200)
        {
            this.issueStatus(CDNStatus.FAILURE, UIKeys.CDN_STATUS_FAILED_FETCH);

            throw new IOException("Failed to fetch /files: HTTP " + response.statusCode());
        }

        return parseRemoteFiles(response.body());
    }

    public void syncOnce() throws IOException, InterruptedException
    {
        List<RemoteFile> remoteFiles = this.fetchRemoteFiles();
        Map<String, String> sha1Map = this.buildLocalSha1Map();

        for (RemoteFile remoteFile : remoteFiles)
        {
            String normalizedPath = remoteFile.path.replace("/", FileSystems.getDefault().getSeparator());
            Path localPath = this.assets.resolve(normalizedPath);
            String localSha1 = sha1Map.get(remoteFile.path);
            boolean needsDownload = !remoteFile.sha1.equalsIgnoreCase(localSha1);

            if (needsDownload)
            {
                System.out.println("[CDN] Downloading: " + remoteFile.path);

                this.downloadFile(remoteFile.path, localPath);
                this.issueStatus(CDNStatus.DOWNLOADED, UIKeys.CDN_STATUS_DOWNLOADED.format(remoteFile.path));
            }
        }

        this.issueStatus(CDNStatus.SUCCESS, UIKeys.CDN_STATUS_SUCCESS_DOWNLOADING);
    }

    private Map<String, String> buildLocalSha1Map() throws IOException
    {
        Map<String, String> result = new HashMap<>();

        Files.walkFileTree(this.assets, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if (!attrs.isRegularFile())
                {
                    return FileVisitResult.CONTINUE;
                }

                String relativePath = normalizeRelativePath(CDNAssetSyncService.this.assets.relativize(file).toString());
                String sha1 = sha1OfFile(file);

                result.put(relativePath, sha1);

                return FileVisitResult.CONTINUE;
            }
        });

        return result;
    }

    private void downloadFile(String remotePath, Path localPath) throws IOException, InterruptedException
    {
        String form = "path=" + URLEncoder.encode(remotePath, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest
            .newBuilder(this.cdn.resolve("/file"))
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build();
        HttpResponse<InputStream> response = this.client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200)
        {
            this.issueStatus(CDNStatus.FAILURE, UIKeys.CDN_STATUS_FAILED_DOWNLOADING.format(remotePath));

            throw new IOException("Failed to download " + remotePath + ": HTTP " + response.statusCode());
        }

        Files.createDirectories(localPath.getParent());

        try (InputStream in = response.body())
        {
            Files.copy(in, localPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void pushChangedFiles(String uploadToken) throws IOException, InterruptedException
    {
        List<RemoteFile> remoteFiles = this.fetchRemoteFiles();
        Map<String, String> remoteSha1 = new HashMap<>();

        for (RemoteFile rf : remoteFiles)
        {
            remoteSha1.put(rf.path, rf.sha1);
        }

        Files.walkFileTree(this.assets, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if (!attrs.isRegularFile())
                {
                    return FileVisitResult.CONTINUE;
                }

                String relativePath = normalizeRelativePath(CDNAssetSyncService.this.assets.relativize(file).toString());

                String localSha1 = sha1OfFile(file);
                String remote = remoteSha1.get(relativePath);

                boolean needsUpload = (remote == null) || !remote.equalsIgnoreCase(localSha1);

                if (needsUpload)
                {
                    System.out.println("[CDN] Uploading: " + relativePath);

                    try
                    {
                        CDNAssetSyncService.this.uploadFileToCDN(file, relativePath, uploadToken);
                        CDNAssetSyncService.this.issueStatus(CDNStatus.UPLOADED, UIKeys.CDN_STATUS_UPLOADED.format(relativePath));
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                        throw new IOException("Upload interrupted for " + relativePath, e);
                    }
                }

                return FileVisitResult.CONTINUE;
            }
        });

        for (RemoteFile remoteFile : remoteFiles)
        {
            String normalizedPath = remoteFile.path.replace("/", FileSystems.getDefault().getSeparator());
            Path localPath = this.assets.resolve(normalizedPath);

            if (!Files.exists(localPath))
            {
                System.out.println("[CDN] Deleting remote: " + remoteFile.path);

                try
                {
                    this.deleteRemoteFile(remoteFile.path, uploadToken);
                    this.issueStatus(CDNStatus.DELETED, UIKeys.CDN_STATUS_DELETED.format(remoteFile.path));
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();

                    throw new IOException("Delete interrupted for " + remoteFile.path, e);
                }
            }
        }

        this.issueStatus(CDNStatus.SUCCESS, UIKeys.CDN_STATUS_SUCCESS_UPLOADING);
    }

    private void uploadFileToCDN(Path file, String remotePath, String uploadToken) throws IOException, InterruptedException
    {
        String boundary = "----CdnBoundary" + System.currentTimeMillis();

        byte[] fileBytes = Files.readAllBytes(file);
        String fileName = file.getFileName().toString();
        String mimeType = Files.probeContentType(file);

        if (mimeType == null)
        {
            mimeType = "application/octet-stream";
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8), true);

        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"path\"\r\n\r\n");
        writer.append(remotePath).append("\r\n");

        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
        writer.append("Content-Type: ").append(mimeType).append("\r\n\r\n");
        writer.flush();

        baos.write(fileBytes);
        baos.write("\r\n".getBytes(StandardCharsets.UTF_8));

        writer.append("--").append(boundary).append("--\r\n");
        writer.flush();

        byte[] body = baos.toByteArray();

        HttpRequest request = HttpRequest
            .newBuilder(this.cdn.resolve("/upload"))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .header("X-Token", uploadToken)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();
        HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200)
        {
            throw new IOException("Failed to upload " + remotePath + ": HTTP " + response.statusCode() + " body=" + response.body());
        }
    }

    private void deleteRemoteFile(String remotePath, String uploadToken) throws IOException, InterruptedException
    {
        String form = "path=" + URLEncoder.encode(remotePath, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest
            .newBuilder(this.cdn.resolve("/delete"))
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("X-Token", uploadToken)
            .build();
        HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();

        if (status != 200 && status != 204 && status != 404)
        {
            throw new IOException("Failed to delete " + remotePath + ": HTTP " + status + " body=" + response.body());
        }
    }

    public static class RemoteFile
    {
        public final String path;
        public final long size;
        public final String sha1;

        public RemoteFile(String path, long size, String sha1)
        {
            this.path = path;
            this.size = size;
            this.sha1 = sha1;
        }
    }

    public static enum CDNStatus
    {
        SUCCESS(Colors.GREEN), DOWNLOADED(Colors.BLUE), UPLOADED(Colors.ACTIVE), DELETED(Colors.ORANGE), FAILURE(Colors.RED);

        public final int color;

        private CDNStatus(int color)
        {
            this.color = color;
        }
    }
}