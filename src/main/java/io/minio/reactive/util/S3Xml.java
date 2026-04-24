package io.minio.reactive.util;

import io.minio.reactive.messages.BucketInfo;
import io.minio.reactive.messages.BucketVersioningConfiguration;
import io.minio.reactive.messages.CompletePart;
import io.minio.reactive.messages.CompletedMultipartUpload;
import io.minio.reactive.messages.DeletedObject;
import io.minio.reactive.messages.DeleteObjectsResult;
import io.minio.reactive.messages.ListObjectsResult;
import io.minio.reactive.messages.ListObjectVersionsResult;
import io.minio.reactive.messages.ListMultipartUploadsResult;
import io.minio.reactive.messages.ListPartsResult;
import io.minio.reactive.messages.DeleteMarkerInfo;
import io.minio.reactive.messages.MultipartUpload;
import io.minio.reactive.messages.MultipartUploadInfo;
import io.minio.reactive.messages.ObjectAttributes;
import io.minio.reactive.messages.ObjectInfo;
import io.minio.reactive.messages.ObjectLegalHoldConfiguration;
import io.minio.reactive.messages.ObjectRetentionConfiguration;
import io.minio.reactive.messages.ObjectVersionInfo;
import io.minio.reactive.messages.PartInfo;
import io.minio.reactive.messages.RestoreObjectRequest;
import io.minio.reactive.messages.S3Error;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** 只依赖 JDK 的 S3 XML 响应和配置载荷辅助工具。 */
public final class S3Xml {
  private S3Xml() {}

  public static List<BucketInfo> parseBuckets(String xml) {
    if (isBlank(xml)) {
      return Collections.emptyList();
    }
    Document document = parse(xml);
    NodeList nodes = document.getElementsByTagName("Bucket");
    List<BucketInfo> result = new ArrayList<BucketInfo>();
    for (int i = 0; i < nodes.getLength(); i++) {
      Element bucket = (Element) nodes.item(i);
      result.add(new BucketInfo(text(bucket, "Name"), text(bucket, "CreationDate")));
    }
    return result;
  }

  public static String parseBucketLocation(String xml) {
    if (isBlank(xml)) {
      return "";
    }
    Document document = parse(xml);
    String text = document.getDocumentElement().getTextContent();
    return text == null ? "" : text.trim();
  }

  public static ListObjectsResult parseListObjectsV2(String xml) {
    Document document = parse(xml);
    Element root = document.getDocumentElement();
    List<ObjectInfo> objects = new ArrayList<ObjectInfo>();
    NodeList contents = root.getElementsByTagName("Contents");
    for (int i = 0; i < contents.getLength(); i++) {
      Element content = (Element) contents.item(i);
      objects.add(
          new ObjectInfo(
              text(content, "Key"),
              text(content, "LastModified"),
              text(content, "ETag"),
              parseLong(text(content, "Size")),
              text(content, "StorageClass")));
    }

    List<String> prefixes = new ArrayList<String>();
    NodeList commonPrefixes = root.getElementsByTagName("CommonPrefixes");
    for (int i = 0; i < commonPrefixes.getLength(); i++) {
      prefixes.add(text((Element) commonPrefixes.item(i), "Prefix"));
    }

    return new ListObjectsResult(
        text(root, "Name"),
        text(root, "Prefix"),
        text(root, "Delimiter"),
        parseBoolean(text(root, "IsTruncated")),
        text(root, "ContinuationToken"),
        text(root, "NextContinuationToken"),
        objects,
        prefixes);
  }

  public static ListObjectVersionsResult parseListObjectVersions(String xml) {
    Document document = parse(xml);
    Element root = document.getDocumentElement();
    List<ObjectVersionInfo> versions = new ArrayList<ObjectVersionInfo>();
    NodeList versionNodes = root.getElementsByTagName("Version");
    for (int i = 0; i < versionNodes.getLength(); i++) {
      Element version = (Element) versionNodes.item(i);
      versions.add(
          new ObjectVersionInfo(
              text(version, "Key"),
              text(version, "VersionId"),
              parseBoolean(text(version, "IsLatest")),
              text(version, "LastModified"),
              text(version, "ETag"),
              parseLong(text(version, "Size")),
              text(version, "StorageClass"),
              text(version, "ID"),
              text(version, "DisplayName")));
    }

    List<DeleteMarkerInfo> deleteMarkers = new ArrayList<DeleteMarkerInfo>();
    NodeList markerNodes = root.getElementsByTagName("DeleteMarker");
    for (int i = 0; i < markerNodes.getLength(); i++) {
      Element marker = (Element) markerNodes.item(i);
      deleteMarkers.add(
          new DeleteMarkerInfo(
              text(marker, "Key"),
              text(marker, "VersionId"),
              parseBoolean(text(marker, "IsLatest")),
              text(marker, "LastModified"),
              text(marker, "ID"),
              text(marker, "DisplayName")));
    }

    return new ListObjectVersionsResult(
        text(root, "Name"),
        text(root, "Prefix"),
        text(root, "KeyMarker"),
        text(root, "VersionIdMarker"),
        text(root, "NextKeyMarker"),
        text(root, "NextVersionIdMarker"),
        text(root, "Delimiter"),
        parseInt(text(root, "MaxKeys")),
        parseBoolean(text(root, "IsTruncated")),
        versions,
        deleteMarkers,
        commonPrefixes(root));
  }

  public static ListMultipartUploadsResult parseListMultipartUploads(String xml) {
    Document document = parse(xml);
    Element root = document.getDocumentElement();
    List<MultipartUploadInfo> uploads = new ArrayList<MultipartUploadInfo>();
    NodeList uploadNodes = root.getElementsByTagName("Upload");
    for (int i = 0; i < uploadNodes.getLength(); i++) {
      Element upload = (Element) uploadNodes.item(i);
      uploads.add(
          new MultipartUploadInfo(
              text(upload, "Key"),
              text(upload, "UploadId"),
              text(upload, "Initiated"),
              text(upload, "StorageClass"),
              text(upload, "ID"),
              text(upload, "DisplayName")));
    }
    return new ListMultipartUploadsResult(
        text(root, "Bucket"),
        text(root, "Prefix"),
        text(root, "KeyMarker"),
        text(root, "UploadIdMarker"),
        text(root, "NextKeyMarker"),
        text(root, "NextUploadIdMarker"),
        text(root, "Delimiter"),
        parseInt(text(root, "MaxUploads")),
        parseBoolean(text(root, "IsTruncated")),
        uploads,
        commonPrefixes(root));
  }

  public static MultipartUpload parseCreateMultipartUpload(String xml) {
    Document document = parse(xml);
    Element root = document.getDocumentElement();
    return new MultipartUpload(text(root, "Bucket"), text(root, "Key"), text(root, "UploadId"));
  }

  public static ListPartsResult parseListParts(String xml) {
    Document document = parse(xml);
    Element root = document.getDocumentElement();
    List<PartInfo> parts = new ArrayList<PartInfo>();
    NodeList nodes = root.getElementsByTagName("Part");
    for (int i = 0; i < nodes.getLength(); i++) {
      Element part = (Element) nodes.item(i);
      parts.add(
          new PartInfo(
              parseInt(text(part, "PartNumber")),
              text(part, "ETag"),
              parseLong(text(part, "Size")),
              text(part, "LastModified")));
    }
    return new ListPartsResult(
        text(root, "Bucket"),
        text(root, "Key"),
        text(root, "UploadId"),
        parseBoolean(text(root, "IsTruncated")),
        parseInt(text(root, "NextPartNumberMarker")),
        parts);
  }

  public static CompletedMultipartUpload parseCompleteMultipartUpload(String xml) {
    Document document = parse(xml);
    Element root = document.getDocumentElement();
    return new CompletedMultipartUpload(
        text(root, "Location"), text(root, "Bucket"), text(root, "Key"), text(root, "ETag"));
  }

  public static DeleteObjectsResult parseDeleteObjects(String xml) {
    if (isBlank(xml)) {
      return new DeleteObjectsResult(Collections.<DeletedObject>emptyList());
    }
    Document document = parse(xml);
    NodeList nodes = document.getDocumentElement().getElementsByTagName("Deleted");
    List<DeletedObject> deleted = new ArrayList<DeletedObject>();
    for (int i = 0; i < nodes.getLength(); i++) {
      Element entry = (Element) nodes.item(i);
      deleted.add(
          new DeletedObject(
              text(entry, "Key"),
              text(entry, "VersionId"),
              parseBoolean(text(entry, "DeleteMarker")),
              text(entry, "DeleteMarkerVersionId")));
    }
    return new DeleteObjectsResult(deleted);
  }

  public static BucketVersioningConfiguration parseBucketVersioning(String xml) {
    if (isBlank(xml)) {
      return BucketVersioningConfiguration.of("", "");
    }
    Document document = parse(xml);
    Element root = document.getDocumentElement();
    return BucketVersioningConfiguration.of(text(root, "Status"), text(root, "MfaDelete"));
  }

  public static String bucketVersioningXml(BucketVersioningConfiguration configuration) {
    BucketVersioningConfiguration safe =
        configuration == null ? BucketVersioningConfiguration.of("", "") : configuration;
    StringBuilder builder = new StringBuilder();
    builder.append("<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">");
    if (!isBlank(safe.status())) {
      builder.append("<Status>").append(escapeXml(safe.status())).append("</Status>");
    }
    if (!isBlank(safe.mfaDelete())) {
      builder.append("<MfaDelete>").append(escapeXml(safe.mfaDelete())).append("</MfaDelete>");
    }
    builder.append("</VersioningConfiguration>");
    return builder.toString();
  }

  public static Map<String, String> parseTagging(String xml) {
    if (isBlank(xml)) {
      return Collections.emptyMap();
    }
    Document document = parse(xml);
    NodeList nodes = document.getDocumentElement().getElementsByTagName("Tag");
    Map<String, String> tags = new LinkedHashMap<String, String>();
    for (int i = 0; i < nodes.getLength(); i++) {
      Element tag = (Element) nodes.item(i);
      tags.put(text(tag, "Key"), text(tag, "Value"));
    }
    return tags;
  }

  public static ObjectRetentionConfiguration parseObjectRetention(String xml) {
    if (isBlank(xml)) {
      return ObjectRetentionConfiguration.of("", "");
    }
    Document document = parse(xml);
    Element root = document.getDocumentElement();
    return ObjectRetentionConfiguration.of(text(root, "Mode"), text(root, "RetainUntilDate"));
  }

  public static String objectRetentionXml(ObjectRetentionConfiguration configuration) {
    if (configuration == null) {
      throw new IllegalArgumentException("对象保留配置不能为空");
    }
    StringBuilder builder = new StringBuilder();
    builder.append("<Retention xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">");
    if (!isBlank(configuration.mode())) {
      builder.append("<Mode>").append(escapeXml(configuration.mode())).append("</Mode>");
    }
    if (!isBlank(configuration.retainUntilDate())) {
      builder
          .append("<RetainUntilDate>")
          .append(escapeXml(configuration.retainUntilDate()))
          .append("</RetainUntilDate>");
    }
    builder.append("</Retention>");
    return builder.toString();
  }

  public static ObjectLegalHoldConfiguration parseObjectLegalHold(String xml) {
    if (isBlank(xml)) {
      return ObjectLegalHoldConfiguration.of("");
    }
    Document document = parse(xml);
    Element root = document.getDocumentElement();
    return ObjectLegalHoldConfiguration.of(text(root, "Status"));
  }

  public static String objectLegalHoldXml(ObjectLegalHoldConfiguration configuration) {
    if (configuration == null) {
      throw new IllegalArgumentException("Legal Hold 配置不能为空");
    }
    StringBuilder builder = new StringBuilder();
    builder.append("<LegalHold xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">");
    if (!isBlank(configuration.status())) {
      builder.append("<Status>").append(escapeXml(configuration.status())).append("</Status>");
    }
    builder.append("</LegalHold>");
    return builder.toString();
  }

  public static String restoreObjectXml(RestoreObjectRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("对象恢复请求不能为空");
    }
    StringBuilder builder = new StringBuilder();
    builder.append("<RestoreRequest xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">");
    builder.append("<Days>").append(request.days()).append("</Days>");
    if (!isBlank(request.tier())) {
      builder
          .append("<GlacierJobParameters><Tier>")
          .append(escapeXml(request.tier()))
          .append("</Tier></GlacierJobParameters>");
    }
    builder.append("</RestoreRequest>");
    return builder.toString();
  }

  public static ObjectAttributes parseObjectAttributes(String xml) {
    if (isBlank(xml)) {
      return new ObjectAttributes("", "", 0L, "", "", "", "", "", 0);
    }
    Document document = parse(xml);
    Element root = document.getDocumentElement();
    Element checksum = firstElement(root, "Checksum");
    Element objectParts = firstElement(root, "ObjectParts");
    return new ObjectAttributes(
        xml,
        text(root, "ETag"),
        parseLong(text(root, "ObjectSize")),
        text(root, "StorageClass"),
        checksum == null ? "" : text(checksum, "ChecksumCRC32"),
        checksum == null ? "" : text(checksum, "ChecksumCRC32C"),
        checksum == null ? "" : text(checksum, "ChecksumSHA1"),
        checksum == null ? "" : text(checksum, "ChecksumSHA256"),
        objectParts == null ? 0 : parseInt(text(objectParts, "TotalPartsCount")));
  }

  private static List<String> commonPrefixes(Element root) {
    List<String> prefixes = new ArrayList<String>();
    NodeList commonPrefixes = root.getElementsByTagName("CommonPrefixes");
    for (int i = 0; i < commonPrefixes.getLength(); i++) {
      prefixes.add(text((Element) commonPrefixes.item(i), "Prefix"));
    }
    return prefixes;
  }


  public static io.minio.reactive.messages.sts.AssumeRoleResult parseAssumeRoleResult(
      String xml) {
    Document document = parse(xml);
    Element root = document.getDocumentElement();
    String accessKey = firstText(root, "AccessKeyId");
    String secretKey = firstText(root, "SecretAccessKey");
    String sessionToken = firstText(root, "SessionToken");
    String expiration = firstText(root, "Expiration");
    return new io.minio.reactive.messages.sts.AssumeRoleResult(
        io.minio.reactive.credentials.ReactiveCredentials.of(accessKey, secretKey, sessionToken),
        expiration,
        xml);
  }

  public static S3Error parseError(String xml) {
    if (isBlank(xml)) {
      return null;
    }
    if (!xml.trim().startsWith("<")) {
      return null;
    }
    try {
      Document document = parse(xml);
      Element root = document.getDocumentElement();
      if (!"Error".equals(root.getNodeName())) {
        return null;
      }
      return new S3Error(
          text(root, "Code"),
          text(root, "Message"),
          text(root, "BucketName"),
          text(root, "Key"),
          text(root, "RequestId"),
          text(root, "HostId"),
          text(root, "Resource"));
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  public static String deleteObjectsXml(Iterable<String> objectNames, boolean quiet) {
    StringBuilder builder = new StringBuilder();
    builder.append("<Delete>");
    builder.append("<Quiet>").append(quiet).append("</Quiet>");
    for (String objectName : objectNames) {
      builder.append("<Object><Key>").append(escapeXml(objectName)).append("</Key></Object>");
    }
    builder.append("</Delete>");
    return builder.toString();
  }

  public static String taggingXml(Map<String, String> tags) {
    StringBuilder builder = new StringBuilder();
    builder.append("<Tagging><TagSet>");
    if (tags != null) {
      for (Map.Entry<String, String> entry : tags.entrySet()) {
        builder
            .append("<Tag><Key>")
            .append(escapeXml(entry.getKey()))
            .append("</Key><Value>")
            .append(escapeXml(entry.getValue()))
            .append("</Value></Tag>");
      }
    }
    builder.append("</TagSet></Tagging>");
    return builder.toString();
  }

  public static String completeMultipartXml(List<CompletePart> parts) {
    StringBuilder builder = new StringBuilder();
    builder.append("<CompleteMultipartUpload>");
    for (CompletePart part : parts) {
      builder
          .append("<Part><PartNumber>")
          .append(part.partNumber())
          .append("</PartNumber><ETag>")
          .append(escapeXml(part.etag()))
          .append("</ETag></Part>");
    }
    builder.append("</CompleteMultipartUpload>");
    return builder.toString();
  }

  public static String escapeXml(String value) {
    if (value == null) {
      return "";
    }
    StringBuilder builder = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);
      switch (ch) {
        case '&':
          builder.append("&amp;");
          break;
        case '<':
          builder.append("&lt;");
          break;
        case '>':
          builder.append("&gt;");
          break;
        case '"':
          builder.append("&quot;");
          break;
        case '\'':
          builder.append("&apos;");
          break;
        default:
          builder.append(ch);
      }
    }
    return builder.toString();
  }

  private static Document parse(String xml) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(false);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setXIncludeAware(false);
      factory.setExpandEntityReferences(false);
      return factory
          .newDocumentBuilder()
          .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to parse S3 XML", e);
    }
  }

  private static String firstText(Element element, String tagName) {
    NodeList nodes = element.getElementsByTagName(tagName);
    if (nodes.getLength() == 0) {
      return "";
    }
    Node node = nodes.item(0);
    String value = node == null ? null : node.getTextContent();
    return value == null ? "" : value.trim();
  }

  private static String text(Element element, String tagName) {
    NodeList nodes = element.getElementsByTagName(tagName);
    if (nodes.getLength() == 0) {
      return "";
    }
    Node node = nodes.item(0);
    String value = node == null ? null : node.getTextContent();
    return value == null ? "" : value.trim();
  }

  private static Element firstElement(Element element, String tagName) {
    NodeList nodes = element.getElementsByTagName(tagName);
    if (nodes.getLength() == 0) {
      return null;
    }
    Node node = nodes.item(0);
    return node instanceof Element ? (Element) node : null;
  }

  private static boolean parseBoolean(String value) {
    return "true".equalsIgnoreCase(value);
  }

  private static int parseInt(String value) {
    if (isBlank(value)) {
      return 0;
    }
    return Integer.parseInt(value.trim());
  }

  private static long parseLong(String value) {
    if (isBlank(value)) {
      return 0L;
    }
    return Long.parseLong(value.trim());
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
