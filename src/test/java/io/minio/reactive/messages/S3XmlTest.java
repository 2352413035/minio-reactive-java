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
  void shouldBuildAndParseTaggingXml() {
    Map<String, String> source = new LinkedHashMap<String, String>();
    source.put("k", "v&1");

    Map<String, String> parsed = S3Xml.parseTagging(S3Xml.taggingXml(source));

    Assertions.assertEquals("v&1", parsed.get("k"));
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
