package io.minio.reactive.messages;

import io.minio.reactive.util.S3Xml;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class S3XmlTest {
  @Test
  void shouldParseBuckets() {
    List<BucketInfo> buckets =
        S3Xml.parseBuckets(
            "<ListAllMyBucketsResult><Buckets><Bucket><Name>a</Name><CreationDate>now</CreationDate></Bucket></Buckets></ListAllMyBucketsResult>");

    Assertions.assertEquals(1, buckets.size());
    Assertions.assertEquals("a", buckets.get(0).name());
  }

  @Test
  void shouldParseListObjectsV2() {
    ListObjectsResult result =
        S3Xml.parseListObjectsV2(
            "<ListBucketResult>"
                + "<Name>b</Name><Prefix>p/</Prefix><IsTruncated>true</IsTruncated><NextContinuationToken>n</NextContinuationToken>"
                + "<Contents><Key>p/a.txt</Key><LastModified>today</LastModified><ETag>\"e\"</ETag><Size>3</Size><StorageClass>STANDARD</StorageClass></Contents>"
                + "<CommonPrefixes><Prefix>p/sub/</Prefix></CommonPrefixes>"
                + "</ListBucketResult>");

    Assertions.assertTrue(result.isTruncated());
    Assertions.assertEquals("n", result.nextContinuationToken());
    Assertions.assertEquals(1, result.contents().size());
    Assertions.assertEquals("p/a.txt", result.contents().get(0).key());
    Assertions.assertEquals(3L, result.contents().get(0).size());
    Assertions.assertEquals(Arrays.asList("p/sub/"), result.commonPrefixes());
  }

  @Test
  void shouldParseListObjectVersions() {
    ListObjectVersionsResult result =
        S3Xml.parseListObjectVersions(
            "<ListVersionsResult>"
                + "<Name>b</Name><Prefix>p/</Prefix><KeyMarker>a</KeyMarker><VersionIdMarker>v0</VersionIdMarker>"
                + "<NextKeyMarker>b</NextKeyMarker><NextVersionIdMarker>v1</NextVersionIdMarker><MaxKeys>100</MaxKeys><IsTruncated>true</IsTruncated>"
                + "<Version><Key>p/a.txt</Key><VersionId>111</VersionId><IsLatest>true</IsLatest><LastModified>today</LastModified><ETag>\"e\"</ETag><Size>3</Size><StorageClass>STANDARD</StorageClass><Owner><ID>owner</ID><DisplayName>root</DisplayName></Owner></Version>"
                + "<DeleteMarker><Key>p/old.txt</Key><VersionId>222</VersionId><IsLatest>false</IsLatest><LastModified>yesterday</LastModified><Owner><ID>owner</ID></Owner></DeleteMarker>"
                + "<CommonPrefixes><Prefix>p/sub/</Prefix></CommonPrefixes>"
                + "</ListVersionsResult>");

    Assertions.assertTrue(result.isTruncated());
    Assertions.assertEquals("b", result.nextKeyMarker());
    Assertions.assertEquals("v1", result.nextVersionIdMarker());
    Assertions.assertEquals(1, result.versions().size());
    Assertions.assertEquals("111", result.versions().get(0).versionId());
    Assertions.assertEquals(1, result.deleteMarkers().size());
    Assertions.assertEquals("p/old.txt", result.deleteMarkers().get(0).key());
    Assertions.assertEquals(Arrays.asList("p/sub/"), result.commonPrefixes());
  }

  @Test
  void shouldParseListMultipartUploads() {
    ListMultipartUploadsResult result =
        S3Xml.parseListMultipartUploads(
            "<ListMultipartUploadsResult>"
                + "<Bucket>b</Bucket><Prefix>p/</Prefix><KeyMarker>a</KeyMarker><UploadIdMarker>u0</UploadIdMarker>"
                + "<NextKeyMarker>b</NextKeyMarker><NextUploadIdMarker>u1</NextUploadIdMarker><MaxUploads>100</MaxUploads><IsTruncated>true</IsTruncated>"
                + "<Upload><Key>p/big.bin</Key><UploadId>upload-1</UploadId><Initiated>today</Initiated><StorageClass>STANDARD</StorageClass><Owner><ID>owner</ID><DisplayName>root</DisplayName></Owner></Upload>"
                + "<CommonPrefixes><Prefix>p/sub/</Prefix></CommonPrefixes>"
                + "</ListMultipartUploadsResult>");

    Assertions.assertTrue(result.isTruncated());
    Assertions.assertEquals("u1", result.nextUploadIdMarker());
    Assertions.assertEquals(1, result.uploads().size());
    Assertions.assertEquals("upload-1", result.uploads().get(0).uploadId());
    Assertions.assertEquals("p/big.bin", result.uploads().get(0).key());
    Assertions.assertEquals(Arrays.asList("p/sub/"), result.commonPrefixes());
  }

  @Test
  void shouldBuildAndParseTaggingXml() {
    Map<String, String> source = new LinkedHashMap<String, String>();
    source.put("k", "v&1");

    Map<String, String> parsed = S3Xml.parseTagging(S3Xml.taggingXml(source));

    Assertions.assertEquals("v&1", parsed.get("k"));
  }

  @Test
  void shouldBuildAndParseObjectGovernanceXml() {
    ObjectRetentionConfiguration retention =
        S3Xml.parseObjectRetention(
            S3Xml.objectRetentionXml(
                ObjectRetentionConfiguration.governance("2030-01-01T00:00:00Z")));
    Assertions.assertEquals(ObjectRetentionConfiguration.GOVERNANCE, retention.mode());
    Assertions.assertEquals("2030-01-01T00:00:00Z", retention.retainUntilDate());

    ObjectLegalHoldConfiguration legalHold =
        S3Xml.parseObjectLegalHold(S3Xml.objectLegalHoldXml(ObjectLegalHoldConfiguration.enabled()));
    Assertions.assertTrue(legalHold.enabledValue());

    String restoreXml = S3Xml.restoreObjectXml(RestoreObjectRequest.of(7, "Standard"));
    Assertions.assertTrue(restoreXml.contains("<Days>7</Days>"));
    Assertions.assertTrue(restoreXml.contains("<Tier>Standard</Tier>"));
  }

  @Test
  void shouldParseObjectAttributes() {
    ObjectAttributes attributes =
        S3Xml.parseObjectAttributes(
            "<GetObjectAttributesOutput>"
                + "<ETag>\"etag\"</ETag><ObjectSize>42</ObjectSize><StorageClass>STANDARD</StorageClass>"
                + "<Checksum><ChecksumCRC32>crc32</ChecksumCRC32><ChecksumSHA256>sha256</ChecksumSHA256></Checksum>"
                + "<ObjectParts><TotalPartsCount>3</TotalPartsCount></ObjectParts>"
                + "</GetObjectAttributesOutput>");

    Assertions.assertEquals("\"etag\"", attributes.etag());
    Assertions.assertEquals(42L, attributes.objectSize());
    Assertions.assertEquals("STANDARD", attributes.storageClass());
    Assertions.assertEquals("crc32", attributes.checksumCrc32());
    Assertions.assertEquals("sha256", attributes.checksumSha256());
    Assertions.assertEquals(3, attributes.totalPartsCount());
    Assertions.assertTrue(attributes.rawXml().contains("GetObjectAttributesOutput"));
  }

  @Test
  void shouldBuildCompleteMultipartXml() {
    String xml = S3Xml.completeMultipartXml(Arrays.asList(new CompletePart(1, "\"etag\"")));

    Assertions.assertTrue(xml.contains("<PartNumber>1</PartNumber>"));
    Assertions.assertTrue(xml.contains("&quot;etag&quot;"));
  }

  @Test
  void shouldParseS3Error() {
    S3Error error =
        S3Xml.parseError(
            "<Error><Code>NoSuchBucket</Code><Message>missing</Message><BucketName>b</BucketName><RequestId>r</RequestId></Error>");

    Assertions.assertEquals("NoSuchBucket", error.code());
    Assertions.assertEquals("missing", error.message());
    Assertions.assertEquals("b", error.bucketName());
  }
}
